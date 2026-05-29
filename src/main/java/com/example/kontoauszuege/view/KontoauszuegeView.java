package com.example.kontoauszuege.view;

import com.example.kontoauszuege.model.BankStatement;
import com.example.kontoauszuege.service.BankStatementService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Kontoauszüge")
public class KontoauszuegeView extends VerticalLayout {

    private final BankStatementService service;
    private final Grid<BankStatement> grid = new Grid<>(BankStatement.class, false);
    private final TextField suchfeld = new TextField();

    private static final String DATE_FORMAT = "dd.MM.yyyy";
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    public KontoauszuegeView(BankStatementService service) {
        this.service = service;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

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

        HorizontalLayout toolbar = new HorizontalLayout(suchfeld);
        toolbar.setAlignItems(Alignment.BASELINE);
        return toolbar;
    }

    private Grid<BankStatement> createGrid() {
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS,
                GridVariant.LUMO_WRAP_CELL_CONTENT);

        grid.addColumn(new LocalDateRenderer<>(BankStatement::getBuchungsdatum, DATE_FORMAT))
                .setHeader("Buchungsdatum")
                .setSortable(true)
                .setComparator(BankStatement::getBuchungsdatum)
                .setWidth("150px")
                .setFlexGrow(0);

        grid.addColumn(new LocalDateRenderer<>(BankStatement::getValutadatum, DATE_FORMAT))
                .setHeader("Valutadatum")
                .setSortable(true)
                .setComparator(BankStatement::getValutadatum)
                .setWidth("140px")
                .setFlexGrow(0);

        grid.addColumn(BankStatement::getAuftraggeber)
                .setHeader("Auftraggeber")
                .setSortable(true)
                .setWidth("200px")
                .setFlexGrow(0);

        grid.addColumn(BankStatement::getEmpfaenger)
                .setHeader("Empfänger")
                .setSortable(true)
                .setWidth("200px")
                .setFlexGrow(0);

        grid.addColumn(new ComponentRenderer<>(statement -> {
            Span span = new Span(statement.getVerwendungszweck());
            span.getStyle().set("white-space", "pre-wrap");
            return span;
        }))
                .setHeader("Verwendungszweck")
                .setFlexGrow(1);

        grid.addColumn(BankStatement::getIban)
                .setHeader("IBAN")
                .setWidth("220px")
                .setFlexGrow(0);

        grid.addColumn(new ComponentRenderer<>(statement -> {
            Span betragSpan = new Span(CURRENCY_FORMAT.format(statement.getBetrag()));
            betragSpan.getStyle().set("font-weight", "bold");
            if (statement.getBetrag().compareTo(BigDecimal.ZERO) < 0) {
                betragSpan.getStyle().set("color", "var(--lumo-error-color)");
            } else {
                betragSpan.getStyle().set("color", "var(--lumo-success-color)");
            }
            return betragSpan;
        }))
                .setHeader("Betrag")
                .setSortable(true)
                .setComparator(BankStatement::getBetrag)
                .setWidth("140px")
                .setFlexGrow(0)
                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END);

        grid.addColumn(new NumberRenderer<>(BankStatement::getKontostand, CURRENCY_FORMAT))
                .setHeader("Kontostand")
                .setSortable(true)
                .setComparator(BankStatement::getKontostand)
                .setWidth("150px")
                .setFlexGrow(0)
                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END);

        return grid;
    }

    private void ladeKontoauszuege(String filter) {
        grid.setItems(service.findByFilter(filter));
    }
}
