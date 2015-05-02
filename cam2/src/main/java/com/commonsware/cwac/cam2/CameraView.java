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
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Class responsible for rendering preview frames on the UI
 * as a View for the user to see and interact with. Also handles
 * maintaining aspect ratios and dealing with full-bleed previews.
 */
public class CameraView extends ViewGroup {
  /**
   * The requested size of the preview frames, or null to just
   * use the size of the view itself
   */
  private Size previewSize=null;

  /**
   * The orientation of the preview frames, where 0 is the natural
   * orientation of the device
   */
  private int orientation=0;

  /**
   * Constructor, used for creating instances from Java code.
   *
   * @param context the Activity that will host this View
   */
  public CameraView(Context context) {
    this(context, null);
  }

  /**
   * Constructor, used by layout inflation.
   *
   * @param context the Activity that will host this View
   * @param attrs the parsed attributes from the layout resource tag
   */
  public CameraView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
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
  }

  /**
   * @return the 0-based orientation, in degrees
   */
  public int getOrientation() {
    return(orientation);
  }

  /**
   * @param orientation the 0-based orientation, in degrees
   */
  public void setOrientation(int orientation) {
    this.orientation=orientation;
  }

  // based on CameraPreview.java from ApiDemos

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int width=
        resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
    final int height=
        resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
    setMeasuredDimension(width, height);
  }

  // based on CameraPreview.java from ApiDemos

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    if (changed && getChildCount() > 0) {
      final View child=getChildAt(0);
      final int width=r - l;
      final int height=b - t;
      int previewWidth=width;
      int previewHeight=height;

      // handle orientation

      if (previewSize != null) {
        if (orientation == 90 || orientation == 270) {
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
      // boolean useFullBleed=getHost().useFullBleedPreview();
      boolean useFullBleed=true;

      if ((useFirstStrategy && !useFullBleed)
          || (!useFirstStrategy && useFullBleed)) {
        final int scaledChildWidth=
            previewWidth * height / previewHeight;
        child.layout((width - scaledChildWidth) / 2, 0,
            (width + scaledChildWidth) / 2, height);
      }
      else {
        final int scaledChildHeight=
            previewHeight * width / previewWidth;
        child.layout(0, (height - scaledChildHeight) / 2, width,
            (height + scaledChildHeight) / 2);
      }
    }
  }
}
