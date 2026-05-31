package com.example.kontoauszuege;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.lumo.Lumo;

@PWA(
    name        = "Kontoauszüge App",
    shortName   = "Kontoauszüge",
    description = "Übersicht und Verwaltung von Kontoauszügen und Überweisungen",
    backgroundColor = "#ddd6fe",
    themeColor      = "#6366f1"
)
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet("styles.css")
public class AppShellConfig implements AppShellConfigurator {
}
