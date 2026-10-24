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
    long countByLesson_Id(int lessonId);

    List<Question> findByLesson_IdIn(java.util.Collection<Integer> lessonIds);

    @Query("select q from Question q left join q.classes c "
            + "where q.subject.id = :subjectId "
            + "and (c.id = :classId or c.id is null) "
            + "and q.lesson is null and q.lessonPlanLesson is null")
    List<Question> findBySubjectAndGradeWithoutLesson(@Param("subjectId") Integer subjectId,
                                                       @Param("classId") Integer classId);

    List<Question> findByReviewStatusAndSchool_IdOrderByQuesIdDesc(String reviewStatus, Integer schoolId);

    List<Question> findByReviewStatusOrderByQuesIdDesc(String reviewStatus);

    long countByReviewStatusAndSchool_Id(String reviewStatus, Integer schoolId);

    long countByReviewStatus(String reviewStatus);

    @Query("select distinct q from Question q "
            + "left join fetch q.subject qsubject "
            + "left join fetch q.classes qclass "
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

    @Query(value = "select distinct q.* from question q "
            + "left join lesson l on q.lesson_id = l.id "
            + "left join chapter c on c.id = l.chapter_id "
            + "left join subject_classes sc on sc.id = c.subject_class_id "
            + "left join lesson_plan_custom_lesson pll on q.lesson_plan_lesson_id = pll.id "
            + "left join lesson_plan_custom_chapter plc on plc.id = pll.chapter_id "
            + "left join lesson_plan lp on lp.id = plc.lesson_plan_id "
            + "where sc.subject_id = :id or lp.subject_id = :id or q.subject_id = :id",
            countQuery = "select count(distinct q.ques_id) from question q "
                    + "left join lesson l on q.lesson_id = l.id "
                    + "left join chapter c on c.id = l.chapter_id "
                    + "left join subject_classes sc on sc.id = c.subject_class_id "
                    + "left join lesson_plan_custom_lesson pll on q.lesson_plan_lesson_id = pll.id "
                    + "left join lesson_plan_custom_chapter plc on plc.id = pll.chapter_id "
                    + "left join lesson_plan lp on lp.id = plc.lesson_plan_id "
                    + "where sc.subject_id = :id or lp.subject_id = :id or q.subject_id = :id", nativeQuery = true)
    Page<Question> findAllBySubject(@Param("id") Optional<Integer> id, Pageable pageable);

    Page<Question> findAllByTitleContaining(String title, Pageable pageable);

    @Query(value = "select distinct q.* from question q "
            + "left join lesson l on q.lesson_id = l.id "
            + "left join chapter c on c.id = l.chapter_id "
            + "left join subject_classes sc on sc.id = c.subject_class_id "
            + "left join lesson_plan_custom_lesson pll on q.lesson_plan_lesson_id = pll.id "
            + "left join lesson_plan_custom_chapter plc on plc.id = pll.chapter_id "
            + "left join lesson_plan lp on lp.id = plc.lesson_plan_id "
            + "where (sc.subject_id = :id or lp.subject_id = :id or q.subject_id = :id) and q.title like concat('%', :name, '%')",
            countQuery = "select count(distinct q.ques_id) from question q "
                    + "left join lesson l on q.lesson_id = l.id "
                    + "left join chapter c on c.id = l.chapter_id "
                    + "left join subject_classes sc on sc.id = c.subject_class_id "
                    + "left join lesson_plan_custom_lesson pll on q.lesson_plan_lesson_id = pll.id "
                    + "left join lesson_plan_custom_chapter plc on plc.id = pll.chapter_id "
                    + "left join lesson_plan lp on lp.id = plc.lesson_plan_id "
                    + "where (sc.subject_id = :id or lp.subject_id = :id or q.subject_id = :id) and q.title like concat('%', :name, '%')",
            nativeQuery = true)
    Page<Question> findAllBySubjectAndTitleContaining(@Param("id") int id, @Param("name") String name, Pageable pageable);
    @Query(nativeQuery = true,value = "SELECT * FROM question WHERE lesson_id = :id")
    List<Question> findByLesson(int id);
    List<Question> findByLessonPlanLesson_Id(Integer id);
    @Query(nativeQuery = true,value = "SELECT * FROM question JOIN exam_question ON exam_question.ques_id = question.ques_id WHERE exam_question.exam_id = :examId")
    List<Question> findByExam(@Param("examId") int examId);
    @Query(nativeQuery = true,value = "SELECT DISTINCT q.* FROM question q "
            + "LEFT JOIN lesson l ON l.id = q.lesson_id "
            + "LEFT JOIN chapter c ON c.id = l.chapter_id "
            + "LEFT JOIN subject_classes sc ON sc.id = c.subject_class_id "
            + "LEFT JOIN lesson_plan_custom_lesson pll ON pll.id = q.lesson_plan_lesson_id "
            + "LEFT JOIN lesson_plan_custom_chapter plc ON plc.id = pll.chapter_id "
            + "LEFT JOIN lesson_plan lp ON lp.id = plc.lesson_plan_id "
            + "WHERE (sc.subject_id = :subId AND sc.class_id = :classId) "
            + "OR (lp.subject_id = :subId AND lp.grade_id = :classId) "
            + "OR (q.subject_id = :subId AND (q.class_id = :classId OR q.class_id IS NULL))")
    List<Question> findBySubAndClass(@Param("subId") int subId, @Param("classId") int classId);
    @Query(nativeQuery = true,value = "SELECT DISTINCT q.* FROM question q "
            + "LEFT JOIN lesson l ON l.id = q.lesson_id "
            + "LEFT JOIN chapter c ON c.id = l.chapter_id "
            + "LEFT JOIN subject_classes sc ON sc.id = c.subject_class_id "
            + "LEFT JOIN lesson_plan_custom_lesson pll ON pll.id = q.lesson_plan_lesson_id "
            + "LEFT JOIN lesson_plan_custom_chapter plc ON plc.id = pll.chapter_id "
            + "LEFT JOIN lesson_plan lp ON lp.id = plc.lesson_plan_id "
            + "WHERE sc.subject_id = :subId OR lp.subject_id = :subId OR q.subject_id = :subId")
    List<Question> findBySubject(@Param("subId") int subId);

    // Lọc câu hỏi cho giáo viên: câu hỏi công khai HOẶC của cùng trường
    @Query(value = "select distinct q.* from question q "
            + "left join lesson l on q.lesson_id = l.id "
            + "left join chapter c on c.id = l.chapter_id "
            + "left join subject_classes sc on sc.id = c.subject_class_id "
            + "left join lesson_plan_custom_lesson pll on q.lesson_plan_lesson_id = pll.id "
            + "left join lesson_plan_custom_chapter plc on plc.id = pll.chapter_id "
            + "left join lesson_plan lp on lp.id = plc.lesson_plan_id "
            + "where (sc.subject_id = :id or lp.subject_id = :id or q.subject_id = :id) "
            + "and (q.is_public = true or q.is_public is null or q.school_id = :schoolId)",
            countQuery = "select count(distinct q.ques_id) from question q "
                    + "left join lesson l on q.lesson_id = l.id "
                    + "left join chapter c on c.id = l.chapter_id "
                    + "left join subject_classes sc on sc.id = c.subject_class_id "
                    + "left join lesson_plan_custom_lesson pll on q.lesson_plan_lesson_id = pll.id "
                    + "left join lesson_plan_custom_chapter plc on plc.id = pll.chapter_id "
                    + "left join lesson_plan lp on lp.id = plc.lesson_plan_id "
                    + "where (sc.subject_id = :id or lp.subject_id = :id or q.subject_id = :id) "
                    + "and (q.is_public = true or q.is_public is null or q.school_id = :schoolId)", nativeQuery = true)
    Page<Question> findAllBySubjectAndSchool(@Param("id") Optional<Integer> id, @Param("schoolId") int schoolId, Pageable pageable);

    @Query(value = "select distinct q.* from question q "
            + "left join lesson l on q.lesson_id = l.id "
            + "left join chapter c on c.id = l.chapter_id "
            + "left join subject_classes sc on sc.id = c.subject_class_id "
            + "left join lesson_plan_custom_lesson pll on q.lesson_plan_lesson_id = pll.id "
            + "left join lesson_plan_custom_chapter plc on plc.id = pll.chapter_id "
            + "left join lesson_plan lp on lp.id = plc.lesson_plan_id "
            + "where q.is_public = true or q.is_public is null or q.school_id = :schoolId",
            countQuery = "select count(distinct q.ques_id) from question q "
                    + "where q.is_public = true or q.is_public is null or q.school_id = :schoolId", nativeQuery = true)
    Page<Question> findAllBySchool(@Param("schoolId") int schoolId, Pageable pageable);

    @Query(value = "select distinct q.* from question q "
            + "where q.title like concat('%', :name, '%') "
            + "and (q.is_public = true or q.is_public is null or q.school_id = :schoolId)",
            countQuery = "select count(distinct q.ques_id) from question q "
                    + "where q.title like concat('%', :name, '%') "
                    + "and (q.is_public = true or q.is_public is null or q.school_id = :schoolId)", nativeQuery = true)
    Page<Question> findAllByTitleContainingAndSchool(@Param("name") String name, @Param("schoolId") int schoolId, Pageable pageable);

    @Query(value = "select distinct q.* from question q "
            + "left join lesson l on q.lesson_id = l.id "
            + "left join chapter c on c.id = l.chapter_id "
            + "left join subject_classes sc on sc.id = c.subject_class_id "
            + "left join lesson_plan_custom_lesson pll on q.lesson_plan_lesson_id = pll.id "
            + "left join lesson_plan_custom_chapter plc on plc.id = pll.chapter_id "
            + "left join lesson_plan lp on lp.id = plc.lesson_plan_id "
            + "where (sc.subject_id = :id or lp.subject_id = :id or q.subject_id = :id) "
            + "and q.title like concat('%', :name, '%') "
            + "and (q.is_public = true or q.is_public is null or q.school_id = :schoolId)",
            countQuery = "select count(distinct q.ques_id) from question q "
                    + "left join lesson l on q.lesson_id = l.id "
                    + "left join chapter c on c.id = l.chapter_id "
                    + "left join subject_classes sc on sc.id = c.subject_class_id "
                    + "left join lesson_plan_custom_lesson pll on q.lesson_plan_lesson_id = pll.id "
                    + "left join lesson_plan_custom_chapter plc on plc.id = pll.chapter_id "
                    + "left join lesson_plan lp on lp.id = plc.lesson_plan_id "
                    + "where (sc.subject_id = :id or lp.subject_id = :id or q.subject_id = :id) "
                    + "and q.title like concat('%', :name, '%') "
                    + "and (q.is_public = true or q.is_public is null or q.school_id = :schoolId)",
            nativeQuery = true)
    Page<Question> findAllBySubjectAndTitleContainingAndSchool(@Param("id") int id, @Param("name") String name, @Param("schoolId") int schoolId, Pageable pageable);

    @Query(value = "select distinct q.* from question q "
            + "left join lesson l on q.lesson_id = l.id "
            + "left join chapter c on c.id = l.chapter_id "
            + "left join subject_classes sc on sc.id = c.subject_class_id "
            + "left join lesson_plan_custom_lesson pll on q.lesson_plan_lesson_id = pll.id "
            + "left join lesson_plan_custom_chapter plc on plc.id = pll.chapter_id "
            + "left join lesson_plan lp on lp.id = plc.lesson_plan_id "
            + "where (:subjectId is null or sc.subject_id = :subjectId or lp.subject_id = :subjectId or q.subject_id = :subjectId) "
            + "and (:classId is null or sc.class_id = :classId or lp.grade_id = :classId or q.class_id = :classId) "
            + "and (:chapterId is null or c.id = :chapterId or plc.id = :chapterId) "
            + "and (:title is null or q.title like concat('%', :title, '%'))",
            countQuery = "select count(distinct q.ques_id) from question q "
            + "left join lesson l on q.lesson_id = l.id "
            + "left join chapter c on c.id = l.chapter_id "
            + "left join subject_classes sc on sc.id = c.subject_class_id "
            + "left join lesson_plan_custom_lesson pll on q.lesson_plan_lesson_id = pll.id "
            + "left join lesson_plan_custom_chapter plc on plc.id = pll.chapter_id "
            + "left join lesson_plan lp on lp.id = plc.lesson_plan_id "
            + "where (:subjectId is null or sc.subject_id = :subjectId or lp.subject_id = :subjectId or q.subject_id = :subjectId) "
            + "and (:classId is null or sc.class_id = :classId or lp.grade_id = :classId or q.class_id = :classId) "
            + "and (:chapterId is null or c.id = :chapterId or plc.id = :chapterId) "
            + "and (:title is null or q.title like concat('%', :title, '%'))",
            nativeQuery = true)
    Page<Question> findByFilters(@Param("subjectId") Integer subjectId, @Param("classId") Integer classId,
                                 @Param("chapterId") Integer chapterId, @Param("title") String title,
                                 Pageable pageable);

    @Query(value = "select distinct q.* from question q "
            + "left join lesson l on q.lesson_id = l.id "
            + "left join chapter c on c.id = l.chapter_id "
            + "left join subject_classes sc on sc.id = c.subject_class_id "
            + "left join lesson_plan_custom_lesson pll on q.lesson_plan_lesson_id = pll.id "
            + "left join lesson_plan_custom_chapter plc on plc.id = pll.chapter_id "
            + "left join lesson_plan lp on lp.id = plc.lesson_plan_id "
            + "where (:subjectId is null or sc.subject_id = :subjectId or lp.subject_id = :subjectId or q.subject_id = :subjectId) "
            + "and (:classId is null or sc.class_id = :classId or lp.grade_id = :classId or q.class_id = :classId) "
            + "and (:chapterId is null or c.id = :chapterId or plc.id = :chapterId) "
            + "and (:title is null or q.title like concat('%', :title, '%')) "
            + "and (q.is_public = true or q.is_public is null or q.school_id = :schoolId)",
            countQuery = "select count(distinct q.ques_id) from question q "
            + "left join lesson l on q.lesson_id = l.id "
            + "left join chapter c on c.id = l.chapter_id "
            + "left join subject_classes sc on sc.id = c.subject_class_id "
            + "left join lesson_plan_custom_lesson pll on q.lesson_plan_lesson_id = pll.id "
            + "left join lesson_plan_custom_chapter plc on plc.id = pll.chapter_id "
            + "left join lesson_plan lp on lp.id = plc.lesson_plan_id "
            + "where (:subjectId is null or sc.subject_id = :subjectId or lp.subject_id = :subjectId or q.subject_id = :subjectId) "
            + "and (:classId is null or sc.class_id = :classId or lp.grade_id = :classId or q.class_id = :classId) "
            + "and (:chapterId is null or c.id = :chapterId or plc.id = :chapterId) "
            + "and (:title is null or q.title like concat('%', :title, '%')) "
            + "and (q.is_public = true or q.is_public is null or q.school_id = :schoolId)",
            nativeQuery = true)
    Page<Question> findByFiltersAndSchool(@Param("subjectId") Integer subjectId, @Param("classId") Integer classId,
                                           @Param("chapterId") Integer chapterId, @Param("title") String title,
                                           @Param("schoolId") int schoolId, Pageable pageable);
}
