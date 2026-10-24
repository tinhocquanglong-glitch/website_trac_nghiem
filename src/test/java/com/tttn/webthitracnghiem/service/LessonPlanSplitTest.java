package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.LessonPlanChapterRequest;
import com.tttn.webthitracnghiem.model.LessonPlanLessonRequest;
import com.tttn.webthitracnghiem.model.LessonPlanRequest;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/** Splitting uploaded lesson plans into topics and lessons, and linking them to the curriculum. */
class LessonPlanSplitTest {
    private final LessonPlanDocumentService service = new LessonPlanDocumentService();

    @Test
    void splitsNumberedTopicsAndBoldLessonsButKeepsNumberedExercisesAsContent() throws Exception {
        LessonPlanRequest request = importDocx(document -> {
            line(document, "CHỦ ĐỀ 1. SỐ TỰ NHIÊN", true);
            line(document, "Bài 1: Tập hợp", true);
            line(document, "Thời gian thực hiện: 2 tiết", false);
            line(document, "I. MỤC TIÊU", true);
            line(document, "Học sinh nhận biết được tập hợp và phần tử.", false);
            line(document, "Sử dụng được kí hiệu thuộc, không thuộc.", false);
            line(document, "II. THIẾT BỊ DẠY HỌC VÀ HỌC LIỆU", true);
            line(document, "Bài 1: Viết tập hợp A các số nhỏ hơn 5.", false);
            line(document, "BÀI 2. CÁCH GHI SỐ TỰ NHIÊN", false);
            line(document, "Nội dung bài 2.", false);
            line(document, "Chủ đề 2. Tính chia hết", true);
            line(document, "Bài 8: Quan hệ chia hết", true);
            line(document, "Nội dung bài 8.", false);
        });

        List<LessonPlanChapterRequest> chapters = request.getChapters();
        assertThat(chapters).extracting(LessonPlanChapterRequest::getName)
                .containsExactly("CHỦ ĐỀ 1. SỐ TỰ NHIÊN", "Chủ đề 2. Tính chia hết");
        assertThat(chapters.get(0).getLessons()).extracting(LessonPlanLessonRequest::getName)
                .containsExactly("Bài 1: Tập hợp", "BÀI 2. CÁCH GHI SỐ TỰ NHIÊN");
        LessonPlanLessonRequest first = chapters.get(0).getLessons().get(0);
        assertThat(first.getPeriods()).isEqualTo(2);
        assertThat(first.getObjectives())
                .contains("nhận biết được tập hợp")
                .contains("kí hiệu thuộc")
                .doesNotContain("THIẾT BỊ");
        // A plain (not bold) exercise line stays inside the lesson content.
        assertThat(first.getContent()).contains("Viết tập hợp A");
        assertThat(chapters.get(1).getLessons()).extracting(LessonPlanLessonRequest::getName)
                .containsExactly("Bài 8: Quan hệ chia hết");
    }

    @Test
    void headingStylesWinOverNumberedLines() throws Exception {
        LessonPlanRequest request = importDocx(document -> {
            styled(document, "Heading1", "Số và phép tính");
            styled(document, "Heading2", "Ôn tập về số");
            line(document, "Bài 1: Đọc các số sau", true);
            styled(document, "Heading2", "Phép cộng có nhớ");
        });

        assertThat(request.getChapters()).hasSize(1);
        assertThat(request.getChapters().get(0).getLessons()).extracting(LessonPlanLessonRequest::getName)
                .containsExactly("Ôn tập về số", "Phép cộng có nhớ");
    }

    @Test
    void matcherLinksByLessonNumberThenByNameAndLeavesUnknownLessonsUnlinked() {
        LessonPlanChapterRequest chapter = new LessonPlanChapterRequest();
        chapter.setName("Chủ đề 1: Số tự nhiên");
        chapter.setLessons(List.of(lesson("Bài 2 - Cách ghi số"), lesson("Tập hợp các số tự nhiên"),
                lesson("Tiết 5: Luyện tập chung"), lesson("Ôn tập cuối năm")));
        List<CurriculumMatcher.Node> curriculum = List.of(
                new CurriculumMatcher.Node(1, "Chủ đề 1. Số tự nhiên", List.of(
                        new CurriculumMatcher.Node(11, "Bài 1. Tập hợp", List.of()),
                        new CurriculumMatcher.Node(12, "Bài 2. Cách ghi số tự nhiên", List.of()),
                        new CurriculumMatcher.Node(13, "Bài 3. Thứ tự trong tập hợp các số tự nhiên", List.of()),
                        new CurriculumMatcher.Node(15, "Bài 5. Phép cộng", List.of()))),
                new CurriculumMatcher.Node(2, "Chủ đề 2. Tính chia hết", List.of()));

        CurriculumMatcher.link(List.of(chapter), curriculum);

        assertThat(chapter.getCurriculumChapterId()).isEqualTo(1);
        assertThat(chapter.getLessons()).extracting(LessonPlanLessonRequest::getCurriculumLessonId)
                // "Tiết 5" is a period number, not lesson 5; "Ôn tập cuối năm" has no counterpart.
                .containsExactly(12, 13, null, null);
    }

    private static LessonPlanLessonRequest lesson(String name) {
        LessonPlanLessonRequest lesson = new LessonPlanLessonRequest();
        lesson.setName(name);
        return lesson;
    }

    private LessonPlanRequest importDocx(Consumer<XWPFDocument> content) throws Exception {
        ByteArrayOutputStream word = new ByteArrayOutputStream();
        try (XWPFDocument document = new XWPFDocument()) {
            content.accept(document);
            document.write(word);
        }
        LessonPlanRequest request = new LessonPlanRequest();
        request.setDocumentImportMode(LessonPlanDocumentService.FULL_STRUCTURE);
        request.setDocumentFile(new MockMultipartFile("documentFile", "giao-an.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", word.toByteArray()));
        service.importInto(request);
        return request;
    }

    private static void line(XWPFDocument document, String text, boolean bold) {
        XWPFRun run = document.createParagraph().createRun();
        run.setBold(bold);
        run.setText(text);
    }

    private static void styled(XWPFDocument document, String styleId, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setStyle(styleId);
        paragraph.createRun().setText(text);
    }
}
