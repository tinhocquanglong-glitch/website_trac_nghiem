package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.model.Lesson;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.SchoolCurriculum;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.ChapterRepository;
import com.tttn.webthitracnghiem.repository.ClassSubjectAssignmentRepository;
import com.tttn.webthitracnghiem.repository.ExamRepository;
import com.tttn.webthitracnghiem.repository.LessonPlanLessonRepository;
import com.tttn.webthitracnghiem.repository.LessonPlanRepository;
import com.tttn.webthitracnghiem.repository.LessonRepository;
import com.tttn.webthitracnghiem.repository.QuestionRepository;
import com.tttn.webthitracnghiem.repository.SchoolCurriculumRepository;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.repository.SubjectClassRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Curriculum (subject-grade, chapters, lessons) per school.
 *
 * <p>Chapters without a school form the shared curriculum. A school reads the shared
 * curriculum until it first needs its own copy (editing, attaching questions, exams or
 * lesson plans); {@link #schoolChapters} then copies the shared chapters and lessons,
 * remembering their origin, and records a {@link SchoolCurriculum} marker so the copy is
 * made only once. Edits never touch another school's copy.
 */
@Service
public class CurriculumService {
    private final SubjectClassRepository subjectClassRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolCurriculumRepository schoolCurriculumRepository;
    private final ClassSubjectAssignmentRepository assignmentRepository;
    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final LessonPlanLessonRepository planLessonRepository;
    private final LessonPlanRepository planRepository;

    public CurriculumService(SubjectClassRepository subjectClassRepository,
                             ChapterRepository chapterRepository,
                             LessonRepository lessonRepository,
                             SchoolRepository schoolRepository,
                             SchoolCurriculumRepository schoolCurriculumRepository,
                             ClassSubjectAssignmentRepository assignmentRepository,
                             QuestionRepository questionRepository,
                             ExamRepository examRepository,
                             LessonPlanLessonRepository planLessonRepository,
                             LessonPlanRepository planRepository) {
        this.subjectClassRepository = subjectClassRepository;
        this.chapterRepository = chapterRepository;
        this.lessonRepository = lessonRepository;
        this.schoolRepository = schoolRepository;
        this.schoolCurriculumRepository = schoolCurriculumRepository;
        this.assignmentRepository = assignmentRepository;
        this.questionRepository = questionRepository;
        this.examRepository = examRepository;
        this.planLessonRepository = planLessonRepository;
        this.planRepository = planRepository;
    }

    // ---------------------------------------------------------------- reading

    public Optional<SubjectClasses> findSubjectClass(int subjectId, int gradeId) {
        return findSubjectClass(subjectId, gradeId, null);
    }

    public Optional<SubjectClasses> findSubjectClass(int subjectId, int gradeId, Integer schoolId) {
        if (schoolId != null) {
            Optional<SubjectClasses> sc = subjectClassRepository.findByClassAndSubjectAndSchool(gradeId, subjectId, schoolId);
            if (sc.isPresent()) {
                return sc;
            }
        }
        return subjectClassRepository.findByClassAndSubjectAndSchoolIsNull(gradeId, subjectId)
                .or(() -> subjectClassRepository.findByClassesAndSubject(gradeId, subjectId));
    }

    /** School a user works or studies in; null for accounts without a school (e.g. the system admin). */
    public Integer schoolIdOf(User user) {
        if (user == null) {
            return null;
        }
        if (user.getSchool() != null) {
            return user.getSchool().getId();
        }
        if (user.getTrackingClass() != null && user.getTrackingClass().getSchool() != null) {
            return user.getTrackingClass().getSchool().getId();
        }
        return null;
    }

    public boolean hasOwnCopy(Integer schoolId, SubjectClasses subjectClass) {
        return schoolId != null
                && schoolCurriculumRepository.existsBySchool_IdAndSubjectClasses_Id(schoolId, subjectClass.getId());
    }

    /**
     * Chapters a school sees, without ever copying: its own copy when it has one,
     * otherwise the shared curriculum. Used for read-only views such as students'.
     */
    @Transactional(readOnly = true)
    public List<Chapter> readChapters(Integer schoolId, SubjectClasses subjectClass) {
        Integer owner = hasOwnCopy(schoolId, subjectClass) ? schoolId : null;
        return chapterRepository.findBySubjectClasses_IdAndSchool_IdOrderBySerialAscIdAsc(subjectClass.getId(), owner);
    }

    /** The school's own chapters, copying the shared curriculum the first time. Null school = shared. */
    @Transactional
    public List<Chapter> schoolChapters(Integer schoolId, SubjectClasses subjectClass) {
        if (schoolId != null && !hasOwnCopy(schoolId, subjectClass)) {
            copySharedToSchool(schoolId, subjectClass);
        }
        return chapterRepository.findBySubjectClasses_IdAndSchool_IdOrderBySerialAscIdAsc(subjectClass.getId(), schoolId);
    }

    public List<Lesson> lessonsOf(Chapter chapter) {
        return lessonRepository.findByChapter_IdOrderBySerialAscIdAsc(chapter.getId());
    }

    /** True when the lesson is part of the given school's (or, for null, the shared) curriculum for the subject-grade. */
    public boolean belongsTo(Lesson lesson, Integer schoolId, SubjectClasses subjectClass) {
        if (lesson == null || lesson.getChapter() == null || lesson.getChapter().getSubjectClasses() == null) {
            return false;
        }
        Chapter chapter = lesson.getChapter();
        Integer lessonSchoolId = chapter.getSchool() == null ? null : chapter.getSchool().getId();
        return chapter.getSubjectClasses().getId() == subjectClass.getId() && Objects.equals(lessonSchoolId, schoolId);
    }

    private void copySharedToSchool(Integer schoolId, SubjectClasses subjectClass) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy trường học."));
        for (Chapter shared : chapterRepository.findBySubjectClasses_IdAndSchool_IdOrderBySerialAscIdAsc(
                subjectClass.getId(), null)) {
            Chapter copy = new Chapter();
            copy.setChapterName(shared.getChapterName());
            copy.setSerial(shared.getSerial());
            copy.setSubjectClasses(subjectClass);
            copy.setSchool(school);
            copy.setTemplateChapter(shared);
            copy = chapterRepository.save(copy);
            for (Lesson sharedLesson : lessonRepository.findByChapter_IdOrderBySerialAscIdAsc(shared.getId())) {
                Lesson lessonCopy = new Lesson();
                lessonCopy.setNameLesson(sharedLesson.getNameLesson());
                lessonCopy.setSerial(sharedLesson.getSerial());
                lessonCopy.setChapter(copy);
                lessonCopy.setTemplateLesson(sharedLesson);
                lessonRepository.save(lessonCopy);
            }
        }
        schoolCurriculumRepository.save(new SchoolCurriculum(school, subjectClass));
    }

    // ---------------------------------------------------------- permissions

    /**
     * Admin edits the shared curriculum and every school's copy; a principal edits their
     * school's copy; a teacher edits only subject-grades assigned to them in their school.
     */
    public boolean canEdit(User user, Integer schoolId, SubjectClasses subjectClass) {
        if (user == null || subjectClass == null) {
            return false;
        }
        if (hasRole(user, "ROLE_ADMIN")) {
            return true;
        }
        Integer userSchoolId = schoolIdOf(user);
        if (!Objects.equals(schoolId, userSchoolId)) {
            return false;
        }
        if (hasRole(user, "ROLE_SCHOOL_ADMIN") || hasRole(user, "ROLE_TEACHER")) {
            return true;
        }
        return false;
    }

    private static boolean hasRole(User user, String roleName) {
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> roleName.equals(role.getRoleName()));
    }

    // ------------------------------------------------------------- editing

    @Transactional
    public Chapter addChapter(User user, Integer schoolId, SubjectClasses subjectClass, String name) {
        requireEdit(user, schoolId, subjectClass);
        List<Chapter> chapters = schoolChapters(schoolId, subjectClass);
        Chapter chapter = new Chapter();
        chapter.setChapterName(cleanName(name, "Tên chủ đề"));
        chapter.setSerial(chapters.size() + 1);
        chapter.setSubjectClasses(subjectClass);
        chapter.setSchool(schoolId == null ? null : schoolRepository.getReferenceById(schoolId));
        return chapterRepository.save(chapter);
    }

    @Transactional
    public void renameChapter(User user, int chapterId, String name) {
        Chapter chapter = editableChapter(user, chapterId);
        chapter.setChapterName(cleanName(name, "Tên chủ đề"));
        chapterRepository.save(chapter);
    }

    @Transactional
    public void moveChapter(User user, int chapterId, int direction) {
        Chapter chapter = editableChapter(user, chapterId);
        List<Chapter> siblings = new ArrayList<>(chapterRepository.findBySubjectClasses_IdAndSchool_IdOrderBySerialAscIdAsc(
                chapter.getSubjectClasses().getId(), schoolIdOf(chapter)));
        swapWithNeighbour(siblings, chapter.getId(), direction, Chapter::getId);
        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setSerial(i + 1);
        }
        chapterRepository.saveAll(siblings);
    }

    @Transactional
    public void deleteChapter(User user, int chapterId) {
        Chapter chapter = editableChapter(user, chapterId);
        List<Lesson> lessons = lessonRepository.findByChapter_IdOrderBySerialAscIdAsc(chapterId);
        for (Lesson lesson : lessons) {
            String blocker = deleteBlocker(lesson);
            if (blocker != null) {
                throw new IllegalStateException("Không thể xóa chủ đề \"" + chapter.getChapterName()
                        + "\": bài \"" + lesson.getNameLesson() + "\" " + blocker);
            }
        }
        long legacyPlans = planRepository.countLegacyLinksToChapter(chapterId);
        if (legacyPlans > 0) {
            throw new IllegalStateException("Không thể xóa chủ đề \"" + chapter.getChapterName()
                    + "\": còn " + legacyPlans + " giáo án cũ tham chiếu tới chủ đề này.");
        }
        for (Lesson lesson : lessons) {
            lessonRepository.detachCopiesOf(lesson.getId());
            lessonRepository.delete(lesson);
        }
        chapterRepository.detachCopiesOf(chapterId);
        chapterRepository.delete(chapter);
        renumberChapters(chapter.getSubjectClasses().getId(), schoolIdOf(chapter));
    }

    @Transactional
    public Lesson addLesson(User user, int chapterId, String name) {
        Chapter chapter = editableChapter(user, chapterId);
        Lesson lesson = new Lesson();
        lesson.setNameLesson(cleanName(name, "Tên bài"));
        lesson.setSerial(lessonRepository.findByChapter_IdOrderBySerialAscIdAsc(chapterId).size() + 1);
        lesson.setChapter(chapter);
        return lessonRepository.save(lesson);
    }

    @Transactional
    public void renameLesson(User user, int lessonId, String name) {
        Lesson lesson = editableLesson(user, lessonId);
        lesson.setNameLesson(cleanName(name, "Tên bài"));
        lessonRepository.save(lesson);
    }

    @Transactional
    public void moveLesson(User user, int lessonId, int direction) {
        Lesson lesson = editableLesson(user, lessonId);
        List<Lesson> siblings = new ArrayList<>(lessonRepository.findByChapter_IdOrderBySerialAscIdAsc(lesson.getChapter().getId()));
        swapWithNeighbour(siblings, lesson.getId(), direction, Lesson::getId);
        for (int i = 0; i < siblings.size(); i++) {
            siblings.get(i).setSerial(i + 1);
        }
        lessonRepository.saveAll(siblings);
    }

    @Transactional
    public void deleteLesson(User user, int lessonId) {
        Lesson lesson = editableLesson(user, lessonId);
        String blocker = deleteBlocker(lesson);
        if (blocker != null) {
            throw new IllegalStateException("Không thể xóa bài \"" + lesson.getNameLesson() + "\": " + blocker);
        }
        int chapterId = lesson.getChapter().getId();
        lessonRepository.detachCopiesOf(lessonId);
        lessonRepository.delete(lesson);
        List<Lesson> remaining = lessonRepository.findByChapter_IdOrderBySerialAscIdAsc(chapterId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setSerial(i + 1);
        }
        lessonRepository.saveAll(remaining);
    }

    /**
     * Removes a subject from a grade with every school's chapters, refusing when any lesson is
     * still in use. Lessons cascade-delete their questions and exams, so this check guards real data.
     */
    @Transactional
    public void deleteSubjectClass(SubjectClasses subjectClass) {
        List<Chapter> chapters = chapterRepository.findBySubjectClasses_Id(subjectClass.getId());
        for (Chapter chapter : chapters) {
            for (Lesson lesson : lessonRepository.findByChapter_IdOrderBySerialAscIdAsc(chapter.getId())) {
                String blocker = deleteBlocker(lesson);
                if (blocker != null) {
                    throw new IllegalStateException("Không thể gỡ môn khỏi khối: bài \"" + lesson.getNameLesson()
                            + "\" " + blocker);
                }
            }
            if (planRepository.countLegacyLinksToChapter(chapter.getId()) > 0) {
                throw new IllegalStateException("Không thể gỡ môn khỏi khối: chủ đề \"" + chapter.getChapterName()
                        + "\" còn giáo án cũ tham chiếu.");
            }
        }
        // Copies first, then the shared rows they point to.
        chapters.sort((a, b) -> Boolean.compare(a.getSchool() == null, b.getSchool() == null));
        for (Chapter chapter : chapters) {
            for (Lesson lesson : lessonRepository.findByChapter_IdOrderBySerialAscIdAsc(chapter.getId())) {
                lessonRepository.detachCopiesOf(lesson.getId());
                lessonRepository.delete(lesson);
            }
            chapterRepository.detachCopiesOf(chapter.getId());
            chapterRepository.delete(chapter);
        }
        schoolCurriculumRepository.deleteBySubjectClasses_Id(subjectClass.getId());
        subjectClassRepository.delete(subjectClass);
    }

    /** Why a lesson cannot be deleted, or null when nothing depends on it. */
    public String deleteBlocker(Lesson lesson) {
        long questions = questionRepository.countByLesson_Id(lesson.getId());
        long exams = examRepository.countCoveringLesson(lesson.getId());
        long planLessons = planLessonRepository.countByCurriculumLesson_Id(lesson.getId())
                + planRepository.countLegacyLinksToLesson(lesson.getId());
        if (questions == 0 && exams == 0 && planLessons == 0) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        if (questions > 0) parts.add(questions + " câu hỏi");
        if (exams > 0) parts.add(exams + " đề thi");
        if (planLessons > 0) parts.add(planLessons + " bài giáo án");
        return "vẫn còn " + String.join(", ", parts) + " gắn với bài này.";
    }

    /**
     * Finds or creates a lesson in a school's curriculum, used when a teacher imports a lesson plan
     * and chooses to add an unmatched lesson. Picks the chapter by id, else by name, else creates it.
     */
    @Transactional
    public Lesson ensureLesson(User user, Integer schoolId, SubjectClasses subjectClass,
                               Integer chapterId, String chapterName, String lessonName) {
        requireEdit(user, schoolId, subjectClass);
        List<Chapter> chapters = schoolChapters(schoolId, subjectClass);
        Chapter chapter = chapters.stream()
                .filter(item -> chapterId != null && item.getId() == chapterId)
                .findFirst()
                .orElseGet(() -> chapters.stream()
                        .filter(item -> chapterName != null && item.getChapterName() != null
                                && item.getChapterName().trim().equalsIgnoreCase(chapterName.trim()))
                        .findFirst()
                        .orElse(null));
        if (chapter == null) {
            chapter = addChapter(user, schoolId, subjectClass,
                    chapterName == null || chapterName.isBlank() ? "Chủ đề mới" : chapterName);
        }
        String cleanLessonName = cleanName(lessonName, "Tên bài");
        for (Lesson existing : lessonRepository.findByChapter_IdOrderBySerialAscIdAsc(chapter.getId())) {
            if (cleanLessonName.equalsIgnoreCase(existing.getNameLesson())) {
                return existing;
            }
        }
        return addLesson(user, chapter.getId(), cleanLessonName);
    }

    // -------------------------------------------------------------- helpers

    private void requireEdit(User user, Integer schoolId, SubjectClasses subjectClass) {
        if (!canEdit(user, schoolId, subjectClass)) {
            throw new SecurityException("Bạn không có quyền sửa chương trình học của môn và khối này.");
        }
    }

    private Chapter editableChapter(User user, int chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chủ đề."));
        requireEdit(user, schoolIdOf(chapter), chapter.getSubjectClasses());
        return chapter;
    }

    private Lesson editableLesson(User user, int lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài học."));
        requireEdit(user, schoolIdOf(lesson.getChapter()), lesson.getChapter().getSubjectClasses());
        return lesson;
    }

    private static Integer schoolIdOf(Chapter chapter) {
        return chapter.getSchool() == null ? null : chapter.getSchool().getId();
    }

    private void renumberChapters(int subjectClassId, Integer schoolId) {
        List<Chapter> chapters = chapterRepository.findBySubjectClasses_IdAndSchool_IdOrderBySerialAscIdAsc(
                subjectClassId, schoolId);
        for (int i = 0; i < chapters.size(); i++) {
            chapters.get(i).setSerial(i + 1);
        }
        chapterRepository.saveAll(chapters);
    }

    private static <T> void swapWithNeighbour(List<T> items, int id, int direction,
                                              java.util.function.ToIntFunction<T> idOf) {
        for (int i = 0; i < items.size(); i++) {
            if (idOf.applyAsInt(items.get(i)) == id) {
                int target = direction < 0 ? i - 1 : i + 1;
                if (target >= 0 && target < items.size()) {
                    T item = items.get(i);
                    items.set(i, items.get(target));
                    items.set(target, item);
                }
                return;
            }
        }
    }

    private static String cleanName(String value, String field) {
        String cleaned = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException(field + " không được để trống.");
        }
        if (cleaned.length() > 255) {
            throw new IllegalArgumentException(field + " quá dài (tối đa 255 ký tự).");
        }
        return cleaned;
    }
}
