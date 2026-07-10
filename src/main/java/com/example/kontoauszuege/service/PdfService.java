package com.example.kontoauszuege.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.springframework.stereotype.Service;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Arrays;

@Service
public class PdfService {

    private static final String REGION_NAME = "selection";
    private volatile byte[] pdfBytes;

    public void setPdf(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("pdfBytes darf nicht leer sein.");
        }
        this.pdfBytes = Arrays.copyOf(pdfBytes, pdfBytes.length);
    }

    public String extractText(int pageNum, double xFrom, double xTo, double yFrom, double yTo) {
        validateCoordinates(xFrom, xTo, yFrom, yTo);

        byte[] bytes = this.pdfBytes;
        if (bytes == null) {
            throw new IllegalStateException("Es wurde noch keine PDF gesetzt. Bitte zuerst setPdf(byte[]) aufrufen.");
        }

        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.getNumberOfPages() == 0) {
                return "";
            }

            // clamp pageNum to valid range
            int total = document.getNumberOfPages();
            int usePage = Math.max(1, Math.min(pageNum, total));
            PDPage page = document.getPage(usePage - 1);
            PDRectangle box = page.getCropBox() != null ? page.getCropBox() : page.getMediaBox();
            if (box == null) {
                throw new IllegalStateException("Die PDF-Seite enthält keine gültige Seitengeometrie.");
            }

            double pageWidth = box.getWidth();
            double pageHeight = box.getHeight();

            double left = percentToAbsolute(xFrom, pageWidth);
            double right = percentToAbsolute(xTo, pageWidth);
            double top = percentToAbsolute(yFrom, pageHeight);
            double bottom = percentToAbsolute(yTo, pageHeight);

            Rectangle2D region = new Rectangle2D.Double(
                    left,
                    top,
                    right - left,
                    bottom - top
            );

            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            stripper.setSortByPosition(true);
            stripper.addRegion(REGION_NAME, region);
            stripper.extractRegions(page);

            return stripper.getTextForRegion(REGION_NAME).trim();
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Auslesen der PDF: " + e.getMessage(), e);
        }
    }

    private void validateCoordinates(double xFrom, double xTo, double yFrom, double yTo) {
        validateRange("xFrom", xFrom);
        validateRange("xTo", xTo);
        validateRange("yFrom", yFrom);
        validateRange("yTo", yTo);

        if (xFrom > xTo) {
            throw new IllegalArgumentException("xFrom muss kleiner oder gleich xTo sein.");
        }
        if (yFrom > yTo) {
            throw new IllegalArgumentException("yFrom muss kleiner oder gleich yTo sein.");
        }
    }

    private void validateRange(String name, double value) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(name + " muss im Bereich 0..100 liegen.");
        }
    }

    private double percentToAbsolute(double value, double maxValue) {
        return (value / 100.0d) * maxValue;
    }
}
