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
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import com.commonsware.cwac.cam2.camera2.CameraTwoEngine;
import com.commonsware.cwac.cam2.classic.ClassicCameraEngine;
import com.commonsware.cwac.cam2.util.Size;
import java.util.List;
import de.greenrobot.event.EventBus;

/**
 * Base class for camera engines, which abstract out camera
 * functionality for different APIs (e.g., android.hardware.Camera,
 * android.hardware.camera2.*).
 */
abstract public class CameraEngine {
  private EventBus bus=EventBus.getDefault();
  private boolean isDebug=false;

  private static class CrashableEvent {
    /**
     * The exception that was raised when trying to process
     * the request, or null if no such exception was raised.
     */
    public final Exception exception;

    public CrashableEvent() {
      this(null);
    }

    public CrashableEvent(Exception exception) {
      this.exception=null;
    }
  }

  /**
   * Event raised when camera descriptors are ready for use.
   * Subscribe to this event if you use loadCameraDescriptors()
   * to get the results. May include an exception if there was
   * an exception accessing the camera.
   */
  public static class CameraDescriptorsEvent extends CrashableEvent {
    /**
     * The camera descriptors loaded in response to a call
     * to loadCameraDescriptors()
     */
    public final List<CameraDescriptor> descriptors;

    public CameraDescriptorsEvent(List<CameraDescriptor> descriptors) {
      this.descriptors=descriptors;
    }

    public CameraDescriptorsEvent(Exception exception) {
      super(exception);
      this.descriptors=null;
    }
  }

  /**
   * Event raised after destroy() is called, to inform you
   * about completion of the work.
   */
  public static class DestroyedEvent {

  }

  /**
   * Event raised when the camera has been opened.
   * Subscribe to this event if you use open()
   * to to find out when the open has succeeded.
   * May include an exception if there was
   * an exception accessing the camera.
   */
  public static class OpenedEvent extends CrashableEvent {
    public OpenedEvent() {
      super();
    }

    public OpenedEvent(Exception exception) {
      super(exception);
    }
  }

  /**
   * Event raised when preview sizes are ready for use.
   * Subscribe to this event if you use loadAvailablePreviewSizes()
   * to get the results. May include an exception if there was
   * an exception accessing the camera.
   */
  public static class PreviewSizesEvent extends CrashableEvent {
    /**
     * The available preview sizes
     */
    final public List<Size> sizes;

    public PreviewSizesEvent(List<Size> sizes) {
      super();
      this.sizes=sizes;
    }

    public PreviewSizesEvent(Exception exception) {
      super(exception);
      this.sizes=null;
    }
  }

  /**
   * Event raised when picture is taken, as a result of a
   * takePicture() call. May include an exception if there was
   * an exception accessing the camera.
   */
  public static class PictureTakenEvent extends CrashableEvent {
    public PictureTakenEvent() {
      super();
    }

    public PictureTakenEvent(Exception exception) {
      super(exception);
    }
  }

  /**
   * Loads a roster of the available cameras for this engine,
   * or the cameras that match the supplied criteria. Subscribe
   * to the CameraDescriptorsEvent to get the results of this
   * call asynchronously.
   *
   * @param criteria requirements for the matching cameras, or
   *                 null to return all cameras
   */
  abstract public void loadCameraDescriptors(CameraSelectionCriteria criteria);

  /**
   * Call this when the engine is to be shut down permanently.
   * A new engine instance should be created if the camera is
   * to be used again in the future.
   */
  abstract public void destroy();

  /**
   * Find out what preview sizes the indicated camera supports.
   * Subscribe to the PreviewSizesEvent to get the results of
   * this call asynchronously.
   *
   * @param camera the CameraDescriptor of the camera of interest
   */
  abstract public void loadAvailablePreviewSizes(CameraDescriptor camera);

  /**
   * Open the requested camera and show a preview on the supplied
   * surface. Subscribe to the OpenEvent to find out when this
   * work is completed.
   *
   * @param rawCamera the CameraDescriptor of the camera of interest
   * @param texture the preview surface
   * @param previewSize size of the requested preview
   */
  abstract public void open(CameraDescriptor rawCamera,
                            SurfaceTexture texture,
                            Size previewSize);

  /**
   * Close the open camera. Note that this work is done
   * synchronously, while most calls to this class are asynchronous.
   *
   * @param rawCamera the CameraDescriptor of the camera of interest
   */
  abstract public void close(CameraDescriptor rawCamera);

  /**
   * Take a picture, on the supplied camera, using the picture
   * configuration from the supplied transaction. Posts a
   * PictureTakenEvent when the request is completed, successfully
   * or unsuccessfully.
   *
   * @param rawCamera the CameraDescriptor of the camera of interest
   * @param xact the configuration of the picture to take
   */
  abstract public void takePicture(CameraDescriptor rawCamera,
                                   PictureTransaction xact);

  /**
   * Builds a CameraEngine instance based on the device's
   * API level.
   *
   * @param ctxt any Context will do
   * @return a new CameraEngine instance
   */
  public static CameraEngine buildInstance(Context ctxt) {
    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
      return(new CameraTwoEngine(ctxt));
    }

    return(new ClassicCameraEngine());
  }

  /**
   * Sets the event bus to use, where the default is the
   * default event bus supplied by the EventBus class.
   *
   * @param bus the bus to use for events
   */
  public void setBus(EventBus bus) {
    this.bus=bus;
  }

  /**
   * @return the bus to use for events
   */
  public EventBus getBus() {
    return(bus);
  }

  /**
   * Sets whether or not exceptions should be logged, in addition
   * to being included in relevant events. The default is false.
   *
   * @param isDebug true if exceptions should be logged, false otherwise
   */
  public void setDebug(boolean isDebug) {
    this.isDebug=isDebug;
  }

  /**
   * @return true if exceptions should be logged, false otherwise
   */
  public boolean isDebug() {
    return(isDebug);
  }
}
