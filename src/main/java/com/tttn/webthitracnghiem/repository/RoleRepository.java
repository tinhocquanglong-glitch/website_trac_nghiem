package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface RoleRepository extends JpaRepository<Role,Integer> {
    Optional<Role> findByRoleName(String roleName);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO role (id, role_name) VALUES (:id, :roleName) ON DUPLICATE KEY UPDATE role_name = :roleName", nativeQuery = true)
    void insertRoleNative(@Param("id") int id, @Param("roleName") String roleName);
}

