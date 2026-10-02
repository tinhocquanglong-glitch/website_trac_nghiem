package com.tttn.webthitracnghiem.service.impl;

import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.SubjectRequest;
import com.tttn.webthitracnghiem.repository.SubjectRepository;
import com.tttn.webthitracnghiem.service.IFileService;
import com.tttn.webthitracnghiem.service.ISubjectService;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.RequestContext;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
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
    public Subject findById(int id) {
        return subjectRepository.findById(id).orElse(null);
    }

    @Override
    public Subject save(SubjectRequest subjectRequest) {
        String avatarPath;
        String oldUrl;
        Subject subject;
        if(subjectRequest.getId() == null){
            subject = modelMapper.map(subjectRequest,Subject.class);
            oldUrl = "/img/default.jpg";
        } else {
            Optional<Subject> findSubject = subjectRepository.findById(subjectRequest.getId());
            subject = modelMapper.map(subjectRequest,Subject.class);
            oldUrl = findSubject.get().getImage();
        }
        String realUrl = new File(".").getAbsolutePath();
        if(subjectRequest.getImage() != null){
            if(!oldUrl.equals("/img/default.jpg")) {
                // Xóa file cũ
                File file = new File(realUrl + "\\src\\main\\resources\\static\\img\\"
                        + oldUrl.substring(oldUrl.lastIndexOf("/") + 1));

                File target = new File(realUrl + "\\target\\classes\\static\\img\\" +
                        oldUrl.substring(oldUrl.lastIndexOf("/") + 1));

                file.delete();
                target.delete();
            }

            // Thêm file mới được chọn
            String pathFile = realUrl+"\\src\\main\\resources\\static\\img";
            File fileSaved = fileService.uploadFile(subjectRequest.getImage(), pathFile);
            avatarPath = "/img/" + fileSaved.getName();
            File targetAvatar = new File(realUrl+"\\target\\classes\\static\\img\\" + fileSaved.getName());
            fileService.copyFile(fileSaved, targetAvatar);
        } else {
            avatarPath = oldUrl;
        }
        subject.setImage(avatarPath);
        subject = subjectRepository.save(subject);
        return subject;
    }

    @Override
    public void remove(Subject subject) {
        subjectRepository.delete(subject);
    }
}
