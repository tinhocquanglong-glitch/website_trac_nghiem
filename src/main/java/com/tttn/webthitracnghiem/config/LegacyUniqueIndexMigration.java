package com.tttn.webthitracnghiem.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Drops unique indexes left behind by earlier entity versions. Hibernate's
 * ddl-auto=update adds the newer school- and teacher-scoped constraints but never
 * removes the old ones, which would still reject the same class name in two
 * schools, or a subject teacher's record next to the homeroom record.
 * Runs on every profile, before demo seeding, because deploy disables seeding.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LegacyUniqueIndexMigration implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(LegacyUniqueIndexMigration.class);

    static final List<LegacyIndex> LEGACY_INDEXES = List.of(
            // (class_name, school_year) -> replaced by uk_tracking_class_year_school
            new LegacyIndex("student_tracking_class", "uk_tracking_class_year"),
            // (student_id, semester, month) -> replaced by uk_student_semester_month_teacher_subject
            new LegacyIndex("student_monthly_record", "uk_student_semester_month"));

    private final JdbcTemplate jdbcTemplate;

    public LegacyUniqueIndexMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        LEGACY_INDEXES.forEach(this::dropIfPresent);
    }

    void dropIfPresent(LegacyIndex legacyIndex) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.statistics "
                            + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                    Integer.class, legacyIndex.table(), legacyIndex.index());
            if (count != null && count > 0) {
                jdbcTemplate.execute("ALTER TABLE " + legacyIndex.table() + " DROP INDEX " + legacyIndex.index());
                logger.info("Dropped legacy unique index {} on {}.", legacyIndex.index(), legacyIndex.table());
            }
        } catch (DataAccessException ex) {
            logger.warn("Could not drop legacy unique index {} on {}: {}",
                    legacyIndex.index(), legacyIndex.table(), ex.getMessage());
        }
    }

    record LegacyIndex(String table, String index) {
    }
}
