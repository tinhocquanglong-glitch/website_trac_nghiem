package com.tttn.webthitracnghiem.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LessonPlanContentSanitizerTest {
    @Test
    void sanitizeKeepsVietnameseAndValidEmojiButDropsInvalidUnicodeCharacters() {
        String html = "<p>Ti\u1ebfng Vi\u1ec7t \uD800\u0001 \uD83D\uDE00</p>";

        String sanitized = LessonPlanContentSanitizer.sanitize(html);

        assertThat(sanitized).contains("Ti\u1ebfng Vi\u1ec7t", "\uD83D\uDE00")
                .doesNotContain("\uD800", "\u0001");
    }

    @Test
    void sanitizeDropsUnicodeNoncharactersThatCanBreakDatabaseWrites() {
        String html = "<p>Trước\uFDD0 giữa\uFFFE và sau\uFFFF</p>";

        String sanitized = LessonPlanContentSanitizer.sanitize(html);

        assertThat(sanitized).isEqualTo("<p>Trước giữa và sau</p>");
    }
}
