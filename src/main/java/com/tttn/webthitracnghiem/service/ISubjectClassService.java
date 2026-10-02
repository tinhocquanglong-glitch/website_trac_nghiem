package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.SubjectClasses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ISubjectClassService {
    Page<SubjectClasses> findAll(Pageable pageable);
    SubjectClasses findById(int id);
    Page<SubjectClasses> findByClassId(int id, Pageable pageable);
    SubjectClasses save(SubjectClasses subjectClasses);
    List<SubjectClasses> findByClassId(int id);
    SubjectClasses findByClassAndSubject(int classId, int subjectId);
    void remove(SubjectClasses subjectClasses);
    List<SubjectClasses> findBySubject(int id);
}
