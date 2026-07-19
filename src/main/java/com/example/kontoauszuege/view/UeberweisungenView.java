package com.example.kontoauszuege.view;

import com.example.kontoauszuege.model.UeberweisungDataObject;
import com.example.kontoauszuege.model.UeberweisungStatus;
import com.example.kontoauszuege.service.BankAccountService;
import com.example.kontoauszuege.service.BankStatementService;
import com.example.kontoauszuege.service.UeberweisungService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.kapott.hbci.manager.HBCIUtils;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
// NumberRenderer import removed (unused)
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.example.kontoauszuege.model.BankAccountDataObject;
import com.example.kontoauszuege.service.BaseService;
import com.example.kontoauszuege.service.BankAccess.BankConnection;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "ueberweisungen", layout = MainLayout.class)
@PageTitle("Überweisungen")
public class UeberweisungenView extends VerticalLayout {

    private final UeberweisungService service;
    private final BaseService baseService;

    private final Grid<UeberweisungDataObject> grid = new Grid<>(UeberweisungDataObject.class, false);

    // Formularfelder
    private final Select<BankAccountDataObject> senderField = new Select<>();
    private final ComboBox<EmpfaengerInfo> empfaengerField = new ComboBox<>("Empfänger");
    private final TextField        verwendungszweck   = new TextField("Verwendungszweck");
    private final TextField        empfaengerIbanField = new TextField("Empfänger-IBAN");
    private final TextField        empfaengerBicField = new TextField("Empfänger-BIC");
    private final TextField        betragField        = new TextField("Betrag (€)");

    private List<BankAccountDataObject> senderItems = List.of();
    private List<EmpfaengerInfo> bekannteEmpfaenger = List.of();
    private UeberweisungDataObject selected = null;
    private Dialog editDialog;
    // data view not used yet; keep for future use
    @SuppressWarnings("unused")
    private GridListDataView<UeberweisungDataObject> gridDataView;
    private Button bearbeitenBtn;
    private Button sendenBtn;
    private Button loeschenBtn;

    public UeberweisungenView(UeberweisungService service,
                              BankAccountService bankAccountService,
                              BankStatementService bankStatementService,
                              BaseService baseService) {
        this.service = service;
        this.baseService = baseService;

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
        senderItems = bankAccountService.getAllBankAccounts().stream()
                .filter(b -> b.getName() != null && !b.getName().isBlank())
                .sorted(java.util.Comparator.comparing(BankAccountDataObject::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        // deduplizieren: jede IBAN/name nur einmal aufnehmen
        java.util.Set<String> nameIbansSet = new java.util.HashSet<>();
        List<EmpfaengerInfo> empfaengerList = new ArrayList<>();

        for (var entry : bankStatementService.getAllStatements()) {
            String iban = entry.getEmpfaengerKontoNr();
            String empfaenger = entry.getEmpfaenger();
            String bic = entry.getEmpfaengerBLZ();

            if (iban != null && empfaenger != null && !iban.isBlank() && !empfaenger.isBlank() && !nameIbansSet.contains(empfaenger+iban)) {
                empfaengerList.add(new EmpfaengerInfo(empfaenger, iban, bic != null ? bic : ""));
                nameIbansSet.add(empfaenger+iban);
            }
        }
        this.bekannteEmpfaenger = empfaengerList;

        editDialog = buildDialog(bekannteEmpfaenger);
        add(editDialog);

        refreshGrid();
    }

    // ── Toolbar ───────────────────────────────────────────────────────────

    private HorizontalLayout createToolbar() {
        Button neuBtn = new Button("Neu", VaadinIcon.PLUS.create(), e -> neueUeberweisung());
        neuBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        bearbeitenBtn = new Button("Bearbeiten", VaadinIcon.EDIT.create(), e -> bearbeiten());
        bearbeitenBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        bearbeitenBtn.setEnabled(false);

        loeschenBtn = new Button("Löschen", VaadinIcon.TRASH.create(), e -> loeschen());
        loeschenBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        loeschenBtn.setEnabled(false);

        sendenBtn = new Button("Senden", VaadinIcon.PAPERPLANE.create(), e -> senden());
        sendenBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        sendenBtn.setEnabled(false);

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
            com.vaadin.flow.component.html.Span badge = new com.vaadin.flow.component.html.Span(
                    u.getStatus() != null ? u.getStatus().name() : UeberweisungStatus.NEW.name());
            String theme = switch (u.getStatus() != null ? u.getStatus() : UeberweisungStatus.NEW) {
                case NEW     -> "badge contrast";
                case SENDING -> "badge";
                case SENT    -> "badge success";
                case ERROR   -> "badge error";
            };
            badge.getElement().setAttribute("theme", theme);
            if (u.getStatus() == UeberweisungStatus.SENT) {
                badge.getStyle().set("color", "var(--lumo-disabled-text-color)");
                badge.getStyle().set("font-style", "italic");
            }
            return badge;
        }).setHeader("Status").setWidth("90px").setFlexGrow(0).setSortable(false);

        grid.addColumn(new ComponentRenderer<>(u -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setAlignItems(FlexComponent.Alignment.CENTER);
            layout.setSpacing(true);
            String senderIban = u.getSenderIban();
            BankAccountDataObject konto = senderIban == null ? null :
                    senderItems.stream().filter(b -> Objects.equals(b.getIban(), senderIban)).findFirst().orElse(null);
            String senderName = konto != null ? konto.getName() : (senderIban != null ? senderIban : "");
            Image icon = senderIcon(konto);
            if (icon != null) layout.add(icon);
            com.vaadin.flow.component.orderedlayout.VerticalLayout textLayout = new com.vaadin.flow.component.orderedlayout.VerticalLayout();
            textLayout.setPadding(false);
            textLayout.setSpacing(false);
            textLayout.add(new Span(senderName != null ? senderName : ""));
            if (konto != null && konto.getIban() != null && !konto.getIban().isBlank()) {
                Span ibanSpan = new Span(konto.getIban());
                ibanSpan.getStyle().set("font-size", "var(--lumo-font-size-xxs)");
                ibanSpan.getStyle().set("color", "var(--lumo-body-text-color)");
                ibanSpan.getStyle().set("opacity", "0.7");
                textLayout.add(ibanSpan);
            }
            layout.add(textLayout);
            if (u.getStatus() == UeberweisungStatus.SENT) {
                layout.getStyle().set("color", "var(--lumo-disabled-text-color)");
                layout.getStyle().set("font-style", "italic");
            }
            return layout;
        })).setHeader("Sender").setResizable(true).setSortable(true).setWidth("210px").setFlexGrow(0);
        grid.addColumn(new ComponentRenderer<>(u -> {
            Span s = new Span(u.getEmpfaenger() != null ? u.getEmpfaenger() : "");
            if (u.getStatus() == UeberweisungStatus.SENT) {
                
                s.getStyle().set("color", "var(--lumo-disabled-text-color)");
                s.getStyle().set("font-style", "italic");
            }
            return s;
        })).setHeader("Empfänger").setResizable(true).setSortable(true).setWidth("240px").setFlexGrow(0);

        grid.addColumn(new ComponentRenderer<>(u -> {
            Span s = new Span(u.getEmpfaengerBic() != null ? u.getEmpfaengerBic() : "");
            if (u.getStatus() == UeberweisungStatus.SENT) {
                
                s.getStyle().set("color", "var(--lumo-disabled-text-color)");
                s.getStyle().set("font-style", "italic");
            }
            return s;
        })).setHeader("Empfänger-BIC").setResizable(true).setSortable(true).setWidth("150px").setFlexGrow(0);

        grid.addColumn(new ComponentRenderer<>(u -> {
            Span s = new Span(u.getEmpfaengerIban() != null ? u.getEmpfaengerIban() : "");
            if (u.getStatus() == UeberweisungStatus.SENT) {
               
                s.getStyle().set("color", "var(--lumo-disabled-text-color)");
                s.getStyle().set("font-style", "italic");
            }
            return s;
        })).setHeader("Empfänger-IBAN").setResizable(true).setSortable(true).setWidth("240px").setFlexGrow(0);

        grid.addColumn(new ComponentRenderer<>(u -> {
            Span s = new Span(u.getVerwendungszweck() != null ? u.getVerwendungszweck() : "");
            if (u.getStatus() == UeberweisungStatus.SENT) {
               
                s.getStyle().set("color", "var(--lumo-disabled-text-color)");
                s.getStyle().set("font-style", "italic");
            }
            return s;
        })).setHeader("Verwendungszweck").setResizable(true).setSortable(true).setFlexGrow(1);

        grid.addColumn(new ComponentRenderer<>(u -> {
            String text = "";
            if (u.getBetrag() != null) {
                text = NumberFormat.getCurrencyInstance(Locale.GERMANY).format(u.getBetrag());
            }
            Span s = new Span(text);
            if (u.getStatus() == UeberweisungStatus.SENT) {
           
                s.getStyle().set("color", "var(--lumo-disabled-text-color)");
                s.getStyle().set("font-style", "italic");
            }
            s.getStyle().set("text-align", "right");
            return s;
        })).setHeader("Betrag").setSortable(true).setComparator(UeberweisungDataObject::getBetrag).setAutoWidth(true)
                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END);

        grid.setWidthFull();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        // Note: per-item row class API not available in this Vaadin version — apply
        // visual dimming in each column's renderer instead.
        grid.addSelectionListener(e -> {
            selected = e.getFirstSelectedItem().orElse(null);
            updateActionButtons();
        });
    }

    // ── Dialog ────────────────────────────────────────────────────────────

    private Dialog buildDialog(List<EmpfaengerInfo> bekannteEmpfaenger) {

        senderField.setLabel("Sender");
        senderField.setItems(senderItems);
        senderField.setItemLabelGenerator(b -> b.getName() != null ? b.getName() : "");
        senderField.setRenderer(new ComponentRenderer<>(b -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setAlignItems(FlexComponent.Alignment.CENTER);
            layout.setSpacing(true);
            layout.getStyle().set("padding", "2px 0");
            String bic = b.getBic();
            if (bic != null && !bic.isBlank()) {
                try {
                    String iconName = baseService.getIconFromBic(bic);
                    if (iconName != null && !iconName.isBlank()) {
                        Image img = new Image("icons/" + iconName, b.getName() != null ? b.getName() : "");
                        img.setHeight("20px");
                        img.getStyle().set("object-fit", "contain");
                        layout.add(img);
                    }
                } catch (Exception ignored) {
                    // kein Icon verfügbar
                }
            }
            com.vaadin.flow.component.orderedlayout.VerticalLayout textLayout = new com.vaadin.flow.component.orderedlayout.VerticalLayout();
            textLayout.setPadding(false);
            textLayout.setSpacing(false);
            textLayout.getStyle().set("min-width", "0");
            textLayout.add(new Span(b.getName() != null ? b.getName() : ""));
            Span ibanSpan = new Span(b.getIban() != null ? b.getIban() : "");
            ibanSpan.getStyle().set("font-size", "var(--lumo-font-size-xxs)");
            ibanSpan.getStyle().set("color", "var(--lumo-body-text-color)");
            ibanSpan.getStyle().set("opacity", "0.7");
            textLayout.add(ibanSpan);
            layout.add(textLayout);
            return layout;
        }));
        senderField.addValueChangeListener(e -> {
            BankAccountDataObject konto = e.getValue();
            senderField.setPrefixComponent(senderIcon(konto));
            if (konto != null && konto.getIban() != null && !konto.getIban().isBlank()) {
                Span ibanSpan = new Span(konto.getIban());
                ibanSpan.getStyle().set("font-size", "var(--lumo-font-size-xs)");
                ibanSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
                senderField.setHelperComponent(ibanSpan);
            } else {
                senderField.setHelperComponent(null);
            }
        });
        senderField.setWidthFull();

        empfaengerField.setItems(bekannteEmpfaenger);
        empfaengerField.setItemLabelGenerator(ei -> ei.name() != null ? ei.name() : "");
        // renderer: show name and IBAN in smaller, muted text
        empfaengerField.setRenderer(new com.vaadin.flow.data.renderer.ComponentRenderer<>(ei -> {
            com.vaadin.flow.component.orderedlayout.VerticalLayout v = new com.vaadin.flow.component.orderedlayout.VerticalLayout();
            v.setPadding(false);
            v.setSpacing(false);
            v.getStyle().set("min-width", "0");
            com.vaadin.flow.component.html.Span nameSpan = new com.vaadin.flow.component.html.Span(ei != null ? ei.name() : "");
            com.vaadin.flow.component.html.Span ibanSpan = new com.vaadin.flow.component.html.Span(ei != null ? ei.iban() : "");
            ibanSpan.getStyle().set("font-size", "var(--lumo-font-size-xxs)");
            ibanSpan.getStyle().set("color", "var(--lumo-body-text-color)");
            ibanSpan.getStyle().set("opacity", "0.7");
            v.add(nameSpan, ibanSpan);
            return v;
        }));
        empfaengerField.setAllowCustomValue(true);
        empfaengerField.addCustomValueSetListener(e -> empfaengerField.setValue(new EmpfaengerInfo(e.getDetail(), "", "")));
        empfaengerField.addValueChangeListener(e -> {
            EmpfaengerInfo ei = e.getValue();
            if (ei != null) {
                if (ei.iban() != null && !ei.iban().isBlank()) {
                    empfaengerIbanField.setValue(ei.iban());
                }
                if (ei.bic() != null && !ei.bic().isBlank()) {
                    empfaengerBicField.setValue(ei.bic());
                }
            }
        });
        empfaengerField.setWidthFull();

        betragField.setPrefixComponent(new Span("€"));
        betragField.setPlaceholder("0,00");
        betragField.setWidthFull();

        // Validate amount on blur and show error like for IBAN
        betragField.addBlurListener(e -> {
            String txt = betragField.getValue() == null ? "" : betragField.getValue().trim();
            if (txt.isEmpty()) {
                betragField.setInvalid(false);
                betragField.setErrorMessage("");
                return;
            }
            String norm = txt.replace(',', '.');
            try {
                new BigDecimal(norm);
                betragField.setInvalid(false);
                betragField.setErrorMessage("");
            } catch (Exception ex) {
                betragField.setInvalid(true);
                betragField.setErrorMessage("Ungültiger Betrag – bitte Zahl eingeben (z. B. 12,50)");
            }
        });

        verwendungszweck.setWidthFull();

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 4));
        form.add(senderField);
        form.setColspan(senderField, 4);
        Hr hr = new Hr();
        form.add(hr);
        form.setColspan(hr, 4);
        form.add(empfaengerField);
        form.setColspan(empfaengerField, 4);
        empfaengerIbanField.getElement().setAttribute("autocomplete", "off");
        empfaengerIbanField.setWidthFull();
        form.add(empfaengerIbanField);
        form.setColspan(empfaengerIbanField, 4);
        // Wenn die IBAN eingegeben/verlässt, versuchen wir die BIC automatisch zu ermitteln
        empfaengerIbanField.addBlurListener(e -> {
            String iban = empfaengerIbanField.getValue();
            // normalize: remove spaces
            String norm = iban == null ? "" : iban.trim().replace(" ", "");
            boolean valid = false;
            try {
                if (!norm.isEmpty()) {
                    valid = HBCIUtils.checkIBANCRC(norm);
                }
            } catch (Exception ex) {
                valid = false;
            }
            if (!valid && !norm.isEmpty()) {
                empfaengerIbanField.setInvalid(true);
                empfaengerIbanField.setErrorMessage("Ungültige IBAN");
            } else {
                empfaengerIbanField.setInvalid(false);
                empfaengerIbanField.setErrorMessage("");
                try {
                    String bic = BankConnection.bicAusIban(norm);
                    if (bic != null && !bic.isBlank()) {
                        empfaengerBicField.setValue(bic);
                    }
                } catch (Exception ex) {
                    // Ignoriere Fehler bei der BIC-Ermittlung
                }
            }
        });
        empfaengerBicField.getElement().setAttribute("autocomplete", "off");
        empfaengerBicField.setWidthFull();
        empfaengerField.getElement().setAttribute("spellcheck", "false");
        empfaengerField.getElement().setAttribute("autocorrect", "off");
        empfaengerField.getElement().setAttribute("autocapitalize", "off");
        form.add(empfaengerBicField);
        form.setColspan(empfaengerBicField, 4);
        empfaengerIbanField.getElement().setAttribute("spellcheck", "false");
        empfaengerIbanField.getElement().setAttribute("autocorrect", "off");
        empfaengerIbanField.getElement().setAttribute("autocapitalize", "off");
        form.add(verwendungszweck);
        form.setColspan(verwendungszweck, 4);
        form.add(betragField);
        empfaengerBicField.getElement().setAttribute("spellcheck", "false");
        empfaengerBicField.getElement().setAttribute("autocorrect", "off");
        empfaengerBicField.getElement().setAttribute("autocapitalize", "off");
        form.setWidthFull();

        Dialog dialog = new Dialog();
        betragField.getElement().setAttribute("spellcheck", "false");
        betragField.getElement().setAttribute("autocorrect", "off");
        betragField.getElement().setAttribute("autocapitalize", "off");
        dialog.setHeaderTitle("Überweisung bearbeiten");
        dialog.setWidth("33vw");
        verwendungszweck.getElement().setAttribute("spellcheck", "false");
        verwendungszweck.getElement().setAttribute("autocorrect", "off");
        verwendungszweck.getElement().setAttribute("autocapitalize", "off");
        dialog.setCloseOnOutsideClick(false);
        dialog.setCloseOnEsc(true);
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
        UeberweisungDataObject neu = new UeberweisungDataObject();
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
        if (selected == null) {
            Notification.show("Bitte zuerst eine Überweisung auswählen.",
                    2500, Notification.Position.MIDDLE);
            return;
        }
        List<UeberweisungDataObject> zumSenden = List.of(selected);
        try {
            DlgView dlg = new DlgView(this);
            UI ui = UI.getCurrent();
            Thread bg = new Thread(() -> {
                try {
                    service.ueberweisungenAusfuehren(zumSenden, dlg);
                    ui.access(() -> {
                        refreshGrid();
                        Notification n = Notification.show(
                                "Überweisung wurde gesendet.",
                                4000, Notification.Position.MIDDLE);
                        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    });
                } catch (Exception ex) {
                    ui.access(() -> {
                        refreshGrid();
                        Notification n = Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE);
                        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    });
                }
            });
            bg.setDaemon(true);
            bg.start();
        } catch (Exception ex) {
            Notification n = Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void speichern() {
        if (selected == null) return;
        // Validierung: IBAN prüfen
        String ibanToCheck = empfaengerIbanField.getValue();
        String norm = ibanToCheck == null ? "" : ibanToCheck.trim().replace(" ", "");
        boolean ibanValid = true;
        try {
            if (!norm.isEmpty()) ibanValid = HBCIUtils.checkIBANCRC(norm);
        } catch (Exception ex) {
            ibanValid = false;
        }
        if (!ibanValid) {
            empfaengerIbanField.setInvalid(true);
            empfaengerIbanField.setErrorMessage("Ungültige IBAN");
            Notification.show("Bitte eine gültige IBAN eingeben.", 3000, Notification.Position.MIDDLE);
            return;
        }
        BankAccountDataObject senderVal = senderField.getValue();
        selected.setSenderIban(senderVal != null ? senderVal.getIban() : "");
        EmpfaengerInfo empfInfo = empfaengerField.getValue();
        selected.setEmpfaenger(empfInfo != null ? empfInfo.name() : "");
        selected.setEmpfaengerBic(empfaengerBicField.getValue());
        selected.setEmpfaengerIban(empfaengerIbanField.getValue());
        selected.setVerwendungszweck(verwendungszweck.getValue());
        String betragText = betragField.getValue().trim().replace(",", ".");
        try {
            selected.setBetrag(betragText.isEmpty() ? BigDecimal.ZERO : new BigDecimal(betragText));
        } catch (NumberFormatException ex) {
            betragField.setInvalid(true);
            betragField.setErrorMessage("Ungültiger Betrag – bitte Zahl eingeben (z. B. 12,50)");
            Notification.show("Ungültiger Betrag – bitte Zahl eingeben (z. B. 12,50)",
                    3000, Notification.Position.MIDDLE);
            return;
        }
        service.update(selected);
        refreshGrid();
        grid.select(selected);
    }

    private void ladeFormular(UeberweisungDataObject u) {
        selected = u;
        String sv = u.getSenderIban();
        BankAccountDataObject senderKonto = sv == null ? null :
                senderItems.stream().filter(b -> Objects.equals(b.getIban(), sv)).findFirst().orElse(null);
        senderField.setValue(senderKonto);
        EmpfaengerInfo empfInfo = bekannteEmpfaenger.stream()
                .filter(ei -> Objects.equals(ei.name(), u.getEmpfaenger()) && Objects.equals(ei.iban(), u.getEmpfaengerIban()))
                .findFirst()
                .orElse(new EmpfaengerInfo(u.getEmpfaenger() != null ? u.getEmpfaenger() : "", "", ""));
        empfaengerField.setValue(empfInfo);
        empfaengerBicField.setValue(u.getEmpfaengerBic() != null ? u.getEmpfaengerBic() : "");
        empfaengerIbanField.setValue(u.getEmpfaengerIban() != null ? u.getEmpfaengerIban() : "");
        verwendungszweck.setValue(u.getVerwendungszweck() != null ? u.getVerwendungszweck() : "");
        betragField.setValue(u.getBetrag() != null
                ? u.getBetrag().toPlainString().replace(".", ",") : "");
        // clear any previous validation state for amount
        betragField.setInvalid(false);
        betragField.setErrorMessage("");
    }

    private void clearFormular() {
        senderField.clear();
        empfaengerField.clear();
        empfaengerBicField.clear();
        empfaengerIbanField.clear();
        verwendungszweck.clear();
        betragField.clear();
        betragField.setInvalid(false);
        betragField.setErrorMessage("");
    }

    private Image senderIcon(BankAccountDataObject konto) {
        if (konto == null) return null;
        String bic = konto.getBic();
        if (bic == null || bic.isBlank()) return null;
        try {
            String iconName = baseService.getIconFromBic(bic);
            if (iconName != null && !iconName.isBlank()) {
                Image img = new Image("icons/" + iconName, konto.getName() != null ? konto.getName() : "");
                img.setHeight("20px");
                img.getStyle().set("object-fit", "contain");
                return img;
            }
        } catch (Exception ignored) {
            // kein Icon verfügbar
        }
        return null;
    }

    private void refreshGrid() {
        List<UeberweisungDataObject> alle = service.findAll();
        gridDataView = grid.setItems(alle);
        selected = null;
        updateActionButtons();
    }

    private void updateActionButtons() {
        boolean hasSelection = selected != null;
        boolean selectableForEditAndSend =
                hasSelection && selected.getStatus() != UeberweisungStatus.SENT;

        if (bearbeitenBtn != null) {
            bearbeitenBtn.setEnabled(selectableForEditAndSend);
        }
        if (sendenBtn != null) {
            sendenBtn.setEnabled(selectableForEditAndSend);
        }
        if (loeschenBtn != null) {
            loeschenBtn.setEnabled(hasSelection);
        }
    }
}
