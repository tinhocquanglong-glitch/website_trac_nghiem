package com.tttn.webthitracnghiem.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileContentExtractorServiceTest {

    private final FileContentExtractorService extractor = new FileContentExtractorService();

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", new byte[0]);
        assertThatThrownBy(() -> extractor.extractText(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vui lòng tải lên một file");
    }

    @Test
    void extractsTextFromPlainTextFile() {
        String content = "Kế hoạch bài dạy môn Toán: Bài 1 Khái niệm hàm số.\n1. Định nghĩa\n2. Tính chất";
        MockMultipartFile file = new MockMultipartFile("file", "giaoan.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));

        String extracted = extractor.extractText(file);
        assertThat(extracted).contains("Khái niệm hàm số").contains("1. Định nghĩa");
    }
}
