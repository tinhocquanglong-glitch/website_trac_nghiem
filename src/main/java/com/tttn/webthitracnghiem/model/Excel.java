package com.tttn.webthitracnghiem.model;

import org.springframework.web.multipart.MultipartFile;

public class Excel {
    private MultipartFile file;

    public MultipartFile getFile() {
        return file;
    }
    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
