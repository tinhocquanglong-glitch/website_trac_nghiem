package com.tttn.webthitracnghiem.controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ExamTemplateTest {

    @Test
    void selectedQuestionCheckboxesUseOneStableSelectorForCompletionAndSubmit() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/exam/createExam.html"),
                StandardCharsets.UTF_8);

        assertThat(template)
                .contains("class=\"check question-selection\"")
                .contains("table.$(\"input.question-selection:checked\")")
                .doesNotContain("table.$(\"input[name=questions]:checked\")");
    }

    @Test
    void examScopeComesFromCurriculumAndQuestionPool() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/exam/createExam.html"),
                StandardCharsets.UTF_8);

        assertThat(template)
                .contains("/api/curriculum", "/api/question/pool", "id=\"scopeTree\"")
                .contains("lessonIds: scopeLessonIds()", "exam.append(\"lessonIds\", id)")
                .doesNotContain("/lesson-plans/catalog", "legacyLessonId", "selectedLessonId");
    }

    @Test
    void examListExposesTeacherStatisticsAction() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/exam/listExam.html"),
                StandardCharsets.UTF_8);

        assertThat(template)
                .contains("/teacher/exams/{id}/statistics")
                .contains("Xem thống kê bài thi");
    }

    @Test
    void lessonExamListRendersLegacyAndCustomLessonPlanExams() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/listExamSubject.html"),
                StandardCharsets.UTF_8);

        assertThat(template)
                .contains("lessonPlanLesson != null")
                .contains("lessonPlanLesson.chapter.name")
                .contains("lessonPlanLesson.name")
                .contains("lessonPlanLesson.chapter.lessonPlan.subject.name");
    }

    @Test
    void legacyLessonExamListOffersNavigationToTheMatchingLessonPlanRoute() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/listExamSubject.html"),
                StandardCharsets.UTF_8);

        assertThat(template)
                .contains("lessonPlanCandidate != null")
                .contains("lessonPlanCandidatePath")
                .contains("Mở bài học trong giáo án");
    }

    @Test
    void customExamEditPageLoadsAndDismissesItsPreloader() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/exam/editCustomExam.html"),
                StandardCharsets.UTF_8);

        assertThat(template)
                .contains("/js/loader.js")
                .contains("id=\"preloader\"");
    }

    @Test
    void editTemplatesDoNotSubmitEmptyExamDates() throws Exception {
        String legacyTemplate = Files.readString(
                Path.of("src/main/resources/templates/exam/editExam.html"),
                StandardCharsets.UTF_8);
        String customTemplate = Files.readString(
                Path.of("src/main/resources/templates/exam/editCustomExam.html"),
                StandardCharsets.UTF_8);

        assertThat(legacyTemplate)
                .contains("th:if=\"${exam.createDate != null}\"")
                .contains("th:if=\"${exam.lastModify != null}\"")
                .contains("if (createDate) exam.append(\"createDate\", createDate);")
                .contains("if (lastModify) exam.append(\"lastModify\", lastModify);");
        assertThat(customTemplate)
                .contains("th:if=\"${exam.createDate != null}\"")
                .contains("th:if=\"${exam.lastModify != null}\"");
    }
}
