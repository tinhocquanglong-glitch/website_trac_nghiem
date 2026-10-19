package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.LessonPlanChapter;
import com.tttn.webthitracnghiem.model.LessonPlanLesson;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.service.IExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
public class PracticeExamController {
    @Autowired
    private IExamService examService;

    @GetMapping("/practice")
    public String practice(@RequestParam(required = false) Integer grade,
                           @RequestParam(required = false) Integer subjectId,
                           @RequestParam(required = false) Integer chapterId,
                           @RequestParam(required = false) String scoreType,
                           Model model) {
        List<Exam> allPracticeExams = examService.getAll().stream()
                .filter(Objects::nonNull)
                .filter(Exam::isPractice)
                .filter(exam -> Boolean.TRUE.equals(exam.getIsPublic()))
                .collect(Collectors.toList());

        List<Exam> selectorExams = allPracticeExams.stream()
                .filter(exam -> grade == null || grade.equals(gradeOf(exam)))
                .collect(Collectors.toList());

        List<Exam> practiceExams = selectorExams.stream()
                 .filter(exam -> subjectId == null || subjectOf(exam) != null
                         && subjectId.equals(subjectOf(exam).getId()))
                 .filter(exam -> chapterId == null || chapterOf(exam) != null
                         && chapterId.equals(chapterOf(exam).getId()))
                .filter(exam -> scoreType == null || scoreType.trim().isEmpty()
                        || scoreType.equalsIgnoreCase(exam.getScoreType()))
                .sorted(Comparator.comparing(Exam::getCreateDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        Map<Integer, Subject> subjectMap = new LinkedHashMap<>();
        Map<Integer, Chapter> chapterMap = new LinkedHashMap<>();
        selectorExams.forEach(exam -> {
                     Subject subject = subjectOf(exam);
                     if (subject != null) {
                         subjectMap.put(subject.getId(), subject);
                     }
                     Chapter chapter = chapterOf(exam);
                     if (chapter != null && (subjectId == null
                             || (subjectOf(exam) != null && subjectId.equals(subjectOf(exam).getId())))) {
                         chapterMap.put(chapter.getId(), chapter);
                     }
                });

        model.addAttribute("practiceExams", practiceExams);
        model.addAttribute("subjects", new ArrayList<>(subjectMap.values()));
        model.addAttribute("chapters", new ArrayList<>(chapterMap.values()));
        model.addAttribute("grades", allPracticeExams.stream()
                .map(this::gradeOf)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList()));
        model.addAttribute("grade", grade);
        model.addAttribute("subjectId", subjectId);
        model.addAttribute("chapterId", chapterId);
        model.addAttribute("scoreType", scoreType);
        return "practice/list";
    }

    private Subject subjectOf(Exam exam) {
        if (exam == null) {
            return null;
        }
        if (exam.getLessonPlanLesson() != null
                && exam.getLessonPlanLesson().getChapter() != null
                && exam.getLessonPlanLesson().getChapter().getLessonPlan() != null) {
            return exam.getLessonPlanLesson().getChapter().getLessonPlan().getSubject();
        }
        if (exam.getLesson() == null || exam.getLesson().getChapter() == null
                || exam.getLesson().getChapter().getSubjectClasses() == null) {
            return null;
        }
        return exam.getLesson().getChapter().getSubjectClasses().getSubject();
    }

    private Chapter chapterOf(Exam exam) {
        if (exam == null) return null;
        if (exam.getLesson() != null) return exam.getLesson().getChapter();
        LessonPlanLesson lesson = exam.getLessonPlanLesson();
        LessonPlanChapter topic = lesson == null ? null : lesson.getChapter();
        if (topic == null) return null;
        return new Chapter(topic.getId() == null ? 0 : topic.getId(), topic.getName(),
                topic.getDisplayOrder(), null, null);
    }

    private Integer gradeOf(Exam exam) {
        if (exam == null) return null;
        if (exam.getTargetGrade() != null) return exam.getTargetGrade();
        LessonPlanLesson lesson = exam.getLessonPlanLesson();
        if (lesson != null && lesson.getChapter() != null && lesson.getChapter().getLessonPlan() != null
                && lesson.getChapter().getLessonPlan().getGrade() != null) {
            return lesson.getChapter().getLessonPlan().getGrade().getGrade();
        }
        return null;
    }
}
