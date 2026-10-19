package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.LessonPlan;
import com.tttn.webthitracnghiem.model.LessonPlanLesson;
import com.tttn.webthitracnghiem.model.LessonPlanRequest;
import com.tttn.webthitracnghiem.model.Classes;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.model.User;

import java.util.List;

public interface ILessonPlanService {
    List<LessonPlan> findVisible(String userId, boolean privileged);
    List<LessonPlan> findVisible(String userId, boolean privileged, String query);
    List<LessonPlan> findVisible(String userId, boolean privileged, String query,
                                 Integer subjectId, Integer gradeId, Integer classId,
                                 String teacherName, boolean mine, String visibility);
    List<LessonPlan> findVisibleForStudent(String userId, Integer subjectId,
                                           Integer gradeId, Integer targetClassId);
    LessonPlan findById(int id);
    LessonPlanLesson findLesson(int id);
    LessonPlan save(LessonPlanRequest request, User owner);
    void delete(int id, String ownerId);
    boolean canView(LessonPlan plan, String userId, boolean privileged);
    List<Subject> findSubjects();
    List<Classes> findGrades(User user, boolean privileged);
    List<StudentTrackingClass> findTargetClasses(User user, boolean privileged, Integer gradeId);
    List<SubjectClasses> findCurricula();
}
