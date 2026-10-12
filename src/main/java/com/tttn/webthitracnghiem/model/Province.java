package com.tttn.webthitracnghiem.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "provinces")
public class Province {

    @Id
    private Integer code;

    @Column(nullable = false)
    private String name;

    @Column(name = "division_type")
    private String divisionType;

    private String codename;

    @Column(name = "phone_code")
    private Integer phoneCode;

    public Province() {
    }

    public Province(Integer code, String name, String divisionType, String codename, Integer phoneCode) {
        this.code = code;
        this.name = name;
        this.divisionType = divisionType;
        this.codename = codename;
        this.phoneCode = phoneCode;
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

    public Integer getPhoneCode() {
        return phoneCode;
    }

    public void setPhoneCode(Integer phoneCode) {
        this.phoneCode = phoneCode;
    }
}
