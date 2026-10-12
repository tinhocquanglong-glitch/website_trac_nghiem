package com.tttn.webthitracnghiem.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(name = "wards", indexes = {
        @Index(name = "idx_wards_district_code", columnList = "district_code")
})
public class Ward {

    @Id
    private Integer code;

    @Column(nullable = false)
    private String name;

    @Column(name = "division_type")
    private String divisionType;

    private String codename;

    @Column(name = "district_code", nullable = false)
    private Integer districtCode;

    public Ward() {
    }

    public Ward(Integer code, String name, String divisionType, String codename, Integer districtCode) {
        this.code = code;
        this.name = name;
        this.divisionType = divisionType;
        this.codename = codename;
        this.districtCode = districtCode;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDivisionType() {
        return divisionType;
    }

    public void setDivisionType(String divisionType) {
        this.divisionType = divisionType;
    }

    public String getCodename() {
        return codename;
    }

    public void setCodename(String codename) {
        this.codename = codename;
    }

    public Integer getDistrictCode() {
        return districtCode;
    }

    public void setDistrictCode(Integer districtCode) {
        this.districtCode = districtCode;
    }
}
