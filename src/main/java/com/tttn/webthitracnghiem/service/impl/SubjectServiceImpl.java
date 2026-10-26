package com.tttn.webthitracnghiem.service.impl;

import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.SubjectRequest;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.SubjectRepository;
import com.tttn.webthitracnghiem.model.Classes;
import com.tttn.webthitracnghiem.model.SubjectClasses;
import com.tttn.webthitracnghiem.repository.ClassesRepository;
import com.tttn.webthitracnghiem.repository.SubjectClassRepository;
import com.tttn.webthitracnghiem.service.IFileService;
import com.tttn.webthitracnghiem.service.ISubjectService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Service
public class SubjectServiceImpl implements ISubjectService {
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private IFileService fileService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private SubjectClassRepository subjectClassRepository;
    @Autowired
    private ClassesRepository classesRepository;

    @Override
    public Iterable<Subject> findAll() {
        return subjectRepository.findAllActive();
    }

    @Override
    public List<Subject> getAll() {
        return subjectRepository.findAllActive();
    }

    @Override
    public Page<Subject> findAll(Pageable pageable) {
        return subjectRepository.findAllActive(pageable);
    }

    @Override
    public Page<Subject> findByNameSubject(String name, Pageable pageable) {
        return subjectRepository.findAllByName(name, pageable);
    }

    @Override
    public Page<Subject> findBySchoolOrGlobal(Integer schoolId, Pageable pageable) {
        if (schoolId == null) {
            return subjectRepository.findAllActive(pageable);
        }
        return subjectRepository.findAllBySchoolOrGlobal(schoolId, pageable);
    }

    @Override
    public Page<Subject> findByNameAndSchoolOrGlobal(String name, Integer schoolId, Pageable pageable) {
        if (schoolId == null) {
            return subjectRepository.findAllByName(name, pageable);
        }
        return subjectRepository.findByNameAndSchoolOrGlobal(name, schoolId, pageable);
    }

    @Override
    public List<Subject> getBySchoolOrGlobal(Integer schoolId) {
        if (schoolId == null) {
            return subjectRepository.findAllActive();
        }
        return subjectRepository.findBySchoolIdOrGlobal(schoolId);
    }

    @Override
    public Subject findById(int id) {
        return subjectRepository.findById(id).orElse(null);
    }

    @Override
    public Subject save(SubjectRequest subjectRequest) {
        return save(subjectRequest, null);
    }

    @Override
    public Subject save(SubjectRequest subjectRequest, School school) {
        String avatarPath;
        String oldUrl;
        Subject subject;
        if (subjectRequest.getId() == null) {
            subject = modelMapper.map(subjectRequest, Subject.class);
            subject.setSchool(school);
            subject.setStatus(1);
            oldUrl = "/img/default.jpg";
        } else {
            Optional<Subject> findSubject = subjectRepository.findById(subjectRequest.getId());
            subject = modelMapper.map(subjectRequest, Subject.class);
            if (findSubject.isPresent()) {
                subject.setSchool(findSubject.get().getSchool());
                subject.setStatus(findSubject.get().getStatus() != null ? findSubject.get().getStatus() : 1);
                oldUrl = findSubject.get().getImage();
            } else {
                subject.setSchool(school);
                subject.setStatus(1);
                oldUrl = "/img/default.jpg";
            }
        }
        String realUrl = new File(".").getAbsolutePath().replace('\\', File.separatorChar).replace('/', File.separatorChar);
        if (subjectRequest.getImage() != null && subjectRequest.getImage().getOriginalFilename() != null
                && !subjectRequest.getImage().getOriginalFilename().isEmpty()) {
            if (oldUrl != null && !oldUrl.equals("/img/default.jpg")) {
                try {
                    String oldFileName = oldUrl.substring(oldUrl.lastIndexOf("/") + 1);
                    File file = new File(realUrl + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "static" + File.separator + "img" + File.separator + oldFileName);
                    File target = new File(realUrl + File.separator + "target" + File.separator + "classes" + File.separator + "static" + File.separator + "img" + File.separator + oldFileName);
                    file.delete();
                    target.delete();
                } catch (Exception ignored) {
                }
            }

            try {
                String pathFile = realUrl + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "static" + File.separator + "img";
                File fileSaved = fileService.uploadFile(subjectRequest.getImage(), pathFile);
                avatarPath = "/img/" + fileSaved.getName();
                File targetAvatar = new File(realUrl + File.separator + "target" + File.separator + "classes" + File.separator + "static" + File.separator + "img" + File.separator + fileSaved.getName());
                fileService.copyFile(fileSaved, targetAvatar);
            } catch (Exception e) {
                avatarPath = oldUrl;
            }
        } else {
            avatarPath = oldUrl;
        }
        subject.setImage(avatarPath);
        Subject saved = subjectRepository.save(subject);
        if (subjectRequest.getClassIds() != null && !subjectRequest.getClassIds().isEmpty()) {
            syncSubjectClasses(saved, subjectRequest.getClassIds(), school);
        }
        return saved;
    }

    private void syncSubjectClasses(Subject subject, List<Integer> classIds, School school) {
        if (classIds == null || classIds.isEmpty()) return;
        Integer schoolId = school != null ? school.getId() : null;
        for (Integer classId : classIds) {
            if (classId == null) continue;
            Optional<Classes> optClass = classesRepository.findById(classId);
            if (optClass.isEmpty()) continue;
            Classes clazz = optClass.get();

            Optional<SubjectClasses> existing;
            if (schoolId == null) {
                existing = subjectClassRepository.findByClassAndSubjectAndSchoolIsNull(classId, subject.getId());
            } else {
                existing = subjectClassRepository.findByClassAndSubjectAndSchool(classId, subject.getId(), schoolId);
            }

            if (existing.isEmpty()) {
                SubjectClasses sc = new SubjectClasses();
                sc.setSubject(subject);
                sc.setClasses(clazz);
                sc.setSchool(school);
                subjectClassRepository.save(sc);
            }
        }
    }

    @Override
    public Subject copyToSchool(int subjectId, School school) {
        Subject original = findById(subjectId);
        if (original == null || school == null) {
            return null;
        }
        Subject copy = new Subject();
        copy.setName(original.getName());
        copy.setImage(original.getImage());
        copy.setSchool(school);
        copy.setStatus(1);
        return subjectRepository.save(copy);
    }

    @Override
    public boolean canManage(Subject subject, User currentUser, boolean isAdmin) {
        if (isAdmin) {
            return true;
        }
        if (subject == null || currentUser == null || currentUser.getSchool() == null) {
            return false;
        }
        return subject.getSchool() != null && subject.getSchool().getId().equals(currentUser.getSchool().getId());
    }

    @Override
    public void remove(Subject subject) {
        if (subject != null) {
            subject.setStatus(0);
            subjectRepository.save(subject);
        }
    }
}
