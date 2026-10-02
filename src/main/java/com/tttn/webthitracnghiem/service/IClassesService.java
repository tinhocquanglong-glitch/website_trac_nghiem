package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.Classes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IClassesService {
    Iterable<Classes> findAll();
    Page<Classes> findAll(Pageable pageable);
    Classes findById(int id);
    void save(Classes classes);
    void remove(Classes classes);
    List<Classes> findAllBySubject(int id);
}
