package com.example.kontoauszuege.model;

import com.example.kontoauszuege.service.DataAccess.DataObject;

public class BankContact extends DataObject {

    private String name;
    private String bic;
    private String user;
    private String bankPin;

    public String getName() {
        return name;
    }
    public void  setName(String str) {
        this.name = str;
    }

    public String getBic() {
        return bic;
    }

    public void setBic(String bic) {
        this.bic = bic;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getBankPin() {
        return bankPin;
    }

    public void setBankPin(String bankPin) {
        this.bankPin = bankPin;
    }
}
