package com.example.kontoauszuege.view;

import com.example.kontoauszuege.model.DocumentDataObject;
import com.example.kontoauszuege.service.DocumentService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Route(value = "dokumente", layout = MainLayout.class)
@PageTitle("Dokumente")
public class DokumenteView extends VerticalLayout {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DocumentService documentService;
    private final Grid<DocumentDataObject> grid = new Grid<>(DocumentDataObject.class, false);
    private DocumentDataObject selected = null;
    private Button entfernenBtn;

    @Autowired
    public DokumenteView(DocumentService documentService) {
        this.documentService = documentService;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        addClassName("dokumente-view");

        add(createToolbar());
        configureGrid();
        add(new Hr(), grid);
        setFlexGrow(1, grid);

        loadData();
    }

    // ── Toolbar ───────────────────────────────────────────────────────────

    private HorizontalLayout createToolbar() {
        entfernenBtn = new Button("Entfernen", VaadinIcon.TRASH.create(), e -> entfernen());
        entfernenBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        entfernenBtn.setEnabled(false);

        HorizontalLayout toolbar = new HorizontalLayout(entfernenBtn);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.getStyle().set("padding-bottom", "var(--lumo-space-s)");
        return toolbar;
    }

    // ── Grid ──────────────────────────────────────────────────────────────

    private void configureGrid() {
        grid.addColumn(doc -> doc.getState() != null ? doc.getState().name() : "")
                .setHeader("Status")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(DocumentDataObject::getFilePath)
                .setHeader("Ordner")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(DocumentDataObject::getFileName)
                .setHeader("Dateiname")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(doc -> {
            if (doc.getFileModifyDate() == null) return "";
            return DATE_FMT.format(doc.getFileModifyDate().atZone(ZoneId.systemDefault()));
        })
                .setHeader("Geändert")
                .setSortable(true)
                .setAutoWidth(true);

        grid.setWidthFull();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.addSelectionListener(e -> {
            selected = e.getFirstSelectedItem().orElse(null);
            updateActionButtons();
        });
    }

    // ── Aktionen ──────────────────────────────────────────────────────────

    private void entfernen() {
        if (selected == null) {
            Notification.show("Bitte zuerst ein Dokument auswählen.",
                    2500, Notification.Position.MIDDLE);
            return;
        }
        documentService.deleteDocument(selected.getPk());
        selected = null;
        updateActionButtons();
        loadData();
    }

    private void updateActionButtons() {
        if (entfernenBtn != null) {
            entfernenBtn.setEnabled(selected != null);
        }
    }

    private void loadData() {
        grid.setItems(documentService.getAllDocuments());
    }
}
