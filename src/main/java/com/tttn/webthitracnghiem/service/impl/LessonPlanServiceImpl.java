package com.tttn.webthitracnghiem.service.impl;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.Classes;
import com.tttn.webthitracnghiem.model.Lesson;
import com.tttn.webthitracnghiem.model.LessonPlan;
import com.tttn.webthitracnghiem.model.LessonPlanChapter;
import com.tttn.webthitracnghiem.model.LessonPlanChapterRequest;
import com.tttn.webthitracnghiem.model.LessonPlanLesson;
import com.tttn.webthitracnghiem.model.LessonPlanLessonRequest;
import com.tttn.webthitracnghiem.model.LessonPlanRequest;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.ChapterRepository;
import com.tttn.webthitracnghiem.repository.ClassesRepository;
import com.tttn.webthitracnghiem.repository.LessonPlanRepository;
import com.tttn.webthitracnghiem.repository.LessonPlanLessonRepository;
import com.tttn.webthitracnghiem.repository.LessonRepository;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.repository.SubjectClassRepository;
import com.tttn.webthitracnghiem.repository.SubjectRepository;
import com.tttn.webthitracnghiem.service.ILessonPlanService;
import com.tttn.webthitracnghiem.service.LessonPlanContentSanitizer;
import com.tttn.webthitracnghiem.service.ISubjectClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.io.IOException;
import java.util.Locale;
import org.springframework.web.multipart.MultipartFile;
import java.util.stream.Collectors;

@Service
public class LessonPlanServiceImpl implements ILessonPlanService {
    @Autowired
    private LessonPlanRepository lessonPlanRepository;
    @Autowired
    private LessonPlanLessonRepository lessonPlanLessonRepository;
    @Autowired
    private SubjectClassRepository subjectClassRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private ClassesRepository classesRepository;
    @Autowired
    private StudentTrackingClassRepository trackingClassRepository;
    @Autowired
    private ChapterRepository chapterRepository;
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private ISubjectClassService subjectClassService;
    @Autowired
    private com.tttn.webthitracnghiem.service.CurriculumService curriculumService;
    @Autowired
    private com.tttn.webthitracnghiem.repository.QuestionRepository questionRepository;
    @Autowired
    private com.tttn.webthitracnghiem.repository.ExamRepository examRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LessonPlan> findVisible(String userId, boolean privileged) {
        return findVisible(userId, privileged, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonPlan> findVisible(String userId, boolean privileged, String query) {
        return findVisible(userId, privileged, query, null, null, null, null, false, "ALL");
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonPlan> findVisible(String userId, boolean privileged, String query,
                                        Integer subjectId, Integer gradeId, Integer classId,
                                        String teacherName, boolean mine, String visibility) {
        List<LessonPlan> plans;
        if (privileged) {
            plans = lessonPlanRepository.findAllByOrderByUpdatedAtDesc();
        } else {
            Map<Integer, LessonPlan> visible = new LinkedHashMap<>();
            lessonPlanRepository.findByIsPublicTrueOrderByUpdatedAtDesc()
                    .forEach(plan -> visible.put(plan.getId(), plan));
            lessonPlanRepository.findByOwner_IdOrderByUpdatedAtDesc(userId)
                    .forEach(plan -> visible.put(plan.getId(), plan));
            plans = visible.values().stream().collect(Collectors.toList());
        }

        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String teacherNeedle = teacherName == null ? "" : teacherName.trim().toLowerCase(Locale.ROOT);
        String requestedVisibility = visibility == null ? "ALL" : visibility.trim().toUpperCase(Locale.ROOT);
        return plans.stream()
                .filter(plan -> !mine || isOwner(plan, userId))
                .filter(plan -> subjectId == null || plan.getSubject() != null
                        && subjectId.equals(plan.getSubject().getId()))
                .filter(plan -> gradeId == null || plan.getGrade() != null
                        && gradeId.equals(plan.getGrade().getId()))
                .filter(plan -> classId == null || plan.getTargetClass() != null
                        && classId.equals(plan.getTargetClass().getId()))
                .filter(plan -> teacherNeedle.isEmpty() || plan.getOwner() != null
                        && (contains(plan.getOwner().getFullName(), teacherNeedle)
                        || contains(plan.getOwner().getId(), teacherNeedle)))
                .filter(plan -> matchesVisibility(plan, requestedVisibility))
                .filter(plan -> needle.isEmpty() || matches(plan, needle))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonPlan> findVisibleForStudent(String userId, Integer subjectId,
                                                  Integer gradeId, Integer targetClassId) {
        Map<Integer, LessonPlan> visible = new LinkedHashMap<>();
        lessonPlanRepository.findByIsPublicTrueOrderByUpdatedAtDesc()
                .forEach(plan -> visible.put(plan.getId(), plan));
        if (targetClassId != null) {
            lessonPlanRepository.findByTargetClass_IdOrderByUpdatedAtDesc(targetClassId)
                    .forEach(plan -> visible.put(plan.getId(), plan));
        }
        return visible.values().stream()
                .filter(plan -> matchesSubject(plan, subjectId))
                .filter(plan -> matchesGrade(plan, gradeId))
                .sorted(Comparator.comparing(LessonPlan::getUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private boolean matchesSubject(LessonPlan plan, Integer subjectId) {
        if (subjectId == null) return true;
        if (plan.getSubject() != null && subjectId.equals(plan.getSubject().getId())) return true;
        return plan.getSubjectClasses() != null && plan.getSubjectClasses().getSubject() != null
                && subjectId.equals(plan.getSubjectClasses().getSubject().getId());
    }

    private boolean matchesGrade(LessonPlan plan, Integer gradeId) {
        if (gradeId == null) return true;
        if (plan.getGrade() != null && gradeId.equals(plan.getGrade().getId())) return true;
        return plan.getSubjectClasses() != null && plan.getSubjectClasses().getClasses() != null
                && gradeId.equals(plan.getSubjectClasses().getClasses().getId());
    }

    private boolean isOwner(LessonPlan plan, String userId) {
        return plan != null && plan.getOwner() != null && userId != null
                && userId.equals(plan.getOwner().getId());
    }

    private boolean matchesVisibility(LessonPlan plan, String visibility) {
        if ("PUBLIC".equals(visibility)) return Boolean.TRUE.equals(plan.getIsPublic());
        if ("PRIVATE".equals(visibility)) return !Boolean.TRUE.equals(plan.getIsPublic());
        return true;
    }

    private boolean matches(LessonPlan plan, String needle) {
        return contains(plan.getName(), needle)
                || contains(plan.getDescription(), needle)
                || (plan.getSubject() != null && contains(plan.getSubject().getName(), needle))
                || (plan.getGrade() != null && (contains(plan.getGrade().getDisplayName(), needle)
                || contains(String.valueOf(plan.getGrade().getGrade()), needle)))
                || (plan.getTargetClass() != null && contains(plan.getTargetClass().getClassName(), needle))
                || (plan.getOwner() != null && (contains(plan.getOwner().getFullName(), needle)
                || contains(plan.getOwner().getId(), needle)));
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonPlan findById(int id) {
        LessonPlan plan = lessonPlanRepository.findDetailedById(id).orElse(null);
        if (plan == null) return null;

        // Hibernate cannot fetch two List associations in one EntityGraph. The
        // nested lessons are initialized in this transaction instead.
        plan.getPlanChapters().forEach(chapter -> chapter.getLessons().size());
        return plan;
    }

    @Override
    @Transactional(readOnly = true)
    public LessonPlanLesson findLesson(int id) {
        return lessonPlanLessonRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public LessonPlan save(LessonPlanRequest request, User owner) {
        if (request == null || owner == null) throw new IllegalArgumentException("Thiếu dữ liệu giáo án");
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập tên giáo án");
        }

        Subject subject;
        Classes grade;
        SubjectClasses legacyCurriculum = null;
        if (request.getSubjectId() != null && request.getGradeId() != null) {
            subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new IllegalArgumentException("Môn học không tồn tại"));
            grade = classesRepository.findById(request.getGradeId())
                    .orElseThrow(() -> new IllegalArgumentException("Khối không tồn tại"));
        } else if (request.getSubjectClassId() != null) {
            legacyCurriculum = subjectClassRepository.findById(request.getSubjectClassId())
                    .orElseThrow(() -> new IllegalArgumentException("Môn học và khối/lớp không tồn tại"));
            subject = legacyCurriculum.getSubject();
            grade = legacyCurriculum.getClasses();
        } else {
            throw new IllegalArgumentException("Vui lòng chọn môn học và khối");
        }

        if (grade == null || grade.getGrade() == null || !isGradeAllowed(owner, grade.getGrade())) {
            throw new SecurityException("Bạn không được phép soạn giáo án cho khối này");
        }

        StudentTrackingClass targetClass = null;
        if (request.getClassId() != null) {
            targetClass = trackingClassRepository.findById(request.getClassId())
                    .orElseThrow(() -> new IllegalArgumentException("Lớp học không tồn tại"));
            validateTargetClass(targetClass, grade, owner);
        }

        LessonPlan plan = request.getId() == null
                ? new LessonPlan()
                : lessonPlanRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Giáo án không tồn tại"));
        if (plan.getOwner() != null && !owner.getId().equals(plan.getOwner().getId())) {
            throw new SecurityException("Bạn không có quyền sửa giáo án này");
        }

        plan.setName(request.getName().trim());
        plan.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        plan.setOwner(owner);
        plan.setSubject(subject);
        plan.setGrade(grade);
        plan.setTargetClass(targetClass);
        plan.setSubjectClasses(legacyCurriculum != null
                ? legacyCurriculum
                : subjectClassRepository.findByClassesAndSubject(grade.getId(), subject.getId()).orElse(null));
        plan.setIsPublic(Boolean.TRUE.equals(request.getIsPublic()));
        plan.setPlanChapters(buildCustomChapters(plan, request.getChapters(), owner));

        // Keep old links readable for records created by the previous form.
        if (request.getChapters() == null || request.getChapters().isEmpty()) {
            plan.setChapters(resolveLegacyChapters(request, legacyCurriculum == null ? null : legacyCurriculum.getId()));
            plan.setLessons(resolveLegacyLessons(request, legacyCurriculum == null ? null : legacyCurriculum.getId()));
        } else {
            plan.setChapters(new LinkedHashSet<>());
            plan.setLessons(new LinkedHashSet<>());
        }
        return lessonPlanRepository.save(plan);
    }

    private List<LessonPlanChapter> buildCustomChapters(LessonPlan plan,
                                                         List<LessonPlanChapterRequest> requests,
                                                         User owner) {
        List<LessonPlanChapter> chapters = new java.util.ArrayList<>();
        if (requests == null) return chapters;
        Map<Integer, LessonPlanChapter> existingChapters = new HashMap<>();
        for (LessonPlanChapter existing : plan.getPlanChapters()) {
            if (existing.getId() != null) existingChapters.put(existing.getId(), existing);
        }
        int chapterOrder = 1;
        for (LessonPlanChapterRequest request : requests) {
            if (request == null || request.getName() == null || request.getName().trim().isEmpty()) continue;
            LessonPlanChapter chapter = request.getId() == null
                    ? new LessonPlanChapter() : existingChapters.get(request.getId());
            if (chapter == null) chapter = new LessonPlanChapter();
            chapter.setLessonPlan(plan);
            chapter.setName(request.getName().trim());
            chapter.setDisplayOrder(chapterOrder++);
            chapter.setCurriculumChapter(request.getCurriculumChapterId() == null ? null
                    : curriculumChapter(request.getCurriculumChapterId(), plan, owner));
            Map<Integer, LessonPlanLesson> existingLessons = new HashMap<>();
            for (LessonPlanLesson existing : chapter.getLessons()) {
                if (existing.getId() != null) existingLessons.put(existing.getId(), existing);
            }
            List<LessonPlanLesson> lessons = new java.util.ArrayList<>();
            int lessonOrder = 1;
            for (LessonPlanLessonRequest lessonRequest : request.getLessons()) {
                if (lessonRequest == null || lessonRequest.getName() == null
                        || lessonRequest.getName().trim().isEmpty()) continue;
                LessonPlanLesson lesson = lessonRequest.getId() == null
                        ? new LessonPlanLesson() : existingLessons.get(lessonRequest.getId());
                if (lesson == null) lesson = new LessonPlanLesson();
                lesson.setChapter(chapter);
                lesson.setName(lessonRequest.getName().trim());
                lesson.setDisplayOrder(lessonOrder++);
                lesson.setContent(LessonPlanContentSanitizer.sanitize(lessonRequest.getContent()));
                lesson.setContentText(LessonPlanContentSanitizer.toPlainText(lesson.getContent()));
                lesson.setObjectives(normalize(lessonRequest.getObjectives()));
                lesson.setPeriods(lessonRequest.getPeriods() == null || lessonRequest.getPeriods() < 1
                        ? null : Math.min(lessonRequest.getPeriods(), 50));
                Lesson curriculumLesson = null;
                if (lessonRequest.getCurriculumLessonId() != null) {
                    curriculumLesson = curriculumLesson(lessonRequest.getCurriculumLessonId(), plan, owner);
                } else if (lessonRequest.isAddToCurriculum()) {
                    curriculumLesson = curriculumService.ensureLesson(owner, curriculumService.schoolIdOf(owner),
                            requireCurriculum(plan), request.getCurriculumChapterId(), chapter.getName(), lesson.getName());
                    if (chapter.getCurriculumChapter() == null) {
                        chapter.setCurriculumChapter(curriculumLesson.getChapter());
                    }
                }
                if (curriculumLesson != null && lesson.getCurriculumLesson() == null && lesson.getId() != null) {
                    moveLegacyContent(lesson, curriculumLesson);
                }
                lesson.setCurriculumLesson(curriculumLesson);
                MultipartFile attachment = lessonRequest.getAttachment();
                if (attachment != null && !attachment.isEmpty()) {
                    validateWordAttachment(attachment);
                    try {
                        lesson.setAttachmentOriginalName(attachment.getOriginalFilename());
                        lesson.setAttachmentContentType(attachment.getContentType());
                        lesson.setAttachmentSize(attachment.getSize());
                        lesson.setAttachmentData(attachment.getBytes());
                    } catch (IOException ex) {
                        throw new IllegalArgumentException("Không thể đọc file Word của bài học");
                    }
                }
                lessons.add(lesson);
            }
            chapter.setLessons(lessons);
            chapters.add(chapter);
        }
        return chapters;
    }

    private String normalize(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private SubjectClasses requireCurriculum(LessonPlan plan) {
        if (plan.getSubjectClasses() == null) {
            throw new IllegalArgumentException("Môn và khối của giáo án chưa có chương trình học để liên kết.");
        }
        return plan.getSubjectClasses();
    }

    /** A curriculum lesson the plan may link: same subject-grade and the owner's school curriculum. */
    private Lesson curriculumLesson(int lessonId, LessonPlan plan, User owner) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Bài trong chương trình học không tồn tại."));
        SubjectClasses subjectClass = requireCurriculum(plan);
        if (!curriculumService.belongsTo(lesson, curriculumService.schoolIdOf(owner), subjectClass)) {
            throw new IllegalArgumentException("Bài \"" + lesson.getNameLesson()
                    + "\" không thuộc chương trình học môn và khối của giáo án này.");
        }
        return lesson;
    }

    private Chapter curriculumChapter(int chapterId, LessonPlan plan, User owner) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("Chủ đề trong chương trình học không tồn tại."));
        SubjectClasses subjectClass = requireCurriculum(plan);
        Integer schoolId = curriculumService.schoolIdOf(owner);
        Integer chapterSchoolId = chapter.getSchool() == null ? null : chapter.getSchool().getId();
        if (chapter.getSubjectClasses() == null || chapter.getSubjectClasses().getId() != subjectClass.getId()
                || !java.util.Objects.equals(chapterSchoolId, schoolId)) {
            throw new IllegalArgumentException("Chủ đề \"" + chapter.getChapterName()
                    + "\" không thuộc chương trình học môn và khối của giáo án này.");
        }
        return chapter;
    }

    /**
     * When an older plan lesson is first linked to the curriculum, its questions and exams move to
     * the curriculum lesson so the question bank and exam scope see them. The exams stop pointing at
     * the plan lesson, which would otherwise delete them along with it.
     */
    private void moveLegacyContent(LessonPlanLesson planLesson, Lesson curriculumLesson) {
        for (com.tttn.webthitracnghiem.model.Question question : questionRepository.findByLessonPlanLesson_Id(planLesson.getId())) {
            if (question.getLesson() == null) {
                question.setLesson(curriculumLesson);
                question.setLessonPlanLesson(null);
                question.setSourcePlanLessonId(planLesson.getId());
                questionRepository.save(question);
            }
        }
        for (com.tttn.webthitracnghiem.model.Exam exam : examRepository.findByLessonPlanLesson_Id(planLesson.getId())) {
            exam.setLesson(curriculumLesson);
            exam.setLessonPlanLesson(null);
            exam.getScopeLessons().add(curriculumLesson);
            exam.setSubject(curriculumLesson.getChapter().getSubjectClasses().getSubject());
            exam.setGradeLevel(curriculumLesson.getChapter().getSubjectClasses().getClasses().getGrade());
            examRepository.save(exam);
        }
        planLesson.getExams().clear();
    }

    private void validateWordAttachment(MultipartFile attachment) {
        String filename = attachment.getOriginalFilename();
        String lowerName = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (!(lowerName.endsWith(".doc") || lowerName.endsWith(".docx"))) {
            throw new IllegalArgumentException("Chỉ chấp nhận file Word có đuôi .doc hoặc .docx");
        }
        if (attachment.getSize() > 5L * 1024L * 1024L) {
            throw new IllegalArgumentException("File Word không được lớn hơn 5 MB");
        }
    }

    private Set<Chapter> resolveLegacyChapters(LessonPlanRequest request, Integer subjectClassId) {
        Set<Chapter> chapters = new LinkedHashSet<>();
        for (Integer chapterId : request.getChapterIds()) {
            Chapter chapter = chapterRepository.findById(chapterId)
                    .orElseThrow(() -> new IllegalArgumentException("Chủ đề được chọn không tồn tại"));
            if (subjectClassId != null) validateChapter(chapter, subjectClassId);
            chapters.add(chapter);
        }
        return chapters;
    }

    private Set<Lesson> resolveLegacyLessons(LessonPlanRequest request, Integer subjectClassId) {
        Set<Lesson> lessons = new LinkedHashSet<>();
        for (Integer lessonId : request.getLessonIds()) {
            Lesson lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new IllegalArgumentException("Bài học được chọn không tồn tại"));
            if (subjectClassId != null) validateChapter(lesson.getChapter(), subjectClassId);
            lessons.add(lesson);
        }
        return lessons;
    }

    private void validateChapter(Chapter chapter, int subjectClassId) {
        if (chapter == null || chapter.getSubjectClasses() == null
                || chapter.getSubjectClasses().getId() != subjectClassId) {
            throw new IllegalArgumentException("Chủ đề hoặc bài học không thuộc môn và khối/lớp đã chọn");
        }
    }

    private boolean isGradeAllowed(User owner, int grade) {
        if (hasRole(owner, "ROLE_ADMIN") || hasRole(owner, "ROLE_SCHOOL_ADMIN")) return true;
        if (owner.getSchoolLevel() != null) {
            return grade >= minGrade(owner.getSchoolLevel()) && grade <= maxGrade(owner.getSchoolLevel());
        }
        if (owner.getSchool() != null && owner.getSchool().getName() != null) {
            String sname = owner.getSchool().getName().toUpperCase();
            if (sname.contains("TIỂU HỌC") || sname.contains("TIEU HOC") || sname.contains(" TH ")) {
                return grade >= 1 && grade <= 5;
            }
            if (sname.contains("THCS") || sname.contains("TRUNG HỌC CƠ SỞ")) {
                return grade >= 6 && grade <= 9;
            }
            if (sname.contains("THPT") || sname.contains("TRUNG HỌC PHỔ THÔNG")) {
                return grade >= 10 && grade <= 12;
            }
        }
        boolean hasInvolved = trackingClassRepository.findClassesByTeacherInvolved(owner.getId()).stream()
                .anyMatch(item -> item.getGrade() != null && item.getGrade() == grade);
        if (hasInvolved) return true;
        return true;
    }

    private void validateTargetClass(StudentTrackingClass targetClass, Classes grade, User owner) {
        if (!grade.getGrade().equals(targetClass.getGrade())) {
            throw new IllegalArgumentException("Lớp không thuộc khối đã chọn");
        }
        if (!hasRole(owner, "ROLE_ADMIN")) {
            if (owner.getSchool() == null || targetClass.getSchool() == null
                    || !owner.getSchool().getId().equals(targetClass.getSchool().getId())) {
                throw new SecurityException("Bạn không có quyền chọn lớp thuộc trường khác");
            }
        }
    }

    private int minGrade(int schoolLevel) {
        return schoolLevel == 1 ? 1 : schoolLevel == 2 ? 6 : schoolLevel == 3 ? 10 : Integer.MIN_VALUE;
    }

    private int maxGrade(int schoolLevel) {
        return schoolLevel == 1 ? 5 : schoolLevel == 2 ? 9 : schoolLevel == 3 ? 12 : Integer.MAX_VALUE;
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> roleName.equals(role.getRoleName()));
    }

    @Override
    @Transactional
    public void delete(int id, String ownerId) {
        LessonPlan plan = lessonPlanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Giáo án không tồn tại"));
        if (plan.getOwner() == null || !plan.getOwner().getId().equals(ownerId)) {
            throw new SecurityException("Bạn không có quyền xóa giáo án này");
        }
        lessonPlanRepository.delete(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canView(LessonPlan plan, String userId, boolean privileged) {
        if (plan == null) return false;
        return privileged || Boolean.TRUE.equals(plan.getIsPublic())
                || (plan.getOwner() != null && plan.getOwner().getId().equals(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subject> findSubjects() {
        return subjectRepository.findAll().stream()
                .sorted(Comparator.comparing(Subject::getName,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Classes> findGrades(User user, boolean privileged) {
        return classesRepository.findAll().stream()
                .filter(item -> item.getGrade() != null && (privileged || isGradeAllowed(user, item.getGrade())))
                .sorted(Comparator.comparing(Classes::getGrade))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentTrackingClass> findTargetClasses(User user, boolean privileged, Integer gradeId) {
        Classes selectedGrade = gradeId == null ? null : classesRepository.findById(gradeId).orElse(null);
        if (gradeId != null && (selectedGrade == null || selectedGrade.getGrade() == null
                || (!privileged && !isGradeAllowed(user, selectedGrade.getGrade())))) {
            return java.util.Collections.emptyList();
        }
        List<StudentTrackingClass> classes;
        if (privileged && hasRole(user, "ROLE_ADMIN")) {
            classes = trackingClassRepository.findAll();
        } else if (user.getSchool() != null) {
            classes = selectedGrade == null
                    ? trackingClassRepository.findBySchoolId(user.getSchool().getId())
                    : trackingClassRepository.findBySchoolIdAndGradeOrderByClassNameAsc(
                    user.getSchool().getId(), selectedGrade.getGrade());
        } else {
            classes = trackingClassRepository.findClassesByTeacherInvolved(user.getId());
        }
        return classes.stream()
                .filter(item -> item.getGrade() != null
                        && (selectedGrade == null || selectedGrade.getGrade().equals(item.getGrade()))
                        && (privileged || isGradeAllowed(user, item.getGrade())))
                .sorted(Comparator.comparing(StudentTrackingClass::getClassName,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    @Override
    public List<SubjectClasses> findCurricula() {
        return subjectClassService.findAll();
    }
}
