package com.example.kontoauszuege.model;

import com.example.kontoauszuege.service.DataAccess.DataObject;

public class BankAccountDataObject extends DataObject {

    private String name;
    private String bic;
    private String iban;
    private Integer orderIndex;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBic() {
        return bic;
    }

    public void setBic(String bic) {
        this.bic = bic;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}