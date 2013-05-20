/*
 * Copyright (C) 2012 Google Inc.
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

package com.googlecode.eyesfree.compat.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;

import com.googlecode.eyesfree.compat.CompatUtils;

import java.lang.reflect.Method;

/**
 * Static utility methods for interacting with services in a
 * backwards-compatible manner.
 */
public class ServiceCompatUtils {
    private static final Class<?> CLASS_Service = Service.class;
    private static final Method METHOD_setForeground = CompatUtils.getMethod(
            CLASS_Service, "setForeground", boolean.class);
    private static final Method METHOD_startForeground = CompatUtils.getMethod(
            CLASS_Service, "startForeground", int.class, Notification.class);
    private static final Method METHOD_stopForeground = CompatUtils.getMethod(
            CLASS_Service, "stopForeground", boolean.class);

    /**
     * Make this service run in the foreground, supplying the ongoing
     * notification to be shown to the user while in this state. By default
     * services are background, meaning that if the system needs to kill them to
     * reclaim more memory (such as to display a large page in a web browser),
     * they can be killed without too much harm. You can set this flag if
     * killing your service would be disruptive to the user, such as if your
     * service is performing background music playback, so the user would notice
     * if their music stopped playing.
     * <p>
     * This method automatically handles compatibility with platform versions
     * prior to API level 5 by calling the older setForeground() method and
     * managing the display of the notification as appropriate.
     *
     * @param receiver The service to run in the foreground.
     * @param id The identifier for this notification as per
     *            {@link NotificationManager#notify(int, Notification)
     *            NotificationManager.notify(int, Notification)}.
     * @param notification The Notification to be displayed.
     * @see #stopForeground(Service, int, boolean)
     */
    public static final void startForeground(Service receiver, int id, Notification notification) {
        if (METHOD_startForeground != null) {
            CompatUtils.invoke(receiver, null, METHOD_startForeground, id, notification);
            return;
        }

        CompatUtils.invoke(receiver, null, METHOD_setForeground, true);

        final NotificationManager notificationManager = (NotificationManager) receiver
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
    }

    /**
     * Remove this service from foreground state, allowing it to be killed if
     * more memory is needed.
     * <p>
     * This method automatically handles compatibility with platform versions
     * prior to API level 5 by calling the older setForeground() method and
     * managing the display of the notification as appropriate.
     *
     * @param receiver The service to remove from the foreground state.
     * @param id The identifier for this notification as per
     *            {@link NotificationManager#notify(int, Notification)
     *            NotificationManager.notify(int, Notification)}.
     * @param removeNotification If true, the notification previously provided
     *            to {@link #startForeground} will be removed. Otherwise it will
     *            remain until a later call removes it (or the service is
     *            destroyed).
     * @see #startForeground(Service, int, Notification)
     */
    public static final void stopForeground(Service receiver, int id, boolean removeNotification) {
        if (METHOD_stopForeground != null) {
            CompatUtils.invoke(receiver, null, METHOD_stopForeground, removeNotification);
            return;
        }

        if (removeNotification) {
            final NotificationManager notificationManager = (NotificationManager) receiver
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(id);
        }

        CompatUtils.invoke(receiver, METHOD_setForeground, METHOD_setForeground, false);
    }
}
