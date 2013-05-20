/*
 * Copyright (C) 2013 Google Inc.
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

import android.content.Context;
import android.content.SharedPreferences;

public class OrientationPrefsManager {
    /** The preferences file in which to store the custom rules. */
    private static final String PREFS_FILE = "com.googlecode.eyesfree.setorientation_orientprefs";
    private static final String PREF_SERVICE_ENABLED = "service_enabled";
    private static final String PREF_DEFAULT_RULE = "orientation";

    /** Shared preferences, used to persist rules. */
    private final SharedPreferences mPrefs;

    public OrientationPrefsManager(Context context) {
        mPrefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    }

    /**
     * Sets the service enabled state and commits to preferences.
     *
     * @param enabled The desired service enabled state.
     */
    public void setServiceEnabled(boolean enabled) {
        final SharedPreferences.Editor mPrefsEditor = mPrefs.edit();
        mPrefsEditor.putBoolean(PREF_SERVICE_ENABLED, enabled);
        mPrefsEditor.commit();
    }

    /**
     * Returns the service enabled state from preferences.
     *
     * @return The desired service enabled state.
     */
    public boolean getServiceEnabled() {
        return mPrefs.getBoolean(PREF_SERVICE_ENABLED, false);
    }

    /**
     * Sets the default orientation rule and commits to preferences.
     *
     * @param orientation The desired default orientation.
     */
    public void setDefaultRule(ScreenOrientation orientation) {
        final SharedPreferences.Editor mPrefsEditor = mPrefs.edit();
        mPrefsEditor.putInt(PREF_DEFAULT_RULE, orientation.getCode());
        mPrefsEditor.commit();
    }

    /**
     * Returns the default orientation rule from preferences.
     *
     * @return The default orientation preference value.
     */
    public ScreenOrientation getDefaultRule() {
        final int code = mPrefs.getInt(PREF_DEFAULT_RULE, ScreenOrientation.UNSPECIFIED.getCode());
        final ScreenOrientation orientation = ScreenOrientation.fromCode(code);
        return orientation;
    }
}
