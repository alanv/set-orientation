/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.setorientation;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;

import com.googlecode.eyesfree.setorientation.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum ScreenOrientation {
    UNSPECIFIED(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, R.string.unspecified, 1),
    SENSOR(ActivityInfo.SCREEN_ORIENTATION_SENSOR, R.string.sensor, 1),
    SENSOR_FULL(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR, R.string.sensor_full, 9),
    LANDSCAPE(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, R.string.landscape, 1),
    LANDSCAPE_REVERSE(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE, R.string.landscape_reverse, 9),
    LANDSCAPE_SENSOR(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, R.string.landscape_sensor, 9),
    PORTRAIT(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, R.string.portrait, 1),
    PORTRAIT_REVERSE(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT, R.string.portrait_reverse, 9),
    PORTRAIT_SENSOR(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, R.string.portrait_sensor, 9);

    static {
        final ScreenOrientation[] values = values();
        final List<ScreenOrientation> supportedValues = new ArrayList<ScreenOrientation>();

        for (ScreenOrientation value : values) {
            if (Build.VERSION.SDK_INT >= value.mMinimumSdk) {
                supportedValues.add(value);
            }
        }

        sSupportedValues = Collections.unmodifiableList(supportedValues);
    }

    private static final List<ScreenOrientation> sSupportedValues;

    private final int mCode;
    private final int mResId;
    private final int mMinimumSdk;

    private ScreenOrientation(int code, int resId, int minimumSdk) {
        mCode = code;
        mResId = resId;
        mMinimumSdk = minimumSdk;
    }

    public static ScreenOrientation fromCode(int orientation) {
        for (ScreenOrientation value : values()) {
            if (value.mCode == orientation) {
                return value;
            }
        }

        return UNSPECIFIED;
    }

    public static List<ScreenOrientation> supportedValues() {
        return sSupportedValues;
    }

    public int getCode() {
        return mCode;
    }

    public String getString(Context context) {
        return context.getString(mResId);
    }
}
