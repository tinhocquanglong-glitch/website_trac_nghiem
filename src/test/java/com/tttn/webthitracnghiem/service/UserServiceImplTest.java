package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.model.Role;
import com.tttn.webthitracnghiem.repository.LessonPlanRepository;
import com.tttn.webthitracnghiem.repository.RoleRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import com.tttn.webthitracnghiem.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LessonPlanRepository lessonPlanRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void softDeletesUserInsteadOfHardDeleting() {
        User user = new User();
        user.setId("teacher-1");
        user.setStatus(1);

        userService.delete(user);

        assertEquals(Integer.valueOf(0), user.getStatus());
        verify(userRepository).clearHomeroomTeacher("teacher-1");
        verify(userRepository).clearTrackingClass("teacher-1");
        verify(userRepository).deleteTeacherAssignments("teacher-1");
        verify(userRepository).save(user);
        verify(userRepository, never()).delete(user);
    }

    @Test
    void givesAdminRoleToFirstRegisteredUserWhenNoAdminExists() {
        Role adminRole = new Role("ROLE_ADMIN");
        Role userRole = new Role("ROLE_USER");
        User user = new User();

        when(roleRepository.findByRoleNameForUpdate("ROLE_ADMIN")).thenReturn(java.util.Optional.of(adminRole));
        when(roleRepository.findByRoleName("ROLE_USER")).thenReturn(java.util.Optional.of(userRole));
        when(userRepository.countUsersByRoleName("ROLE_ADMIN")).thenReturn(0);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.register(user);

        assertEquals(1, user.getRoles().size());
        assertSame(adminRole, user.getRoles().iterator().next());
    }

    @Test
    void givesAdminRoleToNextRegisteredUserWhenUsersExistButNoAdminExists() {
        Role adminRole = new Role("ROLE_ADMIN");
        Role userRole = new Role("ROLE_USER");
        User user = new User();

        when(roleRepository.findByRoleNameForUpdate("ROLE_ADMIN")).thenReturn(java.util.Optional.of(adminRole));
        when(roleRepository.findByRoleName("ROLE_USER")).thenReturn(java.util.Optional.of(userRole));
        when(userRepository.countUsersByRoleName("ROLE_ADMIN")).thenReturn(0);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.register(user);

        assertSame(adminRole, user.getRoles().iterator().next());
    }

    @Test
    void keepsDefaultRoleWhenAnAdminAlreadyExists() {
        Role submittedAdminRole = new Role("ROLE_ADMIN");
        Role userRole = new Role("ROLE_USER");
        User user = new User();
        user.setRoles(java.util.Set.of(submittedAdminRole));

        when(roleRepository.findByRoleNameForUpdate("ROLE_ADMIN")).thenReturn(java.util.Optional.of(submittedAdminRole));
        when(roleRepository.findByRoleName("ROLE_USER")).thenReturn(java.util.Optional.of(userRole));
        when(userRepository.countUsersByRoleName("ROLE_ADMIN")).thenReturn(1);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.register(user);

        assertEquals(1, user.getRoles().size());
        assertSame(userRole, user.getRoles().iterator().next());
    }
}
