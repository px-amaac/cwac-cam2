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
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.view.TextureView;
import android.view.View;
import com.commonsware.cwac.cam2.camera2.CameraTwoEngine;
import com.commonsware.cwac.cam2.classic.ClassicCameraEngine;

/**
 * Base class for camera engines, which abstract out camera
 * functionality for different APIs (e.g., android.hardware.Camera,
 * android.hardware.camera2.*).
 */
public class CameraEngine {
  /**
   * Builds a CameraEngine instance based on the device's
   * API level.
   *
   * @return a new CameraEngine instance
   */
  public static CameraEngine buildInstance() {
    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
      return(new CameraTwoEngine());
    }

    return(new ClassicCameraEngine());
  }

  /**
   * Creates the View to use for camera previews; will be used
   * by CameraView for its main content.
   *
   * @param host the activity hosting the preview View
   * @return the preview View
   */
  public View buildPreviewView(Activity host) {
    return(new Preview(host).getWidget());
  }

  /**
   * A stock implementation of a camera preview manager,
   * based on a TextureView.
   */
  private static class Preview implements TextureView.SurfaceTextureListener {
    private TextureView widget=null;
    private SurfaceTexture surface=null;

    Preview(Activity host) {
      widget=new TextureView(host);
      widget.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                          int width, int height) {
      this.surface=surface;

//      cameraView.previewCreated();
//      cameraView.initPreview(width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                            int width, int height) {
//      cameraView.previewReset(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//      cameraView.previewDestroyed();

      return (true);
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
      // no-op
    }

/*
    @Override
    public void attach(Camera camera) throws IOException {
      camera.setPreviewTexture(surface);
    }

    @Override
    public void attach(MediaRecorder recorder) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        // no-op
      }
      else {
        throw new IllegalStateException(
            "Cannot use TextureView with MediaRecorder");
      }
    }
*/

    /**
     * @return the View that is where previews will be rendered
     * by the engine
     */
    public View getWidget() {
      return (widget);
    }
  }
}
