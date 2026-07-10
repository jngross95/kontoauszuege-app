package com.example.kontoauszuege.view;

import com.example.kontoauszuege.model.DocumentDataObject;
import com.example.kontoauszuege.service.DocumentService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.IFrame;
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

    private final Button leftBtn = new Button(VaadinIcon.ARROW_LEFT.create());
    private final Button rightBtn = new Button(VaadinIcon.ARROW_RIGHT.create());
    private final Button archiveBtn = new Button("Archivieren");

    // title removed per UI request
    private final IFrame pdfFrame = new IFrame();
    private final Div attributesPanel = new Div();

    @Autowired
    public ArchivierenView(DocumentService documentService) {
        this.documentService = documentService;

        setSizeFull();
        setPadding(true);
        addClassName("archivieren-view");

        HorizontalLayout toolbar = new HorizontalLayout(leftBtn, rightBtn, archiveBtn);
        toolbar.setSpacing(true);

        // Icon-only style and accessibility
        leftBtn.addThemeVariants(ButtonVariant.LUMO_ICON);
        rightBtn.addThemeVariants(ButtonVariant.LUMO_ICON);
        leftBtn.getElement().setProperty("title", "Links");
        rightBtn.getElement().setProperty("title", "Rechts");
        leftBtn.getElement().setAttribute("aria-label", "Links");
        rightBtn.getElement().setAttribute("aria-label", "Rechts");

        add(toolbar);

        // Main split: left = PDF viewer (pdf.js), right = attributes
        pdfFrame.setWidth("65%");
        pdfFrame.setHeightFull();
        pdfFrame.getElement().setAttribute("frameBorder", "0");

        attributesPanel.getStyle().set("padding", "8px");
        attributesPanel.getStyle().set("overflow", "auto");
        attributesPanel.setWidth("35%");
        attributesPanel.setHeightFull();

        HorizontalLayout main = new HorizontalLayout(pdfFrame, attributesPanel);
        // allow children to grow to available space without forcing overflow
        main.getStyle().set("min-height", "0");
        pdfFrame.getStyle().set("min-height", "0");
        attributesPanel.getStyle().set("min-height", "0");
        main.setSizeFull();
        main.setFlexGrow(1, pdfFrame);
        main.setFlexGrow(0, attributesPanel);
        // add toolbar and main together and expand main to consume remaining vertical space
        add(toolbar, main);
        expand(main);

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
            // no documents
            pdfFrame.setSrc("");
            attributesPanel.removeAll();
            leftBtn.setEnabled(false);
            rightBtn.setEnabled(false);
            archiveBtn.setEnabled(false);
            return;
        }

        if (currentIndex < 0) currentIndex = 0;
        if (currentIndex >= documents.size()) currentIndex = documents.size() - 1;

        DocumentDataObject current = documents.get(currentIndex);
        // no heading; show filename in attributes panel

        attributesPanel.removeAll();
        Paragraph p = new Paragraph("Datei: " + (current.getFileName() != null ? current.getFileName() : ""));
        attributesPanel.add(p);
        // set pdf viewer src to our pdf.js wrapper
        String viewer = "pdfjs/viewer.html?filepk=" + current.getPk();
        pdfFrame.setSrc(viewer);

        attributesPanel.removeAll();
        Map<String, Object> attrs = current.getAttributes();
        if (attrs != null) {
            for (Map.Entry<String, Object> entry : attrs.entrySet()) {
                Span key = new Span(entry.getKey() + ": ");
                key.getStyle().set("font-weight", "bold");
                Span value = new Span(String.valueOf(entry.getValue()));
                Div row = new Div(key, value);
                attributesPanel.add(row);
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
