package com.example.kontoauszuege.view;

import com.example.kontoauszuege.model.Ueberweisung;
import com.example.kontoauszuege.service.BankStatementService;
import com.example.kontoauszuege.service.UeberweisungService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Route(value = "ueberweisungen", layout = MainLayout.class)
@PageTitle("Überweisungen")
public class UeberweisungenView extends VerticalLayout {

    private final UeberweisungService service;

    private final Grid<Ueberweisung> grid = new Grid<>(Ueberweisung.class, false);

    // Formularfelder
    private final Select<String>   senderField        = new Select<>();
    private final ComboBox<String> empfaengerField    = new ComboBox<>("Empfänger");
    private final TextField        verwendungszweck   = new TextField("Verwendungszweck");
    private final TextField        betragField        = new TextField("Betrag (€)");

    private List<String> senderItems = List.of();
    private Ueberweisung selected = null;
    private Dialog editDialog;

    public UeberweisungenView(UeberweisungService service,
                              BankStatementService bankStatementService) {
        this.service = service;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        addClassName("ueberweisungen-view");

        // ── Teil 1: Toolbar ──────────────────────────────────────────────
        add(createToolbar());
        add(new Hr());

        // ── Teil 2: Tabelle ───────────────────────────────────────────────
        configureGrid();
        add(grid);
        setFlexGrow(1, grid);

        // ── Teil 3: Dialog vorbereiten ────────────────────────────────────
        senderItems = bankStatementService.findAll().stream()
                .map(b -> b.getAuftraggeber())
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        List<String> bekannteEmpfaenger = bankStatementService.findAll().stream()
                .map(b -> b.getEmpfaenger())
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        editDialog = buildDialog(bekannteEmpfaenger);
        add(editDialog);

        refreshGrid();
    }

    // ── Toolbar ───────────────────────────────────────────────────────────

    private HorizontalLayout createToolbar() {
        Button neuBtn = new Button("Neu", VaadinIcon.PLUS.create(), e -> neueUeberweisung());
        neuBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button bearbeitenBtn = new Button("Bearbeiten", VaadinIcon.EDIT.create(), e -> bearbeiten());
        bearbeitenBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        Button loeschenBtn = new Button("Löschen", VaadinIcon.TRASH.create(), e -> loeschen());
        loeschenBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Button sendenBtn = new Button("Senden", VaadinIcon.PAPERPLANE.create(), e -> senden());
        sendenBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        HorizontalLayout toolbar = new HorizontalLayout(neuBtn, bearbeitenBtn, loeschenBtn, sendenBtn);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.getStyle().set("padding-bottom", "var(--lumo-space-s)");
        return toolbar;
    }

    // ── Grid ──────────────────────────────────────────────────────────────

    private void configureGrid() {
        grid.addComponentColumn(u -> {
            Checkbox cb = new Checkbox(u.isAusgewaehlt());
            cb.addValueChangeListener(e -> u.setAusgewaehlt(e.getValue()));
            return cb;
        }).setHeader("Senden").setWidth("80px").setFlexGrow(0);

        grid.addColumn(Ueberweisung::getSender)
                .setHeader("Sender").setResizable(true).setSortable(true).setAutoWidth(true);
        grid.addColumn(Ueberweisung::getEmpfaenger)
                .setHeader("Empfänger").setResizable(true).setSortable(true).setAutoWidth(true);
        grid.addColumn(Ueberweisung::getEmpfaengerIban)
                .setHeader("Empfänger-IBAN").setResizable(true).setSortable(true).setAutoWidth(true);
        grid.addColumn(Ueberweisung::getVerwendungszweck)
                .setHeader("Verwendungszweck").setResizable(true).setSortable(true).setFlexGrow(1);
        grid.addColumn(new NumberRenderer<>(
                        Ueberweisung::getBetrag,
                        NumberFormat.getCurrencyInstance(Locale.GERMANY)))
                .setHeader("Betrag")
                .setSortable(true)
                .setComparator(Ueberweisung::getBetrag)
                .setAutoWidth(true);

        grid.setWidthFull();
        grid.addSelectionListener(e -> {
            selected = e.getFirstSelectedItem().orElse(null);
        });
    }

    // ── Dialog ────────────────────────────────────────────────────────────

    private Dialog buildDialog(List<String> bekannteEmpfaenger) {
        senderField.setLabel("Sender");
        senderField.setItems(senderItems);
        senderField.setWidthFull();

        empfaengerField.setItems(bekannteEmpfaenger);
        empfaengerField.setAllowCustomValue(true);
        empfaengerField.addCustomValueSetListener(e -> empfaengerField.setValue(e.getDetail()));
        empfaengerField.setWidthFull();

        betragField.setPrefixComponent(new Span("€"));
        betragField.setPlaceholder("0,00");
        betragField.setWidthFull();

        verwendungszweck.setWidthFull();

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 4));
        form.add(senderField, empfaengerField, betragField);
        form.setColspan(empfaengerField, 2);
        form.add(verwendungszweck);
        form.setColspan(verwendungszweck, 4);
        form.setWidthFull();

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Überweisung bearbeiten");
        dialog.setWidth("900px");
        dialog.setHeight("400px");
        dialog.add(form);

        Button okBtn = new Button("Ok", VaadinIcon.CHECK.create(), e -> {
            speichern();
            dialog.close();
        });
        okBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button abbrechenBtn = new Button("Abbrechen", e -> dialog.close());
        abbrechenBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(abbrechenBtn, okBtn);

        return dialog;
    }

    // ── Aktionen ──────────────────────────────────────────────────────────

    private void neueUeberweisung() {
        Ueberweisung neu = new Ueberweisung();
        service.add(neu);
        refreshGrid();
        grid.select(neu);
        ladeFormular(neu);
        editDialog.open();
    }

    private void bearbeiten() {
        if (selected == null) {
            Notification.show("Bitte zuerst eine Überweisung auswählen.",
                    2500, Notification.Position.MIDDLE);
            return;
        }
        ladeFormular(selected);
        editDialog.open();
    }

    private void loeschen() {
        if (selected == null) {
            Notification.show("Bitte zuerst eine Überweisung auswählen.",
                    2500, Notification.Position.MIDDLE);
            return;
        }
        service.remove(selected);
        selected = null;
        clearFormular();
        refreshGrid();
    }

    private void senden() {
        List<Ueberweisung> zumSenden = service.findAll().stream()
                .filter(Ueberweisung::isAusgewaehlt)
                .collect(Collectors.toList());
        if (zumSenden.isEmpty()) {
            Notification.show("Keine Überweisung zum Senden markiert.",
                    2500, Notification.Position.MIDDLE);
            return;
        }
        Notification n = Notification.show(
                zumSenden.size() + " Überweisung(en) wurden gesendet.",
                4000, Notification.Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void speichern() {
        if (selected == null) return;
        selected.setSender(senderField.getValue());
        selected.setEmpfaenger(empfaengerField.getValue());
        selected.setVerwendungszweck(verwendungszweck.getValue());
        String betragText = betragField.getValue().trim().replace(",", ".");
        try {
            selected.setBetrag(betragText.isEmpty() ? BigDecimal.ZERO : new BigDecimal(betragText));
        } catch (NumberFormatException ex) {
            Notification.show("Ungültiger Betrag – bitte Zahl eingeben (z. B. 12,50)",
                    3000, Notification.Position.MIDDLE);
            return;
        }
        service.update(selected);
        refreshGrid();
        grid.select(selected);
    }

    private void ladeFormular(Ueberweisung u) {
        selected = u;
        String sv = u.getSender();
        senderField.setValue(sv != null && senderItems.contains(sv) ? sv : null);
        empfaengerField.setValue(u.getEmpfaenger() != null ? u.getEmpfaenger() : "");
        verwendungszweck.setValue(u.getVerwendungszweck() != null ? u.getVerwendungszweck() : "");
        betragField.setValue(u.getBetrag() != null
                ? u.getBetrag().toPlainString().replace(".", ",") : "");
    }

    private void clearFormular() {
        senderField.clear();
        empfaengerField.clear();
        verwendungszweck.clear();
        betragField.clear();
    }

    private void refreshGrid() {
        List<Ueberweisung> alle = service.findAll();
        grid.setItems(alle);
        if (selected != null) {
            alle.stream()
                .filter(u -> u.getId().equals(selected.getId()))
                .findFirst()
                .ifPresent(grid::select);
        }
    }
}

