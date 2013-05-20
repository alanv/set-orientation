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

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import com.googlecode.eyesfree.compat.app.ServiceCompatUtils;
import com.googlecode.eyesfree.setorientation.R;
import com.googlecode.eyesfree.utils.WeakReferenceHandler;
import com.googlecode.setorientation.CustomRuleManager.RuleListener;

public class OrientationService extends Service {
    /** Extra used to specify the orientation to apply. */
    public static final String EXTRA_ORIENTATION = "orientation";

    /** Identifier for the service's ongoing notification. */
    private static final int NOTIFICATION_ID = 1;

    /** Intent filter used to listen for screen on/off. */
    private static final IntentFilter SCREEN_FILTER = new IntentFilter();

    static {
        SCREEN_FILTER.addAction(Intent.ACTION_SCREEN_ON);
        SCREEN_FILTER.addAction(Intent.ACTION_SCREEN_OFF);
    }

    private OrientationPrefsManager mPrefsManager;
    private CustomRuleManager mRuleManager;
    private ActivityPoller mActivityPoller;
    private WindowManager mWindowManager;
    private LayoutParams mLayoutParams;
    private View mOverlayView;
    private NotificationCompat.Builder mNotification;

    /** The currently applied screen orientation. */
    private ScreenOrientation mCurrentOrientation;

    /** Whether the current orientation is the result of a custom rule. */
    private boolean mUsingCustomRule;

    /** Whether the overlay view has been added to the window manager. */
    private boolean mViewAdded;

    @SuppressLint("InlinedApi")
    @Override
    public void onCreate() {
        super.onCreate();

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mPrefsManager = new OrientationPrefsManager(this);

        mRuleManager = new CustomRuleManager(this);
        mRuleManager.setListener(mRuleListener);

        mActivityPoller = new ActivityPoller(this);
        mOverlayView = new View(this);

        mLayoutParams = new LayoutParams();
        mLayoutParams.type = LayoutParams.TYPE_SYSTEM_ERROR;
        mLayoutParams.width = 0;
        mLayoutParams.height = 0;
        mLayoutParams.flags |= LayoutParams.FLAG_HARDWARE_ACCELERATED;
        mLayoutParams.flags |= LayoutParams.FLAG_NOT_FOCUSABLE;
        mLayoutParams.flags |= LayoutParams.FLAG_NOT_TOUCHABLE;
        mLayoutParams.flags &= ~LayoutParams.FLAG_TURN_SCREEN_ON;
        mLayoutParams.flags &= ~LayoutParams.FLAG_KEEP_SCREEN_ON;

        final PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, SetOrientationActivity.class), Intent.FLAG_ACTIVITY_NEW_TASK);

        mNotification = new NotificationCompat.Builder(this).setContentIntent(contentIntent)
                .setWhen(0).setOngoing(true);

        registerReceiver(mScreenReceiver, SCREEN_FILTER);
        requestPolling(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Do nothing.
        return null;
    }

    @Override
    public void onDestroy() {
        if (mViewAdded) {
            mWindowManager.removeView(mOverlayView);
            mViewAdded = false;
        }

        unregisterReceiver(mScreenReceiver);
        requestPolling(false);

        ServiceCompatUtils.stopForeground(this, NOTIFICATION_ID, true);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onStart(Intent intent, int startId) {
        handleCommand(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand(intent);

        return START_REDELIVER_INTENT;
    }

    /**
     * Toggles polling of the top-level activity. If no custom rules are loaded,
     * polling will not start.
     *
     * @param enabled Whether polling should be enabled.
     */
    private void requestPolling(boolean enabled) {
        if (enabled && !mRuleManager.hasComponentRules()) {
            mActivityPoller.startPolling();
        } else {
            mActivityPoller.stopPolling();
        }
    }

    /**
     * Handles changing orientation based on the top-level activity.
     *
     * @param component The component for the top-level activity.
     */
    private void onActivityChanged(ComponentName component) {
        final ScreenOrientation desiredOrientation;

        if (mRuleManager.hasRuleForComponent(component)) {
            mUsingCustomRule = true;
            desiredOrientation = mRuleManager.getRuleForComponent(component);
        } else {
            mUsingCustomRule = false;
            desiredOrientation = mPrefsManager.getDefaultRule();
        }

        setOrientation(desiredOrientation);
    }

    /**
     * Handles commands send to the service via {@link #onStart} and
     * {@link #onStartCommand}.
     *
     * @param intent The command intent.
     */
    private void handleCommand(Intent intent) {
        if (intent == null) {
            return;
        }

        if (intent.hasExtra(EXTRA_ORIENTATION)) {
            final int orientationCode = intent.getIntExtra(EXTRA_ORIENTATION, 0);
            final ScreenOrientation orientation = ScreenOrientation.fromCode(orientationCode);
            setOrientation(orientation);
        }
    }

    /**
     * Sets the current orientation.
     *
     * @param orientation The orientation to set.
     */
    private void setOrientation(ScreenOrientation orientation) {
        if (mCurrentOrientation == orientation) {
            return;
        }

        mCurrentOrientation = orientation;
        mLayoutParams.screenOrientation = orientation.getCode();

        if (mViewAdded) {
            mWindowManager.updateViewLayout(mOverlayView, mLayoutParams);
        } else {
            mWindowManager.addView(mOverlayView, mLayoutParams);
            mViewAdded = true;
        }

        updateNotification();
    }

    /**
     * Updates the service's ongoing notification to reflect the current
     * orientation.
     */
    private void updateNotification() {
        final String orientationLabel = mCurrentOrientation.getString(this);
        final String contentTitle = getString(R.string.orientation_set_to, orientationLabel);
        final String contentText = getString(R.string.select_to_change);
        final int smallIcon = (mUsingCustomRule ? R.drawable.ic_stat_orientation_filled
                : R.drawable.ic_stat_orientation);

        mNotification.setContentTitle(contentTitle)
                .setContentText(contentText).setSmallIcon(smallIcon);

        ServiceCompatUtils.startForeground(this, NOTIFICATION_ID, mNotification.build());
    }

    /**
     * Handles changing the polling state when the screen state changes.
     */
    private final BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                requestPolling(true);
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                requestPolling(false);
            }
        }
    };

    /**
     * Handles changes in rules. Forces the activity poller to clear the most
     * recent foreground activity and start polling, which may result in a rule
     * being applied.
     */
    private final RuleListener mRuleListener = new RuleListener() {
        @Override
        public void onRulesChanged() {
            mActivityPoller.reset();
            mActivityPoller.startPolling();
        }
    };

    /**
     * Polls for the current top-level activity.
     */
    private static class ActivityPoller extends WeakReferenceHandler<OrientationService> {
        private static final int POLL_FOREGROUND = 1;
        private static final long POLL_INTERVAL = 250;

        /** The parent's package name, used to prevent self-checking. */
        private final String mParentPackage;

        /** The component for the most recent foreground activity. */
        private ComponentName mPreviousComponent = null;

        /** Whether this poller is currently active. */
        private boolean mPolling = false;

        public ActivityPoller(OrientationService parent) {
            super(parent);

            mParentPackage = parent.getPackageName();
        }

        /**
         * Start polling. No-op if already polling.
         */
        public void startPolling() {
            if (mPolling) {
                return;
            }

            mPolling = true;

            removeMessages(POLL_FOREGROUND);
            sendEmptyMessage(POLL_FOREGROUND);
        }

        /**
         * Stop polling. No-op if not currently polling.
         */
        public void stopPolling() {
            if (!mPolling) {
                return;
            }

            mPolling = false;

            removeMessages(POLL_FOREGROUND);
        }

        /**
         * Resets the previous component. If the poller is active, this will
         * trigger a call to {@link OrientationService#onActivityChanged} on the
         * next poll event.
         */
        public void reset() {
            mPreviousComponent = null;
        }

        @Override
        protected void handleMessage(Message msg, OrientationService parent) {
            switch (msg.what) {
                case POLL_FOREGROUND:
                    poll(parent);
                    break;
            }
        }

        /**
         * Polls for changes in the foreground activity and calls
         * {@link OrientationService#onActivityChanged} if necessary.
         *
         * @param parent The parent service.
         */
        private void poll(OrientationService parent) {
            if (!mPolling) {
                return;
            }

            final ActivityManager am = (ActivityManager) parent.getSystemService(ACTIVITY_SERVICE);
            final ComponentName cmp = OrientationUtils.getForegroundActivity(am, 0);

            // Don't consider any activities within the parent package.
            if (!mParentPackage.equals(cmp.getPackageName())
                    && !OrientationUtils.equals(mPreviousComponent, cmp)) {
                parent.onActivityChanged(cmp);
                mPreviousComponent = cmp;
            }

            sendEmptyMessageDelayed(1, POLL_INTERVAL);
        }
    }
}
