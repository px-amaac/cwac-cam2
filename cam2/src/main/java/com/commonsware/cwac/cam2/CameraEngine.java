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
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import com.commonsware.cwac.cam2.camera2.CameraTwoEngine;
import com.commonsware.cwac.cam2.classic.ClassicCameraEngine;
import com.commonsware.cwac.cam2.util.Size;
import java.util.List;

/**
 * Base class for camera engines, which abstract out camera
 * functionality for different APIs (e.g., android.hardware.Camera,
 * android.hardware.camera2.*).
 */
abstract public class CameraEngine {
  /**
   * Returns a roster of the available cameras for this engine,
   * or the cameras that match the supplied criteria,
   * in the form of a collection of CameraDescriptor objects.
   *
   * @param criteria requirements for the matching cameras, or
   *                 null to return all cameras
   * @return roster of matching (or all) cameras
   */
  abstract public List<CameraDescriptor> getCameraDescriptors(CameraSelectionCriteria criteria);

  /**
   * Call this when the engine is to be shut down permanently.
   * A new engine instance should be created if the camera is
   * to be used again in the future.
   */
  abstract public void destroy();

  /**
   * Find out what preview sizes the indicated camera supports
   *
   * @param camera the CameraDescriptor of the camera of interest
   * @return the available preview sizes, in no particular order
   */
  abstract public List<Size> getAvailablePreviewSizes(CameraDescriptor camera);

  /**
   * Open the requested camera and show a preview on the supplied
   * surface.
   *
   * @param rawCamera the CameraDescriptor of the camera of interest
   * @param surface the surface for the previews, expected to have
   *                been created from a TextureView
   */
  abstract public void open(CameraDescriptor rawCamera, Surface surface);

  /**
   * Close the open camera, if any.
   */
  abstract public void close();

  /**
   * Builds a CameraEngine instance based on the device's
   * API level.
   *
   * @param ctxt any Context will do
   * @return a new CameraEngine instance
   */
  public static CameraEngine buildInstance(Context ctxt) {
    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
      return(new CameraTwoEngine(ctxt));
    }

    return(new ClassicCameraEngine());
  }
}
