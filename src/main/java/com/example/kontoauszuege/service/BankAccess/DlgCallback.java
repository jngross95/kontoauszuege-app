package com.example.kontoauszuege.service.BankAccess;

@FunctionalInterface
public interface DlgCallback {
    String dlg(String kontaktName, String inputFieldText, String message, byte[] image);
}
