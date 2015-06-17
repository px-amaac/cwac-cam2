/**
 * Copyright (c) 2015 CommonsWare, LLC
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.commonsware.cwac.cam2;

import android.media.MediaScannerConnection;
import java.io.File;
import java.io.FileOutputStream;

/**
 * ImageProcessor that writes a JPEG file out to some form
 * of local storage. At present, it supports writing out to a
 * local filesystem path.
 */
public class JPEGWriter extends AbstractImageProcessor {
  /**
   * Property key to identify the filesystem path where
   * the image should be written. Look up the value for this
   * property in the PictureTransaction.
   */
  public static final String PROP_PATH="path";

  /**
   * Property key to identify if the MediaStore should be
   * updated to reflect the written-out picture (boolean).
   * Look up the value for this property in the PictureTransaction.
   */
  public static final String PROP_UPDATE_MEDIA_STORE="update";

  /**
   * {@inheritDoc}
   */
  @Override
  public void process(PictureTransaction xact, ImageContext imageContext) {
    String path=xact.getProperties().getString(PROP_PATH);
    boolean updateMediaStore=xact
        .getProperties()
        .getBoolean(PROP_UPDATE_MEDIA_STORE, false);

    if (path==null) {
      throw new IllegalArgumentException("Missing PROP_PATH");
    }

    try {
      File f=new File(path);

      f.getParentFile().mkdirs();

      FileOutputStream fos=new FileOutputStream(f);

      fos.write(imageContext.getJpeg());
      fos.flush();
      fos.getFD().sync();
      fos.close();

      if (updateMediaStore) {
        MediaScannerConnection.scanFile(imageContext.getContext(),
            new String[] { path }, new String[] {"image/jpeg"},
            null);
      }
    }
    catch (Exception e) {
      throw new UnsupportedOperationException("Exception when trying to write JPEG", e);
    }
  }
}
