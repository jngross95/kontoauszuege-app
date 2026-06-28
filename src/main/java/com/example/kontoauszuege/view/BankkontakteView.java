package com.example.kontoauszuege.view;

import com.example.kontoauszuege.model.BankContactDataObject;
import com.example.kontoauszuege.service.BankContactService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "bankkontakte", layout = MainLayout.class)
@PageTitle("Bankkontakte")
public class BankkontakteView extends VerticalLayout {

    private final BankContactService bankContactService;

    private final Grid<BankContactDataObject> grid = new Grid<>(BankContactDataObject.class, false);
    private BankContactDataObject selected;

    public BankkontakteView(BankContactService bankContactService) {
        this.bankContactService = bankContactService;

        setSizeFull();
        setPadding(true);
        setSpacing(false);

        add(createToolbar());
        add(new Hr());

        configureGrid();
        add(grid);
        setFlexGrow(1, grid);

        refreshGrid();
    }

    private HorizontalLayout createToolbar() {
        Button neuButton = new Button("Neu", VaadinIcon.PLUS.create(), e -> openNewContactDialog());
        neuButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button loeschenButton = new Button("Löschen", VaadinIcon.TRASH.create(), e -> deleteSelectedContact());
        loeschenButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

        HorizontalLayout toolbar = new HorizontalLayout(neuButton, loeschenButton);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.getStyle().set("padding-bottom", "var(--lumo-space-s)");
        return toolbar;
    }

    private void configureGrid() {
        grid.addColumn(BankContactDataObject::getName)
                .setHeader("Name")
                .setResizable(true)
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(BankContactDataObject::getBic)
                .setHeader("BIC")
                .setResizable(true)
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(BankContactDataObject::getUser)
                .setHeader("Benutzer")
                .setResizable(true)
                .setSortable(true)
                .setAutoWidth(true);

        grid.setWidthFull();
        grid.addSelectionListener(e -> selected = e.getFirstSelectedItem().orElse(null));
    }

    private void openNewContactDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Neuen Bankkontakt anlegen");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("33vw");
        dialog.getElement().setAttribute("autocomplete", "off");

        TextField nameField = new TextField("Name");
        nameField.setWidthFull();
        nameField.getElement().setAttribute("autocomplete", "off");
        nameField.getElement().setAttribute("name", "bank-contact-name");

        TextField bicField = new TextField("BIC");
        bicField.setWidthFull();
        bicField.getElement().setAttribute("autocomplete", "off");
        bicField.getElement().setAttribute("name", "bank-contact-bic");

        TextField userField = new TextField("Benutzer");
        userField.setWidthFull();
        userField.getElement().setAttribute("autocomplete", "off");
        userField.getElement().setAttribute("name", "bank-contact-user");

        TextField pinField = new TextField("Bank-PIN");
        pinField.setWidthFull();
        pinField.getElement().executeJs("this.inputElement.style.webkitTextSecurity = 'disc';");
        pinField.getElement().setAttribute("autocomplete", "off");
        pinField.getElement().setAttribute("name", "bank-contact-pin");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        form.add(nameField, 2);
        form.add(bicField, 2);
        form.add(userField, 2);
        form.add(pinField, 2);

        dialog.add(form);

        Button okButton = new Button("Ok", VaadinIcon.CHECK.create(), e -> {
            if (bicField.isEmpty() || userField.isEmpty() || pinField.isEmpty()) {
                Notification.show("Bitte BIC, Benutzer und Bank-PIN ausfüllen.",
                        3000, Notification.Position.MIDDLE);
                return;
            }

            BankContactDataObject newContact = new BankContactDataObject();
            newContact.setName(nameField.getValue());
            newContact.setBic(bicField.getValue());
            newContact.setUser(userField.getValue());
            newContact.setBankPin(pinField.getValue());

            try {
                bankContactService.addBankContact(newContact);
                pinField.clear();
                dialog.close();
                refreshGrid();

                Notification success = Notification.show("Bankkontakt wurde angelegt.",
                        2500, Notification.Position.MIDDLE);
                success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification error = Notification.show(
                        "Bankzugang konnte nicht geprüft werden: " + ex.getMessage(),
                        5000,
                        Notification.Position.MIDDLE
                );
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button abbrechenButton = new Button("Abbrechen", e -> dialog.close());
        abbrechenButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(abbrechenButton, okButton);
        add(dialog);
        dialog.open();
    }

    private void deleteSelectedContact() {
        if (selected == null) {
            Notification.show("Bitte zuerst einen Kontakt auswählen.",
                    2500, Notification.Position.MIDDLE);
            return;
        }

        bankContactService.deleteBankContact(selected);
        refreshGrid();

        Notification deleted = Notification.show("Bankkontakt wurde gelöscht.",
            2500, Notification.Position.MIDDLE);
        deleted.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void refreshGrid() {
        List<BankContactDataObject> bankContactDataObjects = bankContactService.getAllBankContacts();
        grid.setItems(bankContactDataObjects);
        selected = null;
    }
}