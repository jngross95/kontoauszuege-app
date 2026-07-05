package com.example.kontoauszuege.service.BankAccess;

/**
 * Einfacher Datenträger für Bank-Informationen.
 */
public class BankInformation {
    private final String bic;
    private final String name;
    private final String blz;

    public BankInformation(String bic, String name, String blz) {
        this.bic = bic;
        this.name = name;
        this.blz = blz;
    }

    public String getBic() {
        return bic;
    }

    public String getName() {
        return name;
    }

    public String getBlz() {
        return blz;
    }

    @Override
    public String toString() {
        return "BankInformation{" +
                "bic='" + bic + '\'' +
                ", name='" + name + '\'' +
                ", blz='" + blz + '\'' +
                '}';
    }
}
