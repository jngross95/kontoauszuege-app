package com.example.kontoauszuege.view;

import com.example.kontoauszuege.model.DocumentDataObject;
import com.example.kontoauszuege.service.DocumentService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Route(value = "archivieren", layout = MainLayout.class)
@PageTitle("Archivieren")
public class ArchivierenView extends VerticalLayout {

    private final DocumentService documentService;

    private List<DocumentDataObject> documents = new ArrayList<>();
    private int currentIndex = 0;

    private final Button leftBtn = new Button("Links");
    private final Button rightBtn = new Button("Rechts");
    private final Button archiveBtn = new Button("Archivieren");

    private final H3 title = new H3("");
    private final Div content = new Div();

    @Autowired
    public ArchivierenView(DocumentService documentService) {
        this.documentService = documentService;

        setSizeFull();
        setPadding(true);
        addClassName("archivieren-view");

        HorizontalLayout toolbar = new HorizontalLayout(leftBtn, rightBtn, archiveBtn);
        toolbar.setSpacing(true);
        add(toolbar);

        add(title);
        content.getStyle().set("white-space", "pre-wrap");
        add(content);
        setFlexGrow(1, content);

        leftBtn.addClickListener(e -> showPrevious());
        rightBtn.addClickListener(e -> showNext());
        archiveBtn.addClickListener(e -> archiveCurrent());

        initData();
    }

    private void initData() {
        // Import inbox then load new documents
        documentService.ImportInbox();
        documents = documentService.getAllNewDocuments();
        currentIndex = 0;
        updateView();
    }

    private void updateView() {
        if (documents == null || documents.isEmpty()) {
            title.setText("Keine neuen Dokumente");
            content.removeAll();
            leftBtn.setEnabled(false);
            rightBtn.setEnabled(false);
            archiveBtn.setEnabled(false);
            return;
        }

        if (currentIndex < 0) currentIndex = 0;
        if (currentIndex >= documents.size()) currentIndex = documents.size() - 1;

        DocumentDataObject current = documents.get(currentIndex);
        title.setText((currentIndex + 1) + " / " + documents.size() + " — " + (current.getFileName() != null ? current.getFileName() : "(kein Dateiname)"));

        content.removeAll();
        Paragraph p = new Paragraph("Datei: " + (current.getFileName() != null ? current.getFileName() : ""));
        content.add(p);

        Map<String, Object> attrs = current.getAttributes();
        if (attrs != null) {
            for (Map.Entry<String, Object> entry : attrs.entrySet()) {
                Span line = new Span(entry.getKey() + ": " + String.valueOf(entry.getValue()));
                content.add(line);
                content.add(new Div());
            }
        }

        leftBtn.setEnabled(currentIndex > 0);
        rightBtn.setEnabled(currentIndex < documents.size() - 1);
        archiveBtn.setEnabled(true);
    }

    private void showPrevious() {
        if (currentIndex > 0) {
            currentIndex--;
            updateView();
        }
    }

    private void showNext() {
        if (currentIndex < documents.size() - 1) {
            currentIndex++;
            updateView();
        }
    }

    private void archiveCurrent() {
        if (documents == null || documents.isEmpty()) return;
        DocumentDataObject current = documents.get(currentIndex);
        try {
            documentService.archiveDocument(current);
        } catch (Exception e) {
            // ignore for now; could show notification
        }
        documents.remove(currentIndex);
        if (currentIndex >= documents.size()) currentIndex = documents.size() - 1;
        updateView();
    }
}
