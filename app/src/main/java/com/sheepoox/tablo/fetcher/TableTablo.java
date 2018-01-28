package com.sheepoox.tablo.fetcher;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Created by Tomáš Černý
 * As part of project Zupl, 2017
 */

public class TableTablo extends Tablo implements Serializable {

    /**
     * Individual timetables in html.
     * @see Elements
     */
    protected Elements tablos;

    /**
     * True if the timetable(s) belong to teachers.
     */
    private boolean teacher = false;

    /**
     * Specified owner a timetable.
     */
    private String cls;

    /**
     * True if the given owner's timetable was found in the document.
     */
    private boolean clsFound = false;

    /**
     * True if the owner was given.
     */
    private boolean clsGiven = false;

    /**
     * Timetables.
     * @see TableData
     */
    protected TableData[] dataArr;

    /**
     * Parses all timetables that in the document.
     * @param address   The address of the document.
     */
    public TableTablo(String address) {
        this(address, "");
    }

    /**
     * Parses timetable of the owner if found.
     * @param address   The address of the docuemnt.
     * @param cls       The specified owner.
     */
    public TableTablo(String address, String cls) {
        super(address, "rozvxxxx");
        this.cls = cls;
        this.clsGiven = !cls.isEmpty();
        // Getting the elements from the doc
        this.tablos = this.doc.select("table.tb_rozvrh_1");
        if (this.tablos.size() == 0) {
            this.teacher = true;
            this.tablos = this.doc.select("table.tb_rozvrh_2");
        }
        // Extracting
        this.dataArr = new TableData[this.tablos.size()];

        Iterator<Element> iter = this.tablos.iterator();
        int c = 0;
        while (iter.hasNext() && !this.clsFound) {
            parser(iter.next().select("tr"), c++);
        }
    }

    /**
     * Parses one timetable.
     * @param trs   The timetable
     * @param index The index which the parsed timetable should be added in tableDataArr.
     * @see Elements
     */
    private void parser(Elements trs, int index) {
        TableData t = new TableData();
        // Owner is 1.A, 2.B etc.
        t.setOwner(getOwner(trs.get(1)));
        // Checks if supplied class is same as table's class;
        if (this.clsGiven) {
            if (t.getOwner().toLowerCase().replaceAll(" ", "").replace(".", "")
                    .equals(this.cls.toLowerCase().replaceAll(" ", "").replace(".", ""))) {
                this.clsFound = true;
            } else {
                return;
            }
        }
        // Main teacher of the class
        if (!this.teacher) {
            t.setMentor(getMentor(trs.get(1)));
        } else {
            t.setMentor(t.getOwner());
        }
        // Times
        t.setTimes(getTimes(trs.get(2)));
        t.setLessons(getLessons(trs));

        // If we want only one or many classes
        if (this.clsGiven) {
            this.dataArr[0] = t;
        } else {
            this.dataArr[index] = t;
        }
    }

    /**
     * @param dyn   The element in which the mentor is.
     * @return      The parsed mentor.
     * @see Element
     */
    private String getMentor(Element dyn) {
        String mentor = dyn.select("span[class^=textnormal_]").text();
        if (mentor.isEmpty()) {
            mentor = dyn.select("span[class^=textlargebold_]").text();
        }
        return mentor.substring(1, mentor.contains(",") ? mentor.indexOf(",") : mentor.length()).replaceAll("\\u00A0", "");
    }

    /**
     * @param dyn   The element in which the owner is.
     * @return      The parsed owner.
     * @see Element
     */
    private String getOwner(Element dyn) {
        String owner = dyn.select("span[class^=textlargebold_]").toString();
        return owner.substring(owner.indexOf(">") + 13, owner.indexOf("&nbsp;&nbsp;<"));
    }

    /**
     * @param dyn   The elements in which the times are.
     * @return      The parsed times.
     * @see Element
     */
    private String[] getTimes(Element dyn) {
        Elements timesE = dyn.select("span[class^=textsmall_]");
        String[] times = new String[timesE.size()];
        for (int c = 0; c < timesE.size(); c++) {
            times[c] = timesE.get(c).text();
        }
        return times;
    }

    /**
     * @param trs   The elements in which the lessons are.
     * @return      The lessons for a week.
     * @see Lesson
     * @see Elements
     */
    private Lesson[][] getLessons(Elements trs) {
        ArrayList<Elements> dow = splitIntoDays(trs);
        Lesson[][] allLessons = new Lesson[5][13];
        for (int c = 0; c < dow.size(); c++) {
            allLessons[c] = getDay(dow.get(c));
        }
        return allLessons;
    }

    /**
     * @param day   The elements that make one day.
     * @return      The lessons for a day.
     * @see Elements
     */
    private Lesson[] getDay(Elements day) {
        int maxRowspan = maxRowspan(day.first());
        // Counter for the rows when finding SplitLessons
        int[] linePointer = new int[maxRowspan + 1];
        Elements zero = day.first().select("td[width=7%]");
        Lesson[] lessons = new Lesson[13];
        // For every lesson
        for (int c = 0; c < lessons.length; c++) {
            // I was lazy typing this over and over
            Element td = zero.get(c);
            // Rowspan of the cell
            int rowspan = td.attr("rowspan").isEmpty() ? 1 : Integer.valueOf(td.attr("rowspan"));
            Lesson lesson = new Lesson();
            // Is the lesson for the whole class?
            if (rowspan == maxRowspan) {
                if (td.select("p > span[class^=textnormal_]").size() > 2) {
                    Elements normal = td.select("p > span[class^=textnormal_]");
                    lesson.getSubjects().add(td.select("p > span[class^=textlargebold_]").first().text());
                    lesson.getGroups().add(normal.get(0).text().replaceAll("[()]", ""));
                    lesson.getTeachers().add(normal.get(1).text());
                    lesson.getRooms().add(normal.get(2).text());
                } else if (td.select("p > span[class^=textsmall_]").size() == 0 && td.select("p > span[class^=textnormal_]").size() == 0) {
                    lesson.getSubjects().add("");
                    lesson.getTeachers().add("");
                    lesson.getRooms().add("");
                    lesson.getGroups().add("all");
                } else {
                    lesson.getSubjects().add(td.select("p > span[class^=textlargebold_]").text());
                    Elements roomTeacher = td.select("p > span[class^=textnormal_]");
                    lesson.getTeachers().add(roomTeacher.get(0).text().replaceAll("[()]", ""));
                    lesson.getRooms().add(roomTeacher.get(1).text());
                    lesson.getGroups().add("all");
                }
                lesson.getOrigins().add("default");
            } else {
                for (int j = 0; j < maxRowspan; j += rowspan) {
                    if (j == 0) {
                        // First lesson in block
                        parseSplitLesson(lesson, td);
                    } else {
                        // Other lessons in block
                        parseSplitLesson(lesson, day.get(j).select("td[width=7%]").get(linePointer[j]++));
                    }
                }

            }
            if (teacher) {
                String group = lesson.getSubjects().get(0) + " " + lesson.getGroups().get(0);
                String subject = lesson.getTeachers().get(0);
                lesson.getGroups().set(0, group);
                lesson.getSubjects().set(0, subject);
                lesson.getTeachers().set(0, "");
            }
            lessons[c] = lesson;
        }
        return lessons;
    }

    /**
     * @param split Lesson in which the information will be written.
     * @param cell  Cell which holds the information.
     * @see Lesson
     * @see Element
     */
    private void parseSplitLesson(Lesson split, Element cell) {
        Elements smaller = cell.select("span[class^=textsmaller_]");
        Elements small = cell.select("span[class^=textsmall_]");
        Elements normal = cell.select("span[class^=textnormal_]");
        Elements bold = cell.select("span[class^=textlargebold_]");
        if (bold.size() == 1 && normal.size() == 3) {
            // One in two
            split.addEntry(bold.get(0).text(), normal.get(1).text(), normal.get(2).text(), normal.get(0).text().replaceAll("[()]", ""), "default");
        } else if (normal.size() == 1 && small.size() == 3) {
            // One in three
            split.addEntry(normal.get(0).text(), small.get(1).text(), small.get(2).text(), small.get(0).text().replaceAll("[()]", ""), "default");
        } else if (smaller.size() == 5) {
            // One in more than three, style 1 - not labs
            split.addEntry(smaller.get(0).text() + smaller.get(1).text(), smaller.get(3).text(), smaller.get(4).text(), smaller.get(2).text().replaceAll("[()]", ""), "default");
        } else if (smaller.size() == 4) {
            // One in more than three, style 2 - labs/second languages
            split.addEntry(smaller.get(0).text(), smaller.get(2).text(), smaller.get(3).text(), smaller.get(1).text().replaceAll("[()]", ""), "default");
        } else if (small.size() == 4 && normal.size() == 1) {
            split.addEntry(small.get(0).text() + normal.get(0).text().replaceAll("\\u00A0", ""), small.get(2).text(), small.get(3).text(), small.get(1).text().replaceAll("[()]", ""), "default");
        }
    }

    /**
     * @param day   The element of one day.
     * @return      The highest found rowspan in the given day.
     * @see Element
     */
    private int maxRowspan(Element day) {
        int ret = 1;
        for (Element td : day.select("td[width=7%]")) {
            String attr = td.attr("rowspan");
            int rowspan = attr.isEmpty() ? 0 : Integer.parseInt(attr);
            if (rowspan > ret) {
                ret = rowspan;
            }
        }
        return ret;
    }

    /**
     * @param trs   Elements of a whole week.
     * @return      Split Elements in an ArrayList.
     * @see ArrayList
     * @see Elements
     */
    private ArrayList<Elements> splitIntoDays(Elements trs) {
        ArrayList<Elements> dow = new ArrayList<>(5);
        Elements currentArLi = new Elements();
        for (int c = 3; c < trs.size() - 1; c++) {
            currentArLi.add(trs.get(c));
            if (containsDoW(trs.get(c + 1).select("td[width=3%] > p > span[class^=textlargebold_]").first())) {
                dow.add(currentArLi);
                currentArLi = new Elements();
            }
        }
        dow.add(currentArLi);
        return dow;
    }

    /**
     * @param el    The element which may have a day in it.
     * @return      True if a day is in the element.
     * @see Element
     */
    private boolean containsDoW(Element el) {
        if (el != null) {
            String text = el.text();
            if (text.contains("P o") || text.contains("Ú t")
                    || text.contains("S t") || text.contains("Č t")
                    || text.contains("P á")) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return  The individual timetables in HTML.
     * @see Elements
     */
    public Elements getTablos() {
        return this.tablos;
    }

    /**
     * @return  The parsed timetables.
     * @see TableData
     */
    public TableData[] getTableDataArr() {
        return this.dataArr;
    }
}
