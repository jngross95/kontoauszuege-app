package com.example.kontoauszuege.view;

import com.example.kontoauszuege.model.BankAccountDataObject;
import com.example.kontoauszuege.model.BankStatementDataObject;
import com.example.kontoauszuege.service.BankAccountService;
import com.example.kontoauszuege.service.BaseService;
import com.example.kontoauszuege.service.BankStatementService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Kontoauszüge")
public class KontoauszuegeView extends VerticalLayout {

    private final transient BankStatementService service;
    private final transient BaseService baseService;
    private final Grid<BankStatementDataObject> grid = new Grid<>(BankStatementDataObject.class, false);
    private final TextField suchfeld = new TextField();
    private final Select<BankAccountDataObject> kontoSelect = new Select<>();
    private String aktiveKontoIban = null;

    private static final String DATE_FORMAT = "dd.MM.yyyy";
    private static final String WIDTH_150PX = "150px";
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    public KontoauszuegeView(BankStatementService service, BankAccountService bankAccountService, BaseService baseService) {
        this.service = service;
        this.baseService = baseService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kontoauszuege-view");

        List<BankAccountDataObject> konten = bankAccountService.getAllBankAccounts().stream()
                .filter(k -> k.getName() != null && !k.getName().isBlank())
                .distinct()
                .sorted(java.util.Comparator.comparing(BankAccountDataObject::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        kontoSelect.setLabel("Konto");
        kontoSelect.setItems(konten);
        kontoSelect.setItemLabelGenerator(konto -> {
            if (konto == null || konto.getName() == null || konto.getName().isBlank()) {
                return "";
            }
            return konto.getName();
        });
        kontoSelect.setRenderer(new ComponentRenderer<>(konto -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setAlignItems(FlexComponent.Alignment.CENTER);
            layout.setSpacing(true);
            layout.getStyle().set("padding", "2px 0");
            String bic = konto.getBic();
            if (bic != null && !bic.isBlank()) {
                try {
                    String iconName = baseService.getIconFromBic(bic);
                    if (iconName != null && !iconName.isBlank()) {
                        Image img = new Image("icons/" + iconName, konto.getName() != null ? konto.getName() : "");
                        img.setHeight("20px");
                        img.getStyle().set("object-fit", "contain");
                        layout.add(img);
                    }
                } catch (Exception ignored) {
                    // kein Icon verfügbar
                }
            }
            layout.add(new Span(konto.getName() != null ? konto.getName() : ""));
            return layout;
        }));
        kontoSelect.setEmptySelectionAllowed(true);
        kontoSelect.setEmptySelectionCaption("– alle –");
        kontoSelect.addValueChangeListener(e -> {
            BankAccountDataObject konto = e.getValue();
            aktiveKontoIban = konto == null ? null : konto.getIban();
            ladeKontoauszuege(suchfeld.getValue());
        });

        add(createToolbar());
        add(createGrid());

        ladeKontoauszuege("");
    }

    private HorizontalLayout createToolbar() {
        suchfeld.setPlaceholder("Suche nach Empfänger, Auftraggeber, Verwendungszweck...");
        suchfeld.setPrefixComponent(VaadinIcon.SEARCH.create());
        suchfeld.setClearButtonVisible(true);
        suchfeld.setWidth("400px");
        suchfeld.setValueChangeMode(ValueChangeMode.LAZY);
        suchfeld.addValueChangeListener(e -> ladeKontoauszuege(e.getValue()));

        Button holenButton = new Button("Holen", VaadinIcon.REFRESH.create());
        holenButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        holenButton.addClickListener(e -> ladeKontoauszuege(suchfeld.getValue()));

        Button alleHolenButton = new Button("Alle holen", VaadinIcon.DOWNLOAD.create());
        alleHolenButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        alleHolenButton.addClickListener(e -> {
            try {
                var dlg = new DlgView(this);


                // Capture UI to update it from background thread
                final com.vaadin.flow.component.UI ui = com.vaadin.flow.component.UI.getCurrent();

                Thread bg = new Thread(() -> {
                    try {
                        service.receiveStmts(dlg);

                        // update UI in UI thread
                        ui.access(() -> {
                            suchfeld.clear();
                            kontoSelect.clear();
                            aktiveKontoIban = null;
                            ladeKontoauszuege("");

                            Notification success = Notification.show("Kontoauszüge wurden aktualisiert.",
                                    2500, Notification.Position.MIDDLE);
                            success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        });
                    } catch (Exception ex) {
                        ui.access(() -> {
                            Notification error = Notification.show(
                                    "Fehler beim Holen der Kontoauszüge:: " + ex.getMessage(),
                                    5000,
                                    Notification.Position.MIDDLE
                            );
                            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                        });
                    }
                });
                bg.setDaemon(true);
                bg.start();



            } catch (Exception ex) {
                Notification error = Notification.show(
                        "Fehler beim Holen der Kontoauszüge: " + ex.getMessage(),
                        5000,
                        Notification.Position.MIDDLE
                );
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        HorizontalLayout left = new HorizontalLayout(kontoSelect, holenButton, alleHolenButton);
        left.setAlignItems(Alignment.BASELINE);

        HorizontalLayout toolbar = new HorizontalLayout(left, suchfeld);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.BASELINE);
        toolbar.expand(left);
        return toolbar;
    }

    private Grid<BankStatementDataObject> createGrid() {
        grid.setSizeFull();
        grid.setMultiSort(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS,
                GridVariant.LUMO_WRAP_CELL_CONTENT);

        grid.addColumn(new ComponentRenderer<>(stmt -> {
            var dateStr = stmt.getBuchungsdatum() != null ? new java.text.SimpleDateFormat(DATE_FORMAT).format(stmt.getBuchungsdatum()) : "";
            return new Span(dateStr);
        }))
                .setHeader("Buchungsdatum")
                .setSortable(true)
                .setComparator((s1, s2) -> {
                    if (s1.getBuchungsdatum() == null || s2.getBuchungsdatum() == null) return 0;
                    return s1.getBuchungsdatum().compareTo(s2.getBuchungsdatum());
                })
                .setResizable(true)
                .setWidth(WIDTH_150PX)
                .setFlexGrow(0);

        grid.addColumn(new ComponentRenderer<>(stmt -> {
            var dateStr = stmt.getWertstellungsdatum() != null ? new java.text.SimpleDateFormat(DATE_FORMAT).format(stmt.getWertstellungsdatum()) : "";
            return new Span(dateStr);
        }))
                .setHeader("Wertstellungsdatum")
                .setSortable(true)
                .setComparator((s1, s2) -> {
                    if (s1.getWertstellungsdatum() == null || s2.getWertstellungsdatum() == null) return 0;
                    return s1.getWertstellungsdatum().compareTo(s2.getWertstellungsdatum());
                })
                .setResizable(true)
                .setWidth(WIDTH_150PX)
                .setFlexGrow(0);

        grid.addColumn(BankStatementDataObject::getGeschaeftsvorfall)
                .setHeader("Geschäftsvorfall")
                .setSortable(true)
                .setResizable(true)
                .setWidth("200px")
                .setFlexGrow(0);

        grid.addColumn(BankStatementDataObject::getEmpfaenger)
                .setHeader("Empfänger")
                .setSortable(true)
                .setResizable(true)
                .setWidth("200px")
                .setFlexGrow(0);

        grid.addColumn(new ComponentRenderer<>(statement -> {
            Span span = new Span(statement.getVerwendungszweck());
            span.getStyle().set("white-space", "pre-wrap");
            return span;
        }))
                .setHeader("Verwendungszweck")
                .setResizable(true)
                .setFlexGrow(1);

        grid.addColumn(BankStatementDataObject::getIban)
                .setHeader("IBAN")
                .setResizable(true)
                .setWidth("220px")
                .setFlexGrow(0);

        grid.addColumn(new ComponentRenderer<>(statement -> {
            Span betragSpan = new Span(CURRENCY_FORMAT.format(statement.getBetrag()));
            betragSpan.getStyle().set("font-weight", "bold");
            if (statement.getBetrag().compareTo(BigDecimal.ZERO) < 0) {
                betragSpan.getStyle().set("color", "#e53935");
            } else {
                betragSpan.getStyle().set("color", "#43a047");
            }
            return betragSpan;
        }))
                .setHeader("Betrag")
                .setSortable(true)
                .setComparator(BankStatementDataObject::getBetrag)
                .setResizable(true)
                .setWidth("140px")
                .setFlexGrow(0)
                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END);

        grid.addColumn(new ComponentRenderer<>(stmt -> {
            Span saldoSpan = new Span(CURRENCY_FORMAT.format(stmt.getSaldo()));
            saldoSpan.getStyle().set("font-weight", "bold");
            return saldoSpan;
        }))
                .setHeader("Saldo")
                .setSortable(true)
                .setComparator(BankStatementDataObject::getSaldo)
                .setResizable(true)
                .setWidth(WIDTH_150PX)
                .setFlexGrow(0)
                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END);

        return grid;
    }

    private void ladeKontoauszuege(String filter) {
        var liste = service.getAllStatements().stream()
                .filter(s -> filter == null || filter.isBlank() || 
                        (s.getEmpfaenger() != null && s.getEmpfaenger().toLowerCase().contains(filter.toLowerCase())) ||
                        (s.getVerwendungszweck() != null && s.getVerwendungszweck().toLowerCase().contains(filter.toLowerCase())))
                .toList();
        if (aktiveKontoIban != null && !aktiveKontoIban.isBlank()) {
            liste = liste.stream()
                    .filter(s -> Objects.equals(normalisiereIban(aktiveKontoIban), normalisiereIban(s.getIban())))
                    .toList();
        }
        grid.setItems(liste);
    }

    private String normalisiereIban(String iban) {
        if (iban == null) {
            return "";
        }
        return iban.trim().replace(" ", "").toUpperCase(Locale.ROOT);
    }
}
