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

package com.commonsware.cwac.cam2.classic;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.view.Surface;
import com.commonsware.cwac.cam2.CameraDescriptor;
import com.commonsware.cwac.cam2.CameraEngine;
import com.commonsware.cwac.cam2.CameraSelectionCriteria;
import com.commonsware.cwac.cam2.util.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of a CameraEngine that supports the
 * original android.hardware.Camera API.
 */
public class ClassicCameraEngine extends CameraEngine {
  /**
   * {@inheritDoc}
   */
  public List<CameraDescriptor> getCameraDescriptors(CameraSelectionCriteria criteria) {
    int count=Camera.getNumberOfCameras();
    List<CameraDescriptor> result=new ArrayList<CameraDescriptor>();

    for (int cameraId=0; cameraId<count; cameraId++) {
      if (isMatch(cameraId, criteria)) {
        result.add(new Descriptor(cameraId));
      }
    }

    return(result);
  }

  @Override
  public void close(CameraDescriptor rawCamera) {
    Descriptor descriptor=(Descriptor)rawCamera;
    Camera camera=descriptor.getCamera();

    if (camera!=null) {
      camera.stopPreview();
      camera.release();
      descriptor.setCamera(null);
    }
  }

  @Override
  public void destroy() {
    // TODO: clean up background thread, once I have one
  }

  @Override
  public List<Size> getAvailablePreviewSizes(CameraDescriptor rawCamera) {
    Descriptor descriptor=(Descriptor)rawCamera;
    Camera camera=descriptor.getCamera();
    boolean openedLocally=false;

    if (camera==null) {
      camera=Camera.open(descriptor.getCameraId());
      openedLocally=true;
    }

    Camera.Parameters params=camera.getParameters();

    ArrayList<Size> result=new ArrayList<Size>();

    for (Camera.Size size : params.getSupportedPreviewSizes()) {
      result.add(new Size(size.width, size.height));
    }

    if (openedLocally) {
      camera.release();
    }

    return(result);
  }

  @Override
  public void open(CameraDescriptor rawCamera,
                   SurfaceTexture texture,
                   Size previewSize) {
    Descriptor descriptor=(Descriptor)rawCamera;
    Camera camera=descriptor.getCamera();

    if (camera==null) {
      camera=Camera.open(descriptor.getCameraId());
      descriptor.setCamera(camera);
    }

    try {
      camera.setPreviewTexture(texture);

      Camera.Parameters params=camera.getParameters();

      params.setPreviewSize(previewSize.getWidth(),
          previewSize.getHeight());

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
        //      parameters.setRecordingHint(getHost().getRecordingHint() != CameraHost.RecordingHint.STILL_ONLY);
      }

      // TODO: get all other parameters changes here

      camera.setParameters(params);
      camera.startPreview();
    }
    catch (Exception e) {
      camera.release();
      descriptor.setCamera(null);
      throw new IllegalStateException("Exception trying to open camera", e);
    }
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
}
