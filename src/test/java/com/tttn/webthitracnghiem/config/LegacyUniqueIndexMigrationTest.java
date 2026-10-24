package com.tttn.webthitracnghiem.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegacyUniqueIndexMigrationTest {

    @Test
    void dropsOnlyLegacyIndexesThatStillExist() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class),
                eq("student_tracking_class"), eq("uk_tracking_class_year"))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class),
                eq("student_monthly_record"), eq("uk_student_semester_month"))).thenReturn(0);

        new LegacyUniqueIndexMigration(jdbcTemplate).run();

        verify(jdbcTemplate).execute("ALTER TABLE student_tracking_class DROP INDEX uk_tracking_class_year");
        verify(jdbcTemplate, never()).execute("ALTER TABLE student_monthly_record DROP INDEX uk_student_semester_month");
    }
}
