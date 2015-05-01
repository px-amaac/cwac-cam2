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
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;

/**
 * Stock activity for taking pictures. Supports the same
 * protocol, in terms of extras and return data, as does
 * ACTION_IMAGE_CAPTURE.
 */
public class CameraActivity extends Activity {
  /**
   * The fragment implementing the bulk of the actual UI
   */
  private CameraFragment frag;

  /**
   * Use this method (or its two-parameter counterpart) to
   * create the Intent to use to start this activity, or
   * to start the ACTION_CAPTURE_IMAGE activity (if the app
   * or device is incapable of supporting this activity).
   *
   * @param ctxt any Context will do
   * @return Intent to be used to start up this activity
   */
  public static Intent buildLaunchIntent(Context ctxt) {
    return(buildLaunchIntent(ctxt, true));
  }

  /**
   * Use this method (or its one-parameter counterpart) to
   * create the Intent to use to start this activity.
   *
   * @param ctxt any Context will do
   * @param useFallback supply false to indicate that
   *                    this method should throw an exception
   *                    if the app or device cannot start this
   *                    activity
   * @return Intent to be used to start up this activity
   */
  public static Intent buildLaunchIntent(Context ctxt,
                                         boolean useFallback) {
    Intent result=new Intent(ctxt, CameraActivity.class);

    try {
      Utils.validateEnvironment(ctxt);
    }
    catch (Exception e) {
      if (useFallback) {
        result=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      }
      else {
        throw e;
      }
    }

    return(result);
  }

  /**
   * Standard lifecycle method, serving as the main entry
   * point of the activity.
   *
   * @param savedInstanceState the state of a previous instance
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Utils.validateEnvironment(this);

    frag=(CameraFragment)getFragmentManager()
                            .findFragmentById(android.R.id.content);

    if (frag==null) {
      frag=new CameraFragment();
      getFragmentManager()
          .beginTransaction()
          .add(android.R.id.content, frag)
          .commit();
    }
  }
}
