package com.ibgdn.chapter_7.mat;

import java.util.List;
import java.util.Vector;

public class TraceStudent {
    static List<WebPage> webPages = new Vector<>();

    public static void createWebPages() {
        for (int i = 0; i < 100; i++) {
            WebPage webPage = new WebPage();
            webPage.setUrl("http://www." + Integer.toString(i) + ".com");
            webPage.setContent(Integer.toString(i));
            webPages.add(webPage);
        }
    }

    public static void main(String[] args) {
        createWebPages();
        Student billy = new Student(3, "billy");
        Student alice = new Student(5, "alice");
        Student taotao = new Student(7, "taotao");

        for (int i = 0; i < webPages.size(); i++) {
            if (i % billy.getId() == 0) {
                billy.visit(webPages.get(i));
            }
            if (i % alice.getId() == 0) {
                alice.visit(webPages.get(i));
            }
            if (i % taotao.getId() == 0) {
                taotao.visit(webPages.get(i));
            }
        }
        webPages.clear();
        System.gc();
    }
}
