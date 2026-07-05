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
import com.vaadin.flow.server.VaadinSession;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Kontoauszüge")
public class KontoauszuegeView extends VerticalLayout {

    private final transient BankStatementService service;
    private final transient BaseService baseService;
    private final Grid<BankStatementDataObject> grid = new Grid<>(BankStatementDataObject.class, false);
    private final TextField suchfeld = new TextField();
    private final Select<BankAccountDataObject> kontoSelect = new Select<>();
    private String aktiveKontoIban = null;
    private final Map<String, String> ibanZuKontoname = new HashMap<>();

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

        for (BankAccountDataObject k : konten) {
            if (k.getIban() != null && !k.getIban().isBlank()) {
                ibanZuKontoname.put(normalisiereIban(k.getIban()), k.getName());
            }
        }

  
        kontoSelect.setLabel("Konto");
        kontoSelect.setMinWidth("250px");
        
        kontoSelect.setItems(konten);
        kontoSelect.setItemLabelGenerator(konto -> {
            if (konto == null || konto.getName() == null || konto.getName().isBlank()) {
                return "Alle Konten";
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
            com.vaadin.flow.component.orderedlayout.VerticalLayout textLayout = new com.vaadin.flow.component.orderedlayout.VerticalLayout();
            textLayout.setPadding(false);
            textLayout.setSpacing(false);
            textLayout.getStyle().set("min-width", "0");
            textLayout.add(new Span(konto.getName() != null ? konto.getName() : ""));
            if (konto.getIban() != null && !konto.getIban().isBlank()) {
                Span ibanSpan = new Span(konto.getIban());
                ibanSpan.getStyle().set("font-size", "var(--lumo-font-size-xxs)");
                ibanSpan.getStyle().set("color", "var(--lumo-body-text-color)");
                ibanSpan.getStyle().set("opacity", "0.7");
                textLayout.add(ibanSpan);
            }
            layout.add(textLayout);
            return layout;
        }));
        kontoSelect.setEmptySelectionAllowed(true);
        kontoSelect.setEmptySelectionCaption("– alle –");
        kontoSelect.addValueChangeListener(e -> {
            BankAccountDataObject konto = e.getValue();
            aktiveKontoIban = konto == null ? null : konto.getIban();
            kontoSelect.setPrefixComponent(kontoIcon(konto));
            if (konto != null && konto.getIban() != null && !konto.getIban().isBlank()) {
                Span ibanSpan = new Span(konto.getIban());
                ibanSpan.getStyle().set("font-size", "var(--lumo-font-size-xs)");
                ibanSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
                kontoSelect.setHelperComponent(ibanSpan);
            } else {
                kontoSelect.setHelperComponent(null);
            }
            VaadinSession.getCurrent().setAttribute("kontoauszuege.selectedIban", aktiveKontoIban);
            ladeKontoauszuege(suchfeld.getValue());
        });

        add(createToolbar());
        add(createGrid());

        // Zuletzt ausgewähltes Konto aus der Session wiederherstellen
        String savedIban = (String) VaadinSession.getCurrent().getAttribute("kontoauszuege.selectedIban");
        Optional<BankAccountDataObject> savedKonto = savedIban == null ? Optional.empty()
                : konten.stream().filter(k -> savedIban.equals(k.getIban())).findFirst();
        if (savedKonto.isPresent()) {
            kontoSelect.setValue(savedKonto.get()); // löst Listener aus → setzt Prefix + lädt gefiltert
        } else {
            ladeKontoauszuege("");
        }
    }

    private HorizontalLayout createToolbar() {
        suchfeld.setPlaceholder("Suche nach Empfänger, Auftraggeber, Verwendungszweck...");
        suchfeld.setPrefixComponent(VaadinIcon.SEARCH.create());
        suchfeld.setClearButtonVisible(true);
        suchfeld.setWidth("400px");
        suchfeld.setValueChangeMode(ValueChangeMode.LAZY);
        suchfeld.addValueChangeListener(e -> ladeKontoauszuege(e.getValue()));

        Button alleHolenButton = new Button("Kontoauszüge Holen", VaadinIcon.REFRESH.create());
        alleHolenButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        alleHolenButton.addClickListener(e -> {
            try {
                var dlg = new DlgView(this);


                // Capture UI to update it from background thread
                final com.vaadin.flow.component.UI ui = com.vaadin.flow.component.UI.getCurrent();
                // capture current selection to decide whether to fetch all or single account
                final String selectedIban = aktiveKontoIban;

                Thread bg = new Thread(() -> {
                    try {
                                // delegate to service; it handles null -> all, non-null -> single IBAN
                                service.receiveStmts(dlg, selectedIban);

                        // update UI in UI thread
                        ui.access(() -> {
                            suchfeld.clear();
                            // If we fetched all accounts, clear selection; otherwise keep the selected account
                            if (selectedIban == null || selectedIban.isBlank()) {
                                kontoSelect.clear();
                                aktiveKontoIban = null;
                            } else {
                                // keep aktiveKontoIban as it was
                                aktiveKontoIban = selectedIban;
                            }
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

        HorizontalLayout left = new HorizontalLayout(kontoSelect, alleHolenButton);
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
            String normIban = normalisiereIban(stmt.getIban());
            String kontoName = ibanZuKontoname.getOrDefault(normIban, "");

            VerticalLayout layout = new VerticalLayout();
            layout.setPadding(false);
            layout.setSpacing(false);
            layout.getStyle().set("min-width", "0");

            if (!kontoName.isBlank()) {
                layout.add(new Span(kontoName));
            }
            if (stmt.getIban() != null && !stmt.getIban().isBlank()) {
                Span ibanSpan = new Span(stmt.getIban());
                ibanSpan.getStyle().set("font-size", "var(--lumo-font-size-xxs)");
                ibanSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
                layout.add(ibanSpan);
            }
            return layout;
        }))
                .setHeader("Konto")
                .setSortable(true)
                .setComparator((s1, s2) -> {
                    String n1 = ibanZuKontoname.getOrDefault(normalisiereIban(s1.getIban()),
                            s1.getIban() != null ? s1.getIban() : "");
                    String n2 = ibanZuKontoname.getOrDefault(normalisiereIban(s2.getIban()),
                            s2.getIban() != null ? s2.getIban() : "");
                    return n1.compareToIgnoreCase(n2);
                })
                .setResizable(true)
                .setWidth("200px")
                .setFlexGrow(0);

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

    private Image kontoIcon(BankAccountDataObject konto) {
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

    private String normalisiereIban(String iban) {
        if (iban == null) {
            return "";
        }
        return iban.trim().replace(" ", "").toUpperCase(Locale.ROOT);
    }
}
