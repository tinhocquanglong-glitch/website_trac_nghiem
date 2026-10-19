package com.tttn.webthitracnghiem.controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SchoolUsersTemplateTest {
    @Test
    void teacherAssignmentRendersTheSubjectNameProperty() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/user/school-users.html"), StandardCharsets.UTF_8);

        assertThat(template)
                .contains("a.subject.name")
                .doesNotContain("a.subject.subjectName");
    }
}
