package com.example.project;

public class Singer {
    private final String group;
    private final String debuted;
    private final String name;

    public Singer(String group, String debuted, String name) {
        this.group = group;
        this.debuted = debuted;
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public String getDebuted() {
        return debuted;
    }

    public String getName() {
        return name;
    }
}

