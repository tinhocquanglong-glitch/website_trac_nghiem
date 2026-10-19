package com.tttn.webthitracnghiem.config;

import java.util.List;

/**
 * Static catalog for the demo school network seeded by the local application.
 * The catalog contains no database concerns so its shape can be tested independently.
 */
final class DemoDataPlan {
    private DemoDataPlan() {
    }

    static List<SchoolDefinition> schools() {
        return List.of(
                new SchoolDefinition("DEMO-TH-01", "Trường Tiểu học Bình Minh 01", 1, 1, 5),
                new SchoolDefinition("DEMO-TH-02", "Trường Tiểu học Bình Minh 02", 1, 1, 5),
                new SchoolDefinition("DEMO-TH-03", "Trường Tiểu học Bình Minh 03", 1, 1, 5),
                new SchoolDefinition("DEMO-TH-04", "Trường Tiểu học Bình Minh 04", 1, 1, 5),
                new SchoolDefinition("DEMO-THCS-01", "Trường THCS Bình Minh 01", 2, 6, 9),
                new SchoolDefinition("DEMO-THCS-02", "Trường THCS Bình Minh 02", 2, 6, 9),
                new SchoolDefinition("DEMO-THCS-03", "Trường THCS Bình Minh 03", 2, 6, 9),
                new SchoolDefinition("DEMO-THCS-04", "Trường THCS Bình Minh 04", 2, 6, 9),
                new SchoolDefinition("DEMO-THPT-01", "Trường THPT Bình Minh 01", 3, 10, 12),
                new SchoolDefinition("DEMO-THPT-02", "Trường THPT Bình Minh 02", 3, 10, 12),
                new SchoolDefinition("DEMO-THPT-03", "Trường THPT Bình Minh 03", 3, 10, 12),
                new SchoolDefinition("DEMO-THPT-04", "Trường THPT Bình Minh 04", 3, 10, 12)
        );
    }

    static List<SubjectDefinition> subjectsForLevel(int schoolLevel) {
        if (schoolLevel == 1) {
            return List.of(
                    new SubjectDefinition("toan", "Toán"),
                    new SubjectDefinition("tien-viet", "Tiếng Việt"),
                    new SubjectDefinition("ngoai-ngu", "Ngoại Ngữ"),
                    new SubjectDefinition("lich-su-dia-ly", "Lịch sử địa lý"),
                    new SubjectDefinition("the-duc", "Thể dục"),
                    new SubjectDefinition("my-thuat", "Mỹ thuật"),
                    new SubjectDefinition("dao-duc", "Đạo đức")
            );
        }

        return List.of(
                new SubjectDefinition("toan", "Toán"),
                new SubjectDefinition("vat-ly", "Vật lý"),
                new SubjectDefinition("hoa-hoc", "Hóa học"),
                new SubjectDefinition("sinh-hoc", "Sinh học"),
                new SubjectDefinition("lich-su", "Lịch sử"),
                new SubjectDefinition("dia-ly", "Địa lý"),
                new SubjectDefinition("ngu-van", "Ngữ văn"),
                new SubjectDefinition("tieng-anh", "Tiếng Anh"),
                new SubjectDefinition("giao-duc-cong-dan", "Giáo dục công dân"),
                new SubjectDefinition("my-thuat", "Mỹ thuật")
        );
    }

    static List<String> teacherNames() {
        return List.of(
                "Nguyen Van An", "Tran Thi Binh", "Le Minh Chau", "Pham Thu Ha",
                "Hoang Quoc Huy", "Vu Thi Lan", "Dang Minh Long", "Bui Ngoc Mai",
                "Do Thanh Nam", "Ngo Thi Oanh", "Duong Anh Phuc", "Ly Thu Quynh",
                "Phan Duc Son", "Mai Thi Thao", "Ta Minh Tuan", "Cao Ngoc Van",
                "Ho Gia Bao", "Dinh Khanh Chi", "Nguyen Hai Dang", "Tran Ngoc Giang",
                "Le Hoang Khang", "Pham Ngoc Linh", "Hoang Minh Quan", "Vu Thanh Tam"
        );
    }

    static List<String> studentNames() {
        return List.of(
                "Nguyễn Minh Anh", "Trần Gia Bảo", "Lê Khánh Chi", "Phạm Đức Duy",
                "Hoàng Ngọc Hà", "Vũ Minh Khang", "Đặng Quỳnh Lam", "Bùi Hoàng Nam",
                "Đỗ Hải Phong", "Ngô Thu Trang", "Dương Gia Uyên", "Lý Minh Vũ",
                "Phan Ngọc Yến", "Mai Anh Tú", "Tạ Bảo Châu", "Cao Đức Anh",
                "Hồ Minh Châu", "Đinh Nhật Hạ", "Nguyễn Hoài Nam", "Trần Minh Quân"
        );
    }

    record SchoolDefinition(String code, String name, int schoolLevel, int firstGrade, int lastGrade) {
    }

    record SubjectDefinition(String key, String name) {
    }
}
