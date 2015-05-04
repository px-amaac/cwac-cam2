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
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import com.commonsware.cwac.cam2.CameraDescriptor;
import com.commonsware.cwac.cam2.CameraEngine;
import com.commonsware.cwac.cam2.CameraSelectionCriteria;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of a CameraEngine that supports the
 * Android 5.0+ android.hardware.camera2 API.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraTwoEngine extends CameraEngine {
  private CameraManager mgr;

  /**
   * Standard constructor
   *
   * @param ctxt any Context will do
   */
  public CameraTwoEngine(Context ctxt) {
    mgr=(CameraManager)ctxt.
                        getApplicationContext().
                        getSystemService(Context.CAMERA_SERVICE);
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
}
