/**
 * Copyright (c) 2015 CommonsWare, LLC
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.commonsware.cwac.cam2;

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
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
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
  private CountDownLatch closeLatch=null;

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

  /**
   * {@inheritDoc}
   */
  @Override
  public CameraSession.Builder buildSession(CameraDescriptor descriptor) {
    return (new SessionBuilder(descriptor));
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
              Descriptor camera=new Descriptor(cameraId);

              result.add(camera);

              CameraCharacteristics c=mgr.getCameraCharacteristics(cameraId);
              StreamConfigurationMap map=c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
              android.util.Size[] rawSizes=map.getOutputSizes(SurfaceTexture.class);
              ArrayList<Size> sizes=new ArrayList<Size>();

              for (android.util.Size size : rawSizes) {
                sizes.add(new Size(size.getWidth(), size.getHeight()));
              }

              camera.setPreviewSizes(sizes);
              camera.setPictureSizes(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)));
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
  public void open(final CameraSession session,
                   final SurfaceTexture texture) {
    new Thread() {
      public void run() {
        Descriptor camera=(Descriptor)session.getDescriptor();

        try {
          if (!lock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Time out waiting to lock camera opening.");
          }
android.util.Log.d(getClass().getSimpleName(), "calling openCamera()");

          mgr.openCamera(camera.getId(),
              new InitPreviewTransaction(session, new Surface(texture)),
              handler);
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
  public void close(CameraSession session) {
    final Session s=(Session)session;

    try {
      lock.acquire();

      if (s.captureSession != null) {
        closeLatch=new CountDownLatch(1);
        s.captureSession.close();
        closeLatch.await(2, TimeUnit.SECONDS);
        s.captureSession=null;
      }

      if (s.cameraDevice != null) {
        s.cameraDevice.close();
        s.cameraDevice=null;
      }

      if (s.reader != null) {
        s.reader.close();
      }

      Descriptor camera=(Descriptor)session.getDescriptor();

      camera.setDevice(null);
    }
    catch (Exception e) {
      throw new IllegalStateException("Exception closing camera", e);
    } finally {
      lock.release();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void takePicture(CameraSession session,
                          PictureTransaction xact) {
    final Session s=(Session)session;

    new Thread() {
      public void run() {
        try {
          // This is how to tell the camera to lock focus.
          s.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
              CameraMetadata.CONTROL_AF_TRIGGER_START);
          s.captureSession.setRepeatingRequest(s.previewRequestBuilder.build(),
              new RequestCaptureTransaction(s),
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

    if (criteria != null) {
      CameraCharacteristics info=mgr.getCameraCharacteristics(cameraId);
      Integer facing=info.get(CameraCharacteristics.LENS_FACING);

      if ((criteria.getFacing() == CameraSelectionCriteria.Facing.FRONT &&
          facing != CameraCharacteristics.LENS_FACING_FRONT) ||
          (criteria.getFacing() == CameraSelectionCriteria.Facing.BACK &&
              facing != CameraCharacteristics.LENS_FACING_BACK)) {
        result=false;
      }
    }

    return (result);
  }

  private class InitPreviewTransaction extends CameraDevice.StateCallback {
    private final Session s;
    private final Surface surface;

    InitPreviewTransaction(CameraSession session, Surface surface) {
      this.s=(Session)session;
      this.surface=surface;
    }

    @Override
    public void onOpened(CameraDevice cameraDevice) {
android.util.Log.d(getClass().getSimpleName(), "onOpened()");
      lock.release();
      s.cameraDevice=cameraDevice;
      s.reader=ImageReader.newInstance(s.getPictureSize().getWidth(),
          s.getPictureSize().getHeight(), ImageFormat.JPEG, 2);
      s.reader.setOnImageAvailableListener(CameraTwoEngine.this,
          handler);

      Descriptor camera=(Descriptor)s.getDescriptor();

      camera.setDevice(cameraDevice);

      try {
        cameraDevice.createCaptureSession(
            Arrays.asList(surface, s.reader.getSurface()),
            new StartPreviewTransaction(s, surface), handler);
      }
      catch (CameraAccessException e) {
        getBus().post(new OpenedEvent(e));
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

      if (closeLatch != null) {
        closeLatch.countDown();
      }
    }
  }

  private class StartPreviewTransaction extends CameraCaptureSession.StateCallback {
    private final Surface surface;
    private final Session s;

    StartPreviewTransaction(CameraSession session, Surface surface) {
      this.s=(Session)session;
      this.surface=surface;
    }

    @Override
    public void onConfigured(CameraCaptureSession session) {
android.util.Log.d(getClass().getSimpleName(), "onConfigured()");
      try {
        s.captureSession=session;

        s.previewRequestBuilder=session.getDevice().createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        s.previewRequestBuilder.addTarget(surface);
        s.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        s.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        // TODO: offer other flash support

        s.previewRequest=s.previewRequestBuilder.build();

        session.setRepeatingRequest(s.previewRequest, null, handler);

        getBus().post(new OpenedEvent());
      }
      catch (CameraAccessException e) {
        getBus().post(new OpenedEvent(e));
      }
    }

    @Override
    public void onConfigureFailed(CameraCaptureSession session) {
      // TODO: raise event
    }
  }

  private class RequestCaptureTransaction extends CameraCaptureSession.CaptureCallback {
    private final Session s;
    boolean isWaitingForFocus=true;
    boolean isWaitingForPrecapture=false;
    boolean haveWeStartedCapture=false;

    RequestCaptureTransaction(CameraSession session) {
      this.s=(Session)session;
    }

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

          if (state == null ||
              state == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
            isWaitingForPrecapture=false;
            haveWeStartedCapture=true;
            capture(s);
          } else {
            isWaitingForPrecapture=true;
            precapture(s);
          }
        }
      } else if (isWaitingForPrecapture) {
        Integer state=result.get(CaptureResult.CONTROL_AE_STATE);

        if (state == null ||
            state == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
            state == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
          isWaitingForPrecapture=false;
        }
      } else if (!haveWeStartedCapture) {
        Integer state=result.get(CaptureResult.CONTROL_AE_STATE);

        if (state == null ||
            state != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
          haveWeStartedCapture=true;
          capture(s);
        }
      }
    }

    private void precapture(Session s) {
      try {
        s.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
            CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        s.captureSession.capture(s.previewRequestBuilder.build(), this,
            handler);
      }
      catch (Exception e) {
        getBus().post(new PictureTakenEvent(e));

        if (isDebug()) {
          Log.e(getClass().getSimpleName(), "Exception running precapture", e);
        }
      }
    }

    private void capture(Session s) {
      try {
        CaptureRequest.Builder captureBuilder=
            s.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

        captureBuilder.addTarget(s.reader.getSurface());
        captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

        // Orientation
/*
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
*/

        s.captureSession.stopRepeating();
        s.captureSession.capture(captureBuilder.build(),
            new CapturePictureTransaction(s), null);
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
    private final Session s;

    CapturePictureTransaction(CameraSession session) {
      this.s=(Session)session;
    }

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
        s.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
            CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
        s.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        s.captureSession.capture(s.previewRequestBuilder.build(), null,
            handler);
        s.captureSession.setRepeatingRequest(s.previewRequest, null, handler);
      }
      catch (CameraAccessException e) {
        getBus().post(new PictureTakenEvent(e));

        if (isDebug()) {
          Log.e(getClass().getSimpleName(), "Exception resetting focus", e);
        }
      }
    }
  }

  private static class AreaComparator implements Comparator<Size> {
    @Override
    public int compare(Size lhs, Size rhs) {
      long lhArea=(long)lhs.getWidth() * lhs.getHeight();
      long rhArea=(long)rhs.getWidth() * rhs.getHeight();

      return (Long.signum(lhArea - rhArea));
    }
  }

  static class Descriptor implements CameraDescriptor {
    private String cameraId;
    private CameraDevice device;
    private ArrayList<Size> pictureSizes;
    private ArrayList<Size> previewSizes;

    private Descriptor(String cameraId) {
      this.cameraId=cameraId;
    }

    public String getId() {
      return (cameraId);
    }

    private void setDevice(CameraDevice device) {
      this.device=device;
    }

    private CameraDevice getDevice() {
      return (device);
    }

    @Override
    public boolean isPictureFormatSupported(int format) {
      return (ImageFormat.JPEG == format);
    }

    @Override
    public ArrayList<Size> getPreviewSizes() {
      return (previewSizes);
    }

    private void setPreviewSizes(ArrayList<Size> sizes) {
      previewSizes=sizes;
    }

    @Override
    public ArrayList<Size> getPictureSizes() {
      return (pictureSizes);
    }

    private void setPictureSizes(List<android.util.Size> sizes) {
      pictureSizes=new ArrayList<Size>(sizes.size());

      for (android.util.Size size : sizes) {
        pictureSizes.add(new Size(size.getWidth(), size.getHeight()));
      }
    }
  }

  private static class Session extends CameraSession {
    CameraDevice cameraDevice=null;
    CameraCaptureSession captureSession=null;
    CaptureRequest.Builder previewRequestBuilder=null;
    CaptureRequest previewRequest;
    ImageReader reader;

    private Session(CameraDescriptor descriptor) {
      super(descriptor);
    }
  }

  private static class SessionBuilder extends CameraSession.Builder {
    private SessionBuilder(CameraDescriptor descriptor) {
      super(new Session(descriptor));
    }
  }
}
