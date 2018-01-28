package com.sheepoox.tablo.fetcher;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Tomáš Černý
 * As part of project Zupl, 2017
 */

public class SubsData implements Serializable {

    /**
     * Date at which the substitutions take place.
     * @see Date
     */
    protected Date date;

    /**
     * Date at which this substitutions were added.
     * @see Date
     */
    protected Date dateAdded;

    /**
     * Absent classes.
     * @see HashMap
     * @see Lesson
     */
    protected HashMap<String, Lesson[]> absentClasses = new HashMap<>();

    /**
     * Absent teachers.
     * @see HashMap
     * @see Lesson
     */
    protected HashMap<String, Lesson[]> absentTeachers = new HashMap<>();

    /**
     * Changes for classes.
     * @see HashMap
     * @see Lesson
     */
    protected HashMap<String, HashMap<Integer, Lesson>> changesClasses = new HashMap<>();

    /**
     * Changes for teachers.
     * @see HashMap
     * @see Lesson
     */
    protected HashMap<String, HashMap<Integer, Lesson>> changesTeachers = new HashMap<>();

    /**
     * Default contructor.
     */
    public SubsData() {

    }

    /**
     * @return  Date at which the substitutions take place.
     * @see     Date
     */
    public Date getDate() {
        return date;
    }

    /**
     * @return  Date at which this substitutions were added.
     * @see     Date
     */
    public Date getDateAdded() {
        return dateAdded;
    }

    /**
     * @return  Absent classes.
     * @see HashMap
     * @see Lesson
     */
    public HashMap<String, Lesson[]> getAbsentClasses() {
        return absentClasses;
    }

    /**
     * @return  Absent teachers.
     * @see HashMap
     * @see Lesson
     */
    public HashMap<String, Lesson[]> getAbsentTeachers() {
        return absentTeachers;
    }

    /**
     * @return  Changes for classes.
     * @see HashMap
     * @see Lesson
     */
    public HashMap<String, HashMap<Integer, Lesson>> getChangesClasses() {
        return changesClasses;
    }

    /**
     * @return  Changes for teachers.
     * @see HashMap
     * @see Lesson
     */
    public HashMap<String, HashMap<Integer, Lesson>> getChangesTeachers() {
        return changesTeachers;
    }

    /**
     * @param date Set the date at which this substitutions take place to this value.
     * @see Date
     */
    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * @param dateAdded Set the date at which this substitutions were added to this value.
     * @see Date
     */
    public void setDateAdded(Date dateAdded) {
        this.dateAdded = dateAdded;
    }
}
