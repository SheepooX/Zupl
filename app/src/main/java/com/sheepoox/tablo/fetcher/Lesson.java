package com.sheepoox.tablo.fetcher;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Tomáš Černý
 * As part of project Zupl, 2017
 */

public class Lesson implements Serializable {

    /**
     * The subjects.
     */
    protected ArrayList<String> subjects =  new ArrayList<>();
    /**
     * The teachers.
     */
    protected ArrayList<String> teachers = new ArrayList<>();
    /**
     * The rooms.
     */
    protected ArrayList<String> rooms = new ArrayList<>();
    /**
     * The groups.
     */
    protected ArrayList<String> groups = new ArrayList<>();
    /**
     * The origins.
     */
    protected ArrayList<String> origins = new ArrayList<>();

    /**
     * Default contructor.
     */
    public Lesson() {}

    /**
     * Adds subject, teacher, room, group and origin to corresponding ArrayLists.
     *
     * @param subject   Added to the ArrayList subjects.
     * @param teacher   Added to the ArrayList teachers.
     * @param room      Added to the ArrayList rooms.
     * @param group     Added to the ArrayList groups.
     * @param origin    Added to the ArrayList origins.
     * @see ArrayList
     */
    public void addEntry(String subject, String teacher, String room, String group, String origin) {
        this.subjects.add(subject);
        this.teachers.add(teacher);
        this.rooms.add(room);
        this.groups.add(group);
        this.origins.add(origin);
    }

    /**
     * Sets subject, teacher, room, group and origin in corresponding ArrayLists on specified index.
     *
     * @param index     Position in the ArrayLists
     * @param subject   At position index this replaces the old value in ArrayList subjects.
     * @param teacher   At position index this replaces the old value in ArrayList teachers.
     * @param room      At position index this replaces the old value in ArrayList rooms.
     * @param group     At position index this replaces the old value in ArrayList groups.
     * @param origin    At position index this replaces the old value in ArrayList origins.
     * @see ArrayList
     */
    public void setEntry(int index, String subject, String teacher, String room, String group, String origin) {
        this.subjects.set(index, subject);
        this.teachers.set(index, teacher);
        this.rooms.set(index, room);
        this.groups.set(index, group);
        this.origins.set(index, origin);
    }

    /**
     * @return The ArrayList of subjects.
     * @see ArrayList
     */
    public ArrayList<String> getSubjects() {
        return this.subjects;
    }

    /**
     * @return The ArrayList of teachers.
     * @see ArrayList
     */
    public ArrayList<String> getTeachers() {
        return this.teachers;
    }

    /**
     * @return The ArrayList rooms.
     * @see ArrayList
     */
    public ArrayList<String> getRooms() {
        return this.rooms;
    }

    /**
     * @return The ArrayList groups.
     * @see ArrayList
     */
    public ArrayList<String> getGroups() {
        return this.groups;
    }

    /**
     * @return The ArrayList origins.
     * @see ArrayList
     */
    public ArrayList<String> getOrigins() {
        return origins;
    }

}
