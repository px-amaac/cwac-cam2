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

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.os.Build;
import com.commonsware.cwac.cam2.plugin.OrientationPlugin;
import com.commonsware.cwac.cam2.plugin.SizeAndFormatPlugin;
import com.commonsware.cwac.cam2.util.Size;
import com.commonsware.cwac.cam2.util.Utils;
import de.greenrobot.event.EventBus;

/**
 * Controller for camera-related functions, designed to be used
 * by CameraFragment or the equivalent.
 */
public class CameraController implements CameraView.StateCallback {
  private CameraEngine engine;
  private CameraDescriptor camera;
  private CameraView cv;
  private CameraSession session;

  /**
   * @return the engine being used by this fragment to access
   * the camera(s) on the device
   */
  public CameraEngine getEngine() {
    return(engine);
  }

  /**
   * Setter for the engine. Must be called before onCreateView()
   * is called, preferably shortly after constructing the
   * fragment.
   *
   * @param engine the engine to be used by this fragment to access
   * the camera(s) on the device
   */
  public void setEngine(CameraEngine engine, CameraSelectionCriteria criteria) {
    this.engine=engine;

    EventBus.getDefault().register(this);

    engine.loadCameraDescriptors(criteria);
  }

  /**
   * Call this from onStart() of an activity or fragment, or from
   * an equivalent point in time. If the CameraView is ready,
   * the preview should begin; otherwise, the preview will
   * begin after the CameraView is ready.
   */
  public void start() {
    if (cv.isAvailable()) {
      open();
    }
  }

  /**
   * Call this from onStop() of an activity or fragment, or
   * from an equivalent point in time, to indicate that you want
   * the camera preview to stop.
   */
  public void stop() {
    if (session!=null) {
      engine.close(session);
      session.destroy();
      session=null;

      // TODO: support multiple cameras
    }
  }

  /**
   * Call this from onDestroy() of an activity or fragment,
   * or from an equivalent point in time, to tear down the
   * entire controller and engine. A fresh controller should
   * be created if you want to use the camera again in the future.
   */
  public void destroy() {
    if (engine!=null) {
      engine.destroy();
    }

    EventBus.getDefault().unregister(this);
  }

  /**
   * Setter method for the CameraView that this controller controls
   *
   * @param cv a CameraView
   */
  public void setCameraView(CameraView cv) {
    this.cv=cv;
    cv.setStateCallback(this);
  }

  /**
   * Public because Java interfaces are intrinsically public.
   * This method is not part of the class' API and should not
   * be used by third-party developers.
   *
   * @param cv the CameraView that is now ready
   */
  @Override
  public void onReady(CameraView cv) {
    open();
  }

  /**
   * Public because Java interfaces are intrinsically public.
   * This method is not part of the class' API and should not
   * be used by third-party developers.
   *
   * @param cv the CameraView that is now destroyed
   */
  @Override
  public void onDestroyed(CameraView cv) {
    stop();
  }

  /**
   * Takes a picture, in accordance with the details supplied
   * in the PictureTransaction. Subscribe to the
   * PictureTakenEvent to get the results of the picture.
   *
   * @param xact a PictureTransaction describing what should be taken
   */
  public void takePicture(PictureTransaction xact) {
    engine.takePicture(session, xact);
  }

  private void open() {
    Size previewSize=null;

    if (camera!=null && cv.getWidth()>0 && cv.getHeight()>0) {
      // TODO: optional limit preview to same aspect ratio as chosen picture size

      Size largest=null;
      long currentLargestArea=0;
      Size smallestBiggerThanPreview=null;
      long currentSmallestArea=Integer.MAX_VALUE;

      for (Size size : camera.getPreviewSizes()) {
        long currentArea=size.getWidth()*size.getHeight();

        if (largest==null || size.getWidth()*size.getHeight()>currentLargestArea) {
          largest=size;
          currentLargestArea=currentArea;
        }

        if (size.getWidth()>=cv.getWidth() && size.getHeight()>=cv.getHeight()) {
          if (smallestBiggerThanPreview==null || currentArea<currentSmallestArea) {
            smallestBiggerThanPreview=size;
            currentSmallestArea=currentArea;
          }
        }
      }

      previewSize=(smallestBiggerThanPreview==null
          ? largest
          : smallestBiggerThanPreview);

      cv.setPreviewSize(previewSize);
    }

    SurfaceTexture texture=cv.getSurfaceTexture();

    if (previewSize!=null && texture!=null) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        texture.setDefaultBufferSize(cv.getWidth(), cv.getHeight());
      }

      session=engine
          .buildSession(cv.getContext(), camera)
          .addPlugin(new SizeAndFormatPlugin(previewSize,
              Utils.getLargestPictureSize(camera),
              ImageFormat.JPEG))
          .addPlugin(new OrientationPlugin(cv.getContext()))
          .build();

      engine.open(session, texture);
    }
  }

  @SuppressWarnings("unused")
  public void onEventMainThread(CameraEngine.CameraDescriptorsEvent event) {
    if (event.descriptors.size()>0) {
      camera=event.descriptors.get(0);
      open();
    }
    else {
      EventBus.getDefault().post(new NoSuchCameraEvent());
    }
  }

  public static class NoSuchCameraEvent {

  }
}
