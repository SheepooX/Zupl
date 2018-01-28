package com.sheepoox.tablo.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.gson.Gson;
import com.sheepoox.tablo.R;
import com.sheepoox.tablo.fetcher.Lesson;
import com.sheepoox.tablo.fetcher.TableData;
import com.sheepoox.tablo.fetcher.TableTablo;

import java.util.HashSet;

/**
 * Created by Tomáš Černý
 * Project Zupl, 2017
 */

public class CustomPreferenceActivity extends PreferenceActivity {

    // All downloaded tables.
    private TableData[] allTables;
    // Selected classes' table.
    private TableData currentTable;

    // Dialog that pops up while downloading tables
    private ProgressDialog dialog;

    // Preferences
    private ListPreference cls;
    private Preference notification;
    private Preference notificationTime;
    private Preference group;
    private Preference dlTable;
    private Preference subject;

    // Background task
    private TableFetcher fetcher;

    // Initializes this activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences tablePrefs = getSharedPreferences(getResources().getString(R.string.category_tables), Context.MODE_PRIVATE);
        Gson gson = new Gson();

        cls = (ListPreference) findPreference(getResources().getString(R.string.key_pref_general_class));
        notification = findPreference(getResources().getString(R.string.key_pref_notifications_next_lesson));
        notificationTime = findPreference(getResources().getString(R.string.key_pref_notifications_time));
        group = findPreference(getResources().getString(R.string.key_pref_groups_starter));
        dlTable = findPreference(getResources().getString(R.string.key_pref_dl_starter));
        subject = findPreference(getResources().getString(R.string.key_pref_general_subject));
		
		// Called when subject (student or teacher) is selected
        subject.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference pref, Object newValue) {
				// Set the subtext of the Preference
                pref.setSummary(newValue.toString());
				// Settings folders and their editors
                SharedPreferences.Editor prefsEditor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                SharedPreferences.Editor tablePrefsEditor = getSharedPreferences(getResources().getString(R.string.category_tables), Context.MODE_PRIVATE).edit();
                SharedPreferences tablePrefs = getSharedPreferences(getResources().getString(R.string.category_tables), Context.MODE_PRIVATE);
                // Deletes old settings
				for (String key : tablePrefs.getAll().keySet()) {
                    tablePrefsEditor.remove(key);
                }
                tablePrefsEditor.apply();
                SubsService.unsetLessonNotificationsInBackground(getApplicationContext());
                prefsEditor.remove(getResources().getString(R.string.key_pref_general_class));
                cls.setSummary(getResources().getString(R.string.class_not_selected));
                cls.setEnabled(false);
                group.setEnabled(false);
                if (newValue.toString().equals(getResources().getString(R.string.pupil))) {
                    prefsEditor.putString(getResources().getString(R.string.key_pref_other_table_url), getResources().getString(R.string.pref_table_default));
                } else {
                    prefsEditor.putString(getResources().getString(R.string.key_pref_other_table_url), getResources().getString(R.string.pref_table_default2));
                }
				// Downloads table if the device is connected
                if (MainActivity.isDeviceOnline(getApplicationContext())) {
                    downloadTable();
                }
                prefsEditor.apply();
                return true;
            }
        });
		
		// Sets Preference properties
        cls.setSummary(prefs.getString(getResources().getString(R.string.key_pref_general_class), getResources().getString(R.string.class_not_selected)));
        subject.setSummary(prefs.getString(getResources().getString(R.string.key_pref_general_subject), getResources().getString(R.string.class_not_selected)));
        group.setEnabled(!cls.getSummary().equals(getResources().getString(R.string.class_not_selected)));
        dlTable.setEnabled(!subject.getSummary().equals(getResources().getString(R.string.class_not_selected)));
		notificationTime.setSummary(getResources()
                .getStringArray(R.array.list_preference_notification_times_titles)[Integer.valueOf(prefs.getString(getResources().getString(R.string.key_pref_notifications_time), "0"))]);

        if (tablePrefs.contains(getResources().getString(R.string.ct_all_tables))) {
            setClassPicker(gson.fromJson(tablePrefs.getString(getResources().getString(R.string.ct_classes), null), String[].class));
            allTables = gson.fromJson(tablePrefs.getString(getResources().getString(R.string.ct_all_tables), null), TableData[].class);
        }

        if (tablePrefs.contains(getResources().getString(R.string.ct_current_table))) {
            currentTable = gson.fromJson(tablePrefs.getString(getResources().getString(R.string.ct_current_table), null), TableData.class);
        }
		
		// On selection of student (1.e or 1.d ...) or teacher (John Smith or ...) change
        cls.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference pref, Object newValue) {
                pref.setSummary(newValue.toString());
				// Find selected time table
                for (TableData allTable : allTables) {
                    if (allTable.getOwner().equals(newValue.toString())) {
                        currentTable = allTable;
                        SharedPreferences.Editor editor = getSharedPreferences(getResources().getString(R.string.category_tables), Context.MODE_PRIVATE).edit();
                        String json = new Gson().toJson(currentTable);
                        editor.putString(getResources().getString(R.string.ct_current_table), json);
                        editor.apply();
                        break;
                    }
                }
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                // Enables selection of groups if the user is student
				if (prefs.getString(getResources().getString(R.string.key_pref_general_subject), getResources().getString(R.string.pupil)).equals(getResources().getString(R.string.pupil))) {
                    // Student
					runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            findPreference(getResources().getString(R.string.key_pref_groups_starter)).setEnabled(true);
                        }
                    });
                } else {
					// Teacher
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            findPreference(getResources().getString(R.string.key_pref_groups_starter)).setEnabled(false);
                        }
                    });
                    HashSet<String> groups = new HashSet<>();
                    groups.add("TEACHER");
                    GroupPickerActivity.setGroups(getApplicationContext(), groups);
                }
                MainActivity.settingsChanged = true;
                SharedPreferences.Editor subsTablesPrefsEditor = getSharedPreferences(getResources().getString(R.string.category_subs_files), Context.MODE_PRIVATE).edit();
                SharedPreferences subsTablesPrefs = getSharedPreferences(getResources().getString(R.string.category_subs_files), Context.MODE_PRIVATE);
                // Removes old subs
				for (String key : subsTablesPrefs.getAll().keySet()) {
                    subsTablesPrefsEditor.remove(key);
                }
                subsTablesPrefsEditor.apply();
                return true;
            }
        });
		
		// Sets and unsets lesson notifications based on settings
        notification.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((boolean) newValue) {
                    SubsService.setLessonNotificationsInBackground(getApplicationContext());
                } else {
                    SubsService.unsetLessonNotificationsInBackground(getApplicationContext());
                }
                return true;
            }
        });
		
		// Resets lesson notifications times when the time is changed
        notificationTime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference pref, Object newValue) {
                pref.setSummary(getResources().getStringArray(R.array.list_preference_notification_times_titles)[Integer.valueOf(newValue.toString())]);
                SubsService.setLessonNotificationsInBackground(getApplicationContext());
                return true;
            }
        });
		
		// Starts GroupPickerActivity when the groups preference is clicked
        group.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference pref) {
                Intent intent = new Intent(CustomPreferenceActivity.this, GroupPickerActivity.class);
                intent.putExtra(getResources().getString(R.string.ct_current_table), currentTable);
                startActivity(intent);
                return true;
            }
        });
		
		// Downloads time tables when the download time tables preference is clicked
        dlTable.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference pref) {
                downloadTable();
                MainActivity.settingsChanged = true;
                return true;
            }
        });

    }

    // Pops up the dialog and starts TableFetcher
    private void downloadTable() {
        dialog = ProgressDialog.show(CustomPreferenceActivity.this, "",
                getResources().getString(R.string.loading_table_message), true, true);
        dialog.show();
        fetcher = new TableFetcher();
        fetcher.execute();
    }

    // Sets options of class picker
    private void setClassPicker(String[] classes) {
        ListPreference classPicker = (ListPreference) findPreference(getResources().getString(R.string.key_pref_general_class));
        classPicker.setEntries(classes);
        classPicker.setEntryValues(classes);
        classPicker.setEnabled(true);
    }

    // Downloads tables in background and saves them
    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    private class TableFetcher extends AsyncTask<String, String, String> {
        protected String doInBackground(String... args) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CustomPreferenceActivity.this);
            SharedPreferences.Editor tablePrefsEditor = getSharedPreferences(getResources().getString(R.string.category_tables), Context.MODE_PRIVATE).edit();
            Gson gson = new Gson();
            TableTablo table = null;
            try {
                table = new TableTablo(prefs.getString(getResources().getString(R.string.key_pref_other_table_url), getResources().getString(R.string.pref_table_default)));
            } catch (Exception e) {
                e.printStackTrace();
            }
            dialog.dismiss();
            if (table != null) {
                if (table.isConnectionSuccessful()) {
                    allTables = table.getTableDataArr();
					// Extracts classes or teacher names
                    final String[] classes = new String[table.getTableDataArr().length];
                    for (int i = 0; i < table.getTableDataArr().length; i++) {
                        classes[i] = table.getTableDataArr()[i].getOwner();
                    }
                    final ListPreference classPicker = (ListPreference) findPreference(getResources().getString(R.string.key_pref_general_class));
					// Sets the other preferences based on current time tables
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            classPicker.setEntries(classes);
                            classPicker.setEntryValues(classes);
                            classPicker.setEnabled(true);
                        }
                    });
                    tablePrefsEditor.putString(getResources().getString(R.string.ct_classes), gson.toJson(classes));
                    tablePrefsEditor.putString(getResources().getString(R.string.ct_all_tables), gson.toJson(allTables));
                }
            }
			// Saves the new timetable if the user already selected a class or teacher
            if (!cls.getSummary().equals(getResources().getString(R.string.class_not_selected))) {
                for (TableData t : table.getTableDataArr()) {
                    if (t.getOwner().equals(cls.getSummary())) {
                        currentTable = t;
                        tablePrefsEditor.putString(getResources().getString(R.string.ct_current_table), gson.toJson(currentTable));
                        break;
                    }
                }
            }
            tablePrefsEditor.apply();
            return "";
        }


    }

}
