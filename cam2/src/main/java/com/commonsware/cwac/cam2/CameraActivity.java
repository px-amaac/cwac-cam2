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
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import com.commonsware.cwac.cam2.util.Utils;
import de.greenrobot.event.EventBus;

/**
 * Stock activity for taking pictures. Supports the same
 * protocol, in terms of extras and return data, as does
 * ACTION_IMAGE_CAPTURE.
 */
public class CameraActivity extends Activity
    implements CameraFragment.Contract {
  public static final String EXTRA_FACING="facing";
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

      CameraController ctrl=new CameraController();
      CameraSelectionCriteria.Facing facing=
          (CameraSelectionCriteria.Facing)getIntent().getSerializableExtra(EXTRA_FACING);

      if (facing==null) {
        facing=CameraSelectionCriteria.Facing.BACK_IF_AVAILABLE;
      }

      CameraSelectionCriteria criteria=
          new CameraSelectionCriteria.Builder().facing(facing).build();

      ctrl.setEngine(CameraEngine.buildInstance(this), criteria);
      ctrl.getEngine().setDebug(true);
      frag.setController(ctrl);

      getFragmentManager()
          .beginTransaction()
          .add(android.R.id.content, frag)
          .commit();
    }

    getOutputUri(); // TODO: do something with this
  }

  /**
   * Standard lifecycle method, for when the fragment moves into
   * the started state. Passed along to the CameraController.
   */
  @Override
  public void onStart() {
    super.onStart();

    EventBus.getDefault().register(this);
  }

  /**
   * Standard lifecycle method, for when the fragment moves into
   * the stopped state. Passed along to the CameraController.
   */
  @Override
  public void onStop() {
    EventBus.getDefault().unregister(this);

    super.onStop();
  }

  @SuppressWarnings("unused")
  public void onEventMainThread(CameraController.NoSuchCameraEvent event) {
    finish();
  }

  /**
   * Used by CameraFragment to indicate that the user has
   * taken a photo. While this is public, it is not really
   * part of the API of this activity class.
   */
  public void completeRequest() {
    setResult(RESULT_OK, new Intent()); // TODO: real result

    finish();
  }

  private Uri getOutputUri() {
    Uri output=null;

    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN_MR1) {
      ClipData clipData=getIntent().getClipData();

      if (clipData!=null && clipData.getItemCount() > 0) {
        output=clipData.getItemAt(0).getUri();
      }
    }

    if (output==null) {
      output=(Uri)getIntent().getParcelableExtra(MediaStore.EXTRA_OUTPUT);
    }

    if (output==null) {
      // TODO: come up with one
    }

    return(output);
  }
}
