package com.sheepoox.tablo.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.sheepoox.tablo.R;
import com.sheepoox.tablo.fetcher.Lesson;
import com.sheepoox.tablo.fetcher.SubsData;
import com.sheepoox.tablo.fetcher.SubsTablo;
import com.sheepoox.tablo.fetcher.TableData;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Tomáš Černý
 * Project Zupl, 2017
 */

public class MainActivity extends AppCompatActivity {

    // Selected lesson groups
    private HashSet<String> groups = new HashSet<>();

    // Changes from of the selected week
    private ArrayList<HashMap<Integer, Lesson>> changes;

    // Current Table
    private TableData cTable;

    // Boolean that changes as soon as settings are updated
    static boolean settingsChanged = false;

    // Represents which week is presented to the user
    private int weekOffset = 0;

    private Gson gson = new Gson();

    // Initializes this activity.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set week offset if it is in savedInstanceState object.
        weekOffset = savedInstanceState != null ? savedInstanceState.getInt("weekOffset", 0) : 0;
        setContentView(R.layout.activity_main);
        setChangesForWeek(weekOffset);
        getGroupsAndStartTable();
        SharedPreferences tablePrefs = getSharedPreferences(getResources().getString(R.string.category_tables), Context.MODE_PRIVATE);
        if (tablePrefs.contains(getResources().getString(R.string.ct_current_table)) && groups.size() > 1) {
            // Resets the background subs updater.
            Intent sIntent = new Intent(getApplicationContext(), SubsService.class);
            sIntent.setAction("UPDATE_SUBS");
            PendingIntent sPendingIntent = PendingIntent.getService(getApplicationContext(), 0, sIntent, 0);
            AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 3600000, AlarmManager.INTERVAL_HOUR, sPendingIntent);
        }
		// Update subs and set lesson notifications (or not)
        if (tablePrefs.contains(getResources().getString(R.string.ct_current_table)) && cTable != null) {
            if (groups.size() > 1 || cTable.getOwner().length() > 3) {
                if (isDeviceOnline(getApplicationContext())) {
                    SubsService.updateSubsInBackground(getApplicationContext());
                }
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                if (prefs.getBoolean(getResources().getString(R.string.key_pref_notifications_next_lesson), true)) {
                    SubsService.setLessonNotifications(getApplicationContext());
                } else {
                    SubsService.unsetLessonNotifications(getApplicationContext());
                }
            }
        }
    }

    // Gets called when this activity may be destroyed.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("weekOffset", weekOffset);
    }

    // onResume call
    @Override
    protected void onResume() {
        super.onResume();
        if (settingsChanged) {
            setChangesForWeek(weekOffset);
            getGroupsAndStartTable();
            settingsChanged = false;
        }
    }

    // Creates the table and initializes groups.
    private void getGroupsAndStartTable() {
        SharedPreferences tablePrefs = getSharedPreferences(getResources().getString(R.string.category_tables), Context.MODE_PRIVATE);
		// Check settings
        if (tablePrefs.contains(getResources().getString(R.string.ct_current_table))) {
            if (tablePrefs.contains(getResources().getString(R.string.key_table_pref_groups))) {
                this.groups = (HashSet<String>) tablePrefs.getStringSet(getResources().getString(R.string.key_table_pref_groups), new HashSet<String>());
                if (groups.size() > 1) {
                    try {
                        new TableSetter().execute("" + weekOffset);
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
		// Executes when the app is not setup yet
        TextView weekDisplay = (TextView) findViewById(R.id.weekView);
        weekDisplay.setText(getResources().getString(R.string.advice_settings));
        Intent intent = new Intent(getApplicationContext(), CustomPreferenceActivity.class);
        startActivity(intent);
    }

    // Initializes variable changes
    private void setChangesForWeek(int offset) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences subsTablesPrefs = getSharedPreferences(getResources().getString(R.string.category_subs_files), Context.MODE_PRIVATE);
		// Check settings
        if (prefs.contains(getResources().getString(R.string.key_pref_general_class))) {
			// Calculate days of selected week
            Date now = new Date();
            Date today = new Date(now.getYear(), now.getMonth(), now.getDate() + 7 * offset);
            Date[] thisWeek = new Date[7];
            int day = today.getDay();
            for (int i = 0; i < thisWeek.length; i++) {
                thisWeek[i] = new Date(today.getYear(), today.getMonth(), today.getDate() + i - day);
            }
            TextView weekDisplay = (TextView) findViewById(R.id.weekView);
            weekDisplay.setText("Od " + thisWeek[1].getDate() + "." + (thisWeek[1].getMonth() + 1) + " do " + thisWeek[5].getDate() + "." + (thisWeek[5].getMonth() + 1));

            ArrayList<HashMap<Integer, Lesson>> changesByDay = new ArrayList<>();
            String clas = prefs.getString(getResources().getString(R.string.key_pref_general_class), null);
			// Find subs for every day of seleceted week
            for (int i = 0; i < 5; i++) {
                String key = thisWeek[i + 1].getTime() + "";
                if (subsTablesPrefs.contains(key)) {
                    SubsData subs = gson.fromJson(subsTablesPrefs.getString((thisWeek[i + 1].getTime() + ""), null), SubsData.class);
                    HashMap<String, HashMap<Integer, Lesson>> ch;
                    if (prefs.getString(getResources().getString(R.string.key_pref_general_subject), getResources().getString(R.string.pupil))
                            .equals(getResources().getString(R.string.teacher))) {
                        ch = subs.getChangesTeachers();
                    } else {
                        ch = subs.getChangesClasses();
                    }
                    if (ch.containsKey(clas)) {
                        changesByDay.add(ch.get(prefs.getString(getResources().getString(R.string.key_pref_general_class), null)));
                        continue;
                    }
                }
                changesByDay.add(null);
            }
            changes = changesByDay;
        }
    }

    // Sets variable cTable based on settings, applies changes on current timetable and then sets the table
    private class TableSetter extends AsyncTask<String, String, String> {
        protected String doInBackground(String... args) {
            SharedPreferences tablePrefs = getSharedPreferences(getResources().getString(R.string.category_tables), Context.MODE_PRIVATE);
            String json = tablePrefs.getString(getResources().getString(R.string.ct_current_table), null);
            Gson gson = new Gson();
            cTable = (gson.fromJson(json, TableData.class));
            for (int i = 0; i < changes.size(); i++) {
                if (changes.get(i) != null) {
                    SubsTablo.applyChanges(cTable.getLessons()[i], changes.get(i), cTable.getOwner().length() > 3);
                }
            }
			// Save time table of this week, that will be used by notifications setter
            if (weekOffset == 0) {
                SharedPreferences.Editor editor = getSharedPreferences(getResources().getString(R.string.category_tables), Context.MODE_PRIVATE).edit();
                editor.putString(getResources().getString(R.string.ct_updated_table), gson.toJson(cTable));
                editor.apply();
            }
            return "";
        }

        protected void onPostExecute(String result) {
            if (cTable != null) {
                setTable(cTable);
            }
        }
    }

    // Sets the table
    private void setTable(TableData table) {
		// Find out what screen sizes are optimal
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int cellSize;
        float textSize;
        if (metrics.densityDpi <= metrics.DENSITY_LOW) {
            cellSize = 55;
            textSize = 20f;
        } else if (metrics.densityDpi <= metrics.DENSITY_MEDIUM) {
            cellSize = 110;
            textSize = 30f;
        } else if (metrics.densityDpi <= metrics.DENSITY_HIGH) {
            cellSize = 105;
            textSize = 20f;
        } else if (metrics.densityDpi <= metrics.DENSITY_XHIGH) {
            cellSize = 155;
            textSize = 22f;
        } else {
            cellSize = 240;
            textSize = 26f;
        }
		// Set table owner
        TextView cls = (TextView) findViewById(R.id.class_text);
        cls.setText(table.getOwner());
        LinearLayout times = (LinearLayout) findViewById(R.id.time_cells);
        ViewGroup timesContainer = times;
        timesContainer.removeAllViews();
        int c = 0;
		// Time cells
        for (String timeInterval : table.getTimes()) {
            View cell = getLayoutInflater().inflate(R.layout.time_cell, timesContainer, false);
            cell.getLayoutParams().width = cellSize;
            cell.getLayoutParams().height = cellSize / 2;
            ViewGroup cellContainer = (ViewGroup) cell;
            TextView timeLabel = (TextView) cellContainer.getChildAt(0);
            timeLabel.setTextSize(textSize * 0.5f);
            timeLabel.setText(c++ + "\n" + timeInterval.replace("-", " -"));
            timesContainer.addView(cell);
        }
		// Get day rows
        LinearLayout[] days = {
                (LinearLayout) findViewById(R.id.day0_cells),
                (LinearLayout) findViewById(R.id.day1_cells),
                (LinearLayout) findViewById(R.id.day2_cells),
                (LinearLayout) findViewById(R.id.day3_cells),
                (LinearLayout) findViewById(R.id.day4_cells)
        };
		// Set the lessons
        Lesson[][] lessons = table.getLessons();
        for (int i = 0; i < days.length; i++) {
            ViewGroup container = days[i];
            container.removeAllViews();
			// For every lesson in one day
            for (Lesson l : lessons[i]) {
				// Get TextViews from layout of a cell
                View cell = getLayoutInflater().inflate(R.layout.lesson_cell, container, false);
                cell.getLayoutParams().height = cellSize;
                cell.getLayoutParams().width = cellSize;
                ViewGroup cellContent = (ViewGroup) cell;
                LinearLayout contentContainer = (LinearLayout) cellContent.getChildAt(0);
                RelativeLayout lessonTextContainer = (RelativeLayout) contentContainer.getChildAt(0);
                TextView lessonText = (TextView) lessonTextContainer.getChildAt(0);
                TextView teacherAndRoomText = (TextView) contentContainer.getChildAt(1);
                AppCompatTextView groupText = (AppCompatTextView) cellContent.getChildAt(1);
                if (l.getGroups().size() != 0) {
					// Find which lesson has a user's groups
                    int j = 0;
                    boolean match = false;
                    if (table.getOwner().length() <= 3) {
						// Student
                        for (; j < l.getGroups().size() && !match; j++) {
                            match = groups.contains(l.getGroups().get(j));
                        }
                        j = j - 1;
                    } else {
						// Teacher
                        match = !l.getSubjects().get(0).contains(":");
                    }
					// Non laboratory lessons
                    if (match) {
						// Set groups text
                        if ((!l.getSubjects().get(j).isEmpty() && !l.getGroups().get(j).equals("all")) || table.getOwner().length() > 3) {
                            groupText.setText(l.getGroups().get(j).replace("all", ""));
                        }
						// If the lesson is not canceled, display the subject, teacher and room
                        if (!l.getOrigins().get(j).equals("odpadá") && !l.getOrigins().get(j).equals("výměna >>")
                                && !l.getOrigins().get(j).equals("přesun >>") && !l.getSubjects().get(j).isEmpty()) {
                            lessonText.setText(l.getSubjects().get(j));
                            final Lesson fL = l;
                            final int k = j;
							// Display room position
                            cell.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    String pos = SubsService.getRoomPosition(fL.getRooms().get(k).replaceAll("[()]", ""));
                                    Snackbar.make(v, fL.getRooms().get(k).replaceAll("[()]", "") + " > " + pos + (!fL.getOrigins().get(k).equals("default") ? " | " + fL.getOrigins().get(k) : ""), Snackbar.LENGTH_SHORT).show();
                                }
                            });
                            String teacher = l.getTeachers().get(j);
                            if (teacher.length() > 3) {
                                teacher = teacher.substring(0, 3);
                            }
                            teacherAndRoomText.setText(String.format("%s %s", teacher,
                                    l.getRooms().get(j)));
                        }
						// Set lesson colour
                        if (l.getOrigins().get(j).equals("default")) {
                            // No colour
                        } else if (l.getOrigins().get(j).equals("navíc") || l.getOrigins().get(j).equals("přesun <<")
                                || l.getOrigins().get(j).equals("výměna <<")) {
                            // Red
                            cell.setBackgroundColor(getResources().getColor(R.color.cell_red));
                        } else if (l.getOrigins().get(j).equals("odpadá") || l.getOrigins().get(j).equals("výměna >>")
                                || l.getOrigins().get(j).equals("přesun >>")) {
                            // Green
                            cell.setBackgroundColor(getResources().getColor(R.color.cell_green));
                        } else {
                            // Yellow
                            cell.setBackgroundColor(getResources().getColor(R.color.cell_yellow));
                        }
                    } else if (l.getSubjects().get(0).contains(":")) { // Laboratory Lessons
                        int ii = 0;
                        int[] index = {-1, -1, -1};
                        int ch = -1;
                        int bi = -1;
                        int fy = -1;
						// For every origins check if it is not default
                        for (int k = 0; k < l.getOrigins().size(); k++) {
                            if (!l.getOrigins().get(k).equals("default")) {
                                if (ii < index.length) {
                                    index[ii++] = k;
									// Bright yellow
                                    cell.setBackgroundColor(getResources().getColor(R.color.cell_rich_yellow));
                                }
                            }
							// Find indexes of each subject
                            if (l.getRooms().get(k).length() > 1) {
                                if (l.getSubjects().get(k).contains("CH")) {
                                    ch = k;
                                } else if (l.getSubjects().get(k).contains("BI")) {
                                    bi = k;
                                } else if (l.getSubjects().get(k).contains("FY")) {
                                    fy = k;
                                }
                            }
                        }
                        ch = ch == -1 ? 0 : ch;
                        fy = fy == -1 ? 1 : fy;
                        bi = bi == -1 ? 2 : bi;
                        final int[] fIndex = index;
                        final Lesson fL = l;
						// Display of rooms positions and changes
                        cell.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String ch = "";
                                for (int l : fIndex) {
                                    if (l != -1) {
                                        ch += "\n" + fL.getGroups().get(l) + " - " + fL.getOrigins().get(l);
                                    }
                                }
                                Snackbar.make(v, "LCH/LBI > 2. patro, vedlejší chodba; LFY > 1. patro, vedlejší chodba" + ch, 2 * Snackbar.LENGTH_LONG).show();
                            }
                        });
						// Set subjects, teachers and rooms
                        lessonText.setText(String.format("L:%s/%s/%s",
                                l.getSubjects().get(ch).substring(l.getSubjects().get(ch).indexOf(":") + 1),
                                l.getSubjects().get(bi).substring(l.getSubjects().get(bi).indexOf(":") + 1),
                                l.getSubjects().get(fy).substring(l.getSubjects().get(fy).indexOf(":") + 1))
                                .replaceAll("\\u00A0", ""));
                        teacherAndRoomText.setText(String.format("%s/%s/%s (%s/%s/%s)",
                                l.getTeachers().get(ch).substring(0, Math.min(l.getTeachers().get(ch).length(), 3)),
                                l.getTeachers().get(bi).substring(0, Math.min(l.getTeachers().get(ch).length(), 3)),
                                l.getTeachers().get(fy).substring(0, Math.min(l.getTeachers().get(ch).length(), 3)),
                                l.getRooms().get(ch).replaceAll("[()]", ""),
                                l.getRooms().get(bi).replaceAll("[()]", ""),
                                l.getRooms().get(fy).replaceAll("[()]", "")));
                    }
					// Set text sizes
                    lessonText.setTextSize(lessonText.getText().length() > 5 ? textSize * 0.8f : textSize);
                    teacherAndRoomText.setTextSize(teacherAndRoomText.getText().length() > 9 ? textSize * 0.5f : textSize * 0.7f);
                    groupText.setTextSize(textSize * 0.7f);
                }
				// Add the cell
                container.addView(cell);
            }
        }
    }

    // Initializes the options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_options, menu);
        return true;
    }

    // Handles the actions of app bar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.go_settings) {
            Intent intent = new Intent(this, CustomPreferenceActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_reload) {
            SubsService.updateSubsInBackground(getApplicationContext());
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    // Called when the buttons that change week are clicked
    public void changeWeek(View view) {
        setChangesForWeek(view.getId() == R.id.next_week ? ++weekOffset : --weekOffset);
        getGroupsAndStartTable();
    }

    // Returns true if the device is connected to the internet
    static boolean isDeviceOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

}
