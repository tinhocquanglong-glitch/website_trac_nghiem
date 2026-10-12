package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.District;
import com.tttn.webthitracnghiem.model.Province;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.Ward;
import com.tttn.webthitracnghiem.service.ILocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location")
public class LocationApiController {

    @Autowired
    private ILocationService locationService;

    @GetMapping("/provinces")
    public ResponseEntity<List<Province>> getProvinces() {
        return ResponseEntity.ok(locationService.getAllProvinces());
    }

    @GetMapping("/districts")
    public ResponseEntity<List<District>> getDistricts(@RequestParam("provinceCode") Integer provinceCode) {
        return ResponseEntity.ok(locationService.getDistrictsByProvince(provinceCode));
    }

    @GetMapping("/wards")
    public ResponseEntity<List<Ward>> getWards(@RequestParam("districtCode") Integer districtCode) {
        return ResponseEntity.ok(locationService.getWardsByDistrict(districtCode));
    }

    @GetMapping("/schools")
    public ResponseEntity<List<School>> getSchools(
            @RequestParam(value = "provinceCode", required = false) Integer provinceCode,
            @RequestParam(value = "districtCode", required = false) Integer districtCode,
            @RequestParam(value = "wardCode", required = false) Integer wardCode) {
        return ResponseEntity.ok(locationService.getSchools(provinceCode, districtCode, wardCode));
    }

    @PostMapping("/schools")
    public ResponseEntity<?> createSchool(@RequestBody School school) {
        if (school.getName() == null || school.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Tên trường không được để trống");
        }
        School saved = locationService.saveSchool(school);
        return ResponseEntity.ok(saved);
    }
}
