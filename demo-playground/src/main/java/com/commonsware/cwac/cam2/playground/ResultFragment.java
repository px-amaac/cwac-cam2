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

package com.commonsware.cwac.cam2.playground;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

public class ResultFragment extends Fragment {
  private static final String ARG_BITMAP="bitmap";
  private static final String ARG_URI="uri";

  static ResultFragment newInstance() {
    ResultFragment f=new ResultFragment();

    f.setArguments(new Bundle());

    return(f);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return(new SubsamplingScaleImageView(getActivity()));
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    getSSSIV().setOrientation(SubsamplingScaleImageView.ORIENTATION_0);

    Bitmap bitmap=getArguments().getParcelable(ARG_BITMAP);

    if (bitmap == null) {
      Uri uri=getArguments().getParcelable(ARG_URI);

      if (uri != null) {
        setImage(uri);
      }
    }
    else {
      setImage(bitmap);
    }
  }

  void setImage(Bitmap bitmap) {
    getArguments().putParcelable(ARG_BITMAP, bitmap);
    getArguments().remove(ARG_URI);

    if (getView()!=null) {
      getSSSIV().setImage(ImageSource.bitmap(bitmap));
    }
  }

  void setImage(Uri uri) {
    getArguments().putParcelable(ARG_URI, uri);
    getArguments().remove(ARG_BITMAP);

    if (getView()!=null) {
      getSSSIV().setImage(ImageSource.uri(uri));
    }
  }

  private SubsamplingScaleImageView getSSSIV() {
    return((SubsamplingScaleImageView)getView());
  }
}
