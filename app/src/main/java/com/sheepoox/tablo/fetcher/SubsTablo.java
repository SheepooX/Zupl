package com.sheepoox.tablo.fetcher;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.Serializable;
import java.util.*;

/**
 * Created by Tomáš Černý
 * As part of project Zupl, 2017
 */

public class SubsTablo extends Tablo implements Serializable {

    /**
     * Substitutions present at the time this object was created.
     */
    protected ArrayList<SubsData> subsDataList = new ArrayList<>();

    /**
     * True if absent teachers and classes were parsed.
     */
    public final boolean absent;

    /**
     * @param address   Address of the web page where the substitutions are.
     * @param absent    Determines if absent teachers and classes should be parsed.
     * @see Tablo
     */
    public SubsTablo(String address, boolean absent) {
        super(address, "surrmmdd");
        this.absent = absent;
        parser();
    }

    /**
     * Parses the web page specified by the address.
     */
    private void parser() {
        Elements body = this.doc.select("body > *");
        for (Element e : body) {
            if (e.is("a")) {
                // Nothing
            } else if (e.is("p.textlarge_3")) {
                // Date of the information
                SubsData data = new SubsData();
                this.subsDataList.add(data);
                String[] date = e.text().substring(e.text().indexOf(" ") + 1).split("\\.");
                this.subsDataList.get(this.subsDataList.size() - 1).setDate(new Date(Integer.valueOf(date[2]) - 1900, Integer.valueOf(date[1]) - 1, Integer.valueOf(date[0])));
            } else if (e.is("p.textsmall_3")) {
                // Date at which the information was added
                String date = e.text().substring(e.text().indexOf(":") + 2);
                String[] time = date.substring(date.indexOf("(") + 1, date.indexOf(")") - 1).split(":");
                String[] dmy = date.substring(0, date.indexOf("(") - 1).split("\\.");
                // I use deprecated constructor because I find it reliable
                this.subsDataList.get(this.subsDataList.size() - 1).setDateAdded(new Date(Integer.valueOf(dmy[2]) - 1900, Integer.valueOf(dmy[1]) - 1, Integer.valueOf(dmy[0]),
                        Integer.valueOf(time[0]), Integer.valueOf(time[1])));
            } else if (e.is("table.tb_abtrid_3") && this.absent) {
                // Absent classes
                absentParser(e, false);
            } else if (e.is("table.tb_abucit_3") && this.absent) {
                // Absent teachers
                absentParser(e, true);
            } else if (e.is("table.tb_suplucit_3")) {
                // Changes for teachers
                Elements tableRows = e.select("tr");
                String currentTeacher = "";
                HashMap<String, HashMap<Integer, Lesson>> changesTeachers = this.subsDataList.get(this.subsDataList.size() - 1).getChangesTeachers();
                for (int i = 1; i < tableRows.size(); i++) {
                    Elements cells = tableRows.get(i).select("td");
                    if (cells.size() < 7) {
                        continue;
                    }
                    if (cells.first().text().length() > 2) {
                        currentTeacher = cells.first().text();
                        changesTeachers.put(currentTeacher, new HashMap<Integer, Lesson>());
                    }
                    Lesson change = new Lesson();
                    boolean previouslySet = changesTeachers.get(currentTeacher).get(Integer.valueOf(cells.get(1).text().substring(0, 1))) != null;
                    if (previouslySet) {
                        change = changesTeachers.get(currentTeacher).get(Integer.valueOf(cells.get(1).text().substring(0, 1)));
                    }
                    change.addEntry(cells.get(3).text(), "", cells.get(5).text(), cells.get(4).text(), cells.get(2).text());
                    if (!previouslySet) {
                        changesTeachers.get(currentTeacher).put(Integer.valueOf(cells.get(1).text().substring(0, 1)), change);
                    }
                }
            } else if (e.is("table.tb_supltrid_3")) {
                // Changes for classes
                Elements tableRows = e.select("tr");
                String currentClass = "";
                HashMap<String, HashMap<Integer, Lesson>> changesClasses = this.subsDataList.get(this.subsDataList.size() - 1).getChangesClasses();
                for (int i = 1; i < tableRows.size(); i++) {
                    Elements cells = tableRows.get(i).select("td");
                    if (cells.size() < 8) {
                        continue;
                    }
                    if (cells.first().text().length() > 2) {
                        currentClass = cells.first().text();
                        changesClasses.put(currentClass, new HashMap<Integer, Lesson>());
                    }
                    Lesson change = new Lesson();
                    boolean previouslySet = changesClasses.get(currentClass).get(Integer.valueOf(cells.get(1).text().substring(0, 1))) != null;
                    if (previouslySet) {
                        change = changesClasses.get(currentClass).get(Integer.valueOf(cells.get(1).text().substring(0, 1)));
                    }
                    String group = cells.get(3).text().contains("\\u00A0") ? "" : cells.get(3).text();
                    change.addEntry(cells.get(2).text(), cells.get(6).text(), cells.get(4).text(), group.length() > 1 ? group : "all", cells.get(5).text());
                    if (!previouslySet) {
                        changesClasses.get(currentClass).put(Integer.valueOf(cells.get(1).text().substring(0, 1)), change);
                    }
                }
            }
        }
    }

    /**
     * Overwrites parameter lessons with changes from parameter changes.
     *
     * @param lessons   Default lessons that will be overiden.
     * @param changes   Changes that will overide lessons.
     */
    public static void applyChanges(Lesson[] lessons, HashMap<Integer, Lesson> changes, boolean teacher) {
        for (int k : changes.keySet()) {
            if (teacher) {
                lessons[k] = changes.get(k);
            } else {
                for (int i = 0; i < changes.get(k).getGroups().size(); i++) {
                    int index = lessons[k].getGroups().indexOf(changes.get(k).getGroups().get(i));
                    if (index == -1) {
                        // Add lesson
                        lessons[k].addEntry(changes.get(k).getSubjects().get(i), changes.get(k).getTeachers().get(i),
                                changes.get(k).getRooms().get(i), changes.get(k).getGroups().get(i),
                                changes.get(k).getOrigins().get(i));
                    } else if (lessons[k].getOrigins().get(index).equals("default")) {
                        if (changes.get(k).getOrigins().get(i).equals("odpadá") ||
                                changes.get(k).getOrigins().get(i).equals("přesun >>") ||
                                changes.get(k).getOrigins().get(i).equals("výměna >>")) {
                            // Change origin
                            lessons[k].getOrigins().set(index, changes.get(k).getOrigins().get(i));
                        } else {
                            // Change the lesson
                            lessons[k].setEntry(index, changes.get(k).getSubjects().get(i),
                                    changes.get(k).getTeachers().get(i), changes.get(k).getRooms().get(i),
                                    changes.get(k).getGroups().get(i), changes.get(k).getOrigins().get(i));
                        }
                    } else if(!changes.get(k).getOrigins().get(i).equals("odpadá") &&
                            !changes.get(k).getOrigins().get(i).equals("přesun >>") &&
                            !changes.get(k).getOrigins().get(i).equals("výměna >>")) {
                        // Change the lesson
                        lessons[k].setEntry(index, changes.get(k).getSubjects().get(i),
                                changes.get(k).getTeachers().get(i), changes.get(k).getRooms().get(i),
                                changes.get(k).getGroups().get(i), changes.get(k).getOrigins().get(i));
                    }
                }
            }
        }
    }

    /**
     * @param e         Table that contains absent teachers or classes.
     * @param teacher   Determines if the table is teachers or not.
     */
    private void absentParser(Element e, boolean teacher) {
        Elements tableRows = e.select("tr");
        for (int i = 1; i < tableRows.size(); i++) {
            Elements cells = tableRows.get(i).select(teacher ? "td.td_abucit_3" : "td.td_abtrid_3");
            String name = "";
            for (int j = 0; j < cells.size(); j++) {
                HashMap<String, Lesson[]> absent = teacher ? this.subsDataList.get(this.subsDataList.size() - 1).getAbsentTeachers() : this.subsDataList.get(this.subsDataList.size() - 1).getAbsentClasses();
                if (j == 0) {
                    name = cells.get(j).text();
                    absent.put(name, new Lesson[cells.size()]);
                } else {
                    Lesson lesson = new Lesson();
                    lesson.addEntry(cells.get(j).text(), "", "", "all", "absent");
                    absent.get(name)[j - 1] = lesson;
                }
            }
        }
    }

    /**
     * @return All substitutions which were present at the time of parsing.
     */
    public ArrayList<SubsData> getSubsDataList() {
        return subsDataList;
    }

    /**
     * @return True if absent teachers and classes were parsed.
     */
    public boolean isAbsent() {
        return absent;
    }

}
