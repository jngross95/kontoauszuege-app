package com.example.kontoauszuege.view;

import com.example.kontoauszuege.service.BankAccess.DlgCallback;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.Base64;



import java.util.concurrent.CountDownLatch;

/**
 * DlgView - Ein Dialog-View, der DialogCallback implementiert.
 * Zeigt einen modalen Dialog an, wenn der Callback aufgerufen wird.
 */
public class DlgView extends VerticalLayout implements DlgCallback {

    private final VerticalLayout parent;

    /**
     * Konstruktor für DlgView.
     *
     * @param parent Die Parent-Komponente, in der der Dialog angezeigt werden soll
     */
    public DlgView(VerticalLayout parent) {
        this.parent = parent;
    }

    /**
     * Implementierung des DialogCallback-Interfaces.
     * Erstellt und öffnet einen modalen Dialog mit den übergebenen Parametern.
     * Die Methode blockiert, bis der Benutzer Ok oder Abbrechen klickt.
     *
     * @param kontaktName Der Name des Kontakts
     * @param inputFieldText Der Text aus einem Eingabefeld
     * @param message Die anzuzeigende Nachricht
     * @param image Ein Bild (optional)
     * @return "OK" wenn Ok geklickt wurde, "CANCEL" wenn Abbrechen geklickt wurde
     */
    @Override
    public String dlg(String kontaktName, String inputFieldText, String message, byte[] image) {
        CountDownLatch latch = new CountDownLatch(1);
        String[] result = new String[1]; // null indicates cancellation

        UI ui = parent.getUI().orElseThrow(() -> new IllegalStateException("Parent is not attached to a UI"));
        ui.access(() -> {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle(kontaktName);
            dialog.setCloseOnEsc(false);
            dialog.setCloseOnOutsideClick(false);
            dialog.addThemeVariants(DialogVariant.LUMO_NO_PADDING);
            dialog.setWidth("50vw");

            VerticalLayout content = new VerticalLayout();
            content.setPadding(true);
            content.setSpacing(true);
            content.add(new Paragraph("Kontakt: " + kontaktName));
            content.add(new Paragraph("Vorherige Eingabe: " + (inputFieldText == null ? "" : inputFieldText)));
            content.add(new Paragraph("Nachricht: " + (message == null ? "" : message)));

            if (image != null && image.length > 0) {
                String base64 = Base64.getEncoder().encodeToString(image);
                String src = "data:image/png;base64," + base64;
                Image img = new Image(src, "Kontaktbild");
                img.setMaxWidth("100%");
                content.add(img);
            }

            TextField inputField = new TextField("Antwort");
            inputField.setWidthFull();
            if (inputFieldText != null) {
                inputField.setValue(inputFieldText);
            }
            content.add(inputField);

            dialog.add(content);

            Button okButton = new Button("Ok", VaadinIcon.CHECK.create(), e -> {
                result[0] = inputField.getValue();
                dialog.close();
                latch.countDown();
            });
            okButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            Button cancelButton = new Button("Abbrechen", VaadinIcon.CLOSE.create(), e -> {
                // leave result[0] null to indicate cancellation
                dialog.close();
                latch.countDown();
            });
            cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            HorizontalLayout buttons = new HorizontalLayout(cancelButton, okButton);
            buttons.setSpacing(true);
            dialog.getFooter().add(buttons);

            parent.add(dialog);
            dialog.open();
            try {
                ui.push();
            } catch (Exception ignored) {
                // push might not be available in some environments
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for dialog result", e);
        }

        if (result[0] != null) {
            return result[0];
        }

        throw new RuntimeException("Dialog abgebrochen");
    }
}
