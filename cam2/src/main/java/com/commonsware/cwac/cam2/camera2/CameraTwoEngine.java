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
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.*;
import android.util.Log;
import android.view.Surface;
import com.commonsware.cwac.cam2.CameraDescriptor;
import com.commonsware.cwac.cam2.CameraEngine;
import com.commonsware.cwac.cam2.CameraSelectionCriteria;
import com.commonsware.cwac.cam2.util.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of a CameraEngine that supports the
 * Android 5.0+ android.hardware.camera2 API.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraTwoEngine extends CameraEngine {
  private CameraManager mgr;
  final private HandlerThread handlerThread=new HandlerThread(getClass().getSimpleName(),
      android.os.Process.THREAD_PRIORITY_BACKGROUND);
  final private Handler handler;
  final private Semaphore lock=new Semaphore(1);
  private CameraDevice cameraDevice=null;
  private CameraCaptureSession session=null;
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

  public void destroy() {
    handlerThread.quitSafely();
  }

  /**
   * {@inheritDoc}
   */
  public List<CameraDescriptor> getCameraDescriptors(CameraSelectionCriteria criteria) {
    List<CameraDescriptor> result=new ArrayList<CameraDescriptor>();

    try {
      for (String cameraId : mgr.getCameraIdList()) {
        if (isMatch(cameraId, criteria)) {
          result.add(new Descriptor(cameraId));
        }
      }
    }
    catch (CameraAccessException e) {
      throw new IllegalStateException("Exception accessing camera", e);
    }

    return(result);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void open(CameraDescriptor rawCamera,
                   SurfaceTexture texture,
                   Size previewSize) {
    Descriptor camera=(Descriptor)rawCamera;

    try {
      if (!lock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
        throw new RuntimeException("Time out waiting to lock camera opening.");
      }

      mgr.openCamera(camera.getId(),
          new InitPreviewTransaction(camera, new Surface(texture)),
          handler);
    }
    catch (CameraAccessException e) {
      throw new IllegalStateException("Exception opening camera", e);
    }
    catch (InterruptedException e) {
      throw new IllegalStateException("Interrupted during camera opening", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close(CameraDescriptor rawCamera) {
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

      Descriptor camera=(Descriptor)rawCamera;

      camera.setDevice(null);

      // TODO: image reader close
    }
    catch (InterruptedException e) {
      throw new IllegalStateException("Interrupted during camera closing", e);
    }
    finally {
      lock.release();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Size> getAvailablePreviewSizes(CameraDescriptor rawCamera) {
    ArrayList<Size> result=new ArrayList<Size>();
    Descriptor camera=(Descriptor)rawCamera;

    try {
      CameraCharacteristics c=mgr.getCameraCharacteristics(camera.getId());
      StreamConfigurationMap map=c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
      android.util.Size[] rawSizes=map.getOutputSizes(SurfaceTexture.class);

      for (android.util.Size size : rawSizes) {
        result.add(new Size(size.getWidth(), size.getHeight()));
      }
    }
    catch (CameraAccessException e) {
      throw new IllegalStateException("Exception getting preview sizes", e);
    }

    return(result);
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
        cameraDevice.createCaptureSession(Collections.singletonList(surface),
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
        CaptureRequest.Builder b=session.getDevice().createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

        b.addTarget(surface);
        b.set(CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        b.set(CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        // TODO: offer other flash support

        session.setRepeatingRequest(b.build(), null, handler);
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
}
