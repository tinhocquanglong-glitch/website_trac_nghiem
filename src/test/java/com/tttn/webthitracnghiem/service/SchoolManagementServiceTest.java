package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SchoolManagementServiceTest {
    private final SchoolRepository schoolRepository = mock(SchoolRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final StudentTrackingClassRepository classRepository = mock(StudentTrackingClassRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final SchoolManagementService service = new SchoolManagementService(
            schoolRepository, userRepository, classRepository);

    @Test
    void softDeletesSchoolAndHidesSchoolSubjects() {
        School school = school();
        when(schoolRepository.findById(4)).thenReturn(Optional.of(school));
        when(schoolRepository.save(any(School.class))).thenAnswer(inv -> inv.getArgument(0));

        Query query = mock(Query.class);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(entityManager.createQuery(anyString())).thenReturn(query);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);

        School result = service.deleteSchool(4);

        assertThat(result.isDeleted()).isTrue();
        assertThat(result.isActive()).isFalse();
        verify(schoolRepository).save(school);
        verify(entityManager).createQuery("UPDATE Subject s SET s.status = 0 WHERE s.school.id = :schoolId");
        verify(query).setParameter("schoolId", 4);
        verify(query).executeUpdate();
    }

    @Test
    void throwsWhenSchoolNotFound() {
        when(schoolRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteSchool(99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không tìm thấy trường học.");
    }

    private static School school() {
        School school = new School();
        school.setId(4);
        school.setName("THCS A");
        school.setActive(true);
        school.setDeleted(false);
        return school;
    }
}
