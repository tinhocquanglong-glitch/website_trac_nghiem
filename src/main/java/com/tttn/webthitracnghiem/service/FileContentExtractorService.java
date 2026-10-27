package com.tttn.webthitracnghiem.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class FileContentExtractorService {

    private static final Logger logger = LoggerFactory.getLogger(FileContentExtractorService.class);
    private static final long MAX_FILE_SIZE = 15L * 1024L * 1024L; // 15MB

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng tải lên một file tài liệu.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Dung lượng file vượt quá giới hạn cho phép (tối đa 15MB).");
        }

        String originalFilename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String lowerName = originalFilename.toLowerCase(Locale.ROOT);

        try (InputStream inputStream = file.getInputStream()) {
            if (lowerName.endsWith(".docx")) {
                return extractDocx(inputStream);
            } else if (lowerName.endsWith(".doc")) {
                return extractDoc(inputStream);
            } else if (lowerName.endsWith(".pdf")) {
                return extractPdf(inputStream);
            } else if (lowerName.endsWith(".txt") || lowerName.endsWith(".md")) {
                return extractPlainText(inputStream);
            } else {
                // Thử đọc dưới dạng văn bản thông thường nếu không thuộc các đuôi trên
                return extractPlainText(inputStream);
            }
        } catch (Exception ex) {
            logger.error("Lỗi khi đọc file tài liệu {}: {}", originalFilename, ex.getMessage(), ex);
            throw new IllegalArgumentException("Không thể đọc nội dung file (" + originalFilename + "). Vui lòng đảm bảo file không bị lỗi định dạng hoặc đặt mật khẩu.");
        }
    }

    private String extractDocx(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            for (var element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String text = paragraph.getText();
                    if (text != null && !text.isBlank()) {
                        sb.append(text.trim()).append("\n");
                    }
                } else if (element instanceof XWPFTable table) {
                    table.getRows().forEach(row ->
                            row.getTableCells().forEach(cell -> {
                                String cellText = cell.getText();
                                if (cellText != null && !cellText.isBlank()) {
                                    sb.append(cellText.trim()).append(" | ");
                                }
                            }));
                    sb.append("\n");
                }
            }
        }
        return sb.toString().trim();
    }

    private String extractDoc(InputStream inputStream) throws IOException {
        try (HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText().trim();
        }
    }

    private String extractPdf(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).trim();
        }
    }

    private String extractPlainText(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }
}
