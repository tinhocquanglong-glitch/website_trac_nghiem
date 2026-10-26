package com.tttn.webthitracnghiem.service.impl;

import com.tttn.webthitracnghiem.model.Document;
import com.tttn.webthitracnghiem.model.DocumentRequest;
import com.tttn.webthitracnghiem.model.News;
import com.tttn.webthitracnghiem.repository.DocumentRepository;
import com.tttn.webthitracnghiem.service.IDocumentService;
import com.tttn.webthitracnghiem.service.IFileService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentServiceImpl implements IDocumentService {

    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private IFileService fileService;
    @Autowired
    private ModelMapper modelMapper;
    @Override
    public Page<Document> findAll(Pageable pageable) {
        return documentRepository.findAllActive(pageable);
    }

    @Override
    public Document findById(int id) {
        return documentRepository.findById(id).orElse(null);
    }

    @Override
    public Page<Document> findByTitle(String title, Pageable pageable) {
        return documentRepository.findByTitleContainingActive(title, pageable);
    }

    @Override
    public Document save(DocumentRequest documentRequest) {
        String avatarPath;
        String oldUrl;
        Document document;
        if (documentRequest.getId() == null) {
            document = modelMapper.map(documentRequest, Document.class);
            oldUrl = "/img/default-document.png";
        } else {
            Optional<Document> findDocument = documentRepository.findById(documentRequest.getId());
            document = modelMapper.map(documentRequest, Document.class);
            oldUrl = findDocument.get().getAvatar();
        }
        String realUrl = new File(".").getAbsolutePath().replace('\\', File.separatorChar).replace('/', File.separatorChar);
        if (documentRequest.getAvatar() != null && documentRequest.getAvatar().getOriginalFilename() != null && !documentRequest.getAvatar().getOriginalFilename().isEmpty()) {
            if (oldUrl != null && !oldUrl.equals("/img/default-document.png")) {
                try {
                    String oldFileName = oldUrl.substring(oldUrl.lastIndexOf("/") + 1);
                    File file = new File(realUrl + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "static" + File.separator + "img" + File.separator + oldFileName);
                    File target = new File(realUrl + File.separator + "target" + File.separator + "classes" + File.separator + "static" + File.separator + "img" + File.separator + oldFileName);
                    file.delete();
                    target.delete();
                } catch (Exception ignored) {}
            }
            try {
                String pathFile = realUrl + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "static" + File.separator + "img";
                File fileSaved = fileService.uploadFile(documentRequest.getAvatar(), pathFile);
                avatarPath = "/img/" + fileSaved.getName();
                File targetAvatar = new File(realUrl + File.separator + "target" + File.separator + "classes" + File.separator + "static" + File.separator + "img" + File.separator + fileSaved.getName());
                fileService.copyFile(fileSaved, targetAvatar);
            } catch (Exception e) {
                avatarPath = oldUrl;
            }
        } else {
            avatarPath = oldUrl;
        }
        document.setAvatar(avatarPath);
        document = documentRepository.save(document);
        return document;
    }

    @Override
    public void remove(Document document) {
        if (document != null) {
            document.setStatus(0);
            documentRepository.save(document);
        }
    }

    @Override
    public List<Document> findAll() {
        return documentRepository.findAllActive();
    }

    @Override
    public void save(Document document) {
        documentRepository.save(document);
    }
}
