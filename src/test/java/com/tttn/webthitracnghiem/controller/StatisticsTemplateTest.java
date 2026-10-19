package com.tttn.webthitracnghiem.controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsTemplateTest {
    @Test
    void studentStatisticsTemplateHasSeparateRealAndPracticeSections() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/student/statistics.html"), StandardCharsets.UTF_8);

        assertThat(template)
                .contains("stats-hero", "stats-hero-content", "stats-hero-icon", "/css/navbar.css", "realResults", "practiceResults", "realAverage", "practiceAverage")
                .contains("table-responsive", "empty-state");
    }

    @Test
    void teacherStatisticsTemplateShowsParticipationSummaryAndSorting() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/teacher/exam-statistics.html"), StandardCharsets.UTF_8);

        assertThat(template)
                .contains("stats-hero", "stats-hero-content", "stats-hero-icon", "/css/navbar.css", "takenCount", "pendingCount", "averageScore")
                .contains("sort='score'", "sort='time'", "table-responsive", "empty-state");
    }
}
