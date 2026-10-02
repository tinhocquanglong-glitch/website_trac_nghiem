package com.tttn.webthitracnghiem.controller;

import com.tttn.webthitracnghiem.model.*;
import com.tttn.webthitracnghiem.repository.ResultRepository;
import com.tttn.webthitracnghiem.service.*;
import com.tttn.webthitracnghiem.service.impl.IQServiceImpl;
import com.tttn.webthitracnghiem.service.impl.ResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@Controller
public class MainController {
    private boolean status = true;
    private Date timer = null;
    @Autowired
    Result result;

    @Autowired
    IQ iqTest;
    @Autowired
    IQuizService qService;

    Boolean submitted = false;

    @Autowired
    IQService iqService;
    @Autowired
    IUserService userService;

    @Autowired
    IResultService rService;

    @Autowired
    IExamService examService;
    @Autowired
    ISubjectService subjectService;
    @Autowired
    IQuestionService questionService;

    @ModelAttribute("result")
    public Result getResult() {
        return result;
    }

    @ModelAttribute("iq")
    public IQ getIqTest() {
        return iqTest;
    }

    @GetMapping("/total")
    public String home1(Model model) {
        List<Result> sList = qService.getTopScore();
        model.addAttribute("sList", sList);
        int total = userService.findByTotalUser();
        model.addAttribute("total", total);
        return "";
    }

    @GetMapping(value = "/default")
    public String home(Model m) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String userName = ((UserDetails) principal).getUsername();
            m.addAttribute("userName", userName);
        }
        List<Result> sList = rService.getTopTen();
        m.addAttribute("sList", sList);
        int total = userService.findByTotalUser();
        String newUser = userService.findByNewUser();
        m.addAttribute("total", total);
        m.addAttribute("newUser", newUser);
        m.addAttribute("subjects", subjectService.getAll());
        m.addAttribute("createExam", new CreateExam());
        return "exam/index";
    }
    @GetMapping("/quiz1")
    public String quiz(){
        return "redirect:/default";
    }
    @PostMapping("/quiz1")
    public String quiz(@ModelAttribute CreateExam createExam, Model m, RedirectAttributes ra) throws ParseException {
        List<Question> questions = questionService.findBySubAndClass(createExam.getSubject().getId(), createExam.getClasses().getId());
        if (questions.size() < createExam.getNumQuestion()) {
            ra.addFlashAttribute("message", "Hệ thống đang cập nhập câu hỏi. Hãy thử lại với lựa chọn khác!!!");
            return "redirect:/default";
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        if (username == null) {
            ra.addFlashAttribute("warning", "Vui Lòng Đăng nhập ");
            return "redirect:/";
        }
        submitted = false;
        //Exam exam = examService.findById(35);
        // result.setQuestions(exam);
        User user1 = userService.findById(username);
        iqTest.setUsers(user1);
        iqTest.setStartTime(new Timestamp(System.currentTimeMillis()));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        timer = new Date(System.currentTimeMillis());
        timer.setMinutes(timer.getMinutes() + createExam.getTime());
        System.out.println(formatter.format(timer));
        QuestionForm qForm = qService.getQuestionsBySubAndClass(createExam.getSubject().getId(), createExam.getClasses().getId(),
                createExam.getNumQuestion());
        m.addAttribute("qForm", qForm);
        List<Result> sList = qService.getTopScore();
        m.addAttribute("sList", sList);
        m.addAttribute("exam", createExam);
        int total = userService.findByTotalUser();
        m.addAttribute("total", total);
        m.addAttribute("futureDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(formatter.format(timer)));
        return "exam/quiz";
    }

    @PostMapping("/submit1/{id}")
    public String submit1(@PathVariable(name = "id") int idExam, @ModelAttribute QuestionForm qForm, Model m) {
        this.status = true;
        if (!submitted) {
            result.setMark(qService.getResult(qForm));
            result.setEndTime(new Timestamp(System.currentTimeMillis()));
            qService.saveScore(result);
            long millis = result.getEndTime().getTime() - result.getStartTime().getTime();
            String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                    TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
            m.addAttribute("time", hms);
            result = new Result();
            m.addAttribute("qForm", qForm);
            m.addAttribute("idExam", idExam);
            submitted = true;
        }
        return "exam/resultTest";
    }

    @GetMapping("/quiz1/{id}")
    public String quiz13(@PathVariable("id") int id, Model m, RedirectAttributes ra) throws ParseException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        if (username == null) {
            ra.addFlashAttribute("warning", "Bạn Phải Nhập Tên ");
            return "redirect:/";
        }
        submitted = false;
        // Tăng lượt thi
        Exam exam = examService.findById(id);
        exam.setTurn(exam.getTurn() + 1);
        examService.save(exam);

        User user1 = userService.findById(username);
        result.setUsers(user1);
        result.setExam(exam);
        result.setStartTime(new Timestamp(System.currentTimeMillis()));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (!exam.isFree()) {
            timer = new Date(System.currentTimeMillis());
            timer.setMinutes(timer.getMinutes() + exam.getTime());
            m.addAttribute("futureDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(formatter.format(timer)));
        } else {
            m.addAttribute("isFree", true);
        }
        QuestionForm qForm = qService.getQuestions(id);
        m.addAttribute("qForm", qForm);
        List<Result> sList = qService.getTopScore();
        m.addAttribute("sList", sList);
        int total = userService.findByTotalUser();
        m.addAttribute("total", total);
        m.addAttribute("exam", exam);
        return "exam/quizTest";

    }


//    @GetMapping("/quiz11/{id}")
//    public String beforeQuiz(@PathVariable("id") int id, RedirectAttributes ra) {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String username = authentication.getName();
//        if (username == null) {
//            ra.addFlashAttribute("warning", "Vui Lòng Đăng nhập ");
//            return "redirect:/default";
//        }
//        this.status = true;
//        return "redirect:/quiz1/"+ id;
//    }

    @PostMapping("/submit1")
    public String submit1(@ModelAttribute QuestionForm qForm, Model m) {
        this.status = true;
        if (!submitted) {
            iqTest.setMark(qService.getResult(qForm));
            iqTest.setEndTime(new Timestamp(System.currentTimeMillis()));
            iqService.save(iqTest);
            long millis = iqTest.getEndTime().getTime() - iqTest.getStartTime().getTime();
            String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                    TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
            m.addAttribute("time", hms);
            iqTest = new IQ();
            m.addAttribute("qForm", qForm);
            submitted = true;
        }

        return "exam/result";

    }

    @GetMapping("/score/{id}")
    public String score(@PathVariable(name = "id") int idExam, Model m) {
        List<Result> sList = qService.getTopScoreByExam(idExam);
        m.addAttribute("sList", sList);
        return "exam/scoreboard";
    }

    @GetMapping("/listInformation")
    public String listInformation(Model m) {
        List<Result> sList = rService.getTopTen();
        m.addAttribute("sList", sList);

        return "statistical/ListInformation";
    }

//    @GetMapping("/honorthegoldboard")
//    public String honorthegoldboard(Model m) {
//        int total = userService.findByTotalUser();
//        String newUser = userService.findByNewUser();
//        m.addAttribute("total", total);
//        m.addAttribute("newUser", newUser);
//        return "statistical/HonorTheGoldBoard";
//    }

    @GetMapping("/aboutus")

    public String aboutUS(Model model) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String userName = ((UserDetails) principal).getUsername();
            model.addAttribute("userName", userName);
        }
        return "aboutAs";
    }
}
