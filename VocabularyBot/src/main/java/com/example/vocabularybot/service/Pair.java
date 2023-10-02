package com.example.vocabularybot.service;

public class
Pair<Long, String> {
    private final Long key;
    private final String value;

    public Pair(Long key, String value) {
        this.key = key;
        this.value = value;
    }



    public Long getKey() {
        return this.key;
    }

    public String getValue() {
        return this.value;
    }
}
