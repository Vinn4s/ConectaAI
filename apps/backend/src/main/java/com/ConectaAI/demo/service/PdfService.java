package com.ConectaAI.demo.service;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class PdfService {
    public String readPdf() {
        try {
            File file = new File("empresa.pdf");

            PDDocument document = PDDocument.load(file);

            PDFTextStripper pdfStripper = new PDFTextStripper();

            String text = pdfStripper.getText(document);

            document.close();
            return text;
        } catch (IOException e) {
            e.printStackTrace();
            return "Erro ao ler o PDF.";
        }
    } 
   
}