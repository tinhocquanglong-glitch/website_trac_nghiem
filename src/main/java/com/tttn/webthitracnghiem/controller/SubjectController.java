package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.*;
import com.tttn.webthitracnghiem.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.HttpResource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/subject")
public class SubjectController {

    @Autowired
    ISubjectService subjectService;
    @Autowired
    IUserService userService;
    @Autowired
    ISubjectClassService subjectClassService;
    @Autowired
    IClassesService classesService;
    @Autowired
    IChapterService chapterService;
    @Autowired
    ILessonService lessonService;

    @GetMapping("/list")
    public String index(@RequestParam("keyword") Optional<String> name,
                        @RequestParam("create") Optional<Boolean> create,
                        @RequestParam("update") Optional<Boolean> update,
                        Model model, @PageableDefault(value = 5) Pageable pageable) {
        Page<Subject> subjects;
        if(create.isPresent()){
            model.addAttribute("message","Tạo môn học thành công!");
        }
        if(update.isPresent()){
            model.addAttribute("message","Chỉnh sửa môn học thành công!");
        }
        if (name.isPresent()) {
            subjects = subjectService.findByNameSubject(name.get(), pageable);
            model.addAttribute("subjects", subjects);
            model.addAttribute("keyword", name.get());
            return "subject/list";
        }
        subjects = subjectService.findAll(pageable);
        model.addAttribute("subjects", subjects);
        return "subject/list";
    }
    @GetMapping("/list/{id}")
    public String getSubject(@RequestParam Optional<String> create,
                             @RequestParam Optional<String> update,
                             @PathVariable Integer id, Model model,
                             @PageableDefault(value = 10) Pageable pageable){
        Page<SubjectClasses> page = subjectClassService.findByClassId(id,pageable);
        if(create.isPresent()){
            model.addAttribute("message","Thêm môn học thành công");
        }
        if(update.isPresent()){
            model.addAttribute("message","Cập nhập môn học thành công");
        }
        model.addAttribute("class",classesService.findById(id));
        model.addAttribute("subjects",page);
        return "class/subjectList";
    }
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Integer id, Model model) {
        Subject subject = subjectService.findById(id);
        model.addAttribute("subject", subject);
        return "subject/edit";
    }
    @GetMapping("/create")
    public String create(Model model){
        model.addAttribute("subject",new SubjectRequest());
        return "subject/add";
    }
    @PostMapping("/create")
    public String create(@Validated @ModelAttribute SubjectRequest subject, BindingResult bindingResult, RedirectAttributes re){
        if(bindingResult.hasErrors()){
            return "subject/add";
        } else {
            if(subject.getImage().getOriginalFilename().equals("")){
                subject.setImage(null);
            }
            subjectService.save(subject);
            re.addFlashAttribute("message", "Tạo môn học thành công!");
            return "redirect:/subject/list";
        }
    }
    @GetMapping("/delete/{id}")
    public String remove(@PathVariable Integer id, RedirectAttributes ra){
        Subject subject = subjectService.findById(id);
        subjectService.remove(subject);
        ra.addFlashAttribute("message","Xóa môn học thành công!");
        return "redirect:/subject/list";
    }
    @GetMapping("/class/create/{classId}")
    public String createSubject(Model model, @PathVariable Integer classId){
        List<SubjectClasses> subjectClassesList = subjectClassService.findByClassId(classId);
        List<Subject> subjectList = new ArrayList<>();
        for(int i = 0; i<subjectClassesList.size();i++){
            subjectList.add(subjectClassesList.get(i).getSubject());
        }
        model.addAttribute("subjectClasses",subjectClassesList);
        model.addAttribute("subjects",subjectService.getAll());
        model.addAttribute("subjectOfClass",subjectList);
        model.addAttribute("class",classesService.findById(classId));
        return "/class/addSubjectToClass";
    }
    @GetMapping("/class/{classId}/{subjectId}")
    public String deleteSubject(@PathVariable("classId") Integer classId,
                                @PathVariable("subjectId") Integer subjectId, Model model, RedirectAttributes ra){
        SubjectClasses subjectClasses = subjectClassService.findByClassAndSubject(classId,subjectId);
        List<Chapter> chapters = new ArrayList<>(subjectClasses.getChapters());
        for(int i = 0; i<chapters.size();i++){
            List<Lesson> lessons = new ArrayList<>(chapters.get(i).getLessons());
            for(int j = 0; j<lessons.size();j++){
                lessonService.remove(lessons.get(i));
            }
            chapterService.remove(chapters.get(i));
        }
        subjectClassService.remove(subjectClasses);
        ra.addFlashAttribute("message","Xóa thành công môn học!");
        return "redirect:/subject/list/"+classId;
    }
}
