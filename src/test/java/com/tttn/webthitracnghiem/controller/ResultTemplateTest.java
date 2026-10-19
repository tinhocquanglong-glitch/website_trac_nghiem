package com.tttn.webthitracnghiem.controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTemplateTest {

    @Test
    void resultPageDoesNotDereferenceMissingStudent() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/exam/resultTest.html"),
                StandardCharsets.UTF_8);

        assertThat(template)
                .contains("result != null and result.users != null");
    }
}
