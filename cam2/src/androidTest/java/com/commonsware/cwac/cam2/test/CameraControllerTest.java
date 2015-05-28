/***
 Copyright (c) 2015 CommonsWare, LLC
 Licensed under the Apache License, Version 2.0 (the "License"); you may not
 use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 by applicable law or agreed to in writing, software distributed under the
 License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 OF ANY KIND, either express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 From _The Busy Coder's Guide to Android Development_
 http://commonsware.com/Android
 */

package com.commonsware.cwac.cam2.test;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.commonsware.cwac.cam2.CameraController;
import com.commonsware.cwac.cam2.CameraDescriptor;
import com.commonsware.cwac.cam2.CameraEngine;
import com.commonsware.cwac.cam2.CameraSelectionCriteria;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class CameraControllerTest {
  final private CameraEngine engine=CameraEngine.buildInstance(InstrumentationRegistry.getTargetContext());
  final private CameraController ctrl=new CameraController();

  @Before
  public void init() {
    ctrl.setEngine(engine);
  }

/*
  @Test
  public void cameras() {
    Assert.assertTrue("has both cameras", ctrl.hasBothCameras());
  }
*/
}