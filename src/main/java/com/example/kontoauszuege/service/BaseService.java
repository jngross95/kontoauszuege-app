package com.example.kontoauszuege.service;

import com.example.kontoauszuege.service.BankAccess.BankConnection;
import com.example.kontoauszuege.service.BankAccess.BankInformation;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class BaseService {

    private final Environment environment;

    public BaseService(Environment environment) {
        this.environment = environment;
    }

    /**
     * Bestimmt Icon-Dateinamen für eine Bank anhand der BIC.
     * Liest die `bank-icons` Konfiguration aus `application.yml`.
     *
     * @param bic BIC der Bank
     * @return Icon-Name oder null, wenn kein Icon gefunden wurde
     */
    public String getIconFromBic(String bic) {
        try {
            BankInformation info = BankConnection.GetBankInfos(bic);
            if (info == null) return null;
            String bankName = info.getName();

            List<BankIcon> icons = Binder.get(environment)
                    .bind("bank-icons", Bindable.listOf(BankIcon.class))
                    .orElseThrow(() -> new RuntimeException("Bank icons configuration not found"));

            if (bankName == null) return null;

            String lowerName = bankName.toLowerCase(Locale.ROOT);

            for (BankIcon entry : icons) {
                if (entry.getTarget() == null) continue;
                String[] parts = entry.getTarget().split("\\|");
                for (String p : parts) {
                    if (p == null) continue;
                    String t = p.trim();
                    if (t.length() == 0) continue;
                    if (lowerName.contains(t.toLowerCase(Locale.ROOT))) {
                        return entry.getIcon();
                    }
                }
            }

            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
