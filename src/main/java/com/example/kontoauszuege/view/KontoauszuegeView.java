package com.example.kontoauszuege.view;

import com.example.kontoauszuege.model.BankStatement;
import com.example.kontoauszuege.service.BankStatementService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Kontoauszüge")
public class KontoauszuegeView extends VerticalLayout {

    private final BankStatementService service;
    private final Grid<BankStatement> grid = new Grid<>(BankStatement.class, false);
    private final TextField suchfeld = new TextField();
    private final Select<String> kontoSelect = new Select<>();
    private String aktivesKonto = null;

    private static final String DATE_FORMAT = "dd.MM.yyyy";
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    public KontoauszuegeView(BankStatementService service) {
        this.service = service;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kontoauszuege-view");

        List<String> konten = service.findAll().stream()
                .map(BankStatement::getAuftraggeber)
                .filter(k -> k != null && !k.isBlank())
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        kontoSelect.setLabel("Konto");
        kontoSelect.setItems(konten);
        kontoSelect.setEmptySelectionAllowed(true);
        kontoSelect.setEmptySelectionCaption("– alle –");
        kontoSelect.addValueChangeListener(e -> {
            aktivesKonto = e.getValue();
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
            suchfeld.clear();
            kontoSelect.clear();
            aktivesKonto = null;
            ladeKontoauszuege("");
        });

        HorizontalLayout left = new HorizontalLayout(kontoSelect, holenButton, alleHolenButton);
        left.setAlignItems(Alignment.BASELINE);

        HorizontalLayout toolbar = new HorizontalLayout(left, suchfeld);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.BASELINE);
        toolbar.expand(left);
        return toolbar;
    }

    private Grid<BankStatement> createGrid() {
        grid.setSizeFull();
        grid.setMultiSort(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS,
                GridVariant.LUMO_WRAP_CELL_CONTENT);

        grid.addColumn(new LocalDateRenderer<>(BankStatement::getBuchungsdatum, DATE_FORMAT))
                .setHeader("Buchungsdatum")
                .setSortable(true)
                .setComparator(BankStatement::getBuchungsdatum)
                .setResizable(true)
                .setWidth("150px")
                .setFlexGrow(0);

        grid.addColumn(new LocalDateRenderer<>(BankStatement::getValutadatum, DATE_FORMAT))
                .setHeader("Valutadatum")
                .setSortable(true)
                .setComparator(BankStatement::getValutadatum)
                .setResizable(true)
                .setWidth("140px")
                .setFlexGrow(0);

        grid.addColumn(BankStatement::getAuftraggeber)
                .setHeader("Auftraggeber")
                .setSortable(true)
                .setResizable(true)
                .setWidth("200px")
                .setFlexGrow(0);

        grid.addColumn(BankStatement::getEmpfaenger)
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

        grid.addColumn(BankStatement::getIban)
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
                .setComparator(BankStatement::getBetrag)
                .setResizable(true)
                .setWidth("140px")
                .setFlexGrow(0)
                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END);

        grid.addColumn(new NumberRenderer<>(BankStatement::getKontostand, CURRENCY_FORMAT))
                .setHeader("Kontostand")
                .setSortable(true)
                .setComparator(BankStatement::getKontostand)
                .setResizable(true)
                .setWidth("150px")
                .setFlexGrow(0)
                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END);

        return grid;
    }

    private void ladeKontoauszuege(String filter) {
        var liste = service.findByFilter(filter);
        if (aktivesKonto != null && !aktivesKonto.isBlank()) {
            liste = liste.stream()
                    .filter(s -> aktivesKonto.equals(s.getAuftraggeber()))
                    .toList();
        }
        grid.setItems(liste);
    }
}
