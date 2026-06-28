package com.example.kontoauszuege.service;

import com.example.kontoauszuege.model.BankStatementDataObject;
import com.example.kontoauszuege.service.DataAccess.DataAccessService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BankStatementService {

    private final DataAccessService dataAccessService;

    public BankStatementService(DataAccessService dataAccessService) {
        this.dataAccessService = dataAccessService;
    }

    public List<BankStatementDataObject> getAllStatements() {
        return dataAccessService.getAll(BankStatementDataObject.class);
    }

    public BankStatementDataObject addStatement(BankStatementDataObject statement) {
        return dataAccessService.insert(statement);
    }
}
