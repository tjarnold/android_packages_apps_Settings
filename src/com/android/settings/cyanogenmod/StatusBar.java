/*
 * Copyright (C) 2012 The CyanogenMod project
 * Copyright (C) 2013 BlackoutROM Project
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

package com.android.settings.cyanogenmod;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.format.DateFormat;
import android.widget.EditText;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.util.Date;

public class StatusBar extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "StatusBar";
    private static final String KEY_SMS_BREATH = "sms_breath";
    private static final String KEY_MISSED_CALL_BREATH = "missed_call_breath";
    private static final String KEY_VOICEMAIL_BREATH = "voicemail_breath";
    private static final String PREF_AM_PM_STYLE = "status_bar_am_pm";
    private static final String PREF_CLOCK_DATE_DISPLAY = "clock_date_display";
    private static final String PREF_CLOCK_DATE_FORMAT = "clock_date_format";
    private static final String PREF_COLOR_PICKER = "clock_color";
    private static final String PREF_CLOCK_DATE_STYLE = "clock_date_style";
    private static final String PREF_ENABLE = "clock_style";
    private static final String STATUS_BAR_BATTERY = "status_bar_battery";
    private static final String STATUS_BAR_CLOCK = "status_bar_show_clock";
    private static final String STATUS_BAR_SIGNAL = "status_bar_signal";
    private static final String STATUS_BAR_BATTERY_SHOW_PERCENT = "status_bar_battery_show_percent";

    public static final int CLOCK_DATE_STYLE_LOWERCASE = 1;
    public static final int CLOCK_DATE_STYLE_UPPERCASE = 2;
    private static final int CUSTOM_CLOCK_DATE_FORMAT_INDEX = 18;
    private static final String STATUS_BAR_STYLE_HIDDEN = "4";
    private static final String STATUS_BAR_STYLE_TEXT = "6";

    private CheckBoxPreference mMissedCallBreath;
    private CheckBoxPreference mSMSBreath;
    private CheckBoxPreference mStatusBarClock;
    private CheckBoxPreference mVoicemailBreath;
    private ColorPickerPreference mColorPicker;
    private ListPreference mClockAmPmStyle;
    private ListPreference mClockDateDisplay;
    private ListPreference mClockDateFormat;
    private ListPreference mClockDateStyle;
    private ListPreference mClockStyle;

    private ListPreference mStatusBarBattery;
    private CheckBoxPreference mStatusBarBatteryShowPercent;
    private ListPreference mStatusBarCmSignal;

    private boolean mCheckPreferences;

    private static final int MENU_RESET = Menu.FIRST;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createCustomView();
    }

    private PreferenceScreen createCustomView() {
        mCheckPreferences = false;
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet != null) {
            prefSet.removeAll();
        }

        addPreferencesFromResource(R.xml.status_bar);
        prefSet = getPreferenceScreen();

        ContentResolver resolver = getActivity().getContentResolver();

        CheckBoxPreference statusBarBrightnessControl = (CheckBoxPreference)
                prefSet.findPreference(Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL);

        mStatusBarBattery = (ListPreference) findPreference(STATUS_BAR_BATTERY);
        mStatusBarBatteryShowPercent =
                (CheckBoxPreference) findPreference(STATUS_BAR_BATTERY_SHOW_PERCENT);
        mStatusBarCmSignal = (ListPreference) prefSet.findPreference(STATUS_BAR_SIGNAL);

        try {
            if (Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
                    == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                statusBarBrightnessControl.setEnabled(false);
                statusBarBrightnessControl.setSummary(R.string.status_bar_toggle_info);
            }
        } catch (SettingNotFoundException e) {
            // Do nothing
        }

        mStatusBarClock = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_CLOCK);
        mStatusBarClock.setChecked((Settings.System.getInt(
                getActivity().getApplicationContext().getContentResolver(),
                Settings.System.STATUS_BAR_CLOCK, 1) == 1));
        mStatusBarClock.setOnPreferenceChangeListener(this);

        mClockDateStyle = (ListPreference) findPreference(PREF_CLOCK_DATE_STYLE);
        mClockDateStyle.setOnPreferenceChangeListener(this);
        mClockDateStyle.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUSBAR_CLOCK_DATE_STYLE, 0)));
        mClockDateStyle.setSummary(mClockDateStyle.getEntry());

        int batteryStyle = Settings.System.getInt(resolver, Settings.System.STATUS_BAR_BATTERY, 0);
        mStatusBarBattery.setValue(String.valueOf(batteryStyle));
        mStatusBarBattery.setSummary(mStatusBarBattery.getEntry());
        mStatusBarBattery.setOnPreferenceChangeListener(this);

        int signalStyle = Settings.System.getInt(resolver, Settings.System.STATUS_BAR_SIGNAL_TEXT, 0);
        mStatusBarCmSignal.setValue(String.valueOf(signalStyle));
        mStatusBarCmSignal.setSummary(mStatusBarCmSignal.getEntry());
        mStatusBarCmSignal.setOnPreferenceChangeListener(this);

        mColorPicker = (ColorPickerPreference) findPreference(PREF_COLOR_PICKER);
        mColorPicker.setOnPreferenceChangeListener(this);
        int intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_CLOCK_COLOR, -2);
        if (intColor == -2) {
            intColor = getResources().getColor(
                    com.android.internal.R.color.white);
            mColorPicker.setSummary(getResources().getString(R.string.color_default));
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mColorPicker.setSummary(hexColor);
        }
        mColorPicker.setNewPreviewColor(intColor);

        if (Utils.isWifiOnly(getActivity())) {
            prefSet.removePreference(mStatusBarCmSignal);
        }

        if (Utils.isTablet(getActivity())) {
            prefSet.removePreference(statusBarBrightnessControl);
        }

        mClockDateDisplay = (ListPreference) findPreference(PREF_CLOCK_DATE_DISPLAY);
        mClockDateDisplay.setOnPreferenceChangeListener(this);
        mClockDateDisplay.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUSBAR_CLOCK_DATE_DISPLAY, 0)));
        mClockDateDisplay.setSummary(mClockDateDisplay.getEntry());

//        boolean mClockDateToggle = Settings.System.getInt(getActivity().getContentResolver(),
//                    Settings.System.STATUSBAR_CLOCK_DATE_DISPLAY, 0) != 0;
//        if (!mClockDateToggle) {
//            mClockDateStyle.setEnabled(false);
//            mClockDateFormat.setEnabled(false);
//        }

        mClockStyle = (ListPreference) findPreference(PREF_ENABLE);
        mClockStyle.setOnPreferenceChangeListener(this);
        mClockStyle.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUSBAR_CLOCK_STYLE, 0)));
        mClockStyle.setSummary(mClockStyle.getEntry());

        mClockAmPmStyle = (ListPreference) prefSet.findPreference(PREF_AM_PM_STYLE);
        mClockAmPmStyle.setOnPreferenceChangeListener(this);
        mClockAmPmStyle.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUSBAR_CLOCK_AM_PM_STYLE, 0)));
        mClockAmPmStyle.setSummary(mClockAmPmStyle.getEntry());

        mClockDateFormat = (ListPreference) findPreference(PREF_CLOCK_DATE_FORMAT);
        mClockDateFormat.setOnPreferenceChangeListener(this);
        if (mClockDateFormat.getValue() == null) {
            mClockDateFormat.setValue("EEE");
        }

        parseClockDateFormats();

//        try {
//            if (Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
//                    Settings.System.TIME_12_24) == 24) {
//                mClockAmPmStyle.setEnabled(false);
//                mClockAmPmStyle.setSummary(R.string.status_bar_am_pm_info);
//            }
//        } catch (SettingNotFoundException e ) {
//        }

        mSMSBreath = (CheckBoxPreference) prefSet.findPreference(KEY_SMS_BREATH);
        mSMSBreath.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.KEY_SMS_BREATH, 0) == 1));

        mMissedCallBreath = (CheckBoxPreference) prefSet.findPreference(KEY_MISSED_CALL_BREATH);
        mMissedCallBreath.setChecked((Settings.System.getInt(resolver,
                Settings.System.KEY_MISSED_CALL_BREATH, 0) == 1));

        mVoicemailBreath = (CheckBoxPreference) prefSet.findPreference(KEY_VOICEMAIL_BREATH);
        mVoicemailBreath.setChecked((Settings.System.getInt(resolver,
                Settings.System.KEY_VOICEMAIL_BREATH, 0) == 1));

        enableStatusBarBatteryDependents(mStatusBarBattery.getValue());

        setHasOptionsMenu(true);
        mCheckPreferences = true;
        return prefSet;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!mCheckPreferences) {
            return false;
        }
        AlertDialog dialog;

        if (preference == mStatusBarBattery) {
            int batteryStyle = Integer.valueOf((String) newValue);
            int index = mStatusBarBattery.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY, batteryStyle);
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[index]);

            enableStatusBarBatteryDependents((String)newValue);
            return true;
        } else if (preference == mStatusBarCmSignal) {
            int signalStyle = Integer.valueOf((String) newValue);
            int index = mStatusBarCmSignal.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_SIGNAL_TEXT, signalStyle);
            mStatusBarCmSignal.setSummary(mStatusBarCmSignal.getEntries()[index]);
            return true;
        } else if (preference == mClockAmPmStyle) {
            int val = Integer.parseInt((String) newValue);
            int index = mClockAmPmStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_CLOCK_AM_PM_STYLE, val);
            mClockAmPmStyle.setSummary(mClockAmPmStyle.getEntries()[index]);
            return true;
        } else if (preference == mClockStyle) {
            int val = Integer.parseInt((String) newValue);
            int index = mClockStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_CLOCK_STYLE, val);
            mClockStyle.setSummary(mClockStyle.getEntries()[index]);
            return true;
        } else if (preference == mClockDateStyle) {
            int val = Integer.parseInt((String) newValue);
            int index = mClockDateStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_CLOCK_DATE_STYLE, val);
            mClockDateStyle.setSummary(mClockDateStyle.getEntries()[index]);
            parseClockDateFormats();
            return true;
        } else if (preference == mClockDateDisplay) {
            int val = Integer.parseInt((String) newValue);
            int index = mClockDateDisplay.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_CLOCK_DATE_DISPLAY, val);
            mClockDateDisplay.setSummary(mClockDateDisplay.getEntries()[index]);
            if (val == 0) {
                mClockDateStyle.setEnabled(false);
                mClockDateFormat.setEnabled(false);
            } else {
                mClockDateStyle.setEnabled(true);
                mClockDateFormat.setEnabled(true);
            }
            return true;
        } else if (preference == mStatusBarClock) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_CLOCK,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_CLOCK_COLOR, intHex);
            return true;
        } else if (preference == mClockDateFormat) {
            int index = mClockDateFormat.findIndexOfValue((String) newValue);

            if (index == CUSTOM_CLOCK_DATE_FORMAT_INDEX) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setTitle(R.string.clock_date_string_edittext_title);
                alert.setMessage(R.string.clock_date_string_edittext_summary);

                final EditText input = new EditText(getActivity());
                String oldText = Settings.System.getString(
                    getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_CLOCK_DATE_FORMAT);
                if (oldText != null) {
                    input.setText(oldText);
                }
                alert.setView(input);

                alert.setPositiveButton(R.string.menu_save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int whichButton) {
                        String value = input.getText().toString();
                        if (value.equals("")) {
                            return;
                        }
                        Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.STATUSBAR_CLOCK_DATE_FORMAT, value);

                        return;
                    }
                });

                alert.setNegativeButton(R.string.menu_cancel,
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int which) {
                        return;
                    }
                });
                dialog = alert.create();
                dialog.show();
            } else {
                if ((String) newValue != null) {
                    Settings.System.putString(getActivity().getContentResolver(),
                        Settings.System.STATUSBAR_CLOCK_DATE_FORMAT, (String) newValue);
                }
            }
            return true;
        }
        return false;
    }

    private void enableStatusBarBatteryDependents(String value) {
        boolean enabled = !(value.equals(STATUS_BAR_STYLE_TEXT)
                || value.equals(STATUS_BAR_STYLE_HIDDEN));
        mStatusBarBatteryShowPercent.setEnabled(enabled);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getContentResolver();
        boolean value;
        if (preference == mSMSBreath) {
            value = mSMSBreath.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.KEY_SMS_BREATH, value ? 1 : 0);
            return true;
        } else if (preference == mMissedCallBreath) {
            value = mMissedCallBreath.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.KEY_MISSED_CALL_BREATH, value ? 1 : 0);
            return true;
        } else if (preference == mVoicemailBreath) {
            value = mVoicemailBreath.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.KEY_VOICEMAIL_BREATH, value ? 1 : 0);
            return true;
        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefault();
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetToDefault() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.reset);
        alertDialog.setMessage(R.string.status_bar_clock_style_reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.STATUSBAR_CLOCK_COLOR, -2);
                createCustomView();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    private void parseClockDateFormats() {
        // Parse and repopulate mClockDateFormats's entries based on current date.
        String[] dateEntries = getResources().getStringArray(R.array.clock_date_format_entries_values);
        CharSequence parsedDateEntries[];
        parsedDateEntries = new String[dateEntries.length];
        Date now = new Date();

        int lastEntry = dateEntries.length - 1;
        int dateFormat = Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUSBAR_CLOCK_DATE_STYLE, 2);
        for (int i = 0; i < dateEntries.length; i++) {
            if (i == lastEntry) {
                parsedDateEntries[i] = dateEntries[i];
            } else {
                String newDate;
                CharSequence dateString = DateFormat.format(dateEntries[i], now);
                if (dateFormat == CLOCK_DATE_STYLE_LOWERCASE) {
                    newDate = dateString.toString().toLowerCase();
                } else if (dateFormat == CLOCK_DATE_STYLE_UPPERCASE) {
                    newDate = dateString.toString().toUpperCase();
                } else {
                    newDate = dateString.toString();
                }

                parsedDateEntries[i] = newDate;
            }
        }
        mClockDateFormat.setEntries(parsedDateEntries);
    }
}
