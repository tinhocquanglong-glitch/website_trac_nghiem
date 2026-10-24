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
    @Query("SELECT COUNT(DISTINCT e) FROM Exam e LEFT JOIN e.scopeLessons s WHERE e.lesson.id = :lessonId OR s.id = :lessonId")
    long countCoveringLesson(@Param("lessonId") int lessonId);

    @Query("SELECT DISTINCT e FROM Exam e LEFT JOIN e.scopeLessons s WHERE e.lesson.id IN :lessonIds OR s.id IN :lessonIds")
    List<Exam> findCoveringLessons(@Param("lessonIds") java.util.Collection<Integer> lessonIds);

    List<Exam> findByLessonPlanLesson_Id(Integer lessonPlanLessonId);

    @Query(value = "SELECT exam.* FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE subject_classes.subject_id = :id",
            countQuery = "SELECT COUNT(*) FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE subject_classes.subject_id = :id",nativeQuery = true)
    Page<Exam> findAllBySubject(@Param("id") int id, Pageable pageable);
    @Query(value = "SELECT exam.* FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE subject_classes.subject_id = :id",nativeQuery = true)
    List<Exam> findAllBySubject(@Param("id") int id);
    @Query(value = "select * from exam_question join question on exam_question.ques_id = question.ques_id where exam_question.exam_id = :id",
            nativeQuery = true)
    Page<Exam> findAllByQuestions(@Param("id") int id, Pageable pageable);
    @Query(nativeQuery = true,value = "SELECT * FROM exam WHERE lesson_id = :id",countQuery = "SELECT COUNT(*) FROM exam WHERE lesson_id = :id")
    Page<Exam> findAllByLesson(int id,Pageable pageable);
    List<Exam> findByLessonId(Integer lessonId);
    List<Exam> findByLessonPlanLessonId(Integer lessonPlanLessonId);
    Page<Exam> findAllByLessonPlanLesson_Id(Integer lessonPlanLessonId, Pageable pageable);
    Page<Exam> findByNameExamContainingAndLessonPlanLesson_Id(String name, Integer lessonPlanLessonId,
                                                               Pageable pageable);
    Page<Exam> findAllByNameExamContaining(String name, Pageable pageable);
    @Query(value = "SELECT exam.* FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE subject_classes.subject_id = :id and exam.name_exam like %:name%",
            countQuery = "SELECT COUNT(exam.*) FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE subject_classes.subject_id = :id and exam.name_exam like %:name%",
            nativeQuery = true)
    Page<Exam> findAllBySubjectAndNameExamContaining(@Param("id") int id, @Param("name") String name,Pageable pageable);
    @Query(nativeQuery = true,value = "SELECT * FROM exam WHERE lesson_id = :lessonId AND name_exam like %:name%",
    countQuery = "SELECT COUNT(*) FROM exam WHERE lesson_id = :lessonId AND name_exam like %:name%")
    Page<Exam> findByNameExamContainingAndLesson(@Param("lessonId") int lessonId,@Param("name") String name, Pageable pageable);

    // Lọc theo giáo viên tạo (teacher ownership)
    @Query(nativeQuery = true,
        value = "SELECT * FROM exam WHERE users_id = :userId",
        countQuery = "SELECT COUNT(*) FROM exam WHERE users_id = :userId")
    Page<Exam> findByUsersId(@Param("userId") String userId, Pageable pageable);

    @Query(nativeQuery = true,
        value = "SELECT exam.* FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE subject_classes.subject_id = :subjectId AND exam.users_id = :userId",
        countQuery = "SELECT COUNT(*) FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE subject_classes.subject_id = :subjectId AND exam.users_id = :userId")
    Page<Exam> findByUsersIdAndSubject(@Param("userId") String userId, @Param("subjectId") int subjectId, Pageable pageable);

    @Query(nativeQuery = true,
        value = "SELECT * FROM exam WHERE users_id = :userId AND name_exam LIKE %:name%",
        countQuery = "SELECT COUNT(*) FROM exam WHERE users_id = :userId AND name_exam LIKE %:name%")
    Page<Exam> findByUsersIdAndNameExamContaining(@Param("userId") String userId, @Param("name") String name, Pageable pageable);

    @Query(nativeQuery = true,
        value = "SELECT exam.* FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE subject_classes.subject_id = :subjectId AND exam.users_id = :userId AND exam.name_exam LIKE %:name%",
        countQuery = "SELECT COUNT(*) FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE subject_classes.subject_id = :subjectId AND exam.users_id = :userId AND exam.name_exam LIKE %:name%")
    Page<Exam> findByUsersIdAndSubjectAndNameExamContaining(@Param("userId") String userId, @Param("subjectId") int subjectId, @Param("name") String name, Pageable pageable);
}
