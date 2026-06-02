package com.ConectaAI.demo.service;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class PdfService {

    public String readPdf() {
        File file = new File("empresa.pdf");

        if (!file.exists()) {
            return "Nenhum PDF da empresa foi configurado no momento.";
        }

        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();

            return pdfStripper.getText(document);

        } catch (IOException e) {
            return "Erro ao ler o PDF da empresa.";
        }
    }
}