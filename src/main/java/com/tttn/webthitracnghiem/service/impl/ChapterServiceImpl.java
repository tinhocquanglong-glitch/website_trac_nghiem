package com.tttn.webthitracnghiem.service.impl;

import com.tttn.webthitracnghiem.model.Chapter;
import com.tttn.webthitracnghiem.repository.ChapterRepository;
import com.tttn.webthitracnghiem.service.IChapterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChapterServiceImpl implements IChapterService {
    @Autowired
    private ChapterRepository chapterRepository;
    @Override
    public List<Chapter> getAll() {
        return chapterRepository.findAll();
    }

    @Override
    public List<Chapter> findBySubjectClass(int id) {
        return chapterRepository.findBySubjectAndClass(id);
    }

    @Override
    public Chapter findById(int id) {
        return chapterRepository.findById(id).orElse(null);
    }

    @Override
    public List<Chapter> findBySubjectAndClass(int subId, int classId) {
        return chapterRepository.findBySubjectAndClass(subId,classId);
    }

    @Override
    public void remove(Chapter chapter) {
        chapterRepository.delete(chapter);
    }

    @Override
    public int getSerial(int id) {
        return chapterRepository.getSerial(id).orElse(0);
    }

    @Override
    public Chapter save(Chapter chapter) {
        return chapterRepository.save(chapter);
    }
}
