package com.example.kontoauszuege.service;

import com.example.kontoauszuege.model.BankAccountDataObject;
import com.example.kontoauszuege.service.DataAccess.DataAccessService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class BankAccountService {

    private final DataAccessService dataAccessService;

    public BankAccountService(DataAccessService dataAccessService) {
        this.dataAccessService = dataAccessService;
    }

    public List<BankAccountDataObject> getAllBankAccounts() {
        return dataAccessService.getAll(BankAccountDataObject.class).stream()
                .sorted(Comparator.comparing(BankAccountDataObject::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    public BankAccountDataObject addBankAccount(BankAccountDataObject bankAccountDataObject) {
        return dataAccessService.insert(bankAccountDataObject);
    }

    public BankAccountDataObject updateBankAccount(BankAccountDataObject bankAccountDataObject) {
        return dataAccessService.update(bankAccountDataObject);
    }

    public void deleteBankAccount(BankAccountDataObject bankAccountDataObject) {
        dataAccessService.delete(bankAccountDataObject);
    }
}