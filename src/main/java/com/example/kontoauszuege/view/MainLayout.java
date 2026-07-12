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
    private final Tab tabArchivieren;
    private final Tab tabUeberweisungen;
    private final Tab tabStammdaten;
    

    public MainLayout() {
        tabArchivieren = new Tab(new RouterLink("Archivieren", ArchivierenView.class));
        tabKontoauszuege = new Tab(new RouterLink("Kontoauszüge", KontoauszuegeView.class));
        tabUeberweisungen = new Tab(new RouterLink("Überweisungen", UeberweisungenView.class));
        tabStammdaten   = new Tab(new RouterLink("Stammdaten", StammdatenView.class));
        tabs = new Tabs(tabArchivieren, tabKontoauszuege, tabUeberweisungen, tabStammdaten);

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
        } else if (path.equals("stammdaten") || path.equals("bankkontakte") || path.equals("bankkonten")) {
            tabs.setSelectedTab(tabStammdaten);
        } else if (path.equals("archivieren")) {
            tabs.setSelectedTab(tabArchivieren);
        }
    }
}
