package com.microfinance.service;

import com.microfinance.entity.LoanApplication;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Generates a formatted loan application PDF using Apache PDFBox.
 * Merges borrower details, guarantor info, document list, T&C, and signature.
 */
@Service
public class PdfGenerationService {

    private static final float MARGIN = 50f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public byte[] generateApplicationPdf(LoanApplication app, byte[] signatureImageBytes, String signatureContentType) throws IOException {
        try (PDDocument doc = new PDDocument()) {

            // ── Page 1: Application Details ──
            PDPage page1 = new PDPage(PDRectangle.A4);
            doc.addPage(page1);

            PDType1Font fontBold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontOblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page1)) {
                float y = PDRectangle.A4.getHeight() - MARGIN;

                // Header block
                y = drawCenteredText(cs, fontBold, 18, "MICROFINANCE LOAN APPLICATION", y);
                y -= 4;
                y = drawCenteredText(cs, fontRegular, 10, "Application No: " + app.getApplicationNumber(), y);
                y -= 4;
                y = drawCenteredText(cs, fontOblique, 9, "Submitted: " + (app.getCreatedAt() != null ? app.getCreatedAt().format(DATE_FMT) : "N/A"), y);
                y -= 10;
                y = drawHorizontalRule(cs, y);
                y -= 10;

                // Section 1 – Loan Requirements
                y = drawSectionHeader(cs, fontBold, "1. LOAN REQUIREMENTS", y);
                y = drawField(cs, fontBold, fontRegular, "Principal Amount (INR):", formatAmount(app.getAppliedAmount()), y);
                y = drawField(cs, fontBold, fontRegular, "Tenure:", app.getTenureMonths() + " months", y);
                y = drawField(cs, fontBold, fontRegular, "Purpose:", app.getPurpose(), y);
                y -= 10;

                // Section 2 – Guarantor Details
                y = drawSectionHeader(cs, fontBold, "2. GUARANTOR DETAILS", y);
                y = drawField(cs, fontBold, fontRegular, "Name:", safe(app.getGuarantorName()), y);
                y = drawField(cs, fontBold, fontRegular, "Address:", safe(app.getGuarantorAddress()), y);
                y = drawField(cs, fontBold, fontRegular, "City / PIN:", safe(app.getGuarantorCity()) + " – " + safe(app.getGuarantorZip()), y);
                y = drawField(cs, fontBold, fontRegular, "Aadhaar:", maskAadhaar(app.getGuarantorAadhaar()), y);
                y = drawField(cs, fontBold, fontRegular, "PAN:", safe(app.getGuarantorPan()), y);
                y -= 10;

                // Section 3 – Declaration
                y = drawSectionHeader(cs, fontBold, "3. DECLARATION", y);
                String decl = "I/We hereby declare that all information provided in this application is true, correct, " +
                        "and complete to the best of my/our knowledge. I/We agree to the Terms & Conditions of " +
                        "the Microfinance Loan Origination Platform and consent to the verification of all submitted details.";
                y = drawWrappedText(cs, fontOblique, 9, decl, y, PAGE_WIDTH - 2 * MARGIN);
                y -= 10;

                // Section 4 – Standard Terms & Conditions summary
                y = drawSectionHeader(cs, fontBold, "4. TERMS & CONDITIONS (SUMMARY)", y);
                String[] terms = {
                    "1. The borrower must repay the principal and applicable interest on agreed EMI dates.",
                    "2. Late payment attracts a penalty of 2% per month on the overdue amount.",
                    "3. The guarantor accepts joint and several liability for this loan.",
                    "4. The institution reserves the right to reject applications without stating reasons.",
                    "5. All disputes are subject to jurisdiction of the local civil courts.",
                    "6. The borrower consents to credit bureau reporting of repayment behaviour."
                };
                for (String term : terms) {
                    y = drawWrappedText(cs, fontRegular, 8.5f, term, y, PAGE_WIDTH - 2 * MARGIN);
                    y -= 3;
                }
                y -= 10;

                // Section 5 – Signature
                y = drawSectionHeader(cs, fontBold, "5. APPLICANT SIGNATURE", y);

                if (signatureImageBytes != null && signatureImageBytes.length > 0) {
                    try {
                        PDImageXObject sigImg = PDImageXObject.createFromByteArray(doc, signatureImageBytes, "signature");
                        float sigH = 60f;
                        float sigW = sigImg.getWidth() * (sigH / sigImg.getHeight());
                        if (sigW > 200) sigW = 200;
                        y -= sigH + 10;
                        cs.drawImage(sigImg, MARGIN, y, sigW, sigH);
                        y -= 5;
                    } catch (Exception ex) {
                        y = drawField(cs, fontBold, fontRegular, "Signature:", "[Signature image attached]", y);
                    }
                } else {
                    y = drawField(cs, fontBold, fontRegular, "Signature:", "[Not provided]", y);
                }

                y -= 5;
                y = drawHorizontalRule(cs, y);
                y -= 6;
                y = drawCenteredText(cs, fontOblique, 8,
                        "This is a system-generated document. For queries contact: support@microfinance.in", y);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    // ─── Drawing helpers ─────────────────────────────────────────────────────

    private float drawCenteredText(PDPageContentStream cs, PDType1Font font, float size, String text, float y) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * size;
        float x = (PAGE_WIDTH - textWidth) / 2;
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y - size - 4;
    }

    private float drawSectionHeader(PDPageContentStream cs, PDType1Font font, String text, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, 11);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(text);
        cs.endText();
        return y - 14;
    }

    private float drawField(PDPageContentStream cs, PDType1Font labelFont, PDType1Font valueFont,
                            String label, String value, float y) throws IOException {
        float labelWidth = 160f;
        cs.beginText();
        cs.setFont(labelFont, 9);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(label);
        cs.endText();

        cs.beginText();
        cs.setFont(valueFont, 9);
        cs.newLineAtOffset(MARGIN + labelWidth, y);
        // Truncate if too long
        String safeVal = value != null ? value : "-";
        if (safeVal.length() > 60) safeVal = safeVal.substring(0, 57) + "...";
        cs.showText(safeVal);
        cs.endText();
        return y - 13;
    }

    private float drawWrappedText(PDPageContentStream cs, PDType1Font font, float size, String text, float y, float maxWidth) throws IOException {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String testLine = line.isEmpty() ? word : line + " " + word;
            float lineWidth = font.getStringWidth(testLine) / 1000 * size;
            if (lineWidth > maxWidth && !line.isEmpty()) {
                cs.beginText();
                cs.setFont(font, size);
                cs.newLineAtOffset(MARGIN, y);
                cs.showText(line.toString());
                cs.endText();
                y -= (size + 3);
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(testLine);
            }
        }
        if (!line.isEmpty()) {
            cs.beginText();
            cs.setFont(font, size);
            cs.newLineAtOffset(MARGIN, y);
            cs.showText(line.toString());
            cs.endText();
            y -= (size + 3);
        }
        return y;
    }

    private float drawHorizontalRule(PDPageContentStream cs, float y) throws IOException {
        cs.setLineWidth(0.5f);
        cs.moveTo(MARGIN, y);
        cs.lineTo(PAGE_WIDTH - MARGIN, y);
        cs.stroke();
        return y - 4;
    }

    private String safe(String val) { return val != null ? val : "N/A"; }

    private String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.length() < 4) return "N/A";
        return "XXXX XXXX " + aadhaar.substring(aadhaar.length() - 4);
    }

    private String formatAmount(java.math.BigDecimal amount) {
        if (amount == null) return "N/A";
        return "INR " + String.format("%,.2f", amount);
    }
}
