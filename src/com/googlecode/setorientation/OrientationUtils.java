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

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import java.util.List;

/**
 * Utility methods for Set Orientation.
 */
public class OrientationUtils {
    /**
     * Return the foreground activity at the specified {@code index}, or
     * {@code null} if none.
     * <p>
     * This method may not be accurate on devices that can show more than one
     * foreground window, e.g. the Samsung Galaxy Note 2.
     *
     * @param am An instance of the activity manager.
     * @param index The index of the task to return. The top-level activity is
     *            index {@code 0}, the activity below that is index {@code 1}.
     * @return The activity at the specified index, or {@code null} if none.
     */
    public static ComponentName getForegroundActivity(ActivityManager am, int index) {
        final List<RunningTaskInfo> tasks = am.getRunningTasks(index + 1);
        if (tasks.size() < (index + 1)) {
            return null;
        }

        return tasks.get(index).topActivity;
    }

    /**
     * Return the activity name for a component, or {@code null} on error.
     *
     * @param pm An instance of the package manager.
     * @param component The component to return the activity name for.
     * @return The name of the component's activity, or {@code null} on error.
     */
    public static CharSequence getActivityName(PackageManager pm, ComponentName component) {
        final ActivityInfo activity;

        try {
            activity = pm.getActivityInfo(component, 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        return activity.loadLabel(pm);
    }

    /**
     * Return the app name for a component, or {@code null} on error.
     *
     * @param pm An instance of the package manager.
     * @param component The component to return the app name for.
     * @return The name of the component's app, or {@code null} on error.
     */
    public static CharSequence getApplicationName(PackageManager pm, ComponentName component) {
        final ApplicationInfo application;

        try {
            final String packageName = component.getPackageName();
            application = pm.getApplicationInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        return application.loadLabel(pm);
    }

    /**
     * Returns {@code true} if two {@link Object}s are equal using either
     * {@code ==} comparison or {@code Object#equals(Object)} comparison from
     * the first argument.
     * <p>
     * This method is safe to call with {@code null} arguments.
     *
     * @param a The first object.
     * @param b The second object.
     * @return {@code true} if the objects are equal.
     */
    public static boolean equals(Object a, Object b) {
        return ((a == b) || ((a != null) && a.equals(b)));
    }
}
