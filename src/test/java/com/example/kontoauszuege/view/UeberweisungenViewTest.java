package com.example.kontoauszuege.view;

import com.example.kontoauszuege.service.BankAccountService;
import com.example.kontoauszuege.service.BankStatementService;
import com.example.kontoauszuege.service.BaseService;
import com.example.kontoauszuege.service.UeberweisungService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UeberweisungenViewTest {

    @Test
    void neueUeberweisungAktiviertInstantCheckbox() {
        UeberweisungService ueberweisungService = new UeberweisungService(null, null, null) {
            @Override
            public List<com.example.kontoauszuege.model.UeberweisungDataObject> findAll() {
                return List.of();
            }
        };
        BankAccountService bankAccountService = new BankAccountService(null) {
            @Override
            public List<com.example.kontoauszuege.model.BankAccountDataObject> getAllBankAccounts() {
                return List.of();
            }
        };
        BankStatementService bankStatementService = new BankStatementService(null, null, null) {
            @Override
            public List<com.example.kontoauszuege.model.BankStatementDataObject> getAllStatements() {
                return List.of();
            }
        };
        BaseService baseService = new BaseService(null);

        UeberweisungenView view = new UeberweisungenView(
                ueberweisungService,
                bankAccountService,
                bankStatementService,
                baseService);

        UI.setCurrent(new UI());
        try {
            Button neuButton = findComponent(view, Button.class, "Neu");
            neuButton.click();

            Checkbox instantCheckbox = findComponent(
                    view, Checkbox.class, "Echtzeit (SEPA Instant)");
            assertTrue(instantCheckbox.getValue());
        } finally {
            UI.setCurrent(null);
        }
    }

    private static <T extends Component> T findComponent(
            Component root, Class<T> componentType, String label) {
        return findComponentOptional(root, componentType, label)
                .orElseThrow(() -> new AssertionError(
                        "Komponente nicht gefunden: " + label));
    }

    private static <T extends Component> java.util.Optional<T> findComponentOptional(
            Component root, Class<T> componentType, String label) {
        if (componentType.isInstance(root)) {
            T component = componentType.cast(root);
            String text = component instanceof Button button ? button.getText()
                    : component instanceof Checkbox checkbox ? checkbox.getLabel()
                    : null;
            if (label.equals(text)) {
                return java.util.Optional.of(component);
            }
        }

        return root.getChildren()
                .map(child -> findComponentOptional(child, componentType, label))
                .flatMap(java.util.Optional::stream)
                .findFirst();
    }
}
