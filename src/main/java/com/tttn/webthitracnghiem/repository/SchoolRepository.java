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

    Optional<School> findBySchoolCode(String schoolCode);

    List<School> findAllByActiveTrueOrderByNameAsc();

    List<School> findAllByOrderByNameAsc();

    List<School> findByProvinceCodeAndActiveTrueOrderByNameAsc(Integer provinceCode);

    List<School> findByDistrictCodeAndActiveTrueOrderByNameAsc(Integer districtCode);

    List<School> findByWardCodeAndActiveTrueOrderByNameAsc(Integer wardCode);

    @Query("SELECT s FROM School s WHERE (:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.schoolCode) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:provinceCode IS NULL OR s.provinceCode = :provinceCode)")
    Page<School> searchSchools(@Param("keyword") String keyword, @Param("provinceCode") Integer provinceCode, Pageable pageable);
}
