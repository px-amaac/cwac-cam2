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

public class CameraActivity extends Activity {
  public static Intent buildLaunchIntent(Context ctxt) {
    return(buildLaunchIntent(ctxt, true));
  }

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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Utils.validateEnvironment(this);
  }
}
