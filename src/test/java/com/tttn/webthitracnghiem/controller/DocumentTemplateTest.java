package com.tttn.webthitracnghiem.controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTemplateTest {

    @Test
    void createFormBindsAuthorAndSubjectIdsAsNestedRequestFields() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/document/add.html"),
                StandardCharsets.UTF_8);

        assertThat(template)
                .contains("document.append(\"users.id\", author)")
                .contains("document.append(\"subject.id\", subject)")
                .doesNotContain("document.append(\"users\", author)")
                .doesNotContain("document.append(\"subject\", subject)");
    }
}
