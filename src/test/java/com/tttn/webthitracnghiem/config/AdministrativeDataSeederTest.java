package com.tttn.webthitracnghiem.config;

import com.tttn.webthitracnghiem.model.Role;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.TrackedStudent;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.RoleRepository;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.repository.TrackedStudentRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.Statement;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdministrativeDataSeederTest {

    @Test
    void resetIsDisabledByDefault() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AdministrativeDataSeeder seeder = new AdministrativeDataSeeder();
        setField(seeder, "jdbcTemplate", jdbcTemplate);
        setField(seeder, "demoResetEnabled", false);

        assertThat(seeder.resetDatabaseIfRequired()).isFalse();
        verifyNoJdbcCalls(jdbcTemplate);
    }

    @Test
    void resetRequiresExplicitConfirmation() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AdministrativeDataSeeder seeder = new AdministrativeDataSeeder();
        setField(seeder, "jdbcTemplate", jdbcTemplate);
        setField(seeder, "demoResetEnabled", true);
        setField(seeder, "demoResetVersion", "test-v1");
        setField(seeder, "demoResetConfirmation", "wrong-value");

        assertThatThrownBy(seeder::resetDatabaseIfRequired)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WIPE_DATABASE");
        verifyNoJdbcCalls(jdbcTemplate);
    }

    @Test
    void resetTruncatesBusinessTablesButKeepsSequenceAndMarker() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        doAnswer(invocation -> {
            ConnectionCallback<?> callback = invocation.getArgument(0);
            callback.doInConnection(connection);
            return null;
        }).when(jdbcTemplate).execute(any(ConnectionCallback.class));
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(List.of(), List.of("user", "hibernate_sequence", "deployment_seed_state", "role"));

        AdministrativeDataSeeder seeder = new AdministrativeDataSeeder();
        setField(seeder, "jdbcTemplate", jdbcTemplate);
        setField(seeder, "demoResetEnabled", true);
        setField(seeder, "demoResetVersion", "test-v1");
        setField(seeder, "demoResetConfirmation", "WIPE_DATABASE");

        assertThat(seeder.resetDatabaseIfRequired()).isTrue();
        verify(statement).execute("TRUNCATE TABLE `user`");
        verify(statement).execute("TRUNCATE TABLE `role`");
        verify(statement, never()).execute("TRUNCATE TABLE `hibernate_sequence`");
        verify(statement, never()).execute("TRUNCATE TABLE `deployment_seed_state`");
    }

    @Test
    void resetDoesNotRunAgainForAppliedVersion() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(List.of("test-v1"));

        AdministrativeDataSeeder seeder = new AdministrativeDataSeeder();
        setField(seeder, "jdbcTemplate", jdbcTemplate);
        setField(seeder, "demoResetEnabled", true);
        setField(seeder, "demoResetVersion", "test-v1");
        setField(seeder, "demoResetConfirmation", "WIPE_DATABASE");

        assertThat(seeder.resetDatabaseIfRequired()).isFalse();
        verify(jdbcTemplate, times(1)).queryForList(anyString(), eq(String.class));
        verify(jdbcTemplate, never()).execute(any(ConnectionCallback.class));
    }

    @Test
    void seedsGlobalAndSchoolAdministrators() throws Exception {
        RoleRepository roleRepository = mock(RoleRepository.class);
        SchoolRepository schoolRepository = mock(SchoolRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        Role adminRole = new Role("ROLE_ADMIN");
        Role schoolAdminRole = new Role("ROLE_SCHOOL_ADMIN");
        School school = new School();
        school.setId(1);
        school.setName("Demo school");
        school.setSchoolCode("DEMO");

        when(roleRepository.findByRoleName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByRoleName("ROLE_SCHOOL_ADMIN")).thenReturn(Optional.of(schoolAdminRole));
        when(schoolRepository.findBySchoolCode(anyString())).thenReturn(Optional.of(school));
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdministrativeDataSeeder seeder = new AdministrativeDataSeeder();
        setField(seeder, "roleRepository", roleRepository);
        setField(seeder, "schoolRepository", schoolRepository);
        setField(seeder, "userRepository", userRepository);
        setField(seeder, "demoSeedPassword", "Demo@123456");
        setField(seeder, "demoAdminUsername", "admin");

        seeder.seedDemoAdminAccounts();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(13)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getId()).isEqualTo("admin");
        assertThat(captor.getAllValues().get(0).getRoles()).containsExactly(adminRole);
        assertThat(captor.getAllValues().subList(1, 13))
                .allMatch(user -> user.getId().startsWith("demo_hieu_truong_"))
                .allMatch(user -> user.getRoles().contains(schoolAdminRole));
    }

    @Test
    void gradeCatalogMigrationAddsOnlyMissingGrades() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyInt()))
                .thenAnswer(invocation -> ((Integer) invocation.getArgument(2)) == 6 ? 1 : 0);

        AdministrativeDataSeeder seeder = new AdministrativeDataSeeder();
        Field field = AdministrativeDataSeeder.class.getDeclaredField("jdbcTemplate");
        field.setAccessible(true);
        field.set(seeder, jdbcTemplate);

        seeder.seedGradeCatalog();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(jdbcTemplate, org.mockito.Mockito.times(11))
                .update(eq("INSERT INTO classes (name_class, grade) VALUES (?, ?)"), captor.capture(), anyInt());
        assertThat(captor.getAllValues())
                .containsExactlyInAnyOrder(
                        "Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5",
                        "Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12");
    }

    @Test
    void lessonPlanMigrationConvertsExistingTextColumnsToUtf8mb4() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), anyString()))
                .thenReturn(1);

        AdministrativeDataSeeder seeder = new AdministrativeDataSeeder();
        setField(seeder, "jdbcTemplate", jdbcTemplate);

        invokePrivate(seeder, "migrateLessonPlanSchema");

        verify(jdbcTemplate).execute(
                "ALTER TABLE lesson_plan_custom_lesson CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        verify(jdbcTemplate).execute(
                "ALTER TABLE lesson_plan_custom_lesson MODIFY content TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL");
    }

    @Test
    void lessonPlanEncodingMigrationStillRunsWhenAnOlderSchemaChangeFails() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString(), anyString()))
                .thenReturn(1);
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            if (sql.startsWith("ALTER TABLE lesson_plan MODIFY")) {
                throw new RuntimeException("legacy column is missing");
            }
            return null;
        }).when(jdbcTemplate).execute(anyString());

        AdministrativeDataSeeder seeder = new AdministrativeDataSeeder();
        setField(seeder, "jdbcTemplate", jdbcTemplate);

        invokePrivate(seeder, "migrateLessonPlanSchema");

        verify(jdbcTemplate).execute(
                "ALTER TABLE lesson_plan_custom_lesson CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    }

    @Test
    void demoStudentRosterCreatesStudentAccountLinkedToClass() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        TrackedStudentRepository trackedStudentRepository = mock(TrackedStudentRepository.class);
        when(userRepository.findById("demo_hs_7_1")).thenReturn(Optional.empty());

        School school = new School();
        school.setId(12);
        StudentTrackingClass trackingClass = new StudentTrackingClass("6A1", "2026-2027");
        trackingClass.setId(7);
        trackingClass.setGrade(6);
        trackingClass.setSchool(school);

        TrackedStudent trackedStudent = new TrackedStudent();
        trackedStudent.setId(31);
        trackedStudent.setFullName("Nguyễn Minh Anh");
        trackedStudent.setDisplayOrder(1);
        trackedStudent.setTrackingClass(trackingClass);
        when(trackedStudentRepository.findByTrackingClassIdOrderByDisplayOrderAscIdAsc(7))
                .thenReturn(List.of(trackedStudent));

        AdministrativeDataSeeder seeder = new AdministrativeDataSeeder();
        setField(seeder, "userRepository", userRepository);
        setField(seeder, "trackedStudentRepository", trackedStudentRepository);
        setField(seeder, "demoSeedPassword", "Demo@123456");

        seeder.seedDemoStudentAccounts(trackingClass, new Role("ROLE_USER"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User created = captor.getValue();
        assertThat(created.getId()).isEqualTo("demo_hs_7_1");
        assertThat(created.getFullName()).isEqualTo("Nguyễn Minh Anh");
        assertThat(created.getSchool()).isSameAs(school);
        assertThat(created.getTrackingClass()).isSameAs(trackingClass);
        assertThat(created.getSchoolLevel()).isEqualTo(2);
        assertThat(created.getRoles()).extracting(Role::getRoleName).containsExactly("ROLE_USER");
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void invokePrivate(Object target, String name) throws Exception {
        java.lang.reflect.Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(target);
    }

    private void verifyNoJdbcCalls(JdbcTemplate jdbcTemplate) {
        verify(jdbcTemplate, never()).execute(anyString());
        verify(jdbcTemplate, never()).queryForList(anyString(), eq(String.class));
    }
}
