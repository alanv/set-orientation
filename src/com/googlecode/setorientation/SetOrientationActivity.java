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

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.googlecode.eyesfree.setorientation.R;

public class SetOrientationActivity extends Activity {
    private OrientationPrefsManager mPrefsManager;
    private CustomRuleManager mRuleManager;
    private ScreenOrientationAdapter mAdapter;
    private Spinner mSpinner;
    private CompoundButton mServiceEnabled;
    private CheckBox mApplyToApp;
    private ComponentName mForegroundActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.set_orientation_dialog);

        mPrefsManager = new OrientationPrefsManager(this);
        mRuleManager = new CustomRuleManager(this);
        mAdapter = new ScreenOrientationAdapter(this, android.R.layout.simple_dropdown_item_1line);

        final PackageManager pm = getPackageManager();
        final ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        mForegroundActivity = OrientationUtils.getForegroundActivity(am, 1);

        final boolean hasCustomRule = mRuleManager.hasRuleForComponent(mForegroundActivity);
        final ScreenOrientation orientation;
        if (hasCustomRule) {
            orientation = mRuleManager.getRuleForComponent(mForegroundActivity);
        } else {
            orientation = mPrefsManager.getDefaultRule();
        }

        mServiceEnabled = (CompoundButton) findViewById(R.id.service_enabled);
        mServiceEnabled.setChecked(mPrefsManager.getServiceEnabled());
        mServiceEnabled.setOnCheckedChangeListener(mOnCheckedChangeListener);

        final CharSequence appName = OrientationUtils.getApplicationName(pm, mForegroundActivity);
        mApplyToApp = (CheckBox) findViewById(R.id.scope_app);
        mApplyToApp.setText(getString(R.string.label_scope_app, appName));
        mApplyToApp.setChecked(hasCustomRule);

        final int spinnerPosition = mAdapter.getPosition(orientation);
        mSpinner = (Spinner) findViewById(android.R.id.list);
        mSpinner.setAdapter(mAdapter);
        mSpinner.setSelection(spinnerPosition);
        mSpinner.setOnItemSelectedListener(mOnItemSelectedListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        saveSelectedOrientation();
    }

    private void applySelectedOrientation() {
        final Intent service = new Intent(this, OrientationService.class);

        final boolean serviceEnabled = mServiceEnabled.isChecked();
        if (!serviceEnabled) {
            stopService(service);
            return;
        }

        final ScreenOrientation selection = (ScreenOrientation) mSpinner.getSelectedItem();
        if (selection == null) {
            return;
        }

        service.putExtra(OrientationService.EXTRA_ORIENTATION, selection.getCode());
        startService(service);
    }

    private void saveSelectedOrientation() {
        final boolean enabled = mServiceEnabled.isChecked();
        mPrefsManager.setServiceEnabled(enabled);

        final ScreenOrientation selection = (ScreenOrientation) mSpinner.getSelectedItem();
        if (selection == null) {
            return;
        }

        final ComponentName component = mForegroundActivity;
        final boolean applyToApp = mApplyToApp.isChecked();
        if (applyToApp) {
            mRuleManager.setRuleForPackage(component, selection);
        } else {
            mRuleManager.setRuleForPackage(component, null);
            mPrefsManager.setDefaultRule(selection);
        }
    }

    private final OnCheckedChangeListener mOnCheckedChangeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            applySelectedOrientation();
        }
    };

    private final OnItemSelectedListener mOnItemSelectedListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            applySelectedOrientation();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    };

    private static class ScreenOrientationAdapter extends ArrayAdapter<ScreenOrientation> {
        public ScreenOrientationAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId, ScreenOrientation.supportedValues());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view = super.getView(position, convertView, parent);
            updateViewText(position, view);
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            final View view = super.getDropDownView(position, convertView, parent);
            updateViewText(position, view);
            return view;
        }

        private void updateViewText(int position, final View view) {
            if (view == null) {
                return;
            }

            final ScreenOrientation orientation = getItem(position);
            final TextView text = (TextView) view.findViewById(android.R.id.text1);
            text.setText(orientation.getString(getContext()));
        }
    }
}
