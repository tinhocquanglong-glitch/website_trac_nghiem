package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.Exam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Integer> {
    /** Exams whose primary lesson or curriculum scope includes the lesson. */
    @Query("SELECT COUNT(DISTINCT e) FROM Exam e LEFT JOIN e.scopeLessons s WHERE (e.status IS NULL OR e.status = 1) AND (e.lesson.id = :lessonId OR s.id = :lessonId)")
    long countCoveringLesson(@Param("lessonId") int lessonId);

    @Query("SELECT DISTINCT e FROM Exam e LEFT JOIN e.scopeLessons s WHERE (e.status IS NULL OR e.status = 1) AND (e.lesson.id IN :lessonIds OR s.id IN :lessonIds)")
    List<Exam> findCoveringLessons(@Param("lessonIds") java.util.Collection<Integer> lessonIds);

    @Query("SELECT e FROM Exam e WHERE e.lessonPlanLesson.id = :lessonPlanLessonId AND (e.status IS NULL OR e.status = 1)")
    List<Exam> findByLessonPlanLesson_Id(@Param("lessonPlanLessonId") Integer lessonPlanLessonId);

    @Query("SELECT e FROM Exam e WHERE (e.status IS NULL OR e.status = 1)")
    Page<Exam> findAllActive(Pageable pageable);

    @Query(value = "SELECT exam.* FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE (exam.status IS NULL OR exam.status = 1) AND subject_classes.subject_id = :id",
            countQuery = "SELECT COUNT(*) FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE (exam.status IS NULL OR exam.status = 1) AND subject_classes.subject_id = :id", nativeQuery = true)
    Page<Exam> findAllBySubject(@Param("id") int id, Pageable pageable);

    @Query(value = "SELECT exam.* FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE (exam.status IS NULL OR exam.status = 1) AND subject_classes.subject_id = :id", nativeQuery = true)
    List<Exam> findAllBySubject(@Param("id") int id);

    @Query(value = "select exam.* from exam_question join question on exam_question.ques_id = question.ques_id join exam on exam.id = exam_question.exam_id where (exam.status IS NULL OR exam.status = 1) AND exam_question.exam_id = :id",
            nativeQuery = true)
    Page<Exam> findAllByQuestions(@Param("id") int id, Pageable pageable);

    @Query(nativeQuery = true, value = "SELECT * FROM exam WHERE (status IS NULL OR status = 1) AND lesson_id = :id", countQuery = "SELECT COUNT(*) FROM exam WHERE (status IS NULL OR status = 1) AND lesson_id = :id")
    Page<Exam> findAllByLesson(int id, Pageable pageable);

    @Query("SELECT e FROM Exam e WHERE e.lesson.id = :lessonId AND (e.status IS NULL OR e.status = 1)")
    List<Exam> findByLessonId(@Param("lessonId") Integer lessonId);

    @Query("SELECT e FROM Exam e WHERE e.lessonPlanLesson.id = :lessonPlanLessonId AND (e.status IS NULL OR e.status = 1)")
    List<Exam> findByLessonPlanLessonId(@Param("lessonPlanLessonId") Integer lessonPlanLessonId);

    @Query("SELECT e FROM Exam e WHERE e.lessonPlanLesson.id = :lessonPlanLessonId AND (e.status IS NULL OR e.status = 1)")
    Page<Exam> findAllByLessonPlanLesson_Id(@Param("lessonPlanLessonId") Integer lessonPlanLessonId, Pageable pageable);

    @Query("SELECT e FROM Exam e WHERE e.nameExam LIKE %:name% AND e.lessonPlanLesson.id = :lessonPlanLessonId AND (e.status IS NULL OR e.status = 1)")
    Page<Exam> findByNameExamContainingAndLessonPlanLesson_Id(@Param("name") String name, @Param("lessonPlanLessonId") Integer lessonPlanLessonId, Pageable pageable);

    @Query("SELECT e FROM Exam e WHERE e.nameExam LIKE %:name% AND (e.status IS NULL OR e.status = 1)")
    Page<Exam> findAllByNameExamContaining(@Param("name") String name, Pageable pageable);

    @Query(value = "SELECT exam.* FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE (exam.status IS NULL OR exam.status = 1) AND subject_classes.subject_id = :id and exam.name_exam like %:name%",
            countQuery = "SELECT COUNT(exam.*) FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE (exam.status IS NULL OR exam.status = 1) AND subject_classes.subject_id = :id and exam.name_exam like %:name%",
            nativeQuery = true)
    Page<Exam> findAllBySubjectAndNameExamContaining(@Param("id") int id, @Param("name") String name, Pageable pageable);

    @Query(nativeQuery = true, value = "SELECT * FROM exam WHERE (status IS NULL OR status = 1) AND lesson_id = :lessonId AND name_exam like %:name%",
            countQuery = "SELECT COUNT(*) FROM exam WHERE (status IS NULL OR status = 1) AND lesson_id = :lessonId AND name_exam like %:name%")
    Page<Exam> findByNameExamContainingAndLesson(@Param("lessonId") int lessonId, @Param("name") String name, Pageable pageable);

    // Lọc theo giáo viên tạo (teacher ownership)
    @Query(nativeQuery = true,
        value = "SELECT * FROM exam WHERE (status IS NULL OR status = 1) AND users_id = :userId",
        countQuery = "SELECT COUNT(*) FROM exam WHERE (status IS NULL OR status = 1) AND users_id = :userId")
    Page<Exam> findByUsersId(@Param("userId") String userId, Pageable pageable);

    @Query(nativeQuery = true,
        value = "SELECT exam.* FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE (exam.status IS NULL OR exam.status = 1) AND subject_classes.subject_id = :subjectId AND exam.users_id = :userId",
        countQuery = "SELECT COUNT(*) FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE (exam.status IS NULL OR exam.status = 1) AND subject_classes.subject_id = :subjectId AND exam.users_id = :userId")
    Page<Exam> findByUsersIdAndSubject(@Param("userId") String userId, @Param("subjectId") int subjectId, Pageable pageable);

    @Query(nativeQuery = true,
        value = "SELECT * FROM exam WHERE (status IS NULL OR status = 1) AND users_id = :userId AND name_exam LIKE %:name%",
        countQuery = "SELECT COUNT(*) FROM exam WHERE (status IS NULL OR status = 1) AND users_id = :userId AND name_exam LIKE %:name%")
    Page<Exam> findByUsersIdAndNameExamContaining(@Param("userId") String userId, @Param("name") String name, Pageable pageable);

    @Query(nativeQuery = true,
        value = "SELECT exam.* FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE (exam.status IS NULL OR exam.status = 1) AND subject_classes.subject_id = :subjectId AND exam.users_id = :userId AND exam.name_exam LIKE %:name%",
        countQuery = "SELECT COUNT(*) FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE (exam.status IS NULL OR exam.status = 1) AND subject_classes.subject_id = :subjectId AND exam.users_id = :userId AND exam.name_exam LIKE %:name%")
    Page<Exam> findByUsersIdAndSubjectAndNameExamContaining(@Param("userId") String userId, @Param("subjectId") int subjectId, @Param("name") String name, Pageable pageable);
}
