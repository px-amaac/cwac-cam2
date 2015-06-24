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

package com.commonsware.cwac.cam2.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.ViewConfiguration;
import com.commonsware.cwac.cam2.CameraActivity;
import com.commonsware.cwac.cam2.CameraDescriptor;

/**
 * Home of static utility methods used by the library and
 * offered to developers in the spirit of togetherness.
 */
public class Utils {
  /**
   * Tests the app and the device to confirm that the code
   * in this library should work. This is called automatically
   * by other classes (e.g., CameraActivity), and so you probably
   * do not need to call it yourself. But, hey, it's a public
   * method, so call it if you feel like it.
   *
   * The method will throw an IllegalStateException if the
   * environment is unsatisfactory, where the exception message
   * will tell you what is wrong.
   *
   * @param ctxt any Context will do
   */
  public static void validateEnvironment(Context ctxt) {
    if (Build.VERSION.SDK_INT<Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      throw new IllegalStateException("App is running on device older than API Level 14");
    }

    PackageManager pm=ctxt.getPackageManager();

    if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) &&
        !pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
      throw new IllegalStateException("App is running on device that lacks a camera");
    }

    if (pm.checkPermission(Manifest.permission.CAMERA, ctxt.getPackageName())==PackageManager.PERMISSION_DENIED) {
      throw new IllegalStateException("App lacks the CAMERA permission");
    }

    if (ctxt instanceof CameraActivity) {
      try {
        ActivityInfo info=pm.getActivityInfo(((CameraActivity)ctxt).getComponentName(), 0);

        if (info.exported) {
          throw new IllegalStateException("A CameraActivity cannot be exported!");
        }
      }
      catch (PackageManager.NameNotFoundException e) {
        throw new IllegalStateException("Cannot find this activity!", e);
      }
    }
  }

  /**
   * Algorithm for determining if the system bar is on the
   * bottom or right. Based on implementation of PhoneWindowManager.
   * Pray that it holds up.
   *
   * @param ctxt any Context will do
   * @return true if the system bar should be on the bottom in
   * the current configuration, false otherwise
   */
  public static boolean isSystemBarOnBottom(Context ctxt) {
    Resources res=ctxt.getResources();
    Configuration cfg=res.getConfiguration();
    DisplayMetrics dm=res.getDisplayMetrics();
    boolean canMove=(dm.widthPixels != dm.heightPixels &&
        cfg.smallestScreenWidthDp < 600);

    return(!canMove || dm.widthPixels < dm.heightPixels);
  }

  public static Size getLargestPictureSize(CameraDescriptor descriptor) {
    Size result=null;

    for (Size size : descriptor.getPictureSizes()) {
      if (result == null) {
        result=size;
      }
      else {
        int resultArea=result.getWidth() * result.getHeight();
        int newArea=size.getWidth() * size.getHeight();

        if (newArea > resultArea) {
          result=size;
        }
      }
    }

    return(result);
  }
}
