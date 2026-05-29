package com.example.kontoauszuege.view;

import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "ueberweisungen", layout = MainLayout.class)
@PageTitle("Überweisungen")
public class UeberweisungenView extends VerticalLayout {

    public UeberweisungenView() {
        add(new Paragraph("Überweisungen – in Bearbeitung"));
    }
}
