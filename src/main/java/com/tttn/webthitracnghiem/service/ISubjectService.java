package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.SubjectRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface ISubjectService {
    Iterable<Subject> findAll();
    List<Subject> getAll();
    Page<Subject> findAll(Pageable pageable);
    Page<Subject> findByNameSubject(String name, Pageable pageable);
    Subject findById(int id);
    Subject save(SubjectRequest subjectRequest);
    void remove(Subject subject);
}
