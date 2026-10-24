package com.tttn.webthitracnghiem.service.impl;

import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.SubjectRequest;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.SubjectRepository;
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

    @Override
    public Iterable<Subject> findAll() {
        return subjectRepository.findAll();
    }

    @Override
    public List<Subject> getAll() {
        return subjectRepository.findAll();
    }

    @Override
    public Page<Subject> findAll(Pageable pageable) {
        return subjectRepository.findAll(pageable);
    }

    @Override
    public Page<Subject> findByNameSubject(String name, Pageable pageable) {
        return subjectRepository.findAllByName(name, pageable);
    }

    @Override
    public Page<Subject> findBySchoolOrGlobal(Integer schoolId, Pageable pageable) {
        if (schoolId == null) {
            return subjectRepository.findAll(pageable);
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
            return subjectRepository.findAll();
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
            oldUrl = "/img/default.jpg";
        } else {
            Optional<Subject> findSubject = subjectRepository.findById(subjectRequest.getId());
            subject = modelMapper.map(subjectRequest, Subject.class);
            if (findSubject.isPresent()) {
                subject.setSchool(findSubject.get().getSchool());
                oldUrl = findSubject.get().getImage();
            } else {
                subject.setSchool(school);
                oldUrl = "/img/default.jpg";
            }
        }
        String realUrl = new File(".").getAbsolutePath();
        if (subjectRequest.getImage() != null && subjectRequest.getImage().getOriginalFilename() != null
                && !subjectRequest.getImage().getOriginalFilename().isEmpty()) {
            if (oldUrl != null && !oldUrl.equals("/img/default.jpg")) {
                try {
                    File file = new File(realUrl + "\\src\\main\\resources\\static\\img\\"
                            + oldUrl.substring(oldUrl.lastIndexOf("/") + 1));
                    File target = new File(realUrl + "\\target\\classes\\static\\img\\" +
                            oldUrl.substring(oldUrl.lastIndexOf("/") + 1));
                    file.delete();
                    target.delete();
                } catch (Exception ignored) {
                }
            }

            try {
                String pathFile = realUrl + "\\src\\main\\resources\\static\\img";
                File fileSaved = fileService.uploadFile(subjectRequest.getImage(), pathFile);
                avatarPath = "/img/" + fileSaved.getName();
                File targetAvatar = new File(realUrl + "\\target\\classes\\static\\img\\" + fileSaved.getName());
                fileService.copyFile(fileSaved, targetAvatar);
            } catch (Exception e) {
                avatarPath = oldUrl;
            }
        } else {
            avatarPath = oldUrl;
        }
        subject.setImage(avatarPath);
        return subjectRepository.save(subject);
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
        subjectRepository.delete(subject);
    }
}
