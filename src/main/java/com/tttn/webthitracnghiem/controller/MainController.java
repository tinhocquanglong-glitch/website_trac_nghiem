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
import javax.servlet.http.HttpSession;

import java.math.BigDecimal;
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

    @Autowired(required = false)
    StudentTrackingService trackingService;

    @ModelAttribute("result")
    public Result getResult() {
        return new Result();
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
    public String submit1(@PathVariable(name = "id") int idExam, @ModelAttribute QuestionForm qForm,
                          Model m, HttpSession session) {
        this.status = true;
        if (!Boolean.TRUE.equals(session.getAttribute("examSubmitted"))) {
            double score = qService.getResult(qForm);
            Exam currentExam = examService.findById(idExam);
            boolean practice = currentExam != null && currentExam.isPractice();
            Result currentResult = (Result) session.getAttribute("examResult");
            if (currentResult == null) currentResult = new Result();
            currentResult.setMark(score);
            currentResult.setEndTime(new Timestamp(System.currentTimeMillis()));
            if (currentExam != null) currentResult.setScoreMonth(currentExam.getGradeMonth());
            qService.saveScore(currentResult);
            long millis = currentResult.getStartTime() == null ? 0
                    : currentResult.getEndTime().getTime() - currentResult.getStartTime().getTime();
            String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                    TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
            m.addAttribute("time", hms);

            // Tự động đồng bộ điểm vào sổ theo dõi học sinh nếu đề có cấu hình scoreType
            try {
                if (!practice && currentExam != null && currentExam.getScoreType() != null
                        && !currentExam.getScoreType().trim().isEmpty() && trackingService != null) {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    String username = (authentication != null) ? authentication.getName() : null;
                    User student = (username != null) ? userService.findById(username) : null;
                    if (student != null) {
                        Integer classId = student.getTrackingClass() == null ? null : student.getTrackingClass().getId();
                        if (classId != null) {
                            BigDecimal score10 = BigDecimal.valueOf(Math.round(score * 100.0) / 100.0);
                            trackingService.recordExamScore(classId, student.getFullName(), currentExam.getScoreType(), score10,
                                    currentExam.getGradeMonth(), currentExam.getUsers() == null ? null : currentExam.getUsers().getId(),
                                    subjectIdForExam(currentExam), currentExam.getId());
                        }
                    }
                }
            } catch (Exception ignored) {}

            m.addAttribute("qForm", qForm);
            m.addAttribute("idExam", idExam);
            // The result model attribute is initialized by @ModelAttribute for every request.
            // Replace it with the persisted attempt so the result page can render the student.
            m.addAttribute("result", currentResult);
            session.setAttribute("examSubmitted", true);
            session.removeAttribute("examResult");
        }
        return "exam/resultTest";
    }

    @GetMapping("/quiz1/{id}")
    public String quiz13(@PathVariable("id") int id, Model m, RedirectAttributes ra, HttpSession session) throws ParseException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : null;
        if (username == null || "anonymousUser".equalsIgnoreCase(username)) {
            ra.addFlashAttribute("warning", "Bạn phải đăng nhập để làm bài.");
            return "redirect:/";
        }
        Exam exam = examService.findById(id);
        if (exam == null) {
            ra.addFlashAttribute("warning", "Không tìm thấy đề thi.");
            return "redirect:/default";
        }

        // Thi thử là nội dung công khai: bỏ qua khung giờ và mọi giới hạn trường/khối/lớp.
        boolean practice = exam.isPractice();
        if (practice && !Boolean.TRUE.equals(exam.getIsPublic())) {
            ra.addFlashAttribute("warning", "Đề thi thử này không ở chế độ công khai.");
            return "redirect:/practice";
        }

        // 1. Kiểm tra khung giờ thi (startTime, endTime)
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (!practice && exam.getStartTime() != null && now.before(exam.getStartTime())) {
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            ra.addFlashAttribute("warning", "Kỳ thi chưa bắt đầu. Thời gian mở đề: " + df.format(exam.getStartTime()));
            return "redirect:/default";
        }
        if (!practice && exam.getEndTime() != null && now.after(exam.getEndTime())) {
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            ra.addFlashAttribute("warning", "Kỳ thi đã kết thúc vào lúc: " + df.format(exam.getEndTime()));
            return "redirect:/default";
        }

        // 2. Kiểm tra phạm vi giao đề (targetType: PUBLIC, SCHOOL, GRADE, CLASS)
        User student = userService.findById(username);
        if (student == null) {
            ra.addFlashAttribute("warning", "Bạn cần đăng nhập để làm bài.");
            return "redirect:/login";
        }

        String targetType = exam.getTargetType();
        if (practice) {
            targetType = "PUBLIC";
        }
        if (targetType == null || targetType.trim().isEmpty()) {
            targetType = (exam.getIsPublic() == null || exam.getIsPublic()) ? "PUBLIC" : "SCHOOL";
        }

        if (!practice && "SCHOOL".equalsIgnoreCase(targetType)) {
            School studentSchool = student.getSchool();
            School examSchool = exam.getSchool();
            boolean sameSchool = (studentSchool != null && examSchool != null && studentSchool.getId() == examSchool.getId());
            if (!sameSchool) {
                String schoolName = (examSchool != null) ? examSchool.getName() : "nội bộ";
                ra.addFlashAttribute("warning", "Đề thi này chỉ dành riêng cho học sinh trường " + schoolName + ".");
                return "redirect:/default";
            }
        } else if (!practice && "GRADE".equalsIgnoreCase(targetType)) {
            School studentSchool = student.getSchool();
            School examSchool = exam.getSchool();
            boolean sameSchool = (studentSchool != null && examSchool != null && studentSchool.getId() == examSchool.getId());
            if (!sameSchool && examSchool != null) {
                ra.addFlashAttribute("warning", "Đề thi này chỉ dành riêng cho trường " + examSchool.getName() + ".");
                return "redirect:/default";
            }
            Integer targetGrade = exam.getTargetGrade();
            Integer studentGrade = (student.getTrackingClass() != null) ? student.getTrackingClass().getGrade() : null;
            if (targetGrade != null && (studentGrade == null || !targetGrade.equals(studentGrade))) {
                ra.addFlashAttribute("warning", "Đề thi này dành riêng cho học sinh Khối " + targetGrade + ".");
                return "redirect:/default";
            }
        } else if (!practice && "CLASS".equalsIgnoreCase(targetType)) {
            StudentTrackingClass targetClass = exam.getTargetClass();
            StudentTrackingClass studentClass = student.getTrackingClass();
            if (targetClass != null && (studentClass == null || !targetClass.getId().equals(studentClass.getId()))) {
                String className = targetClass.getClassName();
                ra.addFlashAttribute("warning", "Đề thi này chỉ dành riêng cho học sinh lớp " + className + ".");
                return "redirect:/default";
            }
        } else if (!practice) {
            // PUBLIC nhưng isPublic = false
            Boolean isPublic = exam.getIsPublic();
            if (isPublic != null && !isPublic) {
                School studentSchool = student.getSchool();
                School examSchool = exam.getSchool();
                boolean sameSchool = (studentSchool != null && examSchool != null && studentSchool.getId() == examSchool.getId());
                if (!sameSchool) {
                    String schoolName = (examSchool != null) ? examSchool.getName() : "nội bộ";
                    ra.addFlashAttribute("warning", "Đề thi này chỉ dành riêng cho học sinh trường " + schoolName + ".");
                    return "redirect:/default";
                }
            }
        }

        // Tăng lượt thi
        exam.setTurn(exam.getTurn() + 1);
        examService.save(exam);

        User user1 = userService.findById(username);
        Result examResult = new Result();
        examResult.setUsers(user1);
        examResult.setExam(exam);
        examResult.setStartTime(new Timestamp(System.currentTimeMillis()));
        session.setAttribute("examResult", examResult);
        session.setAttribute("examSubmitted", false);
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

    private Integer subjectIdForExam(Exam exam) {
        if (exam == null) return null;
        if (exam.getLesson() != null && exam.getLesson().getChapter() != null
                && exam.getLesson().getChapter().getSubjectClasses() != null
                && exam.getLesson().getChapter().getSubjectClasses().getSubject() != null) {
            return exam.getLesson().getChapter().getSubjectClasses().getSubject().getId();
        }
        if (exam.getLessonPlanLesson() != null && exam.getLessonPlanLesson().getChapter() != null
                && exam.getLessonPlanLesson().getChapter().getLessonPlan() != null
                && exam.getLessonPlanLesson().getChapter().getLessonPlan().getSubject() != null) {
            return exam.getLessonPlanLesson().getChapter().getLessonPlan().getSubject().getId();
        }
        return null;
    }


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
