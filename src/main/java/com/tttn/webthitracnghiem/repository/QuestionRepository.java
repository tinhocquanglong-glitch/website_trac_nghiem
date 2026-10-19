package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    @Query("select distinct q from Question q "
            + "left join fetch q.lesson l "
            + "left join fetch l.chapter lc "
            + "left join fetch lc.subjectClasses lsc "
            + "left join fetch lsc.subject lsubject "
            + "left join fetch lsc.classes lclass "
            + "left join fetch q.lessonPlanLesson pl "
            + "left join fetch pl.chapter pc "
            + "left join fetch pc.lessonPlan plan "
            + "left join fetch plan.subject psubject "
            + "left join fetch plan.grade pgrade")
    List<Question> findAllWithContext();

    @Query(value = "SELECT * FROM question join exam_question where question.ques_id = exam_question.ques_id and exam_question.exam_id = :id ", nativeQuery = true)
    List<Question> findAllId(@Param("id") int i);

    @Query(value = "SELECT * FROM question join exam where question.exams_id = exam.id and exam.id = :id",
            nativeQuery = true)
    Page<Question> findAllByExams(@Param("id") int id, Pageable pageable);

    @Query(value = "select question.* from question join lesson on question.lesson_id = lesson.id join chapter on chapter.id = lesson.chapter_id join subject_classes on subject_classes.id = chapter.subject_class_id where subject_classes.subject_id = :id",
            countQuery = "SELECT COUNT(*) FROM exam JOIN lesson ON lesson.id = exam.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE subject_classes.subject_id = :id",nativeQuery = true)
    Page<Question> findAllBySubject(@Param("id") Optional<Integer> id, Pageable pageable);

    Page<Question> findAllByTitleContaining(String title, Pageable pageable);

    @Query(value = "select question.* from question join lesson on question.lesson_id = lesson.id join chapter on chapter.id = lesson.chapter_id join subject_classes on subject_classes.id = chapter.subject_class_id where subject_classes.subject_id = :id and question.title like %:name%",
            countQuery = "select COUNT(*) from question join lesson on question.lesson_id = lesson.id join chapter on chapter.id = lesson.chapter_id join subject_classes on subject_classes.id = chapter.subject_class_id where subject_classes.subject_id = :id and question.title like %:name%",
            nativeQuery = true)
    Page<Question> findAllBySubjectAndTitleContaining(@Param("id") int id, @Param("name") String name, Pageable pageable);
    @Query(nativeQuery = true,value = "SELECT * FROM question WHERE lesson_id = :id")
    List<Question> findByLesson(int id);
    List<Question> findByLessonPlanLesson_Id(Integer id);
    @Query(nativeQuery = true,value = "SELECT * FROM question JOIN exam_question ON exam_question.ques_id = question.ques_id WHERE exam_question.exam_id = :examId")
    List<Question> findByExam(@Param("examId") int examId);
    @Query(nativeQuery = true,value = "SELECT * FROM question JOIN lesson ON lesson.id = question.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE subject_classes.subject_id = :subId AND subject_classes.class_id = :classId")
    List<Question> findBySubAndClass(@Param("subId") int subId, @Param("classId") int classId);
    @Query(nativeQuery = true,value = "SELECT * FROM question JOIN lesson ON lesson.id = question.lesson_id JOIN chapter ON chapter.id = lesson.chapter_id JOIN subject_classes ON subject_classes.id = chapter.subject_class_id WHERE subject_classes.subject_id = :subId")
    List<Question> findBySubject(@Param("subId") int subId);

    // Lọc câu hỏi cho giáo viên: câu hỏi công khai HOẶC của cùng trường
    @Query(value = "select question.* from question join lesson on question.lesson_id = lesson.id join chapter on chapter.id = lesson.chapter_id join subject_classes on subject_classes.id = chapter.subject_class_id where subject_classes.subject_id = :id AND (question.is_public = true OR question.school_id = :schoolId)",
            countQuery = "SELECT COUNT(*) FROM question join lesson on question.lesson_id = lesson.id join chapter on chapter.id = lesson.chapter_id join subject_classes on subject_classes.id = chapter.subject_class_id WHERE subject_classes.subject_id = :id AND (question.is_public = true OR question.school_id = :schoolId)",nativeQuery = true)
    Page<Question> findAllBySubjectAndSchool(@Param("id") Optional<Integer> id, @Param("schoolId") int schoolId, Pageable pageable);

    @Query(value = "select question.* from question join lesson on question.lesson_id = lesson.id join chapter on chapter.id = lesson.chapter_id join subject_classes on subject_classes.id = chapter.subject_class_id where (question.is_public = true OR question.school_id = :schoolId)",
            countQuery = "SELECT COUNT(*) FROM question join lesson on question.lesson_id = lesson.id join chapter on chapter.id = lesson.chapter_id join subject_classes on subject_classes.id = chapter.subject_class_id WHERE (question.is_public = true OR question.school_id = :schoolId)",nativeQuery = true)
    Page<Question> findAllBySchool(@Param("schoolId") int schoolId, Pageable pageable);

    @Query(value = "select question.* from question join lesson on question.lesson_id = lesson.id join chapter on chapter.id = lesson.chapter_id join subject_classes on subject_classes.id = chapter.subject_class_id where question.title like %:name% AND (question.is_public = true OR question.school_id = :schoolId)",
            countQuery = "SELECT COUNT(*) FROM question join lesson on question.lesson_id = lesson.id join chapter on chapter.id = lesson.chapter_id join subject_classes on subject_classes.id = chapter.subject_class_id WHERE question.title like %:name% AND (question.is_public = true OR question.school_id = :schoolId)",nativeQuery = true)
    Page<Question> findAllByTitleContainingAndSchool(@Param("name") String name, @Param("schoolId") int schoolId, Pageable pageable);

    @Query(value = "select question.* from question join lesson on question.lesson_id = lesson.id join chapter on chapter.id = lesson.chapter_id join subject_classes on subject_classes.id = chapter.subject_class_id where subject_classes.subject_id = :id and question.title like %:name% AND (question.is_public = true OR question.school_id = :schoolId)",
            countQuery = "select COUNT(*) from question join lesson on question.lesson_id = lesson.id join chapter on chapter.id = lesson.chapter_id join subject_classes on subject_classes.id = chapter.subject_class_id where subject_classes.subject_id = :id and question.title like %:name% AND (question.is_public = true OR question.school_id = :schoolId)",
            nativeQuery = true)
    Page<Question> findAllBySubjectAndTitleContainingAndSchool(@Param("id") int id, @Param("name") String name, @Param("schoolId") int schoolId, Pageable pageable);

    @Query(value = "select question.* from question "
            + "join lesson on question.lesson_id = lesson.id "
            + "join chapter on chapter.id = lesson.chapter_id "
            + "join subject_classes on subject_classes.id = chapter.subject_class_id "
            + "where (:subjectId is null or subject_classes.subject_id = :subjectId) "
            + "and (:classId is null or subject_classes.class_id = :classId) "
            + "and (:chapterId is null or chapter.id = :chapterId) "
            + "and (:title is null or question.title like concat('%', :title, '%'))",
            countQuery = "select count(*) from question "
            + "join lesson on question.lesson_id = lesson.id "
            + "join chapter on chapter.id = lesson.chapter_id "
            + "join subject_classes on subject_classes.id = chapter.subject_class_id "
            + "where (:subjectId is null or subject_classes.subject_id = :subjectId) "
            + "and (:classId is null or subject_classes.class_id = :classId) "
            + "and (:chapterId is null or chapter.id = :chapterId) "
            + "and (:title is null or question.title like concat('%', :title, '%'))",
            nativeQuery = true)
    Page<Question> findByFilters(@Param("subjectId") Integer subjectId, @Param("classId") Integer classId,
                                 @Param("chapterId") Integer chapterId, @Param("title") String title,
                                 Pageable pageable);

    @Query(value = "select question.* from question "
            + "join lesson on question.lesson_id = lesson.id "
            + "join chapter on chapter.id = lesson.chapter_id "
            + "join subject_classes on subject_classes.id = chapter.subject_class_id "
            + "where (:subjectId is null or subject_classes.subject_id = :subjectId) "
            + "and (:classId is null or subject_classes.class_id = :classId) "
            + "and (:chapterId is null or chapter.id = :chapterId) "
            + "and (:title is null or question.title like concat('%', :title, '%')) "
            + "and (question.is_public = true or question.school_id = :schoolId)",
            countQuery = "select count(*) from question "
            + "join lesson on question.lesson_id = lesson.id "
            + "join chapter on chapter.id = lesson.chapter_id "
            + "join subject_classes on subject_classes.id = chapter.subject_class_id "
            + "where (:subjectId is null or subject_classes.subject_id = :subjectId) "
            + "and (:classId is null or subject_classes.class_id = :classId) "
            + "and (:chapterId is null or chapter.id = :chapterId) "
            + "and (:title is null or question.title like concat('%', :title, '%')) "
            + "and (question.is_public = true or question.school_id = :schoolId)",
            nativeQuery = true)
    Page<Question> findByFiltersAndSchool(@Param("subjectId") Integer subjectId, @Param("classId") Integer classId,
                                           @Param("chapterId") Integer chapterId, @Param("title") String title,
                                           @Param("schoolId") int schoolId, Pageable pageable);
}
