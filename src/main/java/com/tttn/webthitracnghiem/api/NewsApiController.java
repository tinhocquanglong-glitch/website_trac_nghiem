package com.tttn.webthitracnghiem.api;

import com.tttn.webthitracnghiem.model.News;
import com.tttn.webthitracnghiem.model.NewsRequest;
import com.tttn.webthitracnghiem.service.INewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
public class NewsApiController {
    @Autowired
    private INewsService newsService;
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<News> save(@ModelAttribute NewsRequest news){
        return ResponseEntity.ok(newsService.save(news));
    }
}
