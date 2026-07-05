package com.example.kontoauszuege.service;

/**
 * POJO zum Binden der Einträge aus `bank-icons` in application.yml
 */
public class BankIcon {
    private String target;
    private String icon;

    public BankIcon() {
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
