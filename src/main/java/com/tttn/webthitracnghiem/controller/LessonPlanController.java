package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.Lesson;
import com.tttn.webthitracnghiem.model.LessonPlan;
import com.tttn.webthitracnghiem.model.LessonPlanChapter;
import com.tttn.webthitracnghiem.model.LessonPlanChapterRequest;
import com.tttn.webthitracnghiem.model.LessonPlanLessonRequest;
import com.tttn.webthitracnghiem.model.LessonPlanLesson;
import com.tttn.webthitracnghiem.model.LessonPlanRequest;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.service.ILessonPlanService;
import com.tttn.webthitracnghiem.service.IUserService;
import com.tttn.webthitracnghiem.service.LessonPlanDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/lesson-plans")
public class LessonPlanController {
    @Autowired
    private ILessonPlanService lessonPlanService;
    @Autowired
    private IUserService userService;
    @Autowired
    private LessonPlanDocumentService documentService;

    @GetMapping({"", "/"})
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Integer subjectId,
                       @RequestParam(required = false) Integer gradeId,
                       @RequestParam(required = false) Integer classId,
                       @RequestParam(required = false) String teacherName,
                       @RequestParam(defaultValue = "false") boolean mine,
                       @RequestParam(defaultValue = "ALL") String visibility,
                       Model model) {
        User user = currentUser();
        boolean privileged = isPrivileged();
        model.addAttribute("plans", lessonPlanService.findVisible(user.getId(), privileged, q,
                subjectId, gradeId, classId, teacherName, mine, visibility));
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("subjectId", subjectId);
        model.addAttribute("gradeId", gradeId);
        model.addAttribute("classId", classId);
        model.addAttribute("teacherName", teacherName == null ? "" : teacherName);
        model.addAttribute("mine", mine);
        model.addAttribute("visibility", visibility);
        model.addAttribute("subjects", lessonPlanService.findSubjects());
        model.addAttribute("grades", lessonPlanService.findGrades(user, privileged));
        model.addAttribute("targetClasses", lessonPlanService.findTargetClasses(user, privileged, gradeId));
        model.addAttribute("canCreate", true);
        return "lesson-plan/list";
    }

    @GetMapping("/create")
    public String create(Model model) {
        addFormModel(model, new LessonPlanRequest(), false, null);
        return "lesson-plan/form";
    }

    @GetMapping("/catalog")
    @ResponseBody
    public List<LessonPlanCatalog> catalog(@RequestParam(required = false) Integer subjectId,
                                           @RequestParam(required = false) Integer grade) {
        User user = currentUser();
        List<LessonPlanCatalog> result = new ArrayList<>();
        for (LessonPlan plan : lessonPlanService.findVisible(user.getId(), isPrivileged())) {
            if (plan.getSubject() == null || (subjectId != null && !subjectId.equals(plan.getSubject().getId()))) continue;
            if (plan.getGrade() == null || (grade != null && !grade.equals(plan.getGrade().getGrade()))) continue;
            LessonPlanCatalog item = new LessonPlanCatalog(plan.getId(), plan.getName(),
                    plan.getOwner() == null ? "" : plan.getOwner().getFullName(), plan.getGrade().getGrade());
            for (LessonPlanChapter topic : plan.getPlanChapters()) {
                LessonPlanCatalog.Topic topicItem = new LessonPlanCatalog.Topic(topic.getId(), topic.getName());
                for (com.tttn.webthitracnghiem.model.LessonPlanLesson lesson : topic.getLessons()) {
                    topicItem.lessons.add(new LessonPlanCatalog.Lesson(lesson.getId(), lesson.getName()));
                }
                item.topics.add(topicItem);
            }
            if (!item.topics.isEmpty()) result.add(item);
        }
        return result;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable int id, Model model, RedirectAttributes ra) {
        LessonPlan plan = lessonPlanService.findById(id);
        if (!lessonPlanService.canView(plan, currentUser().getId(), isPrivileged())) {
            ra.addFlashAttribute("error", "Bạn không có quyền xem giáo án này.");
            return "redirect:/lesson-plans";
        }
        model.addAttribute("plan", plan);
        model.addAttribute("canEdit", isOwner(plan));
        return "lesson-plan/detail";
    }

    @GetMapping("/lesson/{lessonId}/attachment")
    public ResponseEntity<byte[]> attachment(@PathVariable int lessonId) {
        LessonPlanLesson lesson = lessonPlanService.findLesson(lessonId);
        if (lesson == null || lesson.getAttachmentData() == null
                || lesson.getChapter() == null || lesson.getChapter().getLessonPlan() == null
                || !lessonPlanService.canView(lesson.getChapter().getLessonPlan(), currentUser().getId(), isPrivileged())) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (lesson.getAttachmentContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(lesson.getAttachmentContentType());
            } catch (IllegalArgumentException ignored) {
                // Keep a safe download type when an old record has an invalid MIME type.
            }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(lesson.getAttachmentOriginalName() == null ? "bai-hoc.docx" : lesson.getAttachmentOriginalName(),
                        StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(lesson.getAttachmentData());
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable int id, Model model, RedirectAttributes ra) {
        LessonPlan plan = lessonPlanService.findById(id);
        if (!isOwner(plan)) {
            ra.addFlashAttribute("error", "Chỉ chủ sở hữu mới được sửa giáo án.");
            return "redirect:/lesson-plans";
        }
        addFormModel(model, toRequest(plan), true, null);
        return "lesson-plan/form";
    }

    @PostMapping("/save")
    public String save(@Validated @ModelAttribute("lessonPlan") LessonPlanRequest request,
                       BindingResult bindingResult, Model model, RedirectAttributes ra) {
        try {
            documentService.importInto(request);
        } catch (IllegalArgumentException ex) {
            addFormModel(model, request, request.getId() != null, ex.getMessage());
            return "lesson-plan/form";
        }
        if (bindingResult.hasErrors()) {
            addFormModel(model, request, request.getId() != null, "Vui lòng kiểm tra lại thông tin giáo án.");
            return "lesson-plan/form";
        }

        try {
            lessonPlanService.save(request, currentUser());
            ra.addFlashAttribute("message", request.getId() == null
                    ? "Tạo giáo án thành công."
                    : "Cập nhật giáo án thành công.");
            return "redirect:/lesson-plans";
        } catch (SecurityException | IllegalArgumentException ex) {
            addFormModel(model, request, request.getId() != null, ex.getMessage());
            return "lesson-plan/form";
        }
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable int id, RedirectAttributes ra) {
        LessonPlan plan = lessonPlanService.findById(id);
        if (!lessonPlanService.canView(plan, currentUser().getId(), isPrivileged())) {
            ra.addFlashAttribute("error", "Ban khong co quyen xuat giao an nay.");
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/lesson-plans").build();
        }
        byte[] document = documentService.export(plan);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("giao-an-" + id + ".docx").build().toString())
                .body(document);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable int id, RedirectAttributes ra) {
        try {
            lessonPlanService.delete(id, currentUser().getId());
            ra.addFlashAttribute("message", "Đã xóa giáo án.");
        } catch (SecurityException | IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/lesson-plans";
    }

    private void addFormModel(Model model, LessonPlanRequest request, boolean editMode, String error) {
        User user = currentUser();
        model.addAttribute("lessonPlan", request);
        model.addAttribute("subjects", lessonPlanService.findSubjects());
        model.addAttribute("grades", lessonPlanService.findGrades(user, isPrivileged()));
        model.addAttribute("targetClasses", lessonPlanService.findTargetClasses(
                user, isPrivileged(), null));
        model.addAttribute("editMode", editMode);
        if (error != null) model.addAttribute("error", error);
    }

    private LessonPlanRequest toRequest(LessonPlan plan) {
        LessonPlanRequest request = new LessonPlanRequest();
        request.setId(plan.getId());
        request.setName(plan.getName());
        request.setDescription(plan.getDescription());
        if (plan.getSubject() != null) request.setSubjectId(plan.getSubject().getId());
        if (plan.getGrade() != null) request.setGradeId(plan.getGrade().getId());
        if (plan.getTargetClass() != null) request.setClassId(plan.getTargetClass().getId());
        if (plan.getSubjectClasses() != null) {
            request.setSubjectClassId(plan.getSubjectClasses().getId());
            if (request.getSubjectId() == null && plan.getSubjectClasses().getSubject() != null) {
                request.setSubjectId(plan.getSubjectClasses().getSubject().getId());
            }
            if (request.getGradeId() == null && plan.getSubjectClasses().getClasses() != null) {
                request.setGradeId(plan.getSubjectClasses().getClasses().getId());
            }
        }
        Set<Integer> chapterIds = new LinkedHashSet<>();
        if (plan.getChapters() != null) {
            for (Chapter chapter : plan.getChapters()) chapterIds.add(chapter.getId());
        }
        Set<Integer> lessonIds = new LinkedHashSet<>();
        if (plan.getLessons() != null) {
            for (Lesson lesson : plan.getLessons()) lessonIds.add(lesson.getId());
        }
        request.setChapterIds(chapterIds);
        request.setLessonIds(lessonIds);
        request.setIsPublic(plan.getIsPublic());
        List<LessonPlanChapterRequest> customChapters = new ArrayList<>();
        for (LessonPlanChapter chapter : plan.getPlanChapters() == null
                ? java.util.Collections.<LessonPlanChapter>emptyList() : plan.getPlanChapters()) {
            LessonPlanChapterRequest chapterRequest = new LessonPlanChapterRequest();
            chapterRequest.setId(chapter.getId());
            chapterRequest.setName(chapter.getName());
            List<LessonPlanLessonRequest> lessonRequests = new ArrayList<>();
            for (com.tttn.webthitracnghiem.model.LessonPlanLesson lesson : chapter.getLessons() == null
                    ? java.util.Collections.<com.tttn.webthitracnghiem.model.LessonPlanLesson>emptyList()
                    : chapter.getLessons()) {
                LessonPlanLessonRequest lessonRequest = new LessonPlanLessonRequest();
                lessonRequest.setId(lesson.getId());
                lessonRequest.setName(lesson.getName());
                lessonRequest.setContent(lesson.getContent());
                lessonRequest.setAttachmentOriginalName(lesson.getAttachmentOriginalName());
                lessonRequests.add(lessonRequest);
            }
            chapterRequest.setLessons(lessonRequests);
            customChapters.add(chapterRequest);
        }
        request.setChapters(customChapters);
        return request;
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findById(auth.getName());
        if (user == null) throw new IllegalStateException("Không tìm thấy người dùng hiện tại");
        return user;
    }

    private boolean isOwner(LessonPlan plan) {
        return plan != null && plan.getOwner() != null
                && plan.getOwner().getId().equals(currentUser().getId());
    }

    private boolean isPrivileged() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))
                || auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SCHOOL_ADMIN")));
    }

    public static class LessonPlanCatalog {
        private final Integer planId;
        private final String planName;
        private final String ownerName;
        private final Integer grade;
        private final List<Topic> topics = new ArrayList<>();

        public LessonPlanCatalog(Integer planId, String planName, String ownerName, Integer grade) {
            this.planId = planId;
            this.planName = planName;
            this.ownerName = ownerName;
            this.grade = grade;
        }

        public Integer getPlanId() { return planId; }
        public String getPlanName() { return planName; }
        public String getOwnerName() { return ownerName; }
        public Integer getGrade() { return grade; }
        public List<Topic> getTopics() { return topics; }

        public static class Topic {
            private final Integer id;
            private final String name;
            private final List<Lesson> lessons = new ArrayList<>();

            public Topic(Integer id, String name) { this.id = id; this.name = name; }
            public Integer getId() { return id; }
            public String getName() { return name; }
            public List<Lesson> getLessons() { return lessons; }
        }

        public static class Lesson {
            private final Integer id;
            private final String name;

            public Lesson(Integer id, String name) { this.id = id; this.name = name; }
            public Integer getId() { return id; }
            public String getName() { return name; }
        }
    }
}
