package com.example.kontoauszuege.service.BankAccess;

/**
 * Einfacher Datenträger für Bank-Informationen.
 */
public class BankInformation {
    private final String bic;
    private final String name;

    public BankInformation(String bic, String name) {
        this.bic = bic;
        this.name = name;
    }

    public String getBic() {
        return bic;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "BankInformation{" +
                "bic='" + bic + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
