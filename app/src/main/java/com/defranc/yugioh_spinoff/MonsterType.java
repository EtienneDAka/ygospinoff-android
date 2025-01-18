package com.defranc.yugioh_spinoff;

public enum MonsterType {
    S("SPELLCASTER"),
    DR("DRAGON"),
    Z("ZOMBI"),
    W("WARRIOR"),
    B("BEAST"),
    D("DEMON");

    private final String value;

    MonsterType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}