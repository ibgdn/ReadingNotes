package com.ibgdn.chapter_7.mat;

import java.util.List;
import java.util.Vector;

public class Student {
    private int id;
    private String name;
    private List<WebPage> history = new Vector<>();

    public Student(int id, String name) {
        super();
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<WebPage> getHistory() {
        return history;
    }

    public void setHistory(List<WebPage> history) {
        this.history = history;
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", history=" + history +
                '}';
    }

    public void visit(WebPage webPage) {
        System.out.println(webPage);
    }
}
