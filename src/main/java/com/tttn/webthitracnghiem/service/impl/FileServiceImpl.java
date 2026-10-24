package com.tttn.webthitracnghiem.service.impl;

import com.tttn.webthitracnghiem.service.IFileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.text.Normalizer;

@Service
public class FileServiceImpl implements IFileService {

    private String sanitizeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return System.currentTimeMillis() + ".jpg";
        }
        String ext = "";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot >= 0) {
            ext = originalFilename.substring(lastDot).toLowerCase();
            originalFilename = originalFilename.substring(0, lastDot);
        }
        String normalized = Normalizer.normalize(originalFilename, Normalizer.Form.NFD);
        String clean = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-zA-Z0-9_-]", "_")
                .replaceAll("_+", "_");
        if (clean.isBlank()) {
            clean = "upload";
        }
        return System.currentTimeMillis() + "_" + clean + ext;
    }

    @Override
    public File uploadFile(MultipartFile multipartFile, String path) {
        String cleanPath = path.replace('\\', File.separatorChar).replace('/', File.separatorChar);
        File folder = new File(cleanPath);

        if (!folder.exists()) {
            folder.mkdirs();
        }

        String fileName = sanitizeFileName(multipartFile.getOriginalFilename());
        File fileSaved = new File(folder, fileName);

        try (BufferedOutputStream bs = new BufferedOutputStream(new FileOutputStream(fileSaved))) {
            bs.write(multipartFile.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileSaved;
    }

    @Override
    public void copyFile(File source, File target) {
        if (target.getParentFile() != null && !target.getParentFile().exists()) {
            target.getParentFile().mkdirs();
        }
        try (InputStream is = new FileInputStream(source);
             OutputStream os = new FileOutputStream(target)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
