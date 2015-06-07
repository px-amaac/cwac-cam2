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

package com.commonsware.cwac.cam2;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import com.commonsware.cwac.cam2.util.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of a CameraEngine that supports the
 * original android.hardware.Camera API.
 */
@SuppressWarnings("deprecation")
public class ClassicCameraEngine extends CameraEngine {
  /**
   * {@inheritDoc}
   */
  @Override
  public CameraSession.Builder buildSession(CameraDescriptor descriptor) {
    return(new SessionBuilder(descriptor));
  }

  /**
   * {@inheritDoc}
   */
  public void loadCameraDescriptors(final CameraSelectionCriteria criteria) {
    new Thread() {
      public void run() {
        int count=Camera.getNumberOfCameras();
        List<CameraDescriptor> result=new ArrayList<CameraDescriptor>();

        for (int cameraId=0; cameraId<count; cameraId++) {
          if (isMatch(cameraId, criteria)) {
            Descriptor descriptor=new Descriptor(cameraId);

            result.add(descriptor);

            Camera camera=Camera.open(descriptor.getCameraId());
            Camera.Parameters params=camera.getParameters();
            ArrayList<Size> sizes=new ArrayList<Size>();

            for (Camera.Size size : params.getSupportedPreviewSizes()) {
              sizes.add(new Size(size.width, size.height));
            }

            descriptor.setPreviewSizes(sizes);

            sizes=new ArrayList<Size>();

            for (Camera.Size size : params.getSupportedPictureSizes()) {
              sizes.add(new Size(size.width, size.height));
            }

            descriptor.setPictureSizes(sizes);
            camera.release();
          }
        }

        getBus().post(new CameraEngine.CameraDescriptorsEvent(result));
      }
    }.start();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close(final CameraSession session) {
    Descriptor descriptor=(Descriptor)session.getDescriptor();
    Camera camera=descriptor.getCamera();

    if (camera!=null) {
      camera.stopPreview();
      camera.release();
      descriptor.setCamera(null);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void takePicture(final CameraSession session, PictureTransaction xact) {
    new Thread() {
      public void run() {
        Descriptor descriptor=(Descriptor)session.getDescriptor();
        Camera camera=descriptor.getCamera();

        try {
          camera.takePicture(null, null, new TakePictureTransaction());
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

  /**
   * {@inheritDoc}
   */
  @Override
  public void destroy() {
    getBus().post(new DestroyedEvent());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void open(final CameraSession session,
                   final SurfaceTexture texture) {
    new Thread() {
      public void run() {
        Descriptor descriptor=(Descriptor)session.getDescriptor();
        Camera camera=descriptor.getCamera();

        if (camera==null) {
          camera=Camera.open(descriptor.getCameraId());
          descriptor.setCamera(camera);
        }

        try {
          camera.setPreviewTexture(texture);

          Camera.Parameters params=camera.getParameters();

          params.setPreviewSize(session.getPreviewSize().getWidth(),
              session.getPreviewSize().getHeight());

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            //      parameters.setRecordingHint(getHost().getRecordingHint() != CameraHost.RecordingHint.STILL_ONLY);
          }

          // TODO: get all other parameters changes here

          params.setPictureSize(session.getPictureSize().getWidth(),
              session.getPictureSize().getHeight());
          params.setPictureFormat(session.getPictureFormat());

          camera.setParameters(params);
          camera.startPreview();
          getBus().post(new OpenedEvent());
        }
        catch (Exception e) {
          camera.release();
          descriptor.setCamera(null);
          getBus().post(new OpenedEvent(e));

          if (isDebug()) {
            Log.e(getClass().getSimpleName(), "Exception opening camera", e);
          }
        }
      }
    }.start();
  }

  private boolean isMatch(int cameraId, CameraSelectionCriteria criteria) {
    boolean result=true;
    Camera.CameraInfo info=new Camera.CameraInfo();

    if (criteria!=null) {
      Camera.getCameraInfo(cameraId, info);

      if ((criteria.getFacing()==CameraSelectionCriteria.Facing.FRONT &&
          info.facing!=Camera.CameraInfo.CAMERA_FACING_FRONT) ||
          (criteria.getFacing()==CameraSelectionCriteria.Facing.BACK &&
              info.facing!=Camera.CameraInfo.CAMERA_FACING_BACK)) {
        result=false;
      }
    }

    return(result);
  }

  private class TakePictureTransaction implements Camera.PictureCallback {
    @Override
    public void onPictureTaken(byte[] bytes, Camera camera) {
      getBus().post(new PictureTakenEvent());
      camera.startPreview();
    }
  }

  static class Descriptor implements CameraDescriptor {
    private int cameraId;
    private Camera camera;
    private ArrayList<Size> pictureSizes;
    private ArrayList<Size> previewSizes;

    private Descriptor(int cameraId) {
      this.cameraId=cameraId;
    }

    public int getCameraId() {
      return(cameraId);
    }

    private void setCamera(Camera camera) {
      this.camera=camera;
    }

    private Camera getCamera() {
      return(camera);
    }

    @Override
    public ArrayList<Size> getPreviewSizes() {
      return(previewSizes);
    }

    private void setPreviewSizes(ArrayList<Size> sizes) {
      previewSizes=sizes;
    }

    @Override
    public ArrayList<Size> getPictureSizes() {
      return(pictureSizes);
    }

    @Override
    public boolean isPictureFormatSupported(int format) {
      return(ImageFormat.JPEG==format);
    }

    private void setPictureSizes(ArrayList<Size> sizes) {
      pictureSizes=sizes;
    }
  }

  private static class Session extends CameraSession {
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
