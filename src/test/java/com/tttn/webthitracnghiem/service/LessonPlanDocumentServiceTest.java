package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.LessonPlan;
import com.tttn.webthitracnghiem.model.LessonPlanChapter;
import com.tttn.webthitracnghiem.model.LessonPlanChapterRequest;
import com.tttn.webthitracnghiem.model.LessonPlanLesson;
import com.tttn.webthitracnghiem.model.LessonPlanLessonRequest;
import com.tttn.webthitracnghiem.model.LessonPlanRequest;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LessonPlanDocumentServiceTest {
    private final LessonPlanDocumentService service = new LessonPlanDocumentService();

    @Test
    void importsWordFormattingIntoLessonHtml() throws Exception {
        ByteArrayOutputStream word = new ByteArrayOutputStream();
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun bold = paragraph.createRun();
            bold.setBold(true);
            bold.setText("Khái niệm ");
            XWPFRun italic = paragraph.createRun();
            italic.setItalic(true);
            italic.setFontSize(16);
            italic.setText("hàm số");
            document.write(word);
        }

        LessonPlanLessonRequest lesson = new LessonPlanLessonRequest();
        lesson.setName("Bài học");
        LessonPlanChapterRequest chapter = new LessonPlanChapterRequest();
        chapter.setName("Chủ đề");
        chapter.setLessons(List.of(lesson));
        LessonPlanRequest request = new LessonPlanRequest();
        request.setDocumentImportMode(LessonPlanDocumentService.LESSON_CONTENT);
        request.setDocumentFile(new MockMultipartFile(
                "documentFile", "bai-hoc.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                word.toByteArray()));
        request.setChapters(List.of(chapter));
        request.setDocumentChapterIndex(0);
        request.setDocumentLessonIndex(0);

        service.importInto(request);

        String content = request.getChapters().get(0).getLessons().get(0).getContent();
        assertThat(content)
                .contains("<strong>Khái niệm </strong>")
                .contains("<em><font size=\"16\">hàm số</font></em>");
        assertThat(LessonPlanContentSanitizer.sanitize(content))
                .contains("<font size=\"16\">");
    }

    @Test
    void exportsLessonHtmlAsFormattedWordRuns() throws Exception {
        LessonPlan plan = new LessonPlan();
        plan.setName("Giáo án Toán");
        LessonPlanChapter chapter = new LessonPlanChapter();
        chapter.setName("Hàm số");
        LessonPlanLesson lesson = new LessonPlanLesson();
        lesson.setName("Khái niệm");
        lesson.setContent("<p><strong>Định nghĩa</strong> <em><font size=\"16\">hàm số</font></em></p>");
        chapter.setLessons(List.of(lesson));
        plan.setPlanChapters(List.of(chapter));

        byte[] exported = service.export(plan);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(exported))) {
            List<XWPFRun> runs = document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .toList();
            assertThat(runs).anySatisfy(run -> {
                assertThat(run.text()).contains("Định nghĩa");
                assertThat(run.isBold()).isTrue();
            });
            assertThat(runs).anySatisfy(run -> {
                assertThat(run.text()).contains("hàm số");
                assertThat(run.isItalic()).isTrue();
                assertThat(run.getFontSize()).isEqualTo(16);
            });
        }
    }
}
