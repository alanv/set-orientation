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

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.util.HashMap;
import java.util.Map.Entry;

public class CustomRuleManager {
    /** The preferences file in which to store the custom rules. */
    private static final String PREFS_FILE = "com.googlecode.eyesfree.setorientation_ruleprefs";

    /** The preference key in which to store the custom rules. */
    private static final String PREF_COMPONENT_RULES = "component_rules";

    /** Map of package and component names to orientations. */
    private final HashMap<String, ScreenOrientation>
            mComponentRules = new HashMap<String, ScreenOrientation>();

    /** Shared preferences, used to persist rules. */
    private final SharedPreferences mPrefs;

    /** Listener used to send callbacks when rules are modified. */
    private RuleListener mListener;

    /**
     * Constructs a new custom rule manager for the given context.
     *
     * @param context The parent context.
     */
    public CustomRuleManager(Context context) {
        mPrefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        mPrefs.registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);

        loadComponentRules();
    }

    /**
     * Sets a listener to receive rule change callbacks.
     *
     * @param listener The listener to set.
     */
    public void setListener(RuleListener listener) {
        mListener = listener;
    }

    /**
     * Returns whether a custom rule has been set for the specified component.
     *
     * @param component The component to check.
     * @return Whether a custom rule has been set for the specified component.
     */
    public boolean hasRuleForComponent(ComponentName component) {
        return (getRuleForComponent(component) != null);
    }

    /**
     * @return Whether any custom rules have been set.
     */
    public boolean hasComponentRules() {
        return mComponentRules.isEmpty();
    }

    /**
     * Sets a custom rule for the specified package.
     *
     * @param component The component from which to obtain the package.
     * @param orientation The custom rule to set.
     */
    public void setRuleForPackage(ComponentName component, ScreenOrientation orientation) {
        storeRule(component.getPackageName(), orientation);
    }

    /**
     * Sets a custom rule for the specified component, which includes package
     * and class names.
     *
     * @param component The component for which the rule should be set.
     * @param orientation The custom rule to set.
     */
    public void setRuleForComponent(ComponentName component, ScreenOrientation orientation) {
        storeRule(component.flattenToShortString(), orientation);
    }

    /**
     * Sets a custom rule for the specified filter.
     *
     * @param filter A component filter, either a package name or full component
     *            name obtained from {@link ComponentName#flattenToString()}.
     * @param orientation The custom rule to set.
     */
    private void storeRule(String filter, ScreenOrientation orientation) {
        if (orientation == null) {
            mComponentRules.remove(filter);
        } else {
            mComponentRules.put(filter, orientation);
        }

        commitToPreference();
    }

    /**
     * Returns the most specific rule that applies to the specified component,
     * or {@code null} if no rules apply.
     *
     * @param component The component for which to obtain a rule.
     * @return The most specific rule that applies to the specified component,
     *         or {@code null} if no rules apply.
     */
    public ScreenOrientation getRuleForComponent(ComponentName component) {
        if (component == null) {
            return null;
        }

        // First preference goes to rules that specify a package and activity.
        final ScreenOrientation forActivity = mComponentRules.get(component.flattenToShortString());
        if (forActivity != null) {
            return forActivity;
        }

        // Second preference goes to rules that only specify a package.
        final ScreenOrientation forPackage = mComponentRules.get(component.getPackageName());
        if (forPackage != null) {
            return forPackage;
        }

        return null;
    }

    /**
     * Loads custom rules from preferences.
     */
    private void loadComponentRules() {
        mComponentRules.clear();

        // TODO(alanv): Consider moving this to an SQLite database.
        final String rulePref = mPrefs.getString(PREF_COMPONENT_RULES, null);
        if (rulePref == null) {
            // No preference set, this must be a new installation.
            loadDefaultRules();
            return;
        }

        final String[] rules = rulePref.split("[\n,]");

        try {
            for (int i = 0; i < (rules.length - 1); i += 2) {
                final String component = rules[i];
                final int code = Integer.parseInt(rules[i + 1]);
                final ScreenOrientation orientation = ScreenOrientation.fromCode(code);

                mComponentRules.put(component, orientation);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();

            // Rules are broken, overwrite with defaults.
            loadDefaultRules();
            return;
        }

        if (mListener != null) {
            mListener.onRulesChanged();
        }
    }

    /**
     * Loads default rules and commits them to preferences.
     */
    private void loadDefaultRules() {
        mComponentRules.clear();

        mComponentRules.put("com.android.camera", ScreenOrientation.UNSPECIFIED);
        mComponentRules.put("com.google.android.gallery3d/com.android.camera.CameraLauncher",
                ScreenOrientation.UNSPECIFIED);

        commitToPreference();

        if (mListener != null) {
            mListener.onRulesChanged();
        }
    }

    /**
     * Commits all loaded rules to preferences.
     */
    private void commitToPreference() {
        final StringBuffer rules = new StringBuffer();

        for (Entry<String, ScreenOrientation> entry : mComponentRules.entrySet()) {
            rules.append(entry.getKey());
            rules.append(",");
            rules.append(entry.getValue().getCode());
            rules.append("\n");
        }

        final Editor editor = mPrefs.edit();
        editor.putString(PREF_COMPONENT_RULES, rules.toString());
        editor.commit();
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener
            mPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(
                        SharedPreferences sharedPreferences, String key) {
                    loadComponentRules();
                }
            };

    /**
     * Listener for changes in custom rules.
     */
    public interface RuleListener {
        /**
         * Called when custom rules are changed.
         */
        public void onRulesChanged();
    }
}
