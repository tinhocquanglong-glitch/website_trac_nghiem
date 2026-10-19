package com.tttn.webthitracnghiem.config;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DemoDataPlanTest {
    @Test
    void definesFourSchoolsForEachEducationLevel() {
        Map<Integer, Long> schoolsByLevel = DemoDataPlan.schools().stream()
                .collect(Collectors.groupingBy(DemoDataPlan.SchoolDefinition::schoolLevel, Collectors.counting()));

        assertThat(schoolsByLevel).containsEntry(1, 4L).containsEntry(2, 4L).containsEntry(3, 4L);
        assertThat(DemoDataPlan.schools()).hasSize(12);
    }

    @Test
    void definesTheRequestedSubjectsForEachLevel() {
        assertThat(DemoDataPlan.subjectsForLevel(1).stream()
                .map(DemoDataPlan.SubjectDefinition::name).collect(Collectors.toList()))
                .containsExactly("Toán", "Tiếng Việt", "Ngoại Ngữ", "Lịch sử địa lý", "Thể dục", "Mỹ thuật", "Đạo đức");
        assertThat(DemoDataPlan.subjectsForLevel(2).stream()
                .map(DemoDataPlan.SubjectDefinition::name).collect(Collectors.toList()))
                .containsExactly("Toán", "Vật lý", "Hóa học", "Sinh học", "Lịch sử", "Địa lý", "Ngữ văn", "Tiếng Anh", "Giáo dục công dân", "Mỹ thuật");
        assertThat(DemoDataPlan.subjectsForLevel(3)).hasSameElementsAs(DemoDataPlan.subjectsForLevel(2));
    }

    @Test
    void providesNamesForDemoRosters() {
        assertThat(DemoDataPlan.teacherNames()).isNotEmpty().doesNotHaveDuplicates();
        assertThat(DemoDataPlan.studentNames()).isNotEmpty().doesNotHaveDuplicates();
    }
}
