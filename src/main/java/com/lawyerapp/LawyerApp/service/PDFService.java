package com.lawyerapp.LawyerApp.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.lawyerapp.LawyerApp.dao.DataRepository;
import com.lawyerapp.LawyerApp.model.Client;
import com.lawyerapp.LawyerApp.model.User;
import com.lawyerapp.LawyerApp.util.SessionManager;

public class PDFService {

    // Dimensioni e layout migliorati
    private static final float MARGIN = 30;
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float HEADER_HEIGHT = 100;
    private static final float TABLE_TOP_Y = PAGE_HEIGHT - MARGIN - HEADER_HEIGHT;
    private static final float MIN_ROW_HEIGHT = 22; // Aumentato per più spazio
    private static final float CELL_PADDING = 6; // Aumentato per più spazio
    private static final float TABLE_WIDTH = PAGE_WIDTH - (2 * MARGIN);
    private static final float FONT_SIZE_TITLE = 22;
    private static final float FONT_SIZE_SUBTITLE = 14;
    private static final float FONT_SIZE_HEADER = 11;
    private static final float FONT_SIZE_CONTENT = 10;
    private static final float LINE_THICKNESS = 0.5f;
    private static final float ROW_LINE_SPACING = 2f;

    // Font
    private static final PDFont TITLE_FONT = PDType1Font.HELVETICA_BOLD;
    private static final PDFont SUBTITLE_FONT = PDType1Font.HELVETICA;
    private static final PDFont HEADER_FONT = PDType1Font.HELVETICA_BOLD;
    private static final PDFont CONTENT_FONT = PDType1Font.HELVETICA;
    private static final PDFont FOOTER_FONT = PDType1Font.HELVETICA_OBLIQUE;

    // Colori (sfondo bianco)
    private static final PDColor WHITE = new PDColor(new float[]{1f, 1f, 1f}, PDDeviceRGB.INSTANCE);
    private static final PDColor HEADER_BACKGROUND = new PDColor(new float[]{0.95f, 0.95f, 0.95f}, PDDeviceRGB.INSTANCE);
    private static final PDColor ALT_ROW_COLOR = new PDColor(new float[]{0.98f, 0.98f, 0.98f}, PDDeviceRGB.INSTANCE);
    private static final PDColor BORDER_COLOR = new PDColor(new float[]{0.8f, 0.8f, 0.8f}, PDDeviceRGB.INSTANCE);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    private static volatile boolean fontsPreloaded = false;

    public void generateClientReport(File file) throws IOException {
        // Controllo iniziale dell'utente
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            System.err.println("Errore: Utente corrente non trovato nella sessione.");
            return;
        }

        // Preload dei font se necessario
        if (!fontsPreloaded) {
            preloadFonts();
        }
        
        // Configurazione logging per sopprimere i warning dei font
        Logger pdfboxLogger = Logger.getLogger("org.apache.pdfbox");
        Logger fontboxLogger = Logger.getLogger("org.apache.fontbox");
        Level originalLevelPdfbox = pdfboxLogger.getLevel();
        Level originalLevelFontbox = fontboxLogger.getLevel();
        pdfboxLogger.setLevel(Level.SEVERE);
        fontboxLogger.setLevel(Level.SEVERE);

        try {
            List<Client> clients = new DataRepository().getClientsByLawyer(currentUser.getId());

            try (PDDocument document = new PDDocument()) {
                PDPage currentPage = new PDPage(PDRectangle.A4);
                document.addPage(currentPage);

                PDPageContentStream contentStream = new PDPageContentStream(document, currentPage);
                
                // Sfondo bianco
                contentStream.setNonStrokingColor(WHITE);
                contentStream.addRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
                contentStream.fill();
                
                drawPageHeader(contentStream, currentUser, document, 1);

                // Larghezze colonne ottimizzate (MODIFICATO QUI: Aumentato Data a 13%, ridotto Nome e Cognome)
                float[] columnWidths = {
                    TABLE_WIDTH * 0.13f, // Data (Aumentato)
                    TABLE_WIDTH * 0.08f, // Ora
                    TABLE_WIDTH * 0.10f, // Nome (Ridotto)
                    TABLE_WIDTH * 0.10f, // Cognome (Ridotto)
                    TABLE_WIDTH * 0.12f, // N. Proc.
                    TABLE_WIDTH * 0.15f, // Tribunale
                    TABLE_WIDTH * 0.32f  // Note
                };
                
                String[] headers = {"Data", "Ora", "Nome", "Cognome", "N. Proc.", "Tribunale", "Note"};

                float currentY = TABLE_TOP_Y;
                currentY = drawTableHeader(contentStream, headers, columnWidths, currentY);

                int rowIndex = 0;
                int pageNumber = 1;

                for (Client client : clients) {
                    String[] rowData = {
                        client.getAppointmentDate().format(DATE_FORMATTER),
                        client.getAppointmentTime() != null ? client.getAppointmentTime().format(TIME_FORMATTER) : "",
                        client.getName(),
                        client.getSurname(),
                        client.getCaseNumber() != null ? client.getCaseNumber() : "",
                        client.getCourt() != null ? client.getCourt() : "",
                        client.getNotes() != null ? client.getNotes() : ""
                    };

                    float rowHeight = calculateMaxRowHeight(rowData, columnWidths, FONT_SIZE_CONTENT, MIN_ROW_HEIGHT);

                    if (currentY - rowHeight <= MARGIN) {
                        drawFooter(contentStream, pageNumber);
                        contentStream.close();
                        
                        currentPage = new PDPage(PDRectangle.A4);
                        document.addPage(currentPage);
                        contentStream = new PDPageContentStream(document, currentPage);
                        
                        // Sfondo bianco per la nuova pagina
                        contentStream.setNonStrokingColor(WHITE);
                        contentStream.addRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
                        contentStream.fill();
                        
                        pageNumber++;
                        drawPageHeader(contentStream, currentUser, document, pageNumber);
                        currentY = TABLE_TOP_Y;
                        currentY = drawTableHeader(contentStream, headers, columnWidths, currentY);
                        rowIndex = 0;
                    }

                    currentY -= rowHeight;
                    drawTableRow(contentStream, rowData, columnWidths, currentY, rowHeight, rowIndex++);
                }

                drawFooter(contentStream, pageNumber);
                contentStream.close();
                document.save(file);
                System.out.println("PDF generato correttamente: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Errore durante la generazione del PDF: " + e.getMessage());
            throw e;
        } finally {
            // Ripristino livello di logging originale
            if (originalLevelPdfbox != null) pdfboxLogger.setLevel(originalLevelPdfbox);
            if (originalLevelFontbox != null) fontboxLogger.setLevel(originalLevelFontbox);
        }
    }

    private void preloadFonts() {
        new Thread(() -> {
            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.setFont(PDType1Font.HELVETICA, 10);
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
                }
                doc.save(new ByteArrayOutputStream());
                fontsPreloaded = true;
                System.out.println("Font pre-caricati con successo");
            } catch (IOException e) {
                System.err.println("Errore nel pre-caricamento dei font: " + e.getMessage());
            }
        }).start();
    }

    private void drawPageHeader(PDPageContentStream cs, User user, PDDocument doc, int pageNumber) throws IOException {
        // Caricamento del logo con gestione errori migliorata
        try (InputStream imageStream = getClass().getClassLoader().getResourceAsStream("img/prova.png")) {
            if (imageStream != null) {
                byte[] imageBytes = imageStream.readAllBytes();
                if (imageBytes.length > 0) {
                    PDImageXObject logo = PDImageXObject.createFromByteArray(doc, imageBytes, "logo");
                    // Posiziona il logo a sinistra
                    float logoHeight = 50;
                    float logoWidth = 50;
                    float logoY = PAGE_HEIGHT - MARGIN - 40;
                    cs.drawImage(logo, MARGIN, logoY, logoWidth, logoHeight);
                }
            }
        } catch (Exception e) {
            System.err.println("Errore nel caricamento del logo: " + e.getMessage());
        }

        // Titolo centrato
        String title = "REPORT CLIENTS";
        float titleWidth = TITLE_FONT.getStringWidth(title) / 1000 * FONT_SIZE_TITLE;
        float titleX = (PAGE_WIDTH - titleWidth) / 2;
        
        cs.beginText();
        cs.setFont(TITLE_FONT, FONT_SIZE_TITLE);
        cs.setNonStrokingColor(0, 0, 0);
        cs.newLineAtOffset(titleX, PAGE_HEIGHT - MARGIN - 30);
        cs.showText(title);
        cs.endText();

        // Nome avvocato centrato
        String lawyerText = "Avv. " + user.getFullName();
        float lawyerWidth = SUBTITLE_FONT.getStringWidth(lawyerText) / 1000 * FONT_SIZE_SUBTITLE;
        float lawyerX = (PAGE_WIDTH - lawyerWidth) / 2;
        
        cs.beginText();
        cs.setFont(SUBTITLE_FONT, FONT_SIZE_SUBTITLE);
        cs.setNonStrokingColor(0.3f, 0.3f, 0.3f);
        cs.newLineAtOffset(lawyerX, PAGE_HEIGHT - MARGIN - 60);
        cs.showText(lawyerText);
        cs.endText();

        // Linea separatore
        cs.setStrokingColor(BORDER_COLOR);
        cs.setLineWidth(1);
        cs.moveTo(MARGIN, PAGE_HEIGHT - MARGIN - 70);
        cs.lineTo(PAGE_WIDTH - MARGIN, PAGE_HEIGHT - MARGIN - 70);
        cs.stroke();
    }

    private float drawTableHeader(PDPageContentStream cs, String[] headers, float[] widths, float yStart) throws IOException {
        float headerY = yStart - MIN_ROW_HEIGHT;
        
        // Intestazione con sfondo grigio chiaro
        cs.setNonStrokingColor(HEADER_BACKGROUND);
        cs.addRect(MARGIN, headerY, TABLE_WIDTH, MIN_ROW_HEIGHT);
        cs.fill();

        // Bordi intestazione
        cs.setStrokingColor(BORDER_COLOR);
        cs.setLineWidth(LINE_THICKNESS);
        cs.addRect(MARGIN, headerY, TABLE_WIDTH, MIN_ROW_HEIGHT);
        cs.stroke();

        cs.setFont(HEADER_FONT, FONT_SIZE_HEADER);
        cs.setNonStrokingColor(0, 0, 0);

        float x = MARGIN;
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            float textWidth = HEADER_FONT.getStringWidth(header) / 1000 * FONT_SIZE_HEADER;
            float textX = x + (widths[i] - textWidth) / 2; // Testo centrato nella colonna
            
            cs.beginText();
            cs.newLineAtOffset(textX, headerY + CELL_PADDING + 5);
            cs.showText(header);
            cs.endText();
            
            // Linea verticale tra le colonne
            if (i < headers.length - 1) {
                float colEnd = x + widths[i];
                cs.moveTo(colEnd, headerY);
                cs.lineTo(colEnd, headerY + MIN_ROW_HEIGHT);
                cs.stroke();
            }
            
            x += widths[i];
        }

        return headerY;
    }

    private void drawTableRow(PDPageContentStream cs, String[] data, float[] widths, float y, float height, int idx) throws IOException {
        // Sfondo alternato
        cs.setNonStrokingColor(idx % 2 == 0 ? WHITE : ALT_ROW_COLOR);
        cs.addRect(MARGIN, y, TABLE_WIDTH, height);
        cs.fill();

        // Bordi della riga
        cs.setStrokingColor(BORDER_COLOR);
        cs.setLineWidth(LINE_THICKNESS);
        cs.addRect(MARGIN, y, TABLE_WIDTH, height);
        cs.stroke();

        cs.setFont(CONTENT_FONT, FONT_SIZE_CONTENT);
        cs.setNonStrokingColor(0, 0, 0);

        float x = MARGIN;
        for (int i = 0; i < data.length; i++) {
            String text = data[i] != null ? data[i] : "";
            float maxWidth = widths[i] - 2 * CELL_PADDING;
            
            // Calcola l'altezza necessaria per questa cella
            List<String> lines = splitTextIntoLines(text, maxWidth, FONT_SIZE_CONTENT, CONTENT_FONT);
            
            // Calcola l'altezza totale del testo
            float totalTextHeight = lines.size() * (FONT_SIZE_CONTENT + ROW_LINE_SPACING) - ROW_LINE_SPACING;
            
            // Calcola la posizione Y di partenza per centrare verticalmente
            // Questo ora allinea il testo in alto all'interno della cella, non più centrato verticalmente
            float startY = y + height - CELL_PADDING - FONT_SIZE_CONTENT;
            
            for (String line : lines) {
                float textWidth = CONTENT_FONT.getStringWidth(line) / 1000 * FONT_SIZE_CONTENT;
                float textX = x + CELL_PADDING; // Testo allineato a sinistra

                cs.beginText();
                cs.newLineAtOffset(textX, startY);
                cs.showText(line);
                cs.endText();
                
                startY -= (FONT_SIZE_CONTENT + ROW_LINE_SPACING);
            }
            
            // Linea verticale tra le colonne
            if (i < data.length - 1) {
                float colEnd = x + widths[i];
                cs.moveTo(colEnd, y);
                cs.lineTo(colEnd, y + height);
                cs.stroke();
            }
            
            x += widths[i];
        }
    }

    private List<String> splitTextIntoLines(String text, float maxW, float size, PDFont font) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            lines.add("");
            return lines;
        }
        
        // Gestione esplicita dei newline
        String[] paragraphs = text.split("\\R");
        
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add(""); // Aggiungi riga vuota per newline
                continue;
            }
            
            StringBuilder currentLine = new StringBuilder();
            String[] words = paragraph.split("\\s+"); // Split by one or more spaces

            for (String word : words) {
                // Handle very long words that exceed column width
                if (font.getStringWidth(word) / 1000 * size > maxW) {
                    // If currentLine has content, add it before splitting the long word
                    if (currentLine.length() > 0) {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder();
                    }
                    
                    // Split the long word
                    int start = 0;
                    while (start < word.length()) {
                        int end = word.length();
                        while (start < end) {
                            String subWord = word.substring(start, end);
                            if (font.getStringWidth(subWord) / 1000 * size <= maxW) {
                                lines.add(subWord);
                                start = end;
                                break;
                            }
                            end--;
                        }
                        if (start < word.length() && start == end) { // No fit found, take a forced break
                             // If a single character doesn't fit, it's an extreme case,
                             // but we need to break it. For simplicity, force a break.
                            lines.add(word.substring(start));
                            start = word.length();
                        }
                    }
                } else {
                    // Regular word processing
                    String testLine;
                    if (currentLine.length() == 0) {
                        testLine = word;
                    } else {
                        testLine = currentLine + " " + word;
                    }

                    if (font.getStringWidth(testLine) / 1000 * size <= maxW) {
                        currentLine = new StringBuilder(testLine);
                    } else {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder(word);
                    }
                }
            }
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }
        
        return lines;
    }


    private float calculateMaxRowHeight(String[] data, float[] widths, float size, float minHeight) throws IOException {
        float maxHeight = minHeight;
        for (int i = 0; i < data.length; i++) {
            float maxW = widths[i] - 2 * CELL_PADDING;
            List<String> lines = splitTextIntoLines(data[i], maxW, size, CONTENT_FONT);
            // Ensure at least one line height even for empty cells, for consistent row height
            float cellHeight = Math.max(1, lines.size()) * (size + ROW_LINE_SPACING) - ROW_LINE_SPACING; 
            if (cellHeight > maxHeight) {
                maxHeight = cellHeight;
            }
        }
        // Add padding to the final height
        return Math.max(minHeight, maxHeight + 2 * CELL_PADDING);
    }

    private void drawFooter(PDPageContentStream cs, int page) throws IOException {
        String footer = "Pagina " + page + " – Generato il " + java.time.LocalDate.now().format(DATE_FORMATTER);
        float width = FOOTER_FONT.getStringWidth(footer) / 1000 * (FONT_SIZE_CONTENT - 1);
        float centerX = (PAGE_WIDTH - width) / 2;
        
        cs.beginText();
        cs.setFont(FOOTER_FONT, FONT_SIZE_CONTENT - 1);
        cs.setNonStrokingColor(0.4f, 0.4f, 0.4f);
        cs.newLineAtOffset(centerX, MARGIN - 10);
        cs.showText(footer);
        cs.endText();
    }
}