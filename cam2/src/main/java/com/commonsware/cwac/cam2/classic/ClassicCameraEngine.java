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

import android.hardware.Camera;
import com.commonsware.cwac.cam2.CameraDescriptor;
import com.commonsware.cwac.cam2.CameraEngine;
import com.commonsware.cwac.cam2.CameraSelectionCriteria;
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
