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

public class CameraSelectionCriteria {
  private Facing facing;

  public Facing getFacing() {
    return(facing);
  }

  public static class Builder {
    final private CameraSelectionCriteria criteria=new CameraSelectionCriteria();

    public Builder facing(Facing facing) {
      criteria.facing=facing;

      return(this);
    }

    public CameraSelectionCriteria build() {
      return(criteria);
    }
  }

  public static enum Facing {
    FRONT, BACK, ANY;
  }
}
