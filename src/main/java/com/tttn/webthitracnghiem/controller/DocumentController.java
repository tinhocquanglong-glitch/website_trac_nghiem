package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.Document;
import com.tttn.webthitracnghiem.model.News;
import com.tttn.webthitracnghiem.service.IDocumentService;
import com.tttn.webthitracnghiem.service.ISubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/document")
public class DocumentController {

    @Autowired
    private IDocumentService documentService;
    @Autowired
    private ISubjectService subjectService;
    @GetMapping("/list")
    public String index(@RequestParam("keyword") Optional<String> name,
                        @RequestParam("create") Optional<Boolean> create,
                        @RequestParam("update") Optional<Boolean> update,
                        Model model, @PageableDefault(value = 5) Pageable pageable){
        Page<Document> documents;
        if(create.isPresent()){
            model.addAttribute("message","Tạo tài liệu thành công!");
        }
        if(update.isPresent()){
            model.addAttribute("message","Chỉnh sửa tài liệu thành công!");
        }
        if(name.isPresent()){
            documents = documentService.findByTitle(name.get(),pageable);
            model.addAttribute("documents",documents);
            model.addAttribute("keyword",name.get());
            return "document/list";
        }
        documents = documentService.findAll(pageable);
        model.addAttribute("documents",documents);
        return "document/list";
    }
    @GetMapping("/create")
    public String create(Model model){
        model.addAttribute("document",new Document());
        model.addAttribute("subjects",subjectService.findAll());
        return "document/add";
    }
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String id, Model model){
        Document document = documentService.findById(Integer.parseInt(id));
        model.addAttribute("document",document);
        model.addAttribute("subjects",subjectService.findAll());
        return "document/edit";
    }
    @GetMapping("/delete/{id}")
    public String remove(@PathVariable String id, RedirectAttributes ra){
        Document document = documentService.findById(Integer.parseInt(id));
        documentService.remove(document);
        ra.addFlashAttribute("message","Xóa tài liệu thành công!");
        return "redirect:/document/list";
    }

}
