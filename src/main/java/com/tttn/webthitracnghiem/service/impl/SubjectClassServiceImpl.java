package com.tttn.webthitracnghiem.service.impl;

import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.repository.SubjectClassRepository;
import com.tttn.webthitracnghiem.service.ISubjectClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubjectClassServiceImpl implements ISubjectClassService {
    @Autowired
    private SubjectClassRepository subjectClassRepository;
    @Override
    public Page<SubjectClasses> findAll(Pageable pageable) {
        return subjectClassRepository.findAll(pageable);
    }

    @Override
    public SubjectClasses findById(int id) {
        return subjectClassRepository.findById(id).orElse(null);
    }
    @Override
    public Page<SubjectClasses> findByClassId(int id, Pageable pageable) {
        return subjectClassRepository.findByClass(id,pageable);
    }

    @Override
    public SubjectClasses save(SubjectClasses subjectClasses) {
        return subjectClassRepository.save(subjectClasses);
    }

    @Override
    public List<SubjectClasses> findByClassId(int id) {
        return subjectClassRepository.findByClassId(id);
    }

    @Override
    public SubjectClasses findByClassAndSubject(int classId, int subjectId) {
        return subjectClassRepository.findByClassesAndSubject(classId,subjectId).orElse(new SubjectClasses());
    }

    @Override
    public void remove(SubjectClasses subjectClasses) {
        subjectClassRepository.delete(subjectClasses);
    }

    @Override
    public List<SubjectClasses> findBySubject(int id) {
        return subjectClassRepository.findBySubject(id);
    }
}
