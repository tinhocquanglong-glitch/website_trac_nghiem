package com.tttn.webthitracnghiem.service.impl;

import com.tttn.webthitracnghiem.model.Classes;
import com.tttn.webthitracnghiem.repository.ClassesRepository;
import com.tttn.webthitracnghiem.service.IClassesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassesServiceImpl implements IClassesService {
    @Autowired
    private ClassesRepository classesRepository;

    @Override
    public Iterable<Classes> findAll() {
        return classesRepository.findAll();
    }

    @Override
    public Page<Classes> findAll(Pageable pageable) {
        return classesRepository.findAll(pageable);
    }

    @Override
    public Classes findById(int id) {
        return classesRepository.findById(id).orElse(null);
    }

    @Override
    public void save(Classes classes) {
        classesRepository.save(classes);
    }

    @Override
    public void remove(Classes classes) {
        classesRepository.delete(classes);
    }

    @Override
    public List<Classes> findAllBySubject(int id) {
        return classesRepository.findAllBySubjectClasses(id);
    }
}
