package com.exam.exam_system.service;

import com.exam.exam_system.model.Question;
import com.exam.exam_system.model.Ticket;
import com.google.zxing.WriterException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordExportService {

    private final QrCodeService qrCodeService;

    @Value("${app.output-dir:./generated-tickets}")
    private String outputDir;

    private static final int MAX_SCORE = 50;

    // A4 eni: 11906 twip, sol 1701 + sağ 850 = 2551 kənar
    // İstifadə olunan en: 11906 - 2551 = 9355 twip = ~16.5 sm
    // _ simvolu Times New Roman 11pt ~ 7px = hər xətt ~100 simvol lazımdır
    private static final String LINE = "________________________________________________________________________________________________________________________________________________";

    public String generateAllTicketsWord(Long examId, List<Ticket> tickets)
            throws IOException, WriterException {
        Path outputPath = Paths.get(outputDir);
        if (!Files.exists(outputPath)) Files.createDirectories(outputPath);
        String fileName = "exam_" + examId + "_all_tickets.docx";
        String filePath = outputDir + "/" + fileName;
        try (XWPFDocument doc = new XWPFDocument()) {
            setPageSize(doc);
            for (int i = 0; i < tickets.size(); i++) {
                addTicket(doc, tickets.get(i));
                if (i < tickets.size() - 1) {
                    XWPFParagraph pb = doc.createParagraph();
                    pb.setPageBreak(true);
                    pb.setSpacingAfter(0);
                    pb.setSpacingBefore(0);
                }
            }
            try (FileOutputStream out = new FileOutputStream(filePath)) {
                doc.write(out);
            }
        }
        log.info("✅ {} bilet yaradıldı: {}", tickets.size(), fileName);
        return filePath;
    }

    public String generateTicketWord(Ticket ticket) throws IOException, WriterException {
        Path outputPath = Paths.get(outputDir);
        if (!Files.exists(outputPath)) Files.createDirectories(outputPath);
        String fileName = "ticket_" + ticket.getId() + "_bilet_" + ticket.getTicketNumber() + ".docx";
        String filePath = outputDir + "/" + fileName;
        try (XWPFDocument doc = new XWPFDocument()) {
            setPageSize(doc);
            addTicket(doc, ticket);
            try (FileOutputStream out = new FileOutputStream(filePath)) {
                doc.write(out);
            }
        }
        return filePath;
    }

    private void addTicket(XWPFDocument doc, Ticket ticket)
            throws IOException, WriterException {

        // ── BAŞLIQ: QR + məlumat cədvəli ──
        XWPFTable hdr = doc.createTable(1, 2);
        hdr.setWidth("100%");
        removeBorders(hdr);

        // Sol xana — QR kod (80x80)
        XWPFTableCell qrCell = hdr.getRow(0).getCell(0);
        qrCell.setWidth("1500");
        XWPFParagraph qrPara = qrCell.getParagraphs().get(0);
        qrPara.setSpacingAfter(0);
        qrPara.setSpacingBefore(0);
        XWPFRun qrRun = qrPara.createRun();
        byte[] qrBytes = qrCodeService.generateQrCode(ticket.getQrToken(), 120, 120);
        try (InputStream is = new ByteArrayInputStream(qrBytes)) {
            try {
                qrRun.addPicture(is, XWPFDocument.PICTURE_TYPE_PNG,
                        "qr.png", Units.toEMU(80), Units.toEMU(80));
            } catch (org.apache.poi.openxml4j.exceptions.InvalidFormatException e) {
                throw new IOException("QR xəta: " + e.getMessage(), e);
            }
        }

        // Sağ xana — fənn, bilet №, müddət
        XWPFTableCell infoCell = hdr.getRow(0).getCell(1);
        String subject = ticket.getExam().getSubject() != null
                ? ticket.getExam().getSubject() : ticket.getExam().getExamName();
        infoLine(infoCell, "Fənn:", subject);
        infoLine(infoCell, "Bilet №:", String.valueOf(ticket.getTicketNumber()));
        infoLine(infoCell, "Müddət:", ticket.getExam().getDurationMinutes() + " dəq");

        // ── AYIRICI XƏTTİ ──
        hrLine(doc);

        // ── SUALLAR ──
        List<Question> qs = ticket.getQuestions();
        for (int i = 0; i < qs.size(); i++) {
            XWPFParagraph p = doc.createParagraph();
            p.setSpacingBefore(40);
            p.setSpacingAfter(0);
            XWPFRun nr = p.createRun();
            nr.setBold(true);
            nr.setFontSize(11);
            nr.setFontFamily("Times New Roman");
            nr.setText((i + 1) + ".  ");
            XWPFRun tr = p.createRun();
            tr.setFontSize(11);
            tr.setFontFamily("Times New Roman");
            tr.setText(qs.get(i).getQuestionText());
        }

        // Cəmi bal — sağda
        XWPFParagraph cp = doc.createParagraph();
        cp.setAlignment(ParagraphAlignment.RIGHT);
        cp.setSpacingBefore(60);
        cp.setSpacingAfter(0);
        XWPFRun cr = cp.createRun();
        cr.setBold(true);
        cr.setFontSize(11);
        cr.setFontFamily("Times New Roman");
        cr.setText("Cəmi: " + MAX_SCORE + " bal");

        // ── AYIRICI XƏTTİ ──
        hrLine(doc);

        // ── CAVAB XƏTLƏRİ ──
        // A4 hündürlük: 16838 twip
        // Yuxarı + aşağı kənar: 1134 x2 = 2268
        // İstifadə olunan: 16838 - 2268 = 14570 twip = ~25.7 sm
        // Başlıq + suallar (3 sual) ~ 6 sm = ~3400 twip
        // Yoxlayan sətri ~ 1 sm = ~560 twip
        // Ayırıcılar ~ 0.8 sm = ~450 twip
        // Qalan cavab üçün: 14570 - 3400 - 560 - 450 = ~10160 twip = ~17.9 sm
        // Cavab xətləri — tam en boyunca cədvəl ilə
        XWPFTable lineTable = doc.createTable(17, 1);
        lineTable.setWidth("100%");
        CTTblPr tblPr = lineTable.getCTTbl().getTblPr();
        CTTblBorders tblBorders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
        CTBorder noB = CTBorder.Factory.newInstance(); noB.setVal(STBorder.NONE);
        tblBorders.setTop(noB); tblBorders.setLeft(noB); tblBorders.setRight(noB);
        tblBorders.setInsideV(noB); tblBorders.setInsideH(noB);
        CTBorder lineB = CTBorder.Factory.newInstance();
        lineB.setVal(STBorder.SINGLE); lineB.setSz(BigInteger.valueOf(4)); lineB.setColor("999999");
        tblBorders.setBottom(lineB);
        for (int i = 0; i < 17; i++) {
            XWPFTableCell lc = lineTable.getRow(i).getCell(0);
            CTTcPr tcPr = lc.getCTTc().isSetTcPr() ? lc.getCTTc().getTcPr() : lc.getCTTc().addNewTcPr();
            CTTcBorders tcBorders = tcPr.isSetTcBorders() ? tcPr.getTcBorders() : tcPr.addNewTcBorders();
            CTBorder tcNoB = CTBorder.Factory.newInstance(); tcNoB.setVal(STBorder.NONE);
            tcBorders.setTop(tcNoB); tcBorders.setLeft(tcNoB); tcBorders.setRight(tcNoB); tcBorders.setInsideH(tcNoB);
            CTBorder tcBot = CTBorder.Factory.newInstance();
            tcBot.setVal(STBorder.SINGLE); tcBot.setSz(BigInteger.valueOf(4)); tcBot.setColor("AAAAAA");
            tcBorders.setBottom(tcBot);
            XWPFParagraph lp = lc.getParagraphs().get(0);
            lp.setSpacingBefore(0);
            lp.setSpacingAfter(160);
            lp.createRun().setText("");
        }

        // ── YOXLAYAN + BAL + İMZA ──
        XWPFParagraph spacer = doc.createParagraph();
        spacer.setSpacingBefore(300);
        spacer.setSpacingAfter(0);
        spacer.createRun().setText("");

//        hrLine(doc);

        XWPFTable sign = doc.createTable(1, 3);
        sign.setWidth("100%");
        removeBorders(sign);

        XWPFParagraph p1 = sign.getRow(0).getCell(0).getParagraphs().get(0);
        p1.setSpacingAfter(0);
        XWPFRun r1 = p1.createRun();
        r1.setFontSize(11);
        r1.setFontFamily("Times New Roman");
        r1.setText("Yoxlayan: ___________________");

        XWPFParagraph p2 = sign.getRow(0).getCell(1).getParagraphs().get(0);
        p2.setAlignment(ParagraphAlignment.CENTER);
        p2.setSpacingAfter(0);
        XWPFRun r2 = p2.createRun();
        r2.setBold(true);
        r2.setFontSize(11);
        r2.setFontFamily("Times New Roman");
        r2.setText("Bal: _______ / " + MAX_SCORE);

        XWPFParagraph p3 = sign.getRow(0).getCell(2).getParagraphs().get(0);
        p3.setAlignment(ParagraphAlignment.RIGHT);
        p3.setSpacingAfter(0);
        XWPFRun r3 = p3.createRun();
        r3.setFontSize(11);
        r3.setFontFamily("Times New Roman");
        r3.setText("İmza: _______________");
    }

    private void infoLine(XWPFTableCell cell, String label, String value) {
        XWPFParagraph p = cell.addParagraph();
        p.setSpacingBefore(20);
        p.setSpacingAfter(0);
        XWPFRun l = p.createRun();
        l.setBold(true);
        l.setFontSize(11);
        l.setFontFamily("Times New Roman");
        l.setText(label + " ");
        XWPFRun v = p.createRun();
        v.setFontSize(11);
        v.setFontFamily("Times New Roman");
        v.setText(value);
    }

    private void hrLine(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(40);
        p.setSpacingAfter(40);
        CTPPr pPr = p.getCTP().addNewPPr();
        CTPBdr bdr = pPr.addNewPBdr();
        CTBorder b = bdr.addNewBottom();
        b.setVal(STBorder.SINGLE);
        b.setSz(BigInteger.valueOf(4));
        b.setColor("AAAAAA");
        p.createRun().setText("");
    }

    private void setPageSize(XWPFDocument doc) {
        CTDocument1 ctDoc = doc.getDocument();
        CTBody body = ctDoc.getBody();
        if (!body.isSetSectPr()) body.addNewSectPr();
        CTSectPr sectPr = body.getSectPr();
        CTPageSz sz = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        sz.setW(BigInteger.valueOf(11906));
        sz.setH(BigInteger.valueOf(16838));
        CTPageMar m = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        m.setTop(BigInteger.valueOf(1134));
        m.setBottom(BigInteger.valueOf(1134));
        m.setLeft(BigInteger.valueOf(1701));
        m.setRight(BigInteger.valueOf(850));
    }

    private void removeBorders(XWPFTable table) {
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        CTTblBorders borders = tblPr.isSetTblBorders() ?
                tblPr.getTblBorders() : tblPr.addNewTblBorders();
        CTBorder nb = CTBorder.Factory.newInstance();
        nb.setVal(STBorder.NONE);
        borders.setTop(nb);
        borders.setBottom(nb);
        borders.setLeft(nb);
        borders.setRight(nb);
        borders.setInsideH(nb);
        borders.setInsideV(nb);
    }
}