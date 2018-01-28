package com.sheepoox.tablo.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.sheepoox.tablo.R;
import com.sheepoox.tablo.fetcher.Lesson;
import com.sheepoox.tablo.fetcher.TableData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by Tomáš Černý
 * Project Zupl, 2017
 */

public class GroupPickerActivity extends AppCompatActivity {

    // Table from which the groups are taken.
    private TableData table;
    // Group names and their positions.
    private String[][] groupNames;
    // Cells in the activity.
    private View[][] cells;
    // Indicated which cells are selected.
    private int[] selectedCellsPointer;
    private ArrayList<String> addedSeminars = new ArrayList<>();

    // Help the user with the first selection
    private boolean help = true;

    // This method initializes this activity.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        table = (TableData) getIntent().getSerializableExtra(getResources().getString(R.string.ct_current_table));
        setContentView(R.layout.activity_group_picker);
        // Get the groups from table
        TreeMap<String, ArrayList<String>> groupMap = new TreeMap<>();
        ArrayList<String> seminars = new ArrayList<>();
        for (Lesson[] arr : table.getLessons()) {
            for (Lesson l : arr) {
                if (l.getGroups().size() != 0) {
                    for (int i = 0; i < l.getGroups().size(); i++) {
                        if (l.getSubjects().get(i).contains(":") || l.getGroups().get(i).contains("all")) {
                            continue;
                        } else if (l.getGroups().get(i).substring(0, 1).equals("3") || l.getGroups().get(i).substring(0, 1).equals("4")) {
                            if (!seminars.contains(l.getGroups().get(i))) {
                                seminars.add(l.getGroups().get(i));
                            }
                        } else if (groupMap.containsKey(l.getGroups().get(i).substring(0, l.getGroups().get(i).length() - 1))) {
                            if (!groupMap.get(l.getGroups().get(i).substring(0, l.getGroups().get(i).length() - 1)).contains(l.getGroups().get(i))) {
                                groupMap.get(l.getGroups().get(i).substring(0, l.getGroups().get(i).length() - 1)).add(l.getGroups().get(i));
                            }
                        } else {
                            groupMap.put(l.getGroups().get(i).substring(0, l.getGroups().get(i).length() - 1), new ArrayList<String>());
                            groupMap.get(l.getGroups().get(i).substring(0, l.getGroups().get(i).length() - 1)).add(l.getGroups().get(i));
                        }
                    }
                }
            }
        }
        // Create the layout based on the found groups
        ViewGroup groupContainer = (ViewGroup) findViewById(R.id.groups_container);
        int lineCounter = 0;
        ViewGroup currentRow = getGroupRow(groupContainer);
		// For every seminar do this
        for (final String group : seminars) {
            ViewGroup cell = getGroupCell(currentRow);
            AppCompatTextView text = (AppCompatTextView) cell.getChildAt(0);
            SharedPreferences tablePrefs = getSharedPreferences(getResources().getString(R.string.category_tables), Context.MODE_PRIVATE);
            if (tablePrefs.contains(getResources().getString(R.string.key_table_pref_groups))) {
				// Colours already selected seminars and adds them to the selected ones
                HashSet<String> groups = (HashSet<String>) tablePrefs.getStringSet(getResources().getString(R.string.key_table_pref_groups), null);
                if (group != null) {
                    if (groups.contains(group)) {
                        cell.setBackgroundResource(R.color.colorAccent);
                        addedSeminars.add(group);
                    } else {
                        cell.setBackgroundResource(R.color.background_holo_light);
                    }
                }
            }
            text.setText(group);
            cell.setOnClickListener(new View.OnClickListener() {
                // This method executes when the cell is clicked.
                @Override
                public void onClick(View view) {
                    if (addedSeminars.contains(group)) {
                        addedSeminars.remove(group);
                        view.setBackgroundResource(R.color.background_holo_light);
                    } else {
                        addedSeminars.add(group);
                        view.setBackgroundResource(R.color.colorAccent);
                    }
                }
            });
			// Adds cells to the UI. 5 on each lines
            currentRow.addView(cell);
            currentRow.addView(getVerticalLine(currentRow));
            if (lineCounter == 4) {
                lineCounter = 0;
                groupContainer.addView(currentRow);
                groupContainer.addView(getHorizontalLine(groupContainer));
                currentRow = getGroupRow(groupContainer);
            } else {
                lineCounter++;
            }
        }

        if (seminars.size() > 0) {
            groupContainer.addView(getHorizontalLine(groupContainer));
            groupContainer.addView(getHorizontalLine(groupContainer));
        }
		// Other groups
        Set<String> keys = groupMap.keySet();
        int i = 0;
        cells = new RelativeLayout[groupMap.size()][];
        selectedCellsPointer = new int[groupMap.size()];
        groupNames = new String[groupMap.size()][];
        Arrays.fill(selectedCellsPointer, -1);
        for (String key : keys) {
            ArrayList<String> list = groupMap.get(key);
            cells[i] = new RelativeLayout[list.size()];
            groupNames[i] = new String[list.size()];
            Collections.sort(list);
            ViewGroup row = getGroupRow(groupContainer);
            int j = 0;
			// For every group do this
            for (final String group : list) {
                ViewGroup cell = getGroupCell(row);
                AppCompatTextView text = (AppCompatTextView) cell.getChildAt(0);
                SharedPreferences tablePrefs = getSharedPreferences(getResources().getString(R.string.category_tables), Context.MODE_PRIVATE);
                if (tablePrefs.contains(getResources().getString(R.string.key_table_pref_groups))) {
					// Colours already selected groups and adds their index to the array of selected ones
                    HashSet<String> groups = (HashSet<String>) tablePrefs.getStringSet(getResources().getString(R.string.key_table_pref_groups), null);
                    if (group != null) {
                        if (groups.contains(group)) {
                            cell.setBackgroundResource(R.color.colorAccent);
                            selectedCellsPointer[i] = j;
                            help = false;
                        } else {
                            cell.setBackgroundResource(R.color.background_holo_light);
                        }
                    }
                }
                text.setText(group);
                final int y = i;
                final int x = j;
                cell.setOnClickListener(new View.OnClickListener() {

                    // This method executes when the cell is clicked.
                    @Override
                    public void onClick(View view) {
                        if (selectedCellsPointer[y] == -1) {
                            view.setBackgroundResource(R.color.colorAccent);
                            selectedCellsPointer[y] = x;
                        } else if (selectedCellsPointer[y] != x) {
                            view.setBackgroundResource(R.color.colorAccent);
                            cells[y][selectedCellsPointer[y]].setBackgroundResource(R.color.background_holo_light);
                            selectedCellsPointer[y] = x;
                        } else {
                            view.setBackgroundResource(R.color.background_holo_light);
                            selectedCellsPointer[y] = -1;
                            return;
                        }
                        if (help) {
                            help = false;
                            for (int i = 0; i < groupNames.length; i++) {
                                if (i != y) {
                                    for (int j = 0; j < groupNames[i].length; j++) {
                                        if (groupNames[i][j].substring(groupNames[i][j].length() - 1, groupNames[i][j].length())
                                                .equals(groupNames[y][x].substring(groupNames[y][x].length() - 1, groupNames[y][x].length()))) {
                                            cells[i][j].setBackgroundResource(R.color.colorAccent);
                                            selectedCellsPointer[i] = j;
                                        }
                                    }
                                }
                            }
                        }
                    }
                });
                // Add the cell
                cells[i][j] = cell;
                groupNames[i][j] = group;
                row.addView(cell);
                row.addView(getVerticalLine(row));
                j++;
            }
            // Add the row
            groupContainer.addView(row);
            groupContainer.addView(getHorizontalLine(groupContainer));
            i++;
        }
    }

    // Returns horizontal line from xml.
    private View getHorizontalLine(ViewGroup parent) {
        return getLayoutInflater().inflate(R.layout.horizontal_2dp_line, parent, false);
    }

    // Returns vertical line from xml.
    private View getVerticalLine(ViewGroup parent) {
        return getLayoutInflater().inflate(R.layout.vertical_1dp_line, parent, false);
    }

    // Returns group row from xml.
    private ViewGroup getGroupRow(ViewGroup parent) {
        return (ViewGroup) getLayoutInflater().inflate(R.layout.group_row, parent, false);
    }

    // Returns group cell from xml.
    private ViewGroup getGroupCell(ViewGroup parent) {
        return (ViewGroup) getLayoutInflater().inflate(R.layout.group_cell, parent, false);
    }

    // Run when the apply button is clicked.
    public void onClickApply(View view) {
        SubsService.setLessonNotificationsInBackground(getApplicationContext());

        HashSet<String> groups = new HashSet<>();
        groups.addAll(addedSeminars);
        for (int i = 0; i < selectedCellsPointer.length; i++) {
            if (selectedCellsPointer[i] != -1) {
                groups.add(groupNames[i][selectedCellsPointer[i]]);
            }
        }
        setGroups(getApplicationContext(), groups);
        finish();
    }

    // Sets the settings of groups
    static void setGroups(Context context, HashSet<String> groups) {
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager packageManager = context.getPackageManager();
        Intent sIntent = new Intent(context, SubsService.class);
        sIntent.setAction("UPDATE_SUBS");
        PendingIntent sPendingIntent = PendingIntent.getService(context, 0, sIntent, 0);
        AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if (groups.size() > 0) {
            // Enable boot alarm and subs updating
            packageManager.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+ 3600000, AlarmManager.INTERVAL_HOUR, sPendingIntent);
			MainActivity.settingsChanged = true;
        } else {
            // Disable boot alarm and subs updating
            packageManager.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            alarm.cancel(sPendingIntent);
        }
		// Add group that everyone has
        groups.add("all");
        SharedPreferences.Editor editor = context.getSharedPreferences(context.getResources().getString(R.string.category_tables), Context.MODE_PRIVATE).edit();
        editor.putStringSet(context.getResources().getString(R.string.key_table_pref_groups), groups);
        editor.apply();
		// Updates subs
        if (groups.size() > 1) {
            if (MainActivity.isDeviceOnline(context)) {
                SubsService.updateSubsInBackground(context);
            }
            SubsService.setLessonNotificationsInBackground(context);
        }
    }

    // Run when the cancel button is clicked.
    public void onClickCancel(View view) {
        finish();
    }

    // Run when the clear button is clicked.
    public void onClickClear(View view) {
        for (int i = 0; i < selectedCellsPointer.length; i++) {
            if (selectedCellsPointer[i] != -1) {
                cells[i][selectedCellsPointer[i]].setBackgroundResource(R.color.background_holo_light);
                selectedCellsPointer[i] = -1;
                help = true;
            }
        }
    }

}
