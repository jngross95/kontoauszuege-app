package com.example.kontoauszuege.service;

import com.example.kontoauszuege.model.BankAccountDataObject;
import com.example.kontoauszuege.model.BankContactDataObject;
import com.example.kontoauszuege.service.BankAccess.BankAccount;
import com.example.kontoauszuege.service.BankAccess.BankConnection;
import com.example.kontoauszuege.service.BankAccess.DlgCallback;
import com.example.kontoauszuege.service.DataAccess.DataAccessService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BankContactService {

	private final DataAccessService dataAccessService;

	public BankContactService(DataAccessService dataAccessService) {
		this.dataAccessService = dataAccessService;
	}

	public List<BankContactDataObject> getAllBankContacts() {
		return dataAccessService.getAll(BankContactDataObject.class);
	}

	public BankContactDataObject addBankContact(BankContactDataObject bankContactDataObject, DlgCallback callback) throws Exception {
		List<BankAccount> kontenVonBank;
		try (BankConnection connection = new BankConnection(
				bankContactDataObject.getName(),
				bankContactDataObject.getBic(),
				bankContactDataObject.getUser(),
				bankContactDataObject.getBankPin(),callback)) {
			connection.connect();
			kontenVonBank = connection.getAccounts();
		}

		BankContactDataObject gespeicherterKontakt = dataAccessService.insert(bankContactDataObject);
		fuegeFehlendeKontenHinzu(kontenVonBank);
		return gespeicherterKontakt;
	}

	public void deleteBankContact(BankContactDataObject bankContactDataObject) {
		dataAccessService.delete(bankContactDataObject);
	}

	private void fuegeFehlendeKontenHinzu(List<BankAccount> kontenVonBank) {
		if (kontenVonBank == null || kontenVonBank.isEmpty()) {
			return;
		}

		Set<String> vorhandeneIbans = dataAccessService.getAll(BankAccountDataObject.class).stream()
				.map(BankAccountDataObject::getIban)
				.map(this::normalisiereIban)
				.filter(iban -> !iban.isBlank())
				.collect(Collectors.toSet());

		for (BankAccount konto : kontenVonBank) {
			String iban = normalisiereIban(konto.iban);
			if (iban.isBlank() || vorhandeneIbans.contains(iban)) {
				continue;
			}

			BankAccountDataObject neuesKonto = new BankAccountDataObject();
			neuesKonto.setIban(iban);
			neuesKonto.setName(iban);
			neuesKonto.setBic(konto.bic);
			dataAccessService.insert(neuesKonto);

			vorhandeneIbans.add(iban);
		}
	}

	private String normalisiereIban(String iban) {
		if (iban == null) {
			return "";
		}
		return iban.trim().replace(" ", "").toUpperCase(Locale.ROOT);
	}

}
