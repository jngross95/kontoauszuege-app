package com.example.kontoauszuege.service;

import com.example.kontoauszuege.model.BankContactDataObject;
import com.example.kontoauszuege.service.BankAccess.BankConnection;
import com.example.kontoauszuege.service.DataAccess.DataAccessService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BankContactService {

	private final DataAccessService dataAccessService;

	public BankContactService(DataAccessService dataAccessService) {
		this.dataAccessService = dataAccessService;
	}

	public List<BankContactDataObject> getAllBankContacts() {
		return dataAccessService.getAll(BankContactDataObject.class);
	}

	public BankContactDataObject addBankContact(BankContactDataObject bankContactDataObject) throws Exception {
		try (BankConnection connection = new BankConnection(
				bankContactDataObject.getName(),
				bankContactDataObject.getBic(),
				bankContactDataObject.getUser(),
				bankContactDataObject.getBankPin())) {
			connection.connect();
		}

		return dataAccessService.insert(bankContactDataObject);
	}

	public void deleteBankContact(BankContactDataObject bankContactDataObject) {
		dataAccessService.delete(bankContactDataObject);
	}

}
