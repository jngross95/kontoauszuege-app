package com.example.kontoauszuege.view;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private final Tabs tabs;
    private final Tab tabKontoauszuege;
    private final Tab tabUeberweisungen;
    private final Tab tabBankkontakte;

    public MainLayout() {
        tabKontoauszuege = new Tab(new RouterLink("Kontoauszüge", KontoauszuegeView.class));
        tabUeberweisungen = new Tab(new RouterLink("Überweisungen", UeberweisungenView.class));
        tabBankkontakte = new Tab(new RouterLink("Bankkontakte", BankkontakteView.class));
        tabs = new Tabs(tabKontoauszuege, tabUeberweisungen, tabBankkontakte);

        H1 title = new H1("Kontoauszüge App");
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0")
                .set("padding", "0 var(--lumo-space-m)");

        HorizontalLayout navbar = new HorizontalLayout(title, tabs);
        navbar.setAlignItems(FlexComponent.Alignment.CENTER);
        navbar.setWidthFull();

        addToNavbar(navbar);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        String path = event.getLocation().getPath();
        if (path.isEmpty()) {
            tabs.setSelectedTab(tabKontoauszuege);
        } else if (path.equals("ueberweisungen")) {
            tabs.setSelectedTab(tabUeberweisungen);
        } else if (path.equals("bankkontakte")) {
            tabs.setSelectedTab(tabBankkontakte);
        }
    }
}
