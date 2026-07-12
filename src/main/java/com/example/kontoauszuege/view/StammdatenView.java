package com.example.kontoauszuege.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "stammdaten", layout = MainLayout.class)
@PageTitle("Stammdaten")
public class StammdatenView extends VerticalLayout {

    public StammdatenView() {
        setSizeFull();
        setPadding(true);
        setSpacing(false);
        // reuse the Überweisungen background gradient
        addClassName("ueberweisungen-view");

        // obere, zusätzliche NavBar innerhalb der View
        HorizontalLayout innerNav = new HorizontalLayout();
        innerNav.setAlignItems(FlexComponent.Alignment.CENTER);
        innerNav.setSpacing(true);
        innerNav.add(new Button("Allgemein"), new Button("Adressen"), new Button("Bankverbindungen"));
        innerNav.getStyle().set("padding", "0 var(--lumo-space-m)");
        add(innerNav);
        add(new Hr());

        // Platzhalterinhalt
        add(new Span("Hier werden die Stammdaten verwaltet."));
    }
}
