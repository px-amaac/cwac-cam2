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
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import com.commonsware.cwac.cam2.util.Size;

/**
 * Class responsible for rendering preview frames on the UI
 * as a View for the user to see and interact with. Also handles
 * maintaining aspect ratios and dealing with full-bleed previews.
 */
public class CameraView extends TextureView implements TextureView.SurfaceTextureListener {
  interface StateCallback {
    void onReady(CameraView cv);
    void onDestroyed(CameraView cv);
  }

  /**
   * The requested size of the preview frames, or null to just
   * use the size of the view itself
   */
  private Size previewSize;

  private StateCallback stateCallback;

  /**
   * Constructor, used for creating instances from Java code.
   *
   * @param context the Activity that will host this View
   */
  public CameraView(Context context) {
    super(context, null);
    initListener();
  }

  /**
   * Constructor, used by layout inflation.
   *
   * @param context the Activity that will host this View
   * @param attrs the parsed attributes from the layout resource tag
   */
  public CameraView(Context context, AttributeSet attrs) {
    super(context, attrs, 0);
    initListener();
  }

  /**
   * Constructor, used by... something.
   *
   * @param context the Activity that will host this View
   * @param attrs the parsed attributes from the layout resource tag
   * @param defStyle "An attribute in the current theme that
   *                 contains a reference to a style resource
   *                 that supplies default values for the view.
   *                 Can be 0 to not look for defaults."
   */
  public CameraView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    initListener();
  }

  /**
   * @return the requested preview size
   */
  public Size getPreviewSize() {
    return(previewSize);
  }

  /**
   * @param previewSize the requested preview size
   */
  public void setPreviewSize(Size previewSize) {
    this.previewSize=previewSize;

    enterTheMatrix();
  }

  public void setStateCallback(StateCallback cb) {
    stateCallback=cb;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    final int width=
        resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
    final int height=
        resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

    int previewWidth=width;
    int previewHeight=height;

    int rotation=((Activity)getContext()).getWindowManager().getDefaultDisplay().getRotation();

    if (previewSize != null) {
      if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
        previewWidth=previewSize.getHeight();
        previewHeight=previewSize.getWidth();
      }
      else {
        previewWidth=previewSize.getWidth();
        previewHeight=previewSize.getHeight();
      }
    }

    boolean useFirstStrategy=
        (width * previewHeight > height * previewWidth);
    boolean fullBleed=true;

    if ((useFirstStrategy && !fullBleed)
        || (!useFirstStrategy && fullBleed)) {
      setMeasuredDimension(previewWidth * height / previewHeight, height);
    }
    else {
      setMeasuredDimension(width, previewHeight * width / previewWidth);
    }
  }

  private void initListener() {
    setSurfaceTextureListener(this);
  }

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
    if (stateCallback !=null) {
      stateCallback.onReady(this);
    }
  }

  @Override
  public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
    enterTheMatrix();
  }

  @Override
  public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
    if (stateCallback !=null) {
      stateCallback.onDestroyed(this);
    }

    return(false);
  }

  @Override
  public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

  }

  // inspired by https://github.com/googlesamples/android-Camera2Basic/blob/master/Application/src/main/java/com/example/android/camera2basic/Camera2BasicFragment.java

  private void enterTheMatrix() {
    int rotation=((Activity)getContext()).getWindowManager().getDefaultDisplay().getRotation();

    Matrix matrix=new Matrix();
    RectF viewRect=new RectF(0, 0, getWidth(), getHeight());
    RectF bufferRect=new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
    float centerX=viewRect.centerX();
    float centerY=viewRect.centerY();

    if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
      bufferRect.offset(centerX-bufferRect.centerX(), centerY-bufferRect.centerY());
      matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

      float scale=Math.max(
          (float)getHeight()/previewSize.getHeight(),
          (float)getWidth()/previewSize.getWidth());

      matrix.postScale(scale, scale, centerX, centerY);
      matrix.postRotate(90*(rotation-2), centerX, centerY);
    }
    else if (Surface.ROTATION_180==rotation) {
      matrix.postRotate(180, centerX, centerY);
    }

    setTransform(matrix);
  }
}
