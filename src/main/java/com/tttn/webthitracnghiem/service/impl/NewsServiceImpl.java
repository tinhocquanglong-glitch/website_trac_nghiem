package com.tttn.webthitracnghiem.service.impl;

import com.tttn.webthitracnghiem.model.News;
import com.tttn.webthitracnghiem.model.NewsRequest;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.repository.NewsRepository;
import com.tttn.webthitracnghiem.service.IFileService;
import com.tttn.webthitracnghiem.service.INewsService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Service
public class NewsServiceImpl implements INewsService {
    @Autowired
    private NewsRepository newsRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private IFileService fileService;

    @Override
    public Page<News> findAll(Pageable pageable) {
        return newsRepository.findAll(pageable);
    }

    @Override
    public Page<News> findByTitle(String name, Pageable pageable) {
        return newsRepository.findAllByTitleContaining(name, pageable);
    }

    @Override
    public News save(NewsRequest newsRequest) {
        String avatarPath;
        String oldUrl;
        News news;
        if (newsRequest.getId() == null) {
            news = modelMapper.map(newsRequest, News.class);
            oldUrl = "/img/default-news.png";
        } else {
            Optional<News> findNews = newsRepository.findById(newsRequest.getId());
            news = modelMapper.map(newsRequest, News.class);
            oldUrl = findNews.get().getImage();
        }
        String realUrl = new File(".").getAbsolutePath().replace('\\', File.separatorChar).replace('/', File.separatorChar);
        if (newsRequest.getImage() != null && newsRequest.getImage().getOriginalFilename() != null && !newsRequest.getImage().getOriginalFilename().isEmpty()) {
            if (oldUrl != null && !oldUrl.equals("/img/default-news.png")) {
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
                File fileSaved = fileService.uploadFile(newsRequest.getImage(), pathFile);
                avatarPath = "/img/" + fileSaved.getName();
                File targetAvatar = new File(realUrl + File.separator + "target" + File.separator + "classes" + File.separator + "static" + File.separator + "img" + File.separator + fileSaved.getName());
                fileService.copyFile(fileSaved, targetAvatar);
            } catch (Exception e) {
                avatarPath = oldUrl;
            }
        } else {
            avatarPath = oldUrl;
        }
        news.setImage(avatarPath);
        news = newsRepository.save(news);
        return news;
    }

    @Override
    public News findById(int id) {
        return newsRepository.findById(id).orElse(null);
    }

    @Override
    public void remove(News news) {
        newsRepository.delete(news);
    }

    @Override
    public List<News> findAll() {
        return newsRepository.findAll();
    }

    @Override
    public List<News> getNewsInWeek() {
        Date date = new Date(System.currentTimeMillis());
        return newsRepository.newsInWeek(date);
    }

    @Override
    public void save(News news) {
        newsRepository.save(news);
    }
}
