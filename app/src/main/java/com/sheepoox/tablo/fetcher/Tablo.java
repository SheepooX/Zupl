package com.sheepoox.tablo.fetcher;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Created by Tomáš Černý
 * As part of project Zupl, 2017
 */

public class Tablo implements Serializable {

    /**
     * Address of the document.
     */
    protected final String address;

    /**
     * Address of the embedded doc.
     */
    protected final String fileAddress;

    /**
     * The embedded document.
     * @see Document
     */
    protected Document doc;

    /**
     * True if the connection was successful.
     */
    protected boolean connectionSuccessful = false;

    /**
     * Saves the filename's HTML in doc.
     *
     * @param address   Address of the document.
     * @param name      Value of the iframe's attribute name.
     */
    public Tablo(String address, String name) {
        if (isAddressOk(address)) {
            this.address = address;
        } else {
            System.err.println("Address is not valid!");
            this.address = null;
        }
        this.fileAddress = getSwap(address, getSource(address, name), "/");
        System.out.println(this.fileAddress);
        try {
            if (this.fileAddress == null) {
                this.doc = Jsoup.connect(address).get();
                //this.doc = Jsoup.parse(new URL(this.address).openStream(), "UTF-16", this.address);
            } else {
                this.doc = Jsoup.connect(this.fileAddress).get();
                //this.doc = Jsoup.parse(new URL(this.fileAddress).openStream(), "UTF-16", this.fileAddress);
            }
            connectionSuccessful = true;
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * @param address   The address to test.
     * @return          True if the address is valid.
     */
    public static boolean isAddressOk(String address) {
        try {
            URL url = new URL(address);
            url.toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @param path      The original path.
     * @param file      The filename.
     * @param separator The separator between individual path parts.
     * @return          The file's path.
     */
    private String getSwap(String path, String file, String separator) {
        String str = cutAfterX(path, separator);
        if (str == null || file == null) {
            return null;
        } else {
            return str + file;
        }
    }

    /**
     * @param str   The original string.
     * @param x     The place at which the str is cut.
     * @return      Modified string where anything after x is removed.
     */
    private String cutAfterX(String str, String x) {
        if (str.length() > 0 && str != null) {
            return str.substring(0, str.lastIndexOf(x) + 1);
        } else return null;
    }

    /**
     * @param pageAddress   The address of the document.
     * @param name          The value of iframe's attribute name (embedded document).
     * @return              The address of the iframe's document.
     */
    private String getSource(String pageAddress, String name) {
        Document doc;
        try {
            doc = getDoc(pageAddress);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return null;
        }
        Element ele = doc.select("frameset > frame[name=" + name + "]").first();
        return ele.attr("src");
    }

    /**
     * @param address   The address of the document.
     * @return          The embedded TML document.
     * @throws IOException
     * @see Document
     */
    private Document getDoc(String address) throws IOException {
        return Jsoup.connect(address).get();
    }

    /**
     * @return  The embedded HTML document.
     */
    public Document getDoc() {
        return this.doc;
    }

    /**
     * Empties the doc.
     */
    public void emptyDocument() {
        this.doc = null;
    }

    /**
     * @return  The address of the document.
     */
    public String getAddress() {
        return this.address;
    }

    /**
     * @return  The address of the file's document.
     */
    public String getFileAddress() {
        return this.fileAddress;
    }

    /**
     * @return True if the connection was successful.
     */
    public boolean isConnectionSuccessful() {
        return connectionSuccessful;
    }
}
