package com.commonsware.cwac.cam2.util;

import android.hardware.Camera;

/**
 * Created by mmurphy on 5/28/15.
 */
public class ShitToGetRidOf {
  public static Size getLargestPictureSize(Camera.Parameters parameters) {
    Camera.Size result=null;

    for (Camera.Size size : parameters.getSupportedPictureSizes()) {
      if (result == null) {
        result=size;
      }
      else {
        int resultArea=result.width * result.height;
        int newArea=size.width * size.height;

        if (newArea > resultArea) {
          result=size;
        }
      }
    }

    return(new Size(result.width, result.height));
  }
}
