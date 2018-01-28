package com.sheepoox.tablo.fetcher;

import java.io.Serializable;

/**
 * Created by Tomáš Černý
 * As part of project Zupl, 2017
 */

public class TableData implements Serializable {

    /**
     * Owner of the timetable.
     */
    protected String owner; // Class

    /**
     * Mentor of the owner.
     */
    protected String mentor; // Class teacher

    /**
     * Times at which the lessons start and end. E.g. "7:00 -7:45"
     */
    protected String[] times; // times.length = 10

    /**
     * Lessons for a week.
     * @see Lesson
     */
    protected Lesson[][] lessons; // 0: Monday, 1: Tuesday, 2: Wednesday, 3: Thursday, 4: Friday

    /**
     * Default constructor.
     */
    public TableData() {
    }

    /**
     * @return The owner of the timetable.
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @param owner Set the owner of the timetable to this value.
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * @return The mentor of the owner.
     */
    public String getMentor() {
        return mentor;
    }

    /**
     * @param mentor    Set the mentor of the owner to this value.
     */
    public void setMentor(String mentor) {
        this.mentor = mentor;
    }

    /**
     * @return The times at which the lessons start and end.
     */
    public String[] getTimes() {
        return times;
    }

    /**
     * @param times Set the time at which lessons start and end to this value.
     */
    public void setTimes(String[] times) {
        this.times = times;
    }

    /**
     * @return  The lessons for a week.
     */
    public Lesson[][] getLessons() {
        return lessons;
    }

    /**
     * @param lessons   Set the lessons to this value.
     */
    public void setLessons(Lesson[][] lessons) {
        this.lessons = lessons;
    }


}