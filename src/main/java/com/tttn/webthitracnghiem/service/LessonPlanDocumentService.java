package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.LessonPlan;
import com.tttn.webthitracnghiem.model.LessonPlanChapter;
import com.tttn.webthitracnghiem.model.LessonPlanChapterRequest;
import com.tttn.webthitracnghiem.model.LessonPlanLesson;
import com.tttn.webthitracnghiem.model.LessonPlanLessonRequest;
import com.tttn.webthitracnghiem.model.LessonPlanRequest;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class LessonPlanDocumentService {
    public static final String FULL_STRUCTURE = "FULL_STRUCTURE";
    public static final String LESSON_CONTENT = "LESSON_CONTENT";
    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;

    public void importInto(LessonPlanRequest request) {
        MultipartFile file = request.getDocumentFile();
        if (file == null || file.isEmpty()) return;
        validate(file);
        try {
            List<DocumentBlock> blocks = read(file);
            if (FULL_STRUCTURE.equals(request.getDocumentImportMode())) {
                request.setChapters(toStructure(blocks));
            } else {
                writeLessonContent(request, blocks);
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Khong the doc noi dung file Word");
        }
    }

    public byte[] export(LessonPlan plan) {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph title = document.createParagraph();
            XWPFRun titleRun = title.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setText(plan.getName() == null ? "Giao an" : plan.getName());

            XWPFParagraph scope = document.createParagraph();
            scope.createRun().setText(scopeText(plan));
            for (LessonPlanChapter chapter : plan.getPlanChapters()) {
                XWPFParagraph chapterParagraph = document.createParagraph();
                XWPFRun chapterRun = chapterParagraph.createRun();
                chapterRun.setBold(true);
                chapterRun.setFontSize(14);
                chapterRun.setText(chapter.getName());
                for (LessonPlanLesson lesson : chapter.getLessons()) {
                    XWPFParagraph lessonParagraph = document.createParagraph();
                    XWPFRun lessonRun = lessonParagraph.createRun();
                    lessonRun.setBold(true);
                    lessonRun.setFontSize(12);
                    lessonRun.setText(lesson.getName());
                    writeHtml(document, lesson.getContent());
                }
            }
            document.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Khong the tao file Word");
        }
    }

    private List<DocumentBlock> read(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (filename.endsWith(".docx")) return readDocx(file.getInputStream());
        return readDoc(file.getInputStream());
    }

    private List<DocumentBlock> readDocx(InputStream input) throws IOException {
        List<DocumentBlock> blocks = new ArrayList<>();
        try (XWPFDocument document = new XWPFDocument(input)) {
            for (var bodyElement : document.getBodyElements()) {
                if (bodyElement instanceof XWPFParagraph paragraph) {
                    addParagraphBlock(blocks, paragraph);
                } else if (bodyElement instanceof XWPFTable table) {
                    table.getRows().forEach(row -> row.getTableCells().forEach(cell ->
                            cell.getParagraphs().forEach(paragraph -> addParagraphBlock(blocks, paragraph))));
                }
            }
        }
        return blocks;
    }

    private void addParagraphBlock(List<DocumentBlock> blocks, XWPFParagraph paragraph) {
        String text = paragraph.getText() == null ? "" : paragraph.getText().trim();
        if (!text.isEmpty()) {
            blocks.add(new DocumentBlock(text, renderRuns(paragraph.getRuns()),
                    maxFontSize(paragraph.getRuns())));
        }
    }

    private List<DocumentBlock> readDoc(InputStream input) throws IOException {
        List<DocumentBlock> blocks = new ArrayList<>();
        try (HWPFDocument document = new HWPFDocument(input)) {
            Range range = document.getRange();
            for (int i = 0; i < range.numParagraphs(); i++) {
                Paragraph paragraph = range.getParagraph(i);
                String text = paragraph.text() == null ? "" : paragraph.text().trim();
                if (!text.isEmpty()) {
                    blocks.add(new DocumentBlock(text, renderRuns(paragraph, range),
                            maxFontSize(paragraph, range)));
                }
            }
        }
        return blocks;
    }

    private String renderRuns(List<XWPFRun> runs) {
        StringBuilder html = new StringBuilder("<p>");
        for (XWPFRun run : runs) {
            String text = run.text();
            if (text == null || text.isEmpty()) continue;
            html.append(formatText(text, run.isBold(), run.isItalic(),
                    run.getUnderline() != UnderlinePatterns.NONE, fontSize(run)));
        }
        return html.append("</p>").toString();
    }

    private String renderRuns(Paragraph paragraph, Range range) {
        StringBuilder html = new StringBuilder("<p>");
        for (int i = 0; i < paragraph.numCharacterRuns(); i++) {
            CharacterRun run = paragraph.getCharacterRun(i);
            String text = run.text();
            if (text == null || text.isEmpty()) continue;
            html.append(formatText(text, run.isBold(), run.isItalic(),
                    run.getUnderlineCode() != 0, run.getFontSize() / 2));
        }
        return html.append("</p>").toString();
    }

    private String formatText(String text, boolean bold, boolean italic, boolean underline, int fontSize) {
        String value = escapeHtml(text).replace("\r", "").replace("\n", "<br>");
        if (fontSize > 0) value = "<font size=\"" + fontSize + "\">" + value + "</font>";
        if (underline) value = "<u>" + value + "</u>";
        if (italic) value = "<em>" + value + "</em>";
        if (bold) value = "<strong>" + value + "</strong>";
        return value;
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private int maxFontSize(List<XWPFRun> runs) {
        return runs.stream().mapToInt(this::fontSize).filter(size -> size > 0).max().orElse(0);
    }

    private int fontSize(XWPFRun run) {
        if (run.getCTR().isSetRPr() && run.getCTR().getRPr().sizeOfSzArray() > 0
                && run.getCTR().getRPr().getSzArray(0).getVal() != null) {
            Object rawSize = run.getCTR().getRPr().getSzArray(0).getVal();
            return Math.max(0, Integer.parseInt(rawSize.toString()) / 2);
        }
        return 0;
    }

    private int maxFontSize(Paragraph paragraph, Range range) {
        int max = 0;
        for (int i = 0; i < paragraph.numCharacterRuns(); i++) {
            CharacterRun run = paragraph.getCharacterRun(i);
            max = Math.max(max, run.getFontSize() / 2);
        }
        return max;
    }

    private List<LessonPlanChapterRequest> toStructure(List<DocumentBlock> blocks) {
        List<LessonPlanChapterRequest> result = new ArrayList<>();
        if (blocks.isEmpty()) return result;
        List<Integer> sizes = blocks.stream().map(DocumentBlock::fontSize).filter(size -> size > 0)
                .distinct().sorted(Comparator.reverseOrder()).toList();
        if (sizes.size() < 2) {
            LessonPlanChapterRequest chapter = new LessonPlanChapterRequest();
            chapter.setName(blocks.get(0).text());
            LessonPlanLessonRequest lesson = new LessonPlanLessonRequest();
            lesson.setName(blocks.size() > 1 ? blocks.get(1).text() : "Bai hoc");
            lesson.setContent(joinHtml(blocks, blocks.size() > 1 ? 2 : 1));
            chapter.setLessons(List.of(lesson));
            result.add(chapter);
            return result;
        }

        int topicSize = sizes.get(0);
        int lessonSize = sizes.get(1);
        LessonPlanChapterRequest currentChapter = null;
        LessonPlanLessonRequest currentLesson = null;
        StringBuilder content = new StringBuilder();
        for (DocumentBlock block : blocks) {
            if (block.fontSize() == topicSize) {
                flushContent(currentLesson, content);
                currentChapter = new LessonPlanChapterRequest();
                currentChapter.setName(block.text());
                currentChapter.setLessons(new ArrayList<>());
                result.add(currentChapter);
                currentLesson = null;
            } else if (block.fontSize() == lessonSize && currentChapter != null) {
                flushContent(currentLesson, content);
                currentLesson = new LessonPlanLessonRequest();
                currentLesson.setName(block.text());
                currentChapter.getLessons().add(currentLesson);
            } else {
                if (currentChapter == null) {
                    currentChapter = new LessonPlanChapterRequest();
                    currentChapter.setName("Noi dung tu file Word");
                    currentChapter.setLessons(new ArrayList<>());
                    result.add(currentChapter);
                }
                if (currentLesson == null) {
                    currentLesson = new LessonPlanLessonRequest();
                    currentLesson.setName("Bai hoc tu file Word");
                    currentChapter.getLessons().add(currentLesson);
                }
                content.append(block.html());
            }
        }
        flushContent(currentLesson, content);
        return result;
    }

    private void writeLessonContent(LessonPlanRequest request, List<DocumentBlock> blocks) {
        if (blocks.isEmpty()) throw new IllegalArgumentException("File Word khong co noi dung");
        List<LessonPlanChapterRequest> chapters = request.getChapters();
        if (chapters == null) chapters = new ArrayList<>();
        int chapterIndex = request.getDocumentChapterIndex() == null ? 0 : request.getDocumentChapterIndex();
        int lessonIndex = request.getDocumentLessonIndex() == null ? 0 : request.getDocumentLessonIndex();
        while (chapters.size() <= chapterIndex) {
            LessonPlanChapterRequest chapter = new LessonPlanChapterRequest();
            chapter.setName("Noi dung tu file Word");
            chapters.add(chapter);
        }
        LessonPlanChapterRequest chapter = chapters.get(chapterIndex);
        if (chapter.getLessons() == null) chapter.setLessons(new ArrayList<>());
        while (chapter.getLessons().size() <= lessonIndex) {
            LessonPlanLessonRequest lesson = new LessonPlanLessonRequest();
            lesson.setName("Bai hoc tu file Word");
            chapter.getLessons().add(lesson);
        }
        chapter.getLessons().get(lessonIndex).setContent(joinHtml(blocks, 0));
        request.setChapters(chapters);
    }

    private String joinHtml(List<DocumentBlock> blocks, int start) {
        StringBuilder result = new StringBuilder();
        for (int i = start; i < blocks.size(); i++) result.append(blocks.get(i).html());
        return result.toString();
    }

    private void flushContent(LessonPlanLessonRequest lesson, StringBuilder content) {
        if (lesson != null && content.length() > 0) lesson.setContent(content.toString());
        content.setLength(0);
    }

    private void writeHtml(XWPFDocument document, String html) throws IOException {
        if (html == null || html.trim().isEmpty()) {
            document.createParagraph();
            return;
        }
        new ParserDelegator().parse(new StringReader(html), new WordHtmlCallback(document), true);
    }

    private String scopeText(LessonPlan plan) {
        String subject = plan.getSubject() == null ? "" : plan.getSubject().getName();
        String grade = plan.getGrade() == null ? "" : plan.getGrade().getDisplayName();
        return (subject + " - " + grade).trim();
    }

    private void validate(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!(filename.endsWith(".doc") || filename.endsWith(".docx"))) {
            throw new IllegalArgumentException("Chi chap nhan file Word .doc hoac .docx");
        }
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("File Word khong duoc lon hon 5 MB");
    }

    private record DocumentBlock(String text, String html, int fontSize) {
    }

    private static final class WordHtmlCallback extends HTMLEditorKit.ParserCallback {
        private final XWPFDocument document;
        private XWPFParagraph paragraph;
        private boolean bold;
        private boolean italic;
        private boolean underline;
        private int fontSize;
        private int listDepth;
        private boolean orderedList;
        private int listItemNumber;

        private WordHtmlCallback(XWPFDocument document) {
            this.document = document;
        }

        @Override
        public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
            if (tag == HTML.Tag.P || tag == HTML.Tag.DIV) finishParagraph();
            if (tag == HTML.Tag.P || tag == HTML.Tag.DIV || tag == HTML.Tag.LI) ensureParagraph();
            if (tag == HTML.Tag.LI) {
                addPlainText(orderedList ? (listItemNumber++) + ". " : "- ");
            } else if (tag == HTML.Tag.UL || tag == HTML.Tag.OL) {
                listDepth++;
                orderedList = tag == HTML.Tag.OL;
                listItemNumber = 1;
            } else if (tag == HTML.Tag.STRONG || tag == HTML.Tag.B) {
                bold = true;
            } else if (tag == HTML.Tag.EM || tag == HTML.Tag.I) {
                italic = true;
            } else if (tag == HTML.Tag.U) {
                underline = true;
            } else if (tag == HTML.Tag.FONT) {
                fontSize = parseSize(attributes.getAttribute(HTML.Attribute.SIZE));
            }
        }

        @Override
        public void handleEndTag(HTML.Tag tag, int position) {
            if (tag == HTML.Tag.STRONG || tag == HTML.Tag.B) bold = false;
            if (tag == HTML.Tag.EM || tag == HTML.Tag.I) italic = false;
            if (tag == HTML.Tag.U) underline = false;
            if (tag == HTML.Tag.FONT) fontSize = 0;
            if (tag == HTML.Tag.P || tag == HTML.Tag.DIV || tag == HTML.Tag.LI) finishParagraph();
            if (tag == HTML.Tag.UL || tag == HTML.Tag.OL) listDepth = Math.max(0, listDepth - 1);
        }

        @Override
        public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
            if (tag == HTML.Tag.BR) ensureParagraph().createRun().addBreak();
        }

        @Override
        public void handleText(char[] data, int position) {
            if (data.length == 0) return;
            ensureParagraph();
            XWPFRun run = paragraph.createRun();
            run.setBold(bold);
            run.setItalic(italic);
            if (underline) run.setUnderline(UnderlinePatterns.SINGLE);
            if (fontSize > 0) run.setFontSize(fontSize);
            run.setText(new String(data));
        }

        private XWPFParagraph ensureParagraph() {
            if (paragraph == null) paragraph = document.createParagraph();
            return paragraph;
        }

        private void finishParagraph() {
            paragraph = null;
        }

        private void addPlainText(String text) {
            ensureParagraph().createRun().setText(text);
        }

        private int parseSize(Object value) {
            if (value == null) return 0;
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
    }
}
