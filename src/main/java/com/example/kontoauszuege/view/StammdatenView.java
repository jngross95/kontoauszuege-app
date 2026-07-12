package com.example.kontoauszuege.view;

import com.example.kontoauszuege.service.BankAccountService;
import com.example.kontoauszuege.service.BankContactService;
import com.example.kontoauszuege.service.BankStatementService;
import com.example.kontoauszuege.service.BaseService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.LinkedHashMap;
import java.util.Map;

@Route(value = "stammdaten", layout = MainLayout.class)
@PageTitle("Stammdaten")
public class StammdatenView extends VerticalLayout {

    public StammdatenView(BankContactService bankContactService,
                          BankAccountService bankAccountService,
                          BankStatementService bankStatementService,
                          BaseService baseService) {

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        // reuse the Überweisungen background gradient
        addClassName("ueberweisungen-view");

        // Layout: left = vertical tabs, right = content
        Tabs leftTabs = new Tabs();
        leftTabs.setOrientation(Tabs.Orientation.VERTICAL);
        leftTabs.addClassName("stammdaten-left-tabs");

        // content area
        Div content = new Div();
        content.setSizeFull();
        content.addClassName("stammdaten-content");

        // Create tab-content pairs
        Tab tabOrdner = new Tab("Ordner");
        Tab tabBankkontakte = new Tab("Bankkontakte");
        Tab tabBankkonten = new Tab("Bankkonten");

        leftTabs.add(tabOrdner, tabBankkontakte, tabBankkonten);

        // create content components
        Div ordnerContent = new Div(new Span("Ordnerverwaltung (Platzhalter)"));

        // instantiate existing views as embedded components
        BankkontakteView bankkontakteView = new BankkontakteView(bankContactService, baseService);
        BankkontenView bankkontenView = new BankkontenView(bankAccountService, bankStatementService, baseService);

        // ensure sizing
        ordnerContent.setSizeFull();
        bankkontakteView.setSizeFull();
        bankkontenView.setSizeFull();

        Map<Tab, Component> tabsToPages = new LinkedHashMap<>();
        tabsToPages.put(tabOrdner, ordnerContent);
        tabsToPages.put(tabBankkontakte, bankkontakteView);
        tabsToPages.put(tabBankkonten, bankkontenView);

        // add all pages to content area (but hide initially)
        tabsToPages.values().forEach(c -> { c.getElement().getStyle().set("display", "none"); content.add(c); });

        // select first tab by default
        leftTabs.setSelectedIndex(0);
        tabsToPages.get(leftTabs.getSelectedTab()).getElement().getStyle().set("display", "");

        leftTabs.addSelectedChangeListener(event -> {
            tabsToPages.forEach((t, page) -> page.getElement().getStyle().set("display", t.equals(event.getSelectedTab()) ? "" : "none"));
        });

        HorizontalLayout main = new HorizontalLayout(leftTabs, content);
        main.setSizeFull();
        main.setPadding(false);
        main.setSpacing(true);
        main.setFlexGrow(0, leftTabs);
        main.setFlexGrow(1, content);
        main.setAlignItems(FlexComponent.Alignment.STRETCH);

        add(main);
    }
}
