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

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import com.commonsware.cwac.cam2.util.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Implementation of a CameraEngine that supports the
 * original android.hardware.Camera API.
 */
@SuppressWarnings("deprecation")
public class ClassicCameraEngine extends CameraEngine {
  private List<CameraDescriptor> descriptors=null;

  /**
   * {@inheritDoc}
   */
  @Override
  public CameraSession.Builder buildSession(Context ctxt, CameraDescriptor descriptor) {
    return(new SessionBuilder(ctxt, descriptor));
  }

  /**
   * {@inheritDoc}
   */
  public void loadCameraDescriptors(final CameraSelectionCriteria criteria) {
    getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        if (descriptors == null) {
          int count=Camera.getNumberOfCameras();
          List<CameraDescriptor> result=new ArrayList<CameraDescriptor>();
          Camera.CameraInfo info=new Camera.CameraInfo();

          for (int cameraId=0; cameraId < count; cameraId++) {
            Camera.getCameraInfo(cameraId, info);
            Descriptor descriptor=new Descriptor(cameraId, info);

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

          descriptors=result;
        }

        List<CameraDescriptor> result=new ArrayList<CameraDescriptor>(descriptors);

        Collections.sort(result, new Comparator<CameraDescriptor>() {
          @Override
          public int compare(CameraDescriptor descriptor, CameraDescriptor t1) {
            Descriptor lhs=(Descriptor)descriptor;
            Descriptor rhs=(Descriptor)t1;

            // descending, so invert normal side-ness

            int lhScore=rhs.getScore(criteria);
            int rhScore=lhs.getScore(criteria);

            // from Integer.compare(), which is new to API Level 19

            return(lhScore < rhScore ? -1 : (lhScore == rhScore ? 0 : 1));
          }
        });

        getBus().post(new CameraEngine.CameraDescriptorsEvent(result));
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close(final CameraSession session) {
      Descriptor descriptor=(Descriptor)session.getDescriptor();
      Camera camera=descriptor.getCamera();

      if (camera != null) {
        camera.stopPreview();
        camera.release();
        descriptor.setCamera(null);
      }

      getBus().post(new ClosedEvent());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void takePicture(final CameraSession session, final PictureTransaction xact) {
    getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        Descriptor descriptor=(Descriptor)session.getDescriptor();
        Camera camera=descriptor.getCamera();

        try {
          camera.takePicture(new Camera.ShutterCallback() {
                               @Override
                               public void onShutter() {
                                 // empty plays a sound -- go figure
                               }
                             }, null,
              new TakePictureTransaction(session.getContext(), xact));
        }
        catch (Exception e) {
          getBus().post(new PictureTakenEvent(e));

          if (isDebug()) {
            Log.e(getClass().getSimpleName(), "Exception taking picture", e);
          }
        }
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void open(final CameraSession session,
                   final SurfaceTexture texture) {
    getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        Descriptor descriptor=(Descriptor)session.getDescriptor();
        Camera camera=descriptor.getCamera();

        if (camera == null) {
          camera=Camera.open(descriptor.getCameraId());
          descriptor.setCamera(camera);
        }

        try {
          camera.setParameters(((Session)session).configure());
          camera.setPreviewTexture(texture);
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
    });
  }

  private class TakePictureTransaction implements Camera.PictureCallback {
    private final PictureTransaction xact;
    private final Context ctxt;

    TakePictureTransaction(Context ctxt, PictureTransaction xact) {
      this.ctxt=ctxt.getApplicationContext();
      this.xact=xact;
    }

    @Override
    public void onPictureTaken(final byte[] bytes, final Camera camera) {
      getThreadPool().execute(new Runnable() {
        @Override
        public void run() {
          getBus().post(new PictureTakenEvent(xact.process(new ImageContext(ctxt, bytes))));
          camera.startPreview();
        }
      });
    }
  }

  static class Descriptor implements CameraDescriptor {
    private int cameraId;
    private Camera camera;
    private ArrayList<Size> pictureSizes;
    private ArrayList<Size> previewSizes;
    private final int facing;

    private Descriptor(int cameraId, Camera.CameraInfo info) {
      this.cameraId=cameraId;
      this.facing=info.facing;
    }

    public int getCameraId() {
      return (cameraId);
    }

    private void setCamera(Camera camera) {
      this.camera=camera;
    }

    private Camera getCamera() {
      return (camera);
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

    @Override
    public boolean isPictureFormatSupported(int format) {
      return (ImageFormat.JPEG == format);
    }

    private void setPictureSizes(ArrayList<Size> sizes) {
      pictureSizes=sizes;
    }

    private int getScore(CameraSelectionCriteria criteria) {
      int score=10;

      if (criteria != null) {
        if ((criteria.getFacing().isFront() &&
            facing != Camera.CameraInfo.CAMERA_FACING_FRONT) ||
            (!criteria.getFacing().isFront() &&
                facing != Camera.CameraInfo.CAMERA_FACING_BACK)) {
          score=0;
        }
      }

      return(score);
    }
  }

  private static class Session extends CameraSession {
    private Session(Context ctxt, CameraDescriptor descriptor) {
      super(ctxt, descriptor);
    }

    Camera.Parameters configure() {
      final Descriptor descriptor=(Descriptor)getDescriptor();
      final Camera camera=descriptor.getCamera();
      Camera.Parameters params=camera.getParameters();
      Camera.CameraInfo info=new Camera.CameraInfo();

      Camera.getCameraInfo(descriptor.getCameraId(), info);

      for (CameraPlugin plugin : getPlugins()) {
        ClassicCameraConfigurator configurator=plugin.buildConfigurator(ClassicCameraConfigurator.class);

        if (configurator != null) {
          params=configurator.configure(info, camera, params);
        }
      }

      return (params);
    }
  }

  private static class SessionBuilder extends CameraSession.Builder {
    private SessionBuilder(Context ctxt, CameraDescriptor descriptor) {
      super(new Session(ctxt, descriptor));
    }
  }
}