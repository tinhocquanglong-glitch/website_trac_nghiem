package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.News;
import com.tttn.webthitracnghiem.service.INewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/news")
public class NewsController {
    @Autowired
    private INewsService newsService;
    @GetMapping("/list")
    public String index(@RequestParam("keyword") Optional<String> name,
                        @RequestParam("create") Optional<Boolean> create,
                        @RequestParam("update") Optional<Boolean> update,
                        Model model, @PageableDefault(value = 5) Pageable pageable){
        if(create.isPresent()){
            model.addAttribute("message","Thêm tin tức thành công!");
        }
        if(update.isPresent()){
            model.addAttribute("message","Chỉnh sửa tin tức thành công!");
        }
        Page<News> news;
        if(name.isPresent()){
            news = newsService.findByTitle(name.get(),pageable);
            model.addAttribute("newses",news);
            model.addAttribute("keyword",name.get());
            return "news/list";
        }
        news = newsService.findAll(pageable);
        model.addAttribute("newses",news);
        return "news/list";
    }
    @GetMapping("/create")
    public String create(Model model){
        model.addAttribute("news",new News());
        return "news/add";
    }
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String id, Model model){
        News news = newsService.findById(Integer.parseInt(id));
        model.addAttribute("news",news);
        return "news/edit";
    }
    @GetMapping("/delete/{id}")
    public String remove(@PathVariable String id, RedirectAttributes ra){
        News news = newsService.findById(Integer.parseInt(id));
        newsService.remove(news);
        ra.addFlashAttribute("message","Xóa tin tức thành công!");
        return "redirect:/news/list";
    }
}
