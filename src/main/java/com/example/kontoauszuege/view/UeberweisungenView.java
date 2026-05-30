package com.example.kontoauszuege.view;

import com.example.kontoauszuege.model.Ueberweisung;
import com.example.kontoauszuege.service.BankStatementService;
import com.example.kontoauszuege.service.UeberweisungService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
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
    private VerticalLayout formSection;

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

        add(new Hr());

        // ── Teil 3: Formular ──────────────────────────────────────────────
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

        formSection = createForm(bekannteEmpfaenger);
        formSection.setVisible(false);
        add(formSection);

        refreshGrid();
    }

    // ── Toolbar ───────────────────────────────────────────────────────────

    private HorizontalLayout createToolbar() {
        Button neuBtn = new Button("Neu", VaadinIcon.PLUS.create(), e -> neueUeberweisung());
        neuBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button loeschenBtn = new Button("Löschen", VaadinIcon.TRASH.create(), e -> loeschen());
        loeschenBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

        Button sendenBtn = new Button("Senden", VaadinIcon.PAPERPLANE.create(), e -> senden());
        sendenBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        HorizontalLayout toolbar = new HorizontalLayout(neuBtn, loeschenBtn, sendenBtn);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.getStyle().set("padding-bottom", "var(--lumo-space-s)");
        return toolbar;
    }

    // ── Grid ──────────────────────────────────────────────────────────────

    private void configureGrid() {
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
            if (e.getFirstSelectedItem().isPresent()) {
                ladeFormular(e.getFirstSelectedItem().get());
                formSection.setVisible(true);
            } else {
                selected = null;
                clearFormular();
                formSection.setVisible(false);
            }
        });
    }

    // ── Formular ──────────────────────────────────────────────────────────

    private VerticalLayout createForm(List<String> bekannteEmpfaenger) {
        senderField.setLabel("Sender");
        senderField.setItems(senderItems);
        senderField.setWidthFull();

        empfaengerField.setItems(bekannteEmpfaenger);
        empfaengerField.setAllowCustomValue(true);
        empfaengerField.addCustomValueSetListener(e -> empfaengerField.setValue(e.getDetail()));
        empfaengerField.setWidthFull();

        betragField.setPrefixComponent(new Span("€"));
        betragField.setPlaceholder("0,00");
        betragField.setWidth("14ch");

        verwendungszweck.setWidthFull();

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 3));
        form.add(senderField, empfaengerField, betragField); // Zeile 1: Sender | Empfänger | Betrag
        form.add(verwendungszweck);                          // Zeile 2: Verwendungszweck (volle Breite)
        form.setColspan(verwendungszweck, 3);
        form.setWidthFull();

        Button speichernBtn = new Button("Speichern", VaadinIcon.CHECK.create(), e -> speichern());
        speichernBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        H3 titel = new H3("Überweisung bearbeiten");
        titel.getStyle().set("margin", "var(--lumo-space-s) 0");

        VerticalLayout section = new VerticalLayout(titel, form, speichernBtn);
        section.setPadding(false);
        section.setSpacing(true);
        section.setWidthFull();
        section.getStyle().set("padding-top", "var(--lumo-space-s)");
        return section;
    }

    // ── Aktionen ──────────────────────────────────────────────────────────

    private void neueUeberweisung() {
        Ueberweisung neu = new Ueberweisung();
        service.add(neu);
        refreshGrid();
        grid.select(neu);
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
        if (selected == null) {
            Notification.show("Bitte zuerst eine Überweisung auswählen.",
                    2500, Notification.Position.MIDDLE);
            return;
        }
        Notification n = Notification.show(
                "Überweisung über " + selected.getBetrag() + " € an "
                + selected.getEmpfaengerIban() + " wurde gesendet.",
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

