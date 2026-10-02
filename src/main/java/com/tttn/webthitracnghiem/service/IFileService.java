package com.tttn.webthitracnghiem.service;

import org.apache.tomcat.util.http.fileupload.FileItem;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface IFileService {
    File uploadFile(MultipartFile multipartFile, String path);
    void copyFile(File source, File target);
}
