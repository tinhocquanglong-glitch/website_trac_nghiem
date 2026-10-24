package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.SubjectRequest;
import com.tttn.webthitracnghiem.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ISubjectService {
    Iterable<Subject> findAll();
    List<Subject> getAll();
    Page<Subject> findAll(Pageable pageable);
    Page<Subject> findByNameSubject(String name, Pageable pageable);
    Subject findById(int id);
    Subject save(SubjectRequest subjectRequest);
    Subject save(SubjectRequest subjectRequest, School school);
    void remove(Subject subject);

    Page<Subject> findBySchoolOrGlobal(Integer schoolId, Pageable pageable);
    Page<Subject> findByNameAndSchoolOrGlobal(String name, Integer schoolId, Pageable pageable);
    List<Subject> getBySchoolOrGlobal(Integer schoolId);
    Subject copyToSchool(int subjectId, School school);
    boolean canManage(Subject subject, User currentUser, boolean isAdmin);
}
