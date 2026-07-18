package com.example.kontoauszuege.view;

import com.example.kontoauszuege.model.DocumentDataObject;
import com.example.kontoauszuege.model.DocumentState;
import com.example.kontoauszuege.service.DocumentService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Hr;
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

    @Autowired
    public DokumenteView(DocumentService documentService) {
        this.documentService = documentService;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        addClassName("dokumente-view");

        configureGrid();
        add(new Hr(), grid);
        setFlexGrow(1, grid);

        loadData();
    }

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
    }

    private void loadData() {
        grid.setItems(documentService.getAllDocuments());
    }
}
