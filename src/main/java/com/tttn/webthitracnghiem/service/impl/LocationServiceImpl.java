package com.tttn.webthitracnghiem.service.impl;

import com.tttn.webthitracnghiem.model.District;
import com.tttn.webthitracnghiem.model.Province;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.Ward;
import com.tttn.webthitracnghiem.repository.DistrictRepository;
import com.tttn.webthitracnghiem.repository.ProvinceRepository;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.repository.WardRepository;
import com.tttn.webthitracnghiem.service.ILocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationServiceImpl implements ILocationService {

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Override
    public List<Province> getAllProvinces() {
        return provinceRepository.findAllByOrderByNameAsc();
    }

    @Override
    public List<District> getDistrictsByProvince(Integer provinceCode) {
        if (provinceCode == null) {
            return List.of();
        }
        return districtRepository.findByProvinceCodeOrderByNameAsc(provinceCode);
    }

    @Override
    public List<Ward> getWardsByDistrict(Integer districtCode) {
        if (districtCode == null) {
            return List.of();
        }
        return wardRepository.findByDistrictCodeOrderByNameAsc(districtCode);
    }

    @Override
    public List<School> getSchools(Integer provinceCode, Integer districtCode, Integer wardCode) {
        if (wardCode != null) {
            return schoolRepository.findByWardCodeAndActiveTrueOrderByNameAsc(wardCode);
        }
        if (districtCode != null) {
            return schoolRepository.findByDistrictCodeAndActiveTrueOrderByNameAsc(districtCode);
        }
        if (provinceCode != null) {
            return schoolRepository.findByProvinceCodeAndActiveTrueOrderByNameAsc(provinceCode);
        }
        return schoolRepository.findAllByActiveTrueOrderByNameAsc();
    }

    @Override
    public School findSchoolById(Integer id) {
        if (id == null) {
            return null;
        }
        return schoolRepository.findById(id).orElse(null);
    }

    @Override
    public School saveSchool(School school) {
        if (school.getProvinceCode() != null && (school.getProvinceName() == null || school.getProvinceName().isEmpty())) {
            provinceRepository.findById(school.getProvinceCode()).ifPresent(p -> school.setProvinceName(p.getName()));
        }
        if (school.getDistrictCode() != null && (school.getDistrictName() == null || school.getDistrictName().isEmpty())) {
            districtRepository.findById(school.getDistrictCode()).ifPresent(d -> school.setDistrictName(d.getName()));
        }
        if (school.getWardCode() != null && (school.getWardName() == null || school.getWardName().isEmpty())) {
            wardRepository.findById(school.getWardCode()).ifPresent(w -> school.setWardName(w.getName()));
        }
        return schoolRepository.save(school);
    }

    @Override
    public Page<School> searchSchools(String keyword, Integer provinceCode, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return schoolRepository.searchSchools(cleanKeyword, provinceCode, pageable);
    }
}
