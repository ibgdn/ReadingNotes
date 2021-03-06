package com.ibgdn.chapter_9;

public class MagicNumber {
    public static final int TYPE = 1;

    private int id;
    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        try {
            this.id = id;
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    public String getNameJ() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}