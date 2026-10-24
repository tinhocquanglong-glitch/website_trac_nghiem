package com.tttn.webthitracnghiem.model;

import org.junit.jupiter.api.Test;

import javax.persistence.Column;
import javax.persistence.Lob;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class LessonPlanLessonMappingTest {
    @Test
    void lessonContentUsesTextStringMappingSoVietnameseCanBePersisted() throws NoSuchFieldException {
        Field content = LessonPlanLesson.class.getDeclaredField("content");
        Column column = content.getAnnotation(Column.class);

        assertThat(content.isAnnotationPresent(Lob.class)).isFalse();
        assertThat(column.columnDefinition()).containsIgnoringCase("text");
        assertThat(column.columnDefinition()).containsIgnoringCase("utf8mb4");
    }

    @Test
    void attachmentDataRemainsLobForBinaryWordFiles() throws NoSuchFieldException {
        Field attachmentData = LessonPlanLesson.class.getDeclaredField("attachmentData");

        assertThat(attachmentData.isAnnotationPresent(Lob.class)).isTrue();
    }
}
