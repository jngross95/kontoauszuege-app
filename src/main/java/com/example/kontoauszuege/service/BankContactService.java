package com.example.kontoauszuege.service;

import com.example.kontoauszuege.model.BankContact;
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

	public List<BankContact> getAllBankContacts() {
		return dataAccessService.getAll(BankContact.class);
	}

	public BankContact addBankContact(BankContact bankContact) throws Exception {
		try (BankConnection connection = new BankConnection(
				bankContact.getName(),
				bankContact.getBic(),
				bankContact.getUser(),
				bankContact.getBankPin())) {
			connection.connect();
		}

		return dataAccessService.insert(bankContact);
	}

}
