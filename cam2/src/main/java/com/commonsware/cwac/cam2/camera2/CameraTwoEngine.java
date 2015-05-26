/***
 Copyright (c) 2015 CommonsWare, LLC

 Licensed under the Apache License, Version 2.0 (the "License"); you may
 not use this file except in compliance with the License. You may obtain
 a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.commonsware.cwac.cam2.camera2;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.*;
import android.util.Log;
import android.view.Surface;
import com.commonsware.cwac.cam2.CameraDescriptor;
import com.commonsware.cwac.cam2.CameraEngine;
import com.commonsware.cwac.cam2.CameraSelectionCriteria;
import com.commonsware.cwac.cam2.PictureTransaction;
import com.commonsware.cwac.cam2.util.Size;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of a CameraEngine that supports the
 * Android 5.0+ android.hardware.camera2 API.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraTwoEngine extends CameraEngine
    implements ImageReader.OnImageAvailableListener {
  private CameraManager mgr;
  final private HandlerThread handlerThread=new HandlerThread(getClass().getSimpleName(),
      android.os.Process.THREAD_PRIORITY_BACKGROUND);
  final private Handler handler;
  final private Semaphore lock=new Semaphore(1);
  private CameraDevice cameraDevice=null;
  private CameraCaptureSession session=null;
  private CaptureRequest.Builder previewRequestBuilder=null;
  private CountDownLatch closeLatch=null;
  private CaptureRequest previewRequest;
  private ImageReader reader;

  /**
   * Standard constructor
   *
   * @param ctxt any Context will do
   */
  public CameraTwoEngine(Context ctxt) {
    mgr=(CameraManager)ctxt.
                        getApplicationContext().
                        getSystemService(Context.CAMERA_SERVICE);
    handlerThread.start();
    handler=new Handler(handlerThread.getLooper());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void destroy() {
    handlerThread.quitSafely();
    getBus().post(new DestroyedEvent());
  }

  @Override
  public void onImageAvailable(ImageReader imageReader) {
    // TODO: something with the image
    getBus().post(new PictureTakenEvent());

    Log.e(getClass().getSimpleName(), "onImageAvailable");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void loadCameraDescriptors(final CameraSelectionCriteria criteria) {
    new Thread() {
      public void run() {
        List<CameraDescriptor> result=new ArrayList<CameraDescriptor>();

        try {
          for (String cameraId : mgr.getCameraIdList()) {
            if (isMatch(cameraId, criteria)) {
              result.add(new Descriptor(cameraId));
            }
          }

          getBus().post(new CameraEngine.CameraDescriptorsEvent(result));
        }
        catch (CameraAccessException e) {
          getBus().post(new CameraEngine.CameraDescriptorsEvent(e));

          if (isDebug()) {
            Log.e(getClass().getSimpleName(), "Exception accessing camera", e);
          }
        }
      }
    }.start();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void open(final CameraDescriptor rawCamera,
                   final SurfaceTexture texture,
                   final Size previewSize) {
    new Thread() {
      public void run() {
        Descriptor camera=(Descriptor)rawCamera;

        try {
          if (!lock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Time out waiting to lock camera opening.");
          }

          mgr.openCamera(camera.getId(),
              new InitPreviewTransaction(camera, new Surface(texture)),
              handler);

          getBus().post(new OpenedEvent());
        }
        catch (Exception e) {
          getBus().post(new OpenedEvent(e));

          if (isDebug()) {
            Log.e(getClass().getSimpleName(), "Exception opening camera", e);
          }
        }
      }
    }.start();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close(final CameraDescriptor rawCamera) {
    try {
      lock.acquire();

      if (session!=null) {
        closeLatch=new CountDownLatch(1);
        session.close();
        closeLatch.await(2, TimeUnit.SECONDS);
        session=null;
      }

      if (cameraDevice!=null) {
        cameraDevice.close();
        cameraDevice=null;
      }

      if (reader!=null) {
        reader.close();
      }

      Descriptor camera=(Descriptor)rawCamera;

      camera.setDevice(null);
    }
    catch (Exception e) {
      throw new IllegalStateException("Exception closing camera", e);
    }
    finally {
      lock.release();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void loadAvailablePreviewSizes(final CameraDescriptor rawCamera) {
    new Thread() {
      public void run() {
        ArrayList<Size> result=new ArrayList<Size>();
        Descriptor camera=(Descriptor)rawCamera;

        try {
          CameraCharacteristics c=mgr.getCameraCharacteristics(camera.getId());
          StreamConfigurationMap map=c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
          android.util.Size[] rawSizes=map.getOutputSizes(SurfaceTexture.class);

          for (android.util.Size size : rawSizes) {
            result.add(new Size(size.getWidth(), size.getHeight()));
          }

          // TODO: figure out better spot for following ImageReader stuff

          android.util.Size largest = Collections.max(
              Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
              new AreaComparator());
          reader=ImageReader.newInstance(largest.getWidth(),
              largest.getHeight(), ImageFormat.JPEG, 2);
          reader.setOnImageAvailableListener(CameraTwoEngine.this,
              handler);

          getBus().post(new PreviewSizesEvent(result));
        }
        catch (Exception e) {
          getBus().post(new PreviewSizesEvent(e));

          if (isDebug()) {
            Log.e(getClass().getSimpleName(), "Exception loading preview sizes", e);
          }
        }
      }
    }.start();
  }

  /**
   * {@inheritDoc}
   */
  public void takePicture(CameraDescriptor rawCamera,
                          PictureTransaction xact) {
    new Thread() {
      public void run() {
        try {
          // This is how to tell the camera to lock focus.
          previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
              CameraMetadata.CONTROL_AF_TRIGGER_START);
          session.setRepeatingRequest(previewRequestBuilder.build(),
              new RequestCaptureTransaction(),
              handler);
        }
        catch (Exception e) {
          getBus().post(new PictureTakenEvent(e));

          if (isDebug()) {
            Log.e(getClass().getSimpleName(), "Exception taking picture", e);
          }
        }
      }
    }.start();
  }

  private boolean isMatch(String cameraId, CameraSelectionCriteria criteria)
      throws CameraAccessException {
    boolean result=true;

    if (criteria!=null) {
      CameraCharacteristics info=mgr.getCameraCharacteristics(cameraId);
      Integer facing=info.get(CameraCharacteristics.LENS_FACING);

      if ((criteria.getFacing()==CameraSelectionCriteria.Facing.FRONT &&
          facing!=CameraCharacteristics.LENS_FACING_FRONT) ||
          (criteria.getFacing()==CameraSelectionCriteria.Facing.BACK &&
              facing!=CameraCharacteristics.LENS_FACING_BACK)) {
        result=false;
      }
    }

    return(result);
  }

  private class InitPreviewTransaction extends CameraDevice.StateCallback {
    private final Descriptor camera;
    private final Surface surface;

    InitPreviewTransaction(Descriptor camera, Surface surface) {
      this.camera=camera;
      this.surface=surface;
    }

    @Override
    public void onOpened(CameraDevice cameraDevice) {
      lock.release();
      CameraTwoEngine.this.cameraDevice=cameraDevice;
      camera.setDevice(cameraDevice);

      try {
        cameraDevice.createCaptureSession(
            Arrays.asList(surface, reader.getSurface()),
            new StartPreviewTransaction(surface), handler);
      }
      catch (CameraAccessException e) {
        Log.e(getClass().getSimpleName(), "Exception creating capture session", e);
        // TODO: raise event, replacing this
      }
    }

    @Override
    public void onDisconnected(CameraDevice cameraDevice) {
      lock.release();
      cameraDevice.close();
    }

    @Override
    public void onError(CameraDevice cameraDevice, int i) {
      lock.release();
      cameraDevice.close();
      // TODO: raise event
    }

    @Override
    public void onClosed(CameraDevice camera) {
      super.onClosed(camera);

      if (closeLatch!=null) {
        closeLatch.countDown();
      }
    }
  }

  private class StartPreviewTransaction extends CameraCaptureSession.StateCallback {
    private final Surface surface;

    StartPreviewTransaction(Surface surface) {
      this.surface=surface;
    }

    @Override
    public void onConfigured(CameraCaptureSession session) {
      try {
        CameraTwoEngine.this.session=session;

        previewRequestBuilder=session.getDevice().createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        previewRequestBuilder.addTarget(surface);
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        // TODO: offer other flash support

        previewRequest=previewRequestBuilder.build();

        session.setRepeatingRequest(previewRequest, null, handler);
      }
      catch (CameraAccessException e) {
        Log.e(getClass().getSimpleName(), "Exception creating capture request", e);
        // TODO: raise event, replacing this
      }
    }

    @Override
    public void onConfigureFailed(CameraCaptureSession session) {
      // TODO: raise event
    }
  }

  private class RequestCaptureTransaction extends CameraCaptureSession.CaptureCallback {
    boolean isWaitingForFocus=true;
    boolean isWaitingForPrecapture=false;
    boolean haveWeStartedCapture=false;

    @Override
    public void onCaptureProgressed(CameraCaptureSession session,
                                    CaptureRequest request, CaptureResult partialResult) {
      capture(partialResult);
    }

    @Override
    public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
      // TODO: raise event
    }

    @Override
    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
      capture(result);
    }

    private void capture(CaptureResult result) {
      if (isWaitingForFocus) {
        isWaitingForFocus=false;

        int autoFocusState=result.get(CaptureResult.CONTROL_AF_STATE);

        if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == autoFocusState ||
            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == autoFocusState) {
          Integer state=result.get(CaptureResult.CONTROL_AE_STATE);

          if (state==null ||
              state==CaptureResult.CONTROL_AE_STATE_CONVERGED) {
            isWaitingForPrecapture=false;
            haveWeStartedCapture=true;
            capture();
          }
          else {
            isWaitingForPrecapture=true;
            precapture();
          }
        }
      }
      else if (isWaitingForPrecapture) {
        Integer state=result.get(CaptureResult.CONTROL_AE_STATE);

        if (state==null ||
            state==CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
            state==CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
          isWaitingForPrecapture=false;
        }
      }
      else if (!haveWeStartedCapture) {
        Integer state=result.get(CaptureResult.CONTROL_AE_STATE);

        if (state==null ||
            state!=CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
          haveWeStartedCapture=true;
          capture();
        }
      }
    }

    private void precapture() {
      try {
        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
            CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        session.capture(previewRequestBuilder.build(), this,
            handler);
      }
      catch (Exception e) {
        getBus().post(new PictureTakenEvent(e));

        if (isDebug()) {
          Log.e(getClass().getSimpleName(), "Exception running precapture", e);
        }
      }
    }

    private void capture() {
      try {
        CaptureRequest.Builder captureBuilder=
            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

        captureBuilder.addTarget(reader.getSurface());
        captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

        // Orientation
/*
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
*/

        session.stopRepeating();
        session.capture(captureBuilder.build(),
            new CapturePictureTransaction(), null);
      }
      catch (Exception e) {
        getBus().post(new PictureTakenEvent(e));

        if (isDebug()) {
          Log.e(getClass().getSimpleName(), "Exception running capture", e);
        }
      }
    }
  }

  private class CapturePictureTransaction extends CameraCaptureSession.CaptureCallback {
    @Override
    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
      // TODO: something useful with the picture
      unlockFocus();
    }

    @Override
    public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
      // TODO: raise event
    }

    private void unlockFocus() {
      try {
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
            CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        session.capture(previewRequestBuilder.build(), null,
            handler);
        session.setRepeatingRequest(previewRequest, null, handler);
      }
      catch (CameraAccessException e) {
        getBus().post(new PictureTakenEvent(e));

        if (isDebug()) {
          Log.e(getClass().getSimpleName(), "Exception resetting focus", e);
        }
      }
    }
  }

  private static class AreaComparator implements Comparator<android.util.Size> {
    @Override
    public int compare(android.util.Size lhs, android.util.Size rhs) {
      long lhArea=(long)lhs.getWidth()*lhs.getHeight();
      long rhArea=(long)rhs.getWidth() * rhs.getHeight();

      return(Long.signum(lhArea-rhArea));
    }
  }
}
