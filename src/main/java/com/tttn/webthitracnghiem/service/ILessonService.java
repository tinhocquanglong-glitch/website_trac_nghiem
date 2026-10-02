package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ILessonService {
    Page<Lesson> findAll(Pageable pageable);
    Page<Lesson> findByChapterId(int chapId, Pageable pageable);
    Lesson findById(int id);
    List<Lesson> findByChapId(int chapId);
    void remove(Lesson lesson);
    int getSerialMax(int lesson);
    Lesson save(Lesson lesson);
}
