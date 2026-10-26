package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.School;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolRepository extends JpaRepository<School, Integer> {

    @Query("SELECT s FROM School s WHERE s.schoolCode = :schoolCode AND s.deleted = false")
    Optional<School> findBySchoolCode(@Param("schoolCode") String schoolCode);

    @Query("SELECT s FROM School s WHERE s.active = true AND s.deleted = false ORDER BY s.name ASC")
    List<School> findAllByActiveTrueOrderByNameAsc();

    @Query("SELECT s FROM School s WHERE s.deleted = false ORDER BY s.name ASC")
    List<School> findAllByOrderByNameAsc();

    @Query("SELECT s FROM School s WHERE s.provinceCode = :provinceCode AND s.active = true AND s.deleted = false ORDER BY s.name ASC")
    List<School> findByProvinceCodeAndActiveTrueOrderByNameAsc(@Param("provinceCode") Integer provinceCode);

    @Query("SELECT s FROM School s WHERE s.districtCode = :districtCode AND s.active = true AND s.deleted = false ORDER BY s.name ASC")
    List<School> findByDistrictCodeAndActiveTrueOrderByNameAsc(@Param("districtCode") Integer districtCode);

    @Query("SELECT s FROM School s WHERE s.wardCode = :wardCode AND s.active = true AND s.deleted = false ORDER BY s.name ASC")
    List<School> findByWardCodeAndActiveTrueOrderByNameAsc(@Param("wardCode") Integer wardCode);

    @Query("SELECT s FROM School s WHERE s.deleted = false " +
           "AND (:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.schoolCode) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:provinceCode IS NULL OR s.provinceCode = :provinceCode)")
    Page<School> searchSchools(@Param("keyword") String keyword, @Param("provinceCode") Integer provinceCode, Pageable pageable);
}
