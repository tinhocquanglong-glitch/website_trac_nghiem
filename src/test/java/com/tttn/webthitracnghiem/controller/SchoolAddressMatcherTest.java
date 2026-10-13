package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.School;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchoolAddressMatcherTest {

    @Test
    void matchesPartialWardDistrictAndProvinceWithoutDiacritics() {
        School school = new School();
        school.setWardName("Phường Bến Nghé");
        school.setDistrictName("Quận 1");
        school.setProvinceName("Thành phố Hồ Chí Minh");

        assertThat(SchoolAddressMatcher.matches(school, "ben nghe")).isTrue();
        assertThat(SchoolAddressMatcher.matches(school, "quan 1")).isTrue();
        assertThat(SchoolAddressMatcher.matches(school, "ho chi minh")).isTrue();
        assertThat(SchoolAddressMatcher.matches(school, "quan 3")).isFalse();

        school.setWardName("Phường Đa Kao");
        assertThat(SchoolAddressMatcher.matches(school, "da kao")).isTrue();
    }
}
