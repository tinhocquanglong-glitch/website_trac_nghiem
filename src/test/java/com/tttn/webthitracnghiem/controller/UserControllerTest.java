package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import com.tttn.webthitracnghiem.service.IUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserControllerTest {

    @AfterEach
    void clearSecurityContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void filtersSchoolsByPartialAdministrativeAddressWithoutShowingAllByDefault() {
        School firstSchool = school(1, "School A", "Phuong Ben Nghe", "Quan 1", "Thanh pho Ho Chi Minh");
        School secondSchool = school(2, "School B", "Phuong Ben Nghe", "Quan 1", "Thanh pho Ho Chi Minh");
        School otherSchool = school(3, "School C", "Phuong Vo Thi Sau", "Quan 3", "Thanh pho Ho Chi Minh");

        SchoolRepository schoolRepository = mock(SchoolRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        IUserService userService = mock(IUserService.class);
        when(schoolRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(firstSchool, secondSchool, otherSchool));
        when(userRepository.findAllSchoolAdmins()).thenReturn(List.of());
        when(userRepository.findTeachersBySchool(anyInt())).thenReturn(List.of());
        when(userRepository.findStudentsBySchool(anyInt())).thenReturn(List.of());
        when(userService.findAll(any())).thenReturn(new PageImpl<>(List.of()));

        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "schoolRepository", schoolRepository);
        ReflectionTestUtils.setField(controller, "userRepository", userRepository);
        ReflectionTestUtils.setField(controller, "userService", userService);

        ExtendedModelMap emptyModel = new ExtendedModelMap();
        controller.showList(Optional.empty(), "schools", null, emptyModel, PageRequest.of(0, 10));
        assertThat(emptyModel.get("schools")).isEqualTo(List.of());
        assertThat(emptyModel.get("hasSchoolSearch")).isEqualTo(false);

        ExtendedModelMap filteredModel = new ExtendedModelMap();
        controller.showList(Optional.empty(), "schools", "quan 1", filteredModel, PageRequest.of(0, 10));

        assertThat(filteredModel.get("addressKeyword")).isEqualTo("quan 1");
        assertThat(filteredModel.get("hasSchoolSearch")).isEqualTo(true);
        assertThat(filteredModel.get("schools")).isEqualTo(List.of(firstSchool, secondSchool));
    }

    private School school(Integer id, String name, String ward, String district, String province) {
        School school = new School();
        school.setId(id);
        school.setName(name);
        school.setWardName(ward);
        school.setDistrictName(district);
        school.setProvinceName(province);
        return school;
    }
}
