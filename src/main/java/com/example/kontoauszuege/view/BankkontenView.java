package com.example.kontoauszuege.view;

import com.example.kontoauszuege.model.BankAccountDataObject;
import com.example.kontoauszuege.service.BankAccountService;
import com.example.kontoauszuege.service.BaseService;
import com.example.kontoauszuege.service.BankStatementService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "bankkonten", layout = MainLayout.class)
@PageTitle("Bankkonten")
public class BankkontenView extends VerticalLayout {

    private static final String AUTOCOMPLETE_ATTR = "autocomplete";

    private final transient BankAccountService bankAccountService;
    private final transient BankStatementService bankStatementService;
    private final transient BaseService baseService;

    private final Grid<BankAccountDataObject> grid = new Grid<>(BankAccountDataObject.class, false);
    private transient BankAccountDataObject selected;

    public BankkontenView(BankAccountService bankAccountService,
                          BankStatementService bankStatementService,
                          BaseService baseService) {
        this.bankAccountService = bankAccountService;
        this.bankStatementService = bankStatementService;
        this.baseService = baseService;

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
        Button newButton = new Button("Neu", VaadinIcon.PLUS.create(), e -> openNewAccountDialog());
        newButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button editButton = new Button("Bearbeiten", VaadinIcon.EDIT.create(), e -> openEditAccountDialog());
        editButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        Button deleteButton = new Button("Löschen", VaadinIcon.TRASH.create(), e -> deleteSelectedAccount());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

        HorizontalLayout toolbar = new HorizontalLayout(newButton, editButton, deleteButton);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.getStyle().set("padding-bottom", "var(--lumo-space-s)");
        return toolbar;
    }

    private void configureGrid() {
        // Reorder column with Up / Down buttons
        var reorderColumn = grid.addColumn(new ComponentRenderer<>(account -> {
            boolean isSelected = selected != null
                    && selected.getPk() != null
                    && selected.getPk().equals(account.getPk());

            HorizontalLayout layout = new HorizontalLayout();
            layout.setSpacing(false);
            layout.setPadding(false);
            layout.setAlignItems(FlexComponent.Alignment.CENTER);

            Button up = new Button(VaadinIcon.ARROW_UP.create(), e -> moveUp(account));
            up.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            up.getElement().setAttribute("title", "Nach oben");
            up.setEnabled(isSelected);

            Button down = new Button(VaadinIcon.ARROW_DOWN.create(), e -> moveDown(account));
            down.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            down.getElement().setAttribute("title", "Nach unten");
            down.setEnabled(isSelected);

            layout.add(up, down);
            return layout;
        }));
        reorderColumn.setHeader("text");
        reorderColumn.setAutoWidth(false);
        reorderColumn.setFlexGrow(0);
        reorderColumn.setWidth("80px");
        reorderColumn.setFrozen(true);

        grid.addColumn(new ComponentRenderer<>(account -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setAlignItems(FlexComponent.Alignment.CENTER);
            layout.setSpacing(true);

            String bic = account.getBic();
            if (bic != null && !bic.isBlank()) {
                try {
                    String iconName = baseService.getIconFromBic(bic);
                    if (iconName != null && !iconName.isBlank()) {
                        Image img = new Image("icons/" + iconName, account.getName());
                        img.setHeight("20px");
                        img.getStyle().set("object-fit", "contain");
                        layout.add(img);
                    }
                } catch (Exception ignored) {
                    // kein Icon verfügbar
                }
            }
            layout.add(new Span(account.getName() != null ? account.getName() : ""));
            return layout;
        }))
                .setHeader("Name")
                .setResizable(true)
                .setSortable(true)
                .setComparator(java.util.Comparator.comparing(
                        a -> a.getName() != null ? a.getName() : "",
                        String.CASE_INSENSITIVE_ORDER))
                .setAutoWidth(true);

        grid.addColumn(BankAccountDataObject::getBic)
                .setHeader("BIC")
                .setResizable(true)
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(BankAccountDataObject::getIban)
                .setHeader("IBAN")
                .setResizable(true)
                .setSortable(true)
                .setAutoWidth(true);

        grid.setWidthFull();
        grid.addSelectionListener(e -> {
            selected = e.getFirstSelectedItem().orElse(null);
            grid.getDataProvider().refreshAll();
        });

        // Rechtsklick selektiert die Zeile (JS-seitig, synct zurück zum Server)
        grid.getElement().executeJs("""
                this.addEventListener('contextmenu', e => {
                    const ctx = this.getEventContext(e);
                    if (ctx && ctx.item != null) {
                        this.selectedItems = [ctx.item];
                    }
                }, true);
                """);

        ContextMenu contextMenu = new ContextMenu(grid);

        contextMenu.addItem("Menü1", e ->
                Notification.show("Menü1", 2000, Notification.Position.BOTTOM_START));

        contextMenu.addItem("Menü2", e ->
                Notification.show("Menü2", 2000, Notification.Position.BOTTOM_START));

        var separator = contextMenu.addItem(new Hr());
        separator.setEnabled(false);
        separator.getStyle().set("padding", "0").set("min-height", "0");
        contextMenu.addItem("Browser-Menü", e ->
                Notification.show(
                        "Tipp: Umschalt + Rechtsklick öffnet das native Browser-Menü.",
                        4000, Notification.Position.BOTTOM_START));
    }

    private void openEditAccountDialog() {
        if (selected == null) {
            Notification.show("Bitte zuerst ein Konto auswählen.",
                    2500, Notification.Position.MIDDLE);
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Bankkonto bearbeiten");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("33vw");

        TextField nameField = new TextField("Name");
        nameField.setWidthFull();
        nameField.setValue(selected.getName() != null ? selected.getName() : "");
        nameField.getElement().setAttribute(AUTOCOMPLETE_ATTR, "off");

        TextField bicField = new TextField("BIC");
        bicField.setWidthFull();
        bicField.setValue(selected.getBic() != null ? selected.getBic() : "");
        bicField.setReadOnly(true);

        TextField ibanField = new TextField("IBAN");
        ibanField.setWidthFull();
        ibanField.setValue(selected.getIban() != null ? selected.getIban() : "");
        ibanField.setReadOnly(true);

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        form.add(nameField, 2);
        form.add(bicField, 2);
        form.add(ibanField, 2);
        dialog.add(form);

        Button okButton = new Button("Speichern", VaadinIcon.CHECK.create(), e -> {
            if (nameField.isEmpty()) {
                Notification.show("Bitte einen Namen eingeben.",
                        3000, Notification.Position.MIDDLE);
                return;
            }
            selected.setName(nameField.getValue());
            bankAccountService.updateBankAccount(selected);
            dialog.close();
            refreshGrid();

            Notification success = Notification.show("Bankkonto wurde gespeichert.",
                    2500, Notification.Position.MIDDLE);
            success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Abbrechen", e -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(cancelButton, okButton);
        add(dialog);
        dialog.open();
    }

    private void openNewAccountDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Neues Bankkonto anlegen");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("33vw");
        dialog.getElement().setAttribute(AUTOCOMPLETE_ATTR, "off");

        TextField nameField = new TextField("Name");
        nameField.setWidthFull();
        nameField.getElement().setAttribute(AUTOCOMPLETE_ATTR, "off");

        TextField bicField = new TextField("BIC");
        bicField.setWidthFull();
        bicField.getElement().setAttribute(AUTOCOMPLETE_ATTR, "off");

        TextField ibanField = new TextField("IBAN");
        ibanField.setWidthFull();
        ibanField.getElement().setAttribute(AUTOCOMPLETE_ATTR, "off");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        form.add(nameField, 2);
        form.add(bicField, 2);
        form.add(ibanField, 2);

        dialog.add(form);

        Button okButton = new Button("Ok", VaadinIcon.CHECK.create(), e -> {
            if (nameField.isEmpty() || bicField.isEmpty() || ibanField.isEmpty()) {
                Notification.show("Bitte Name, BIC und IBAN ausfüllen.",
                        3000, Notification.Position.MIDDLE);
                return;
            }

            BankAccountDataObject newAccount = new BankAccountDataObject();
            newAccount.setName(nameField.getValue());
            newAccount.setBic(bicField.getValue());
            newAccount.setIban(ibanField.getValue());

            bankAccountService.addBankAccount(newAccount);
            dialog.close();
            refreshGrid();

            Notification success = Notification.show("Bankkonto wurde angelegt.",
                    2500, Notification.Position.MIDDLE);
            success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Abbrechen", e -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(cancelButton, okButton);
        add(dialog);
        dialog.open();
    }

    private void deleteSelectedAccount() {
        if (selected == null) {
            Notification.show("Bitte zuerst ein Konto auswählen.",
                    2500, Notification.Position.MIDDLE);
            return;
        }

        bankStatementService.deleteAccountAndStatements(selected);
        refreshGrid();

        Notification deleted = Notification.show("Bankkonto wurde gelöscht.",
                2500, Notification.Position.MIDDLE);
        deleted.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void moveUp(BankAccountDataObject account) {
        if (account == null) return;
        List<BankAccountDataObject> all = bankAccountService.getAllBankAccounts();
        int idx = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getPk().equals(account.getPk())) {
                idx = i;
                break;
            }
        }
        if (idx > 0) {
            BankAccountDataObject prev = all.get(idx - 1);
            Integer aIdx = account.getOrderIndex() != null ? account.getOrderIndex() : 0;
            Integer pIdx = prev.getOrderIndex() != null ? prev.getOrderIndex() : 0;
            account.setOrderIndex(pIdx);
            prev.setOrderIndex(aIdx);
            bankAccountService.updateBankAccount(account);
            bankAccountService.updateBankAccount(prev);
            refreshGrid(account.getPk());
        }
    }

    private void moveDown(BankAccountDataObject account) {
        if (account == null) return;
        List<BankAccountDataObject> all = bankAccountService.getAllBankAccounts();
        int idx = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getPk().equals(account.getPk())) {
                idx = i;
                break;
            }
        }
        if (idx >= 0 && idx < all.size() - 1) {
            BankAccountDataObject next = all.get(idx + 1);
            Integer aIdx = account.getOrderIndex() != null ? account.getOrderIndex() : 0;
            Integer nIdx = next.getOrderIndex() != null ? next.getOrderIndex() : 0;
            account.setOrderIndex(nIdx);
            next.setOrderIndex(aIdx);
            bankAccountService.updateBankAccount(account);
            bankAccountService.updateBankAccount(next);
            refreshGrid(account.getPk());
        }
    }

    private void refreshGrid() {
        refreshGrid(null);
    }

    private void refreshGrid(String selectPk) {
        List<BankAccountDataObject> bankAccountDataObjects = bankAccountService.getAllBankAccounts();
        // Indices normalisieren: nach orderIndex sortieren, dann 0 … n-1 neu vergeben
        bankAccountDataObjects = new java.util.ArrayList<>(bankAccountDataObjects);
        bankAccountDataObjects.sort(java.util.Comparator.comparing(
                BankAccountDataObject::getOrderIndex,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
        for (int i = 0; i < bankAccountDataObjects.size(); i++) {
            BankAccountDataObject a = bankAccountDataObjects.get(i);
            if (a.getOrderIndex() == null || a.getOrderIndex() != i) {
                a.setOrderIndex(i);
                bankAccountService.updateBankAccount(a);
            }
        }
        grid.setItems(bankAccountDataObjects);
        if (selectPk != null) {
            bankAccountDataObjects.stream()
                    .filter(a -> selectPk.equals(a.getPk()))
                    .findFirst()
                    .ifPresent(a -> {
                        selected = a;
                        grid.select(a);
                    });
        } else {
            selected = null;
        }
    }
}