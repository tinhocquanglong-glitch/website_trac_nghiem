package com.tttn.webthitracnghiem.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(name = "districts", indexes = {
        @Index(name = "idx_districts_province_code", columnList = "province_code")
})
public class District {

    @Id
    private Integer code;

    @Column(nullable = false)
    private String name;

    @Column(name = "division_type")
    private String divisionType;

    private String codename;

    @Column(name = "province_code", nullable = false)
    private Integer provinceCode;

    public District() {
    }

    public District(Integer code, String name, String divisionType, String codename, Integer provinceCode) {
        this.code = code;
        this.name = name;
        this.divisionType = divisionType;
        this.codename = codename;
        this.provinceCode = provinceCode;
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

    public Integer getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(Integer provinceCode) {
        this.provinceCode = provinceCode;
    }
}
