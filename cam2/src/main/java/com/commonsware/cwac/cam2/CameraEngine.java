package com.commonsware.cwac.cam2;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.view.TextureView;
import android.view.View;
import com.commonsware.cwac.cam2.camera2.CameraTwoEngine;
import com.commonsware.cwac.cam2.classic.ClassicCameraEngine;

abstract public class CameraEngine {
  public static CameraEngine buildInstance() {
    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
      return(new CameraTwoEngine());
    }

    return(new ClassicCameraEngine());
  }

  public View buildPreviewView(Activity host) {
    return(new Preview(host).getWidget());
  }

  static class Preview implements TextureView.SurfaceTextureListener {
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

    public View getWidget() {
      return (widget);
    }
  }
}
