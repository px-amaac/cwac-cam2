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

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

public class MainActivity extends Activity
    implements PlaygroundFragment.Contract {
  private static final int REQUEST_CAMERA=1337;
  private PlaygroundFragment playground=null;
  private ResultFragment result=null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Fragment f=getFragmentManager().findFragmentById(android.R.id.content);

    if (f!=null) {
      if (f instanceof PlaygroundFragment) {
        playground=(PlaygroundFragment)f;
      }
      else {
        result=(ResultFragment)f;
      }
    }

    if (playground==null) {
      playground=new PlaygroundFragment();
      getFragmentManager()
          .beginTransaction()
          .add(android.R.id.content, playground)
          .commit();
    }

    if (result==null) {
      result=ResultFragment.newInstance();
      getFragmentManager()
          .beginTransaction()
          .add(android.R.id.content, result)
          .hide(result)
          .commit();
    }
  }

  public void takePicture(Intent i) {
    startActivityForResult(i, REQUEST_CAMERA);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode==REQUEST_CAMERA)
      if (resultCode==Activity.RESULT_OK) {
        Bitmap bitmap=data.getParcelableExtra("data");

        if (bitmap==null) {
          result.setImage(data.getData());
        }
        else {
          result.setImage(bitmap);
        }

        getFragmentManager()
            .beginTransaction()
            .hide(playground)
            .show(result)
            .addToBackStack(null)
            .commit();
      }

    super.onActivityResult(requestCode, resultCode, data);
  }
}
