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

package com.commonsware.cwac.cam2.support;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.Window;
import com.commonsware.cwac.cam2.CameraController;
import com.commonsware.cwac.cam2.CameraEngine;
import com.commonsware.cwac.cam2.CameraSelectionCriteria;
import com.commonsware.cwac.cam2.ImageContext;
import com.commonsware.cwac.cam2.util.Utils;
import java.io.File;
import de.greenrobot.event.EventBus;

/**
 * Stock activity for taking pictures. Supports the same
 * protocol, in terms of extras and return data, as does
 * ACTION_IMAGE_CAPTURE.
 */
public class CameraActivity extends FragmentActivity
  implements ConfirmationFragment.Contract {
  /**
   * Extra name for indicating what facing rule for the
   * camera you wish to use. The value should be a
   * CameraSelectionCriteria.Facing instance.
   */
  public static final String EXTRA_FACING="cwac_cam2_facing";

  /**
   * Extra name for indicating whether extra diagnostic
   * information should be reported, particularly for errors.
   * Default is false.
   */
  public static final String EXTRA_DEBUG_ENABLED="cwac_cam2_debug";

  /**
   * Extra name for indicating whether a confirmation screen
   * should appear after taking the picture, or whether taking
   * the picture should immediately return said picture. Defaults
   * to true, meaning that the user should confirm the picture.
   */
  public static final String EXTRA_CONFIRM="cwac_cam2_confirm";

  private CameraFragment cameraFrag;
  private ConfirmationFragment confirmFrag;
  private boolean needsThumbnail=false;

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

    getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

    Fragment f=getSupportFragmentManager().findFragmentById(android.R.id.content);

    if (f instanceof CameraFragment) {
      cameraFrag=(CameraFragment)f;
    }
    else {
      confirmFrag=(ConfirmationFragment)f;
    }

    if (cameraFrag==null) {
      Uri output=getOutputUri();

      cameraFrag=CameraFragment.newInstance(output);
      needsThumbnail=(output==null);

      CameraController ctrl=new CameraController();

      cameraFrag.setController(ctrl);

      CameraSelectionCriteria.Facing facing=
          (CameraSelectionCriteria.Facing)getIntent().getSerializableExtra(EXTRA_FACING);

      if (facing==null) {
        facing=CameraSelectionCriteria.Facing.BACK;
      }

      CameraSelectionCriteria criteria=
          new CameraSelectionCriteria.Builder().facing(facing).build();

      ctrl.setEngine(CameraEngine.buildInstance(this), criteria);
      ctrl.getEngine().setDebug(getIntent().getBooleanExtra(EXTRA_DEBUG_ENABLED, false));
      getSupportFragmentManager()
          .beginTransaction()
          .add(android.R.id.content, cameraFrag)
          .commit();
    }

    if (confirmFrag==null) {
      confirmFrag=ConfirmationFragment.newInstance();
      getSupportFragmentManager()
          .beginTransaction()
          .add(android.R.id.content, confirmFrag)
          .commit();
    }

    if (!cameraFrag.isVisible() && !confirmFrag.isVisible()) {
      getSupportFragmentManager()
          .beginTransaction()
          .hide(confirmFrag)
          .show(cameraFrag)
          .commit();
    }
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

  @SuppressWarnings("unused")
  public void onEventMainThread(CameraController.ControllerDestroyedEvent event) {
    finish();
  }

  @SuppressWarnings("unused")
  public void onEventMainThread(CameraEngine.PictureTakenEvent event) {
    if (getIntent().getBooleanExtra(EXTRA_CONFIRM, true)) {
      confirmFrag.setImage(event.getImageContext());

      getSupportFragmentManager()
          .beginTransaction()
          .hide(cameraFrag)
          .show(confirmFrag)
          .commit();
    }
    else {
      completeRequest(event.getImageContext(), true);
    }
  }

  @Override
  public void retakePicture() {
    getSupportFragmentManager()
        .beginTransaction()
        .hide(confirmFrag)
        .show(cameraFrag)
        .commit();
  }

  @Override
  public void completeRequest(ImageContext imageContext, boolean isOK) {
    if (!isOK) {
      setResult(RESULT_CANCELED);
    }
    else {
      if (needsThumbnail) {
        final Intent result=new Intent();

        result.putExtra("data", imageContext.buildResultThumbnail());

        findViewById(android.R.id.content).post(new Runnable() {
          @Override
          public void run() {
            setResult(RESULT_OK, result);
            removeFragments();
          }
        });
      }
      else {
        findViewById(android.R.id.content).post(new Runnable() {
          @Override
          public void run() {
            setResult(RESULT_OK);
            removeFragments();
          }
        });
      }
    }
  }

  private void removeFragments() {
    getSupportFragmentManager()
        .beginTransaction()
        .remove(confirmFrag)
        .remove(cameraFrag)
        .commit();
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
      output=getIntent().getParcelableExtra(MediaStore.EXTRA_OUTPUT);
    }

    return(output);
  }

  /**
   * Class to build an Intent used to start the CameraActivity.
   * Call setComponent() on the Intent if you are using your
   * own subclass of CameraActivty.
   */
  public static class IntentBuilder {
    private final Intent result;

    /**
     * Standard constructor. May throw a runtime exception
     * if the environment is not set up properly (see
     * validateEnvironment() on Utils).
     *
     * @param ctxt any Context will do
     */
    public IntentBuilder(Context ctxt) {
      Utils.validateEnvironment(ctxt);
      result=new Intent(ctxt, CameraActivity.class);
    }

    /**
     * Returns the Intent defined by the builder.
     *
     * @return the Intent to use to start the CameraActivity
     */
    public Intent build() {
      return(result);
    }

    /**
     * Indicates what camera should be used as the starting
     * point. Defaults to the rear-facing camera.
     *
     * @param facing which camera to use
     * @return the builder, for further configuration
     */
    public IntentBuilder facing(CameraSelectionCriteria.Facing facing) {
      result.putExtra(EXTRA_FACING, facing);

      return(this);
    }

    /**
     * Call if you want extra diagnostic information dumped to
     * LogCat. Not ideal for use in production.
     *
     * @return the builder, for further configuration
     */
    public IntentBuilder debug() {
      result.putExtra(EXTRA_DEBUG_ENABLED, true);

      return(this);
    }

    /**
     * Call to skip the confirmation screen, so once the user
     * takes the picture, you get control back right away.
     *
     * @return the builder, for further configuration
     */
    public IntentBuilder skipConfirm() {
      result.putExtra(EXTRA_CONFIRM, false);

      return(this);
    }

    /**
     * Indicates where to write the picture to. Defaults to
     * returning a thumbnail bitmap in the "data" extra, as
     * with ACTION_IMAGE_CAPTURE. Note that you need to have
     * write access to the supplied file.
     *
     * @param f file in which to write the picture
     * @return the builder, for further configuration
     */
    public IntentBuilder to(File f) {
      return(to(Uri.fromFile(f)));
    }

    /**
     * Indicates where to write the picture to. Defaults to
     * returning a thumbnail bitmap in the "data" extra, as
     * with ACTION_IMAGE_CAPTURE. Note that you need to have
     * write access to the supplied Uri.
     *
     * @param output Uri to which to write the picture
     * @return the builder, for further configuration
     */
    public IntentBuilder to(Uri output) {
      result.putExtra(MediaStore.EXTRA_OUTPUT, output);

      return(this);
    }
  }
}
