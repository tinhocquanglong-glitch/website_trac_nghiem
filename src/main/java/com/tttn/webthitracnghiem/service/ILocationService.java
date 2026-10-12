package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.District;
import com.tttn.webthitracnghiem.model.Province;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.Ward;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ILocationService {
    List<Province> getAllProvinces();
    List<District> getDistrictsByProvince(Integer provinceCode);
    List<Ward> getWardsByDistrict(Integer districtCode);
    List<School> getSchools(Integer provinceCode, Integer districtCode, Integer wardCode);
    School findSchoolById(Integer id);
    School saveSchool(School school);
    Page<School> searchSchools(String keyword, Integer provinceCode, Pageable pageable);
}
