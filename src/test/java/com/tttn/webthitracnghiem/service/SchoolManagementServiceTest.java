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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchoolManagementServiceTest {
    private final SchoolRepository schoolRepository = mock(SchoolRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final StudentTrackingClassRepository classRepository = mock(StudentTrackingClassRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final SchoolManagementService service = new SchoolManagementService(
            schoolRepository, userRepository, classRepository);

    @Test
    void refusesToDeleteSchoolThatStillHasAccountsOrClasses() {
        School school = school();
        when(schoolRepository.findById(4)).thenReturn(Optional.of(school));
        when(userRepository.countBySchoolId(4)).thenReturn(3L);
        when(classRepository.countBySchoolId(4)).thenReturn(2L);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);

        assertThatThrownBy(() -> service.deleteSchool(4))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("3 tài khoản")
                .hasMessageContaining("2 lớp");
        verify(schoolRepository, never()).delete(any());
        verify(entityManager, never()).createQuery(anyString());
    }

    @Test
    void deletesEmptySchoolAfterDetachingContentAndRemovingChatRooms() {
        School school = school();
        Query query = mock(Query.class);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(schoolRepository.findById(4)).thenReturn(Optional.of(school));
        ReflectionTestUtils.setField(service, "entityManager", entityManager);

        assertThat(service.deleteSchool(4)).isSameAs(school);

        verify(entityManager).createQuery("UPDATE Exam e SET e.school = NULL WHERE e.school.id = :schoolId");
        verify(entityManager).createQuery("DELETE FROM ChatRoom r WHERE r.school.id = :schoolId");
        verify(schoolRepository).delete(school);
    }

    private static School school() {
        School school = new School();
        school.setId(4);
        school.setName("THCS A");
        return school;
    }
}
