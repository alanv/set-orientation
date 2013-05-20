/*
 * Copyright (C) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.googlecode.setorientation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Catches the boot up event and automatically starts the
 * {@link OrientationService}
 *
 * @author caseyburkhardt@google.com (Casey Burkhardt)
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final OrientationPrefsManager prefsManager = new OrientationPrefsManager(context);
        if (!prefsManager.getServiceEnabled()) {
            return;
        }

        final ScreenOrientation defaultOrientation = prefsManager.getDefaultRule();
        final Intent service = new Intent(context, OrientationService.class);
        service.putExtra(OrientationService.EXTRA_ORIENTATION, defaultOrientation.getCode());
        context.startService(service);
    }
}
