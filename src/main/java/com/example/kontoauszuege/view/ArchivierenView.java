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
import java.util.Map;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.splitlayout.SplitLayout;

import org.springframework.beans.factory.annotation.Autowired;
import com.example.kontoauszuege.service.FileSystemService;

import java.util.ArrayList;
import java.util.List;

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
    private final com.vaadin.flow.component.textfield.TextField archivOrdnerField = new com.vaadin.flow.component.textfield.TextField();
    private final com.vaadin.flow.component.dialog.Dialog archivOrdnerDialog = new com.vaadin.flow.component.dialog.Dialog();
    private final com.vaadin.flow.component.combobox.ComboBox<String> archivDateinameCombo = new com.vaadin.flow.component.combobox.ComboBox<>();
    private final java.util.Map<String, java.util.List<String>> ordnerChildren = new java.util.HashMap<>();
    private String selectedFolderPath = null;
    private boolean restoringSelection = false;

    private final FileSystemService fileSystemService;

    @Autowired
    public ArchivierenView(DocumentService documentService, FileSystemService fileSystemService) {
        this.documentService = documentService;
        this.fileSystemService = fileSystemService;

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

        // configure Ordner tree (hierarchische Ansicht) from FileSystemService
        rebuildOrdnerTree();
        ordnerTree.addHierarchyColumn(item -> {
            if (item == null) return "";
            if (item.contains("/")) return item.substring(item.lastIndexOf('/') + 1);
            return item;
        }).setHeader("Archiv-Ordner");
        ordnerTree.setWidthFull();
        ordnerTree.setHeight("320px");

        // configure archivOrdnerField (read-only input that opens the tree dialog)
        archivOrdnerField.setLabel("Archiv-Ordner");
        archivOrdnerField.setReadOnly(true);
        archivOrdnerField.setWidthFull();

        // prepare dialog containing the tree
        archivOrdnerDialog.setWidth("420px");
        archivOrdnerDialog.setHeight("360px");
        com.vaadin.flow.component.orderedlayout.VerticalLayout dlgLayout = new com.vaadin.flow.component.orderedlayout.VerticalLayout();
        dlgLayout.setPadding(false);
        dlgLayout.setSpacing(false);
        dlgLayout.setSizeFull();
        dlgLayout.add(ordnerTree);
        archivOrdnerDialog.add(dlgLayout);

        // configure archivDateiname ComboBox (allow custom text + suggestions)
        archivDateinameCombo.setLabel("Archiv-Dateiname");
        archivDateinameCombo.setAllowCustomValue(true);
        archivDateinameCombo.setWidthFull();
        archivDateinameCombo.addCustomValueSetListener(evt -> archivDateinameCombo.setValue(evt.getDetail()));

        // selection: accept any node (including non-leaf). Selecting sets the folder.
        ordnerTree.addSelectionListener(e -> {
            java.util.Optional<String> sel = e.getFirstSelectedItem();
            if (sel.isPresent()) {
                String v = sel.get();
                // store full path internally and show full path in the field
                selectedFolderPath = v;
                archivOrdnerField.setValue(v);
                // update filename suggestions for the newly selected folder
                updateArchivDateiSuggestions(v);
                if (!restoringSelection) {
                    archivOrdnerDialog.close();
                }
            }
        });

        // open dialog when clicking the field (use element click listener)
        archivOrdnerField.getElement().addEventListener("click", evt -> archivOrdnerDialog.open());
        archivOrdnerDialog.addOpenedChangeListener(ev -> {
            if (ev.isOpened()) {
                rebuildOrdnerTree();
                if (selectedFolderPath != null && !selectedFolderPath.isBlank()) {
                    expandAndSelect(selectedFolderPath);
                }
            }
        });

        SplitLayout split = new SplitLayout(pdfFrame, attributesPanel);
        // allow children to grow to available space without forcing overflow
        split.getStyle().set("min-height", "0");
        pdfFrame.getStyle().set("min-height", "0");
        attributesPanel.getStyle().set("min-height", "0");
        split.setSizeFull();
        // default: viewer 50% / attributes 50%
        try {
            split.setSplitterPosition(50);
        } catch (Exception ignore) {
        }
        // add toolbar and split together and expand split to consume remaining vertical space
        add(toolbar, split);
        // attach dialog to this view so it's part of the UI tree
        add(archivOrdnerDialog);
        expand(split);

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

    private void rebuildOrdnerTree() {
        ordnerChildren.clear();
        // deduplicate and sort all subdirectory paths
        java.util.Set<String> subsSet = new java.util.TreeSet<>(fileSystemService.getSubDirectories());
        java.util.Set<String> rootsSet = new java.util.TreeSet<>();
        for (String p : subsSet) {
            String[] parts = p.split("/");
            if (parts.length == 0) continue;
            rootsSet.add(parts[0]);
            String parent = parts[0];
            for (int i = 1; i < parts.length; i++) {
                String child = parent + "/" + parts[i];
                java.util.List<String> children = ordnerChildren.computeIfAbsent(parent, k -> new java.util.ArrayList<>());
                // avoid adding the same child multiple times
                if (!children.contains(child)) {
                    children.add(child);
                }
                parent = child;
            }
        }
        // sort children lists by their final path segment (folder name)
        for (java.util.Map.Entry<String, java.util.List<String>> en : ordnerChildren.entrySet()) {
            en.getValue().sort((a, b) -> {
                String na = a.contains("/") ? a.substring(a.lastIndexOf('/') + 1) : a;
                String nb = b.contains("/") ? b.substring(b.lastIndexOf('/') + 1) : b;
                return na.compareToIgnoreCase(nb);
            });
        }

        // create sorted roots list (by name)
        java.util.List<String> roots = new java.util.ArrayList<>(rootsSet);
        roots.sort(String::compareToIgnoreCase);
        ordnerTree.setItems(roots, item -> ordnerChildren.getOrDefault(item, java.util.Collections.emptyList()));
    }

    /**
     * Aktualisiert die Vorschlagsliste für den Archiv-Dateinamen basierend auf
     * dem relativen Ordnerpfad. Wenn der Pfad null oder leer ist, werden die
     * Dateien des Basisverzeichnisses vorgeschlagen.
     */
    private void updateArchivDateiSuggestions(String relativeFolderPath) {
        try {
            java.util.List<String> names = fileSystemService.getFileNames(relativeFolderPath);
            archivDateinameCombo.setItems(names);
        } catch (Exception e) {
            archivDateinameCombo.setItems(java.util.Collections.emptyList());
        }
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
        // add archivOrdner selector at top (read-only field that opens dialog)
        attributesPanel.add(archivOrdnerField);
        // add archiv dateiname selector (allows custom value)
        // populate suggestions based on selected folder
        updateArchivDateiSuggestions(selectedFolderPath);
        // prefill with existing attribute or original filename
        if (current.getAttributes() != null && current.getAttributes().containsKey("archivFileName")) {
            Object afn = current.getAttributes().get("archivFileName");
            if (afn != null) archivDateinameCombo.setValue(String.valueOf(afn));
        } else {
            // default to original filename
            if (current.getFileName() != null) archivDateinameCombo.setValue(current.getFileName());
        }
        attributesPanel.add(archivDateinameCombo);
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
                archivOrdnerField.setValue(selectedFolderPath);
            } else {
                selectedFolderPath = null;
                archivOrdnerField.clear();
            }
        } else {
            selectedFolderPath = null;
            archivOrdnerField.clear();
        }

        // if folder was restored from attributes, update filename suggestions
        updateArchivDateiSuggestions(selectedFolderPath);

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
                    String selectedFolder = archivOrdnerField.getValue();
                    if (selectedFolder != null && !selectedFolder.isEmpty()) {
                    current.getAttributes().put("folder", selectedFolder);
                }
            }
            // persist chosen archive filename (if any)
            try {
                String chosen = archivDateinameCombo.getValue();
                if (chosen != null && !chosen.isBlank()) {
                    current.getAttributes().put("archivFileName", chosen);
                }
            } catch (Exception ignore) {
            }
            documentService.archiveDocument(current);
        } catch (Exception e) {
            // ignore for now; could show notification
        }
        documents.remove(currentIndex);
        if (currentIndex >= documents.size()) currentIndex = documents.size() - 1;
        updateView();
    }

    private void expandAndSelect(String path) {
        // Expand all ancestor nodes so the selected folder is visible
        String[] parts = path.split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) sb.append("/");
            sb.append(parts[i]);
            ordnerTree.expand(sb.toString());
        }
        // Programmatically select without triggering the close-dialog side-effect
        restoringSelection = true;
        ordnerTree.select(path);
        restoringSelection = false;
        // Scroll so the selected entry is visible
        ordnerTree.scrollToItem(path);
    }
}
