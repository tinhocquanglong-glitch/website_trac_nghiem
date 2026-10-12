package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistrictRepository extends JpaRepository<District, Integer> {
    List<District> findByProvinceCodeOrderByNameAsc(Integer provinceCode);
}
