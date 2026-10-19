package com.tttn.webthitracnghiem.controller;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;

import com.tttn.webthitracnghiem.repository.LessonPlanRepository;
import com.tttn.webthitracnghiem.model.LessonPlanLessonRequest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LessonPlanTemplateTest {
    @Test
    void detailedLessonPlanFetchDoesNotJoinNestedListBags() throws Exception {
        EntityGraph graph = LessonPlanRepository.class
                .getMethod("findDetailedById", Integer.class)
                .getAnnotation(EntityGraph.class);

        assertThat(graph).isNotNull();
        assertThat(graph.attributePaths())
                .contains("planChapters")
                .doesNotContain("planChapters.lessons");
    }

    @Test
    void formSubmitsIndependentScopeAndCustomContent() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/lesson-plan/form.html"), StandardCharsets.UTF_8);

        assertThat(template)
                .contains("th:field=\"*{subjectId}\"")
                .contains("th:field=\"*{gradeId}\"")
                .contains("th:field=\"*{classId}\"")
                .contains("chapterTemplate")
                .contains("lessonTemplate")
                .contains("addChapter")
                .contains("th:field=\"*{isPublic}\"")
                .contains("data-grade")
                .contains("main.right_col")
                .contains("grades.isEmpty()")
                .contains("enctype=\"multipart/form-data\"")
                .contains("Chủ đề")
                .contains("content")
                .contains("attachment");
    }

    @Test
    void listShowsOwnerAndVisibility() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/lesson-plan/list.html"), StandardCharsets.UTF_8);

        assertThat(template)
                .contains("plan.owner.fullName")
                .contains("plan.isPublic")
                .contains("plan.grade.displayName")
                .contains("plan.subject.name")
                .contains("/lesson-plans/create")
                .contains("name=\"q\"")
                .contains("name=\"subjectId\"")
                .contains("name=\"gradeId\"")
                .contains("name=\"classId\"")
                .contains("name=\"teacherName\"")
                .contains("name=\"mine\"")
                .contains("name=\"visibility\"")
                .contains("custom.min.js");
    }

    @Test
    void detailShowsTopicLessonContentAndAttachment() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/lesson-plan/detail.html"), StandardCharsets.UTF_8);

        assertThat(template)
                .contains("Chủ đề")
                .contains("lesson.content")
                .contains("lesson.attachmentOriginalName")
                .contains("/lesson-plans/lesson/{lessonId}/attachment")
                .contains("custom.min.js");
    }

    @Test
    void editRequestKeepsExistingAttachmentNameForTheForm() {
        LessonPlanLessonRequest request = new LessonPlanLessonRequest();

        request.setAttachmentOriginalName("bai-hoc.docx");

        assertThat(request.getAttachmentOriginalName()).isEqualTo("bai-hoc.docx");
    }
}
