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

import com.commonsware.cwac.cam2.util.Size;

/**
 * Class representing a session with a camera. While
 * a CameraDescriptor is simply a declaration of "there exists
 * a camera with these capabilities", CameraSession indicates
 * what you want to have happen with a particular camera, for
 * general operation. This is roughly analogous to Camera.Parameters
 * in the classic Camera API.
 *
 * To create instances, call buildSession() on an
 * instance of a CameraEngine, then use the resulting
 * CameraSession.Builder to describe what you want, followed
 * by a call to build() to give you the actual CameraSession.
 */
public class CameraSession {
  private final CameraDescriptor descriptor;
  private Size pictureSize;
  private Size previewSize;
  private int pictureFormat;

  /**
   * Constructor.
   *
   * @param descriptor the camera to use for this session
   */
  CameraSession(CameraDescriptor descriptor) {
    this.descriptor=descriptor;
  }

  /**
   * @return the camera to use for this session
   */
  public CameraDescriptor getDescriptor() {
    return(descriptor);
  }

  /**
   * @return the desired picture size, out of the available
   * picture sizes for this camera
   */
  public Size getPictureSize() {
    return(pictureSize);
  }

  /**
   * @return the desired preview size, out of the available
   * preview sizes for this camera
   */
  public Size getPreviewSize() {
    return(previewSize);
  }

  /**
   * @return the desired picture format, as an ImageFormat value
   * (e.g., ImageFormat.JPEG)
   */
  public int getPictureFormat() {
    return(pictureFormat);
  }

  /**
   * Class to build an instance of a CameraSession. Get an instance
   * from buildSession() on your chosen CameraEngine.
   */
  abstract public static class Builder {
    protected final CameraSession session;

    protected Builder(CameraSession session) {
      this.session=session;
    }

    /**
     * Establish the picture size to use. Must be a size that
     * the CameraDescriptor for this session claims to support.
     *
     * @param pictureSize the desired picture size
     * @return the builder, for more building
     */
    public Builder pictureSize(Size pictureSize) {
      if (session.descriptor.getPictureSizes().contains(pictureSize)) {
        session.pictureSize=pictureSize;
      }
      else {
        throw new IllegalArgumentException("Requested picture size is not one that the camera supports");
      }

      return(this);
    }

    /**
     * Establish the preview size to use. Must be a size that
     * the CameraDescriptor for this session claims to support.
     *
     * @param previewSize the desired preview size
     * @return the builder, for more building
     */
    public Builder previewSize(Size previewSize) {
      if (session.descriptor.getPreviewSizes().contains(previewSize)) {
        session.previewSize=previewSize;
      }
      else {
        throw new IllegalArgumentException("Requested preview size is not one that the camera supports");
      }

      return(this);
    }

    /**
     * Establish the picture format to use. Must be a format that
     * the CameraDescriptor for this session claims to support.
     *
     * @param format an ImageFormat value (e.g., ImageFormat.JPEG)
     * @return the builder, for more building
     */
    public Builder pictureFormat(int format) {
      if (session.descriptor.isPictureFormatSupported(format)) {
        session.pictureFormat=format;
      }
      else {
        throw new IllegalArgumentException("Requested picture format is not one that the camera supports");
      }

      return(this);
    }

    /**
     * @return the CameraSession, configured as you requested
     */
    public CameraSession build() {
      return(session);
    }
  }
}
