package com.example.kontoauszuege.service.BankAccess;

public class BankAccount {
    public String iban;
    public String bic;
    public BankAccount() {
    }

    public BankAccount(String iban, String bic) {
        this.iban = iban;
        this.bic = bic;
    }
}
