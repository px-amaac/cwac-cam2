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

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import com.commonsware.cwac.cam2.util.Utils;
import com.github.polok.flipview.FlipView;
import java.util.List;

/**
 * Controller for camera-related functions, designed to be used
 * by CameraFragment or the equivalent.
 */
public class CameraController {
  private CameraEngine engine;
  private CameraDescriptor backCamera;
  private CameraDescriptor frontCamera;

  /**
   * @return the engine being used by this fragment to access
   * the camera(s) on the device
   */
  public CameraEngine getEngine() {
    return(engine);
  }

  /**
   * Setter for the engine. Must be called before onCreateView()
   * is called, preferably shortly after constructing the
   * fragment.
   *
   * @param engine the engine to be used by this fragment to access
   * the camera(s) on the device
   */
  public void setEngine(CameraEngine engine) {
    this.engine=engine;

    CameraSelectionCriteria criteria=
        new CameraSelectionCriteria.Builder()
            .facing(CameraSelectionCriteria.Facing.FRONT).build();
    List<CameraDescriptor> cameras=engine.getCameraDescriptors(criteria);

    if (cameras.size()>0) {
      frontCamera=cameras.get(0);
    }

    criteria=
        new CameraSelectionCriteria.Builder()
            .facing(CameraSelectionCriteria.Facing.BACK).build();
    cameras=engine.getCameraDescriptors(criteria);

    if (cameras.size()>0) {
      backCamera=cameras.get(0);
    }
  }

  /**
   * @return true if the device has both a front-facing camera and
   * a back-facing camera
   */
  public boolean hasBothCameras() {
    return(frontCamera!=null && backCamera!=null);
  }
}
