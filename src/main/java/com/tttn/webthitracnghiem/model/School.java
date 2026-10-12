package com.tttn.webthitracnghiem.model;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

@Entity
@Table(name = "school", indexes = {
        @Index(name = "idx_school_province_code", columnList = "province_code"),
        @Index(name = "idx_school_district_code", columnList = "district_code"),
        @Index(name = "idx_school_ward_code", columnList = "ward_code")
})
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Tên trường không được để trống")
    @Column(nullable = false)
    private String name;

    @Column(name = "school_code")
    private String schoolCode;

    @Column(name = "province_code")
    private Integer provinceCode;

    @Column(name = "district_code")
    private Integer districtCode;

    @Column(name = "ward_code")
    private Integer wardCode;

    @Column(name = "province_name")
    private String provinceName;

    @Column(name = "district_name")
    private String districtName;

    @Column(name = "ward_name")
    private String wardName;

    @Column(name = "address_detail")
    private String addressDetail;

    @Column(nullable = false)
    private boolean active = true;

    public School() {
    }

    public School(String name, String schoolCode, Integer provinceCode, Integer districtCode, Integer wardCode, String addressDetail) {
        this.name = name;
        this.schoolCode = schoolCode;
        this.provinceCode = provinceCode;
        this.districtCode = districtCode;
        this.wardCode = wardCode;
        this.addressDetail = addressDetail;
        this.active = true;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchoolCode() {
        return schoolCode;
    }

    public void setSchoolCode(String schoolCode) {
        this.schoolCode = schoolCode;
    }

    public Integer getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(Integer provinceCode) {
        this.provinceCode = provinceCode;
    }

    public Integer getDistrictCode() {
        return districtCode;
    }

    public void setDistrictCode(Integer districtCode) {
        this.districtCode = districtCode;
    }

    public Integer getWardCode() {
        return wardCode;
    }

    public void setWardCode(Integer wardCode) {
        this.wardCode = wardCode;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public String getDistrictName() {
        return districtName;
    }

    public void setDistrictName(String districtName) {
        this.districtName = districtName;
    }

    public String getWardName() {
        return wardName;
    }

    public void setWardName(String wardName) {
        this.wardName = wardName;
    }

    public String getAddressDetail() {
        return addressDetail;
    }

    public void setAddressDetail(String addressDetail) {
        this.addressDetail = addressDetail;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getFullLocationString() {
        StringBuilder sb = new StringBuilder();
        if (addressDetail != null && !addressDetail.trim().isEmpty()) {
            sb.append(addressDetail).append(", ");
        }
        if (wardName != null && !wardName.trim().isEmpty()) {
            sb.append(wardName).append(", ");
        }
        if (districtName != null && !districtName.trim().isEmpty()) {
            sb.append(districtName).append(", ");
        }
        if (provinceName != null && !provinceName.trim().isEmpty()) {
            sb.append(provinceName);
        }
        String res = sb.toString().trim();
        if (res.endsWith(",")) {
            res = res.substring(0, res.length() - 1);
        }
        return res.isEmpty() ? "Toàn quốc / Chưa xác định" : res;
    }
}
