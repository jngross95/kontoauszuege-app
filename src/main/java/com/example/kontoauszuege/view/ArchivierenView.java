package com.example.kontoauszuege.view;

import com.example.kontoauszuege.model.DocumentDataObject;
import com.example.kontoauszuege.service.DocumentService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Paragraph;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.treegrid.TreeGrid;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
    private final Span indexLabel = new Span("0 / 0");

    // title removed per UI request
    private final IFrame pdfFrame = new IFrame();
    private final Div attributesPanel = new Div();
    private final TreeGrid<String> ordnerTree = new TreeGrid<>();
    private final com.vaadin.flow.component.textfield.TextField ordnerField = new com.vaadin.flow.component.textfield.TextField();
    private final com.vaadin.flow.component.dialog.Dialog ordnerDialog = new com.vaadin.flow.component.dialog.Dialog();
    private final java.util.Map<String, java.util.List<String>> ordnerChildren = new java.util.HashMap<>();
    private String selectedFolderPath = null;

    @Autowired
    public ArchivierenView(DocumentService documentService) {
        this.documentService = documentService;

        setSizeFull();
        setPadding(true);
        addClassName("archivieren-view");

        HorizontalLayout toolbar = new HorizontalLayout(leftBtn, indexLabel, rightBtn, archiveBtn);
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

        // configure Ordner tree (hierarchische Ansicht)
        ordnerChildren.put("KG", Arrays.asList("KG/Pirckheimer", "KG/Schwalbennest"));
        ordnerChildren.put("Jürgen", Arrays.asList("Jürgen/Gesundheit", "Jürgen/Rente"));

        java.util.List<String> roots = Arrays.asList("KG", "Jürgen");
        ordnerTree.addHierarchyColumn(item -> {
            if (item == null) return "";
            if (item.contains("/")) return item.substring(item.lastIndexOf('/') + 1);
            return item;
        }).setHeader("Ordner");
        ordnerTree.setItems(roots, item -> ordnerChildren.getOrDefault(item, java.util.Collections.emptyList()));
        ordnerTree.setWidthFull();
        ordnerTree.setHeight("320px");

        // configure ordnerField (read-only input that opens the tree dialog)
        ordnerField.setLabel("Ordner");
        ordnerField.setReadOnly(true);
        ordnerField.setWidthFull();

        // prepare dialog containing the tree
        ordnerDialog.setWidth("420px");
        ordnerDialog.setHeight("360px");
        com.vaadin.flow.component.orderedlayout.VerticalLayout dlgLayout = new com.vaadin.flow.component.orderedlayout.VerticalLayout();
        dlgLayout.setPadding(false);
        dlgLayout.setSpacing(false);
        dlgLayout.setSizeFull();
        dlgLayout.add(ordnerTree);
        ordnerDialog.add(dlgLayout);

        // selection: only accept leaf nodes (nodes without children)
        ordnerTree.addSelectionListener(e -> {
            java.util.Optional<String> sel = e.getFirstSelectedItem();
            if (sel.isPresent()) {
                String v = sel.get();
                boolean isLeaf = !ordnerChildren.containsKey(v);
                if (isLeaf) {
                    // store full path internally and show full path in the field
                    selectedFolderPath = v;
                    ordnerField.setValue(v);
                    ordnerDialog.close();
                } else {
                    // toggle expansion for parent nodes
                    if (ordnerTree.isExpanded(v)) ordnerTree.collapse(v); else ordnerTree.expand(v);
                }
            }
        });

        // open dialog when clicking the field (use element click listener)
        ordnerField.getElement().addEventListener("click", evt -> ordnerDialog.open());

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
        // attach dialog to this view so it's part of the UI tree
        add(ordnerDialog);
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
            indexLabel.setText("0 / 0");
            return;
        }

        if (currentIndex < 0) currentIndex = 0;
        if (currentIndex >= documents.size()) currentIndex = documents.size() - 1;

        DocumentDataObject current = documents.get(currentIndex);
        // no heading; show filename in attributes panel

        attributesPanel.removeAll();
        // add ordner selector at top (read-only field that opens dialog)
        attributesPanel.add(ordnerField);
        Paragraph p = new Paragraph("Datei: " + (current.getFileName() != null ? current.getFileName() : ""));
        attributesPanel.add(p);
        // show file modification date if available
        if (current.getFileModifyDate() != null) {
            ZonedDateTime zdt = ZonedDateTime.ofInstant(current.getFileModifyDate(), ZoneId.systemDefault());
            String fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(zdt);
            Paragraph mod = new Paragraph("Geändert: " + fmt);
            attributesPanel.add(mod);
        }
        // set pdf viewer src to our pdf.js wrapper
        String viewer = "pdfjs/viewer.html?filepk=" + current.getPk();
        pdfFrame.setSrc(viewer);

        //attributesPanel.removeAll();
        // restore previously selected folder (if any)
        Map<String, Object> attrs = current.getAttributes();
        if (attrs != null && attrs.containsKey("folder")) {
            Object f = attrs.get("folder");
            if (f != null) {
                selectedFolderPath = String.valueOf(f);
                ordnerField.setValue(selectedFolderPath);
            } else {
                selectedFolderPath = null;
                ordnerField.clear();
            }
        } else {
            selectedFolderPath = null;
            ordnerField.clear();
        }

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
        // show 1-based index and total count
        indexLabel.setText((currentIndex + 1) + " / " + documents.size());
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
            // persist selected folder into document attributes before archiving
            if (selectedFolderPath != null && !selectedFolderPath.isEmpty()) {
                current.getAttributes().put("folder", selectedFolderPath);
            } else {
                String selectedFolder = ordnerField.getValue();
                if (selectedFolder != null && !selectedFolder.isEmpty()) {
                    current.getAttributes().put("folder", selectedFolder);
                }
            }
            documentService.archiveDocument(current);
        } catch (Exception e) {
            // ignore for now; could show notification
        }
        documents.remove(currentIndex);
        if (currentIndex >= documents.size()) currentIndex = documents.size() - 1;
        updateView();
    }
}
