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
            .sorted(Comparator.comparing(BankAccountDataObject::getOrderIndex,
                Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Comparator.comparing(BankAccountDataObject::getName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))))
            .toList();
    }

    public BankAccountDataObject addBankAccount(BankAccountDataObject bankAccountDataObject) {
        // Ensure a stable orderIndex: append to end if not provided
        if (bankAccountDataObject.getOrderIndex() == null) {
            List<BankAccountDataObject> all = getAllBankAccounts();
            int max = all.stream()
                    .map(BankAccountDataObject::getOrderIndex)
                    .filter(i -> i != null)
                    .max(Integer::compareTo)
                    .orElse(0);
            bankAccountDataObject.setOrderIndex(max + 1);
        }
        return dataAccessService.insert(bankAccountDataObject);
    }

    public BankAccountDataObject updateBankAccount(BankAccountDataObject bankAccountDataObject) {
        return dataAccessService.update(bankAccountDataObject);
    }

    public void deleteBankAccount(BankAccountDataObject bankAccountDataObject) {
        dataAccessService.delete(bankAccountDataObject);
    }
}