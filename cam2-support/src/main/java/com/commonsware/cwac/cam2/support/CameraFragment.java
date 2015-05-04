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

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import com.commonsware.cwac.cam2.CameraController;
import com.commonsware.cwac.cam2.CameraView;
import com.commonsware.cwac.cam2.util.Utils;
import com.github.polok.flipview.FlipView;

/**
 * Fragment for displaying a camera preview, with hooks to allow
 * you (or the user) to take a picture.
 */
public class CameraFragment extends Fragment {
  /**
   * Interface that all hosting activities must implement.
   */
  public interface Contract {
    /**
     * Used by CameraFragment to indicate that the user has
     * taken a photo, for activities that wish to take a specific
     * action at this point (e.g., set a result and finish).
     */
    void completeRequest();
  }

  private CameraController ctrl;

  /**
   * Standard fragment entry point.
   *
   * @param savedInstanceState State of a previous instance
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setRetainInstance(true);
  }

  /**
   * Standard lifecycle method, for when the fragment becomes
   * attached to an activity. Used here to validate the contract.
   *
   * @param activity the hosting activity
   */
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    if (!(activity instanceof Contract)) {
      throw new IllegalArgumentException("Hosting activity must implement CameraFragment.Contract");
    }
  }

  /**
   * Standard callback method to create the UI managed by
   * this fragment.
   *
   * @param inflater Used to inflate layouts
   * @param container Parent of the fragment's UI (eventually)
   * @param savedInstanceState State of a previous instance
   * @return the UI being managed by this fragment
   */
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    CameraView cv=new CameraView(getActivity());

    cv.setEngine(ctrl.getEngine());

    cv.setOnLongClickListener(new View.OnLongClickListener() {
      @Override
      public boolean onLongClick(View view) {
        getContract().completeRequest();

        return (true);
      }
    });

    boolean isLegacy=ViewConfiguration.get(getActivity()).hasPermanentMenuKey();

    int layoutId=isLegacy || Utils.isSystemBarOnBottom(getActivity())
        ? com.commonsware.cwac.cam2.R.layout.cwac_cam2_fragment_main
        : com.commonsware.cwac.cam2.R.layout.cwac_cam2_fragment_main_alt;

    ViewGroup main=(ViewGroup)inflater.inflate(layoutId, container, false);

    main.addView(cv, 0);

    FlipView lens=(FlipView)main.findViewById(com.commonsware.cwac.cam2.R.id.cwac_cam2_fragment_lens);

    if (!ctrl.hasBothCameras()) {
      lens.setVisibility(View.GONE);
    }

    return(main);
  }

  /**
   * @return the CameraController this fragment delegates to
   */
  public CameraController getController() {
    return(ctrl);
  }

  /**
   * Establishes the controller that this fragment delegates to
   *
   * @param ctrl the controller that this fragment delegates to
   */
  public void setController(CameraController ctrl) {
    this.ctrl=ctrl;
  }

  private Contract getContract() {
    return((Contract)getActivity());
  }
}
