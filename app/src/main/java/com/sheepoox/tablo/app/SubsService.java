package com.sheepoox.tablo.app;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

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
import java.util.LinkedHashSet;
import java.util.TreeMap;

/**
 * Created by Tomáš Černý
 * Project Zupl, 2017
 */
public class SubsService extends IntentService {

    public SubsService(String name) {
        super(name);
    }

    public SubsService() {
        super("default");
    }

    // This method is run when the service is called
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals("UPDATE_SUBS")) {
            if (MainActivity.isDeviceOnline(getApplicationContext())) {
                updateSubs(getApplicationContext());
            }
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            if (prefs.getBoolean(getResources().getString(R.string.key_pref_notifications_next_lesson), true)) {
                setLessonNotifications(getApplicationContext());
            } else {
                unsetLessonNotifications(getApplicationContext());
            }
        }
    }

    // This method unsets lesson notifications.
    static void unsetLessonNotifications(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        for (int i = 1; i < 15; i++) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, i, new Intent(context, NotificationReceiver.class), 0);
            alarmManager.cancel(pendingIntent);
        }
    }

    // This method sets lesson notifications for this day.
    static void setLessonNotifications(Context context) {
        Gson gson = new Gson();
		// Get the information required from saved objects
        SharedPreferences tablePrefs = context.getSharedPreferences(context.getResources().getString(R.string.category_tables), Context.MODE_PRIVATE);
        TableData cTable = gson.fromJson(tablePrefs.getString(context.getResources().getString(R.string.ct_updated_table), null), TableData.class);
        HashSet<String> groups = (HashSet<String>) tablePrefs.getStringSet(context.getResources().getString(R.string.key_table_pref_groups), new HashSet<String>());
        if (cTable != null) {
            Date today = new Date();
		// Skip if it is Saturday (6) or Sunday (0)
            if (today.getDay() != 0 && today.getDay() != 6) {
                Lesson[] lessons = cTable.getLessons()[today.getDay() - 1];
                boolean firstLesson = true;
				// For every lesson
                for (int i = 0; i < lessons.length; i++) {
                    if (lessons[i].getGroups().size() != 0) {
                        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
						// Filter by groups
                        int j = 0;
                        boolean match = false;
                        if (cTable.getOwner().length() <= 3) {
                            for (; j < lessons[i].getGroups().size() && !match; j++) {
                                match = groups.contains(lessons[i].getGroups().get(j));
                            }
                            j = j - 1;
                        } else {
                            match = true;
                        }
						// Do not set notifications if the lesson is empty or cancelled
                        if (match && !lessons[i].getOrigins().get(j).equals("odpadá") && !lessons[i].getOrigins().get(j).equals("výměna >>")
                                && !lessons[i].getOrigins().get(j).equals("přesun >>") && !lessons[i].getSubjects().get(j).isEmpty()) {
                            String timePreference = prefs.getString(context.getResources().getString(R.string.key_pref_notifications_time), "0");
                            Date d0 = new Date(today.getYear(), today.getMonth(), today.getDate(), Integer.valueOf(cTable.getTimes()[i].substring(0, cTable.getTimes()[i].indexOf(":"))), 0);
                            // Set the time based on selected option
							if (timePreference.equals("0")) {
                                // At the end of previous, -10 min for first lesson
                                if (firstLesson) {
                                    firstLesson = false;
                                    d0.setMinutes(Integer.valueOf(cTable.getTimes()[i].substring(cTable.getTimes()[i].indexOf(":") + 1, cTable.getTimes()[i].indexOf("-"))) - 10);
                                } else {
                                    String hour = cTable.getTimes()[i - 1].substring(cTable.getTimes()[i - 1].indexOf("-") + 1).replace(" ", "");
                                    d0.setHours(Integer.valueOf(hour.substring(0, hour.indexOf(":"))));
                                    d0.setMinutes(Integer.valueOf(cTable.getTimes()[i - 1].substring(cTable.getTimes()[i - 1].length() - 2)) - 10);
                                }
                            } else if (timePreference.equals("1")) {
                                // -10 min
                                d0.setMinutes(Integer.valueOf(cTable.getTimes()[i].substring(cTable.getTimes()[i].indexOf(":") + 1, cTable.getTimes()[i].indexOf("-"))) - 10);
                            } else if (timePreference.equals("2")) {
                                // -5 min
                                d0.setMinutes(Integer.valueOf(cTable.getTimes()[i].substring(cTable.getTimes()[i].indexOf(":") + 1, cTable.getTimes()[i].indexOf("-"))) - 5);
                            } else {
                                // -3 min
                                d0.setMinutes(Integer.valueOf(cTable.getTimes()[i].substring(cTable.getTimes()[i].indexOf(":") + 1, cTable.getTimes()[i].indexOf("-"))) - 3);
                            }
							// Create the texts of notification
                            long time = d0.getTime();
                            Intent intent = new Intent(context, NotificationReceiver.class);
                            intent.putExtra("cls", prefs.getString(context.getResources().getString(R.string.key_pref_general_class), "1234"));
                            String[] msgs = new String[2];
                            String room = lessons[i].getRooms().get(j).replaceAll("[()]", "");
                            String msgShort;
                            if (cTable.getOwner().length() > 3) {
								// Teacher
                                msgs[0] = String.format("Další hodina je %s [%s]", lessons[i].getSubjects().get(j), cTable.getTimes()[i]);
                                msgs[1] = String.format("s %s v %s; %s", lessons[i].getGroups().get(j).replaceAll("all", ""), room, getRoomPosition(room));
                                msgShort = String.format("[%s] %s v %s", cTable.getTimes()[i], lessons[i].getSubjects().get(j),
                                        lessons[i].getRooms().get(j).replaceAll("[()]", ""));
                            } else {
								// Student
                                String teacher = lessons[i].getTeachers().get(j).substring(0, Math.min(lessons[i].getTeachers().get(j).length(), 3));
                                msgs[0] = String.format("Další hodina je %s [%s]", lessons[i].getSubjects().get(j), cTable.getTimes()[i]);
                                msgs[1] = String.format("s %s v %s; %s", teacher,
                                        room, getRoomPosition(room));
                                msgShort = String.format("[%s] %s %s v %s", cTable.getTimes()[i], lessons[i].getSubjects().get(j), teacher,
                                        room);
                            }
                            intent.putExtra("msgs", msgs);
                            intent.putExtra("msg_short", msgShort);
							// Id of each notification is (i + 2), from 2 to ...
							// Ids 0 and 1 are reserved for subs update notifications
                            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, i + 2, intent, 0);
							// If the time is valid (reserve of 70 seconds)
                            if (time > today.getTime() - 70000) {
								// Cancel old one
                                alarm.cancel(pendingIntent);
                                alarm.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
                            }
                        }
                    }
                }
            }
        }
    }

    // This methods return the position of a room.
    static String getRoomPosition(String room) {
        room = room.toUpperCase();
        switch (room) {
            case "A1":
            case "N1":
            case "VV":
            case "HV":
                return "Přízemí, hlavní chodba";
            case "P1":
            case "P2":
            case "P3":
            case "1A":
            case "1B":
            case "1C":
            case "1D":
                return "1. patro, hlavní chodba";
            case "LFY":
            case "RJ":
            case "M1":
            case "1E":
            case "FY":
            case "4C":
                return "1. patro, vedlejší chodba";
            case "A2":
            case "N2":
            case "M2":
            case "2A":
            case "BI2":
            case "2B":
            case "CH2":
            case "2C":
            case "ZE2":
            case "2D":
            case "ZE1":
            case "2E":
                return "2. patro, hlavní chodba";
            case "LCH":
            case "CH1":
            case "4D":
            case "LBI":
            case "BI1":
            case "4E":
                return "2. patro, vedlejší chodba";
            case "FJ":
            case "4A":
            case "ZSV":
            case "4B":
            case "DĚ":
            case "3A":
            case "ČJ":
            case "3B":
            case "3C":
            case "3D":
            case "3E":
                return "3. patro, hlavní chodba";
            case "STU":
                return "3. patro, vedlejší chodba";
            case "TV1":
            case "TV2":
                return "1. patro, tělocvičny";
            case "MIM":
                return "mimo školu";
            default:
                return "";
        }
    }

    // This methods downloads substitutions and compares the with existing ones. If there are either new or updated ones,
    // a notification pops up.
    static void updateSubs(Context context) {
        SubsTablo table = new SubsTablo(PreferenceManager.getDefaultSharedPreferences(context).getString(context.getResources().getString(R.string.key_pref_other_subs_url),
                context.getResources().getString(R.string.pref_subs_default)), false);
        ArrayList<SubsData> data = table.getSubsDataList();
		// Settings and data
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences subsTablesPrefs = context.getSharedPreferences(context.getResources().getString(R.string.category_subs_files), Context.MODE_PRIVATE);
        SharedPreferences.Editor subsTablesPrefsEditor = context.getSharedPreferences(context.getResources().getString(R.string.category_subs_files), Context.MODE_PRIVATE).edit();

        boolean prefsContainsClass = prefs.contains(context.getResources().getString(R.string.key_pref_general_class));
        String cls = prefs.getString(context.getResources().getString(R.string.key_pref_general_class), "1234");
		
        Gson gson = new Gson();
        boolean todayUpdated = false;
		// For every subs data
        for (SubsData day : data) {
            Date d = day.getDate();
            String saveName = "" + d.getTime();
			// Check if the subs is already saved
            if (subsTablesPrefs.contains(saveName)) {
                SubsData old = gson.fromJson(subsTablesPrefs.getString(saveName, null), SubsData.class);
				// Check time of post
                if (old.getDateAdded().getTime() < day.getDateAdded().getTime()) {
                    subsTablesPrefsEditor.putString(saveName, gson.toJson(day));
                    if (prefsContainsClass) {
                        boolean changed = false;
						// Student or Teacher?
                        if (prefs.getString(context.getResources()
								.getString(R.string.key_pref_general_subject), context.getResources()
								.getString(R.string.pupil))
                                .equals(context.getResources()
								.getString(R.string.teacher))) {
                            // Teacher
                            String jsonOld = gson.toJson(old.getChangesTeachers().get(cls));
                            String jsonNew = gson.toJson(day.getChangesTeachers().get(cls));
                            changed = !jsonOld.equals(jsonNew);
                        } else {
                            // Student
                            SharedPreferences tablePrefs = context.getSharedPreferences(context.getResources()
							.getString(R.string.category_tables), Context.MODE_PRIVATE);
                            HashSet<String> groups = (HashSet<String>) tablePrefs.getStringSet(context.getResources()
							.getString(R.string.key_table_pref_groups), new HashSet<String>());
                            HashSet<String> a = getUsersLessons(old.getChangesClasses().get(cls), groups);
                            HashSet<String> b = getUsersLessons(day.getChangesClasses().get(cls), groups);
                            if (a.size() != b.size()) {
                                changed = true;
                            } else {
                                b.removeAll(a);
                                if (b.size() > 0) {
                                    changed = true;
                                }
                            }
                        }
                        if (changed) {
                            // Notify the user
                            notifyUser(1, context, cls, context.getResources().getString(R.string.updated_supl_msg_short), context.getResources().getString(R.string.updated_supl_msg));
                            Date today = new Date();
							// Today?
                            if (day.getDate().getYear() == today.getYear() && day.getDate().getMonth() == today.getMonth() && day.getDate().getDate() == today.getDate()) {
                                todayUpdated = true;
                            }
                        }
                    }
                }
            } else { // Not saved
                if (prefsContainsClass) {
                    HashMap<String, HashMap<Integer, Lesson>> subs = null;
					// Student or Teacher?
                    if (prefs.getString(context.getResources().getString(R.string.key_pref_general_subject), context.getResources().getString(R.string.pupil))
                            .equals(context.getResources().getString(R.string.teacher))) {
						// Teacher
                        subs = day.getChangesTeachers();
                    } else {
						// Student
                        subs = day.getChangesClasses();
                    }
					// Matches the selected class or teacher name?
                    if (subs.containsKey(prefs.getString(context.getResources().getString(R.string.key_pref_general_class), "1234"))) {
                        // Notify the user
                        subsTablesPrefsEditor.putString(saveName, gson.toJson(day));
                        notifyUser(0, context, cls, context.getResources().getString(R.string.new_supl_msg_short), "");
                        Date today = new Date();
						// Today?
                        if (day.getDate().getYear() == today.getYear() && day.getDate().getMonth() == today.getMonth() && day.getDate().getDate() == today.getDate()) {
                            todayUpdated = true;
                        }
                    }
                }
            }
        }
		// Reset lesson notifications today has any changes
        if (todayUpdated) {
            setLessonNotificationsInBackground(context);
        }
        subsTablesPrefsEditor.apply();
    }
	
	// Find only users lessons
    private static HashSet<String> getUsersLessons(HashMap<Integer, Lesson> changes, 
													HashSet<String> groups) {
        HashSet<String> ret = new HashSet<>();
        for (int k : changes.keySet()) {
            for (int i = 0; i < changes.get(k).getGroups().size(); i++) {
                if (groups.contains(changes.get(k).getGroups().get(i))) {
                    ret.add(changes.get(k).getSubjects().get(i).toString() +
							changes.get(k).getTeachers().get(i).toString() +
                            changes.get(k).getRooms().get(i).toString() + 
							changes.get(k).getGroups().get(i).toString() +
                            changes.get(k).getOrigins().get(i).toString());
                    ret.size();
                }
            }
        }
        return ret;
    }

    // Notifies the user based on given parameters.
    static void notifyUser(int id, Context context, String cls, String shortMsg, String... msg) {
        final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder notificationBuilder = new Notification.Builder(context)
                .setTicker(cls + " " + shortMsg)
                .setSmallIcon(R.mipmap.ic_launcherz_notif)
                .setAutoCancel(true)
                .setContentTitle(cls + " " + shortMsg)
                .setContentIntent(pendingIntent);
        if (msg.length > 1) {
            notificationBuilder.setSubText(msg[1]);
            notificationBuilder.setContentText(msg[0]);
        } else if (msg.length == 1) {
            notificationBuilder.setContentText(msg[0]);
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notificationBuilder.build());
    }

    // Executes updateSubs on another thread.
    static void updateSubsInBackground(final Context context) {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SubsService.updateSubs(context);
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Executes setLessonNotifications on another thread.
    static void setLessonNotificationsInBackground(final Context context) {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SubsService.setLessonNotifications(context);
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Executes unsetLessonNotifications on another thread.
    static void unsetLessonNotificationsInBackground(final Context context) {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SubsService.unsetLessonNotifications(context);
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
