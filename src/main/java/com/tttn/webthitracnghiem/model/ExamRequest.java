package com.tttn.webthitracnghiem.model;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

public class ExamRequest {
    private int id;
    private String idName;
    private boolean free = true;
    private int time;
    private Date createDate;
    private Date lastModify;
    private String nameExam;
    private String usersId;
    private int lessonId;
    private Integer lessonPlanLessonId;
    private List<QuestionRequest> questions;
    private Boolean isPublic = true;
    private Integer schoolId;

    private String targetType = "PUBLIC"; // PUBLIC, SCHOOL, GRADE, CLASS
    private Integer targetGrade;
    private Integer targetClassId;
    private Timestamp startTime;
    private Timestamp endTime;
    private String scoreType;
    private Integer gradeMonth;
    private boolean practice;

    public ExamRequest() {
    }

    public ExamRequest(int id, String idName, boolean free, int time, Date createDate, Date lastModify,
                       String nameExam, String usersId, int lessonId, List<QuestionRequest> questions) {
        this.id = id;
        this.idName = idName;
        this.free = free;
        this.time = time;
        this.createDate = createDate;
        this.lastModify = lastModify;
        this.nameExam = nameExam;
        this.usersId = usersId;
        this.lessonId = lessonId;
        this.questions = questions;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIdName() {
        return idName;
    }

    public void setIdName(String idName) {
        this.idName = idName;
    }

    public boolean isFree() {
        return free;
    }

    public void setFree(boolean free) {
        this.free = free;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getLastModify() {
        return lastModify;
    }

    public void setLastModify(Date lastModify) {
        this.lastModify = lastModify;
    }

    public String getNameExam() {
        return nameExam;
    }

    public void setNameExam(String nameExam) {
        this.nameExam = nameExam;
    }

    public String getUsersId() {
        return usersId;
    }

    public void setUsersId(String usersId) {
        this.usersId = usersId;
    }

    public int getLessonId() {
        return lessonId;
    }

    public void setLessonId(int lessonId) {
        this.lessonId = lessonId;
    }

    public Integer getLessonPlanLessonId() {
        return lessonPlanLessonId;
    }

    public void setLessonPlanLessonId(Integer lessonPlanLessonId) {
        this.lessonPlanLessonId = lessonPlanLessonId;
    }

    public List<QuestionRequest> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionRequest> questions) {
        this.questions = questions;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Integer getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Integer schoolId) {
        this.schoolId = schoolId;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Integer getTargetGrade() {
        return targetGrade;
    }

    public void setTargetGrade(Integer targetGrade) {
        this.targetGrade = targetGrade;
    }

    public Integer getTargetClassId() {
        return targetClassId;
    }

    public void setTargetClassId(Integer targetClassId) {
        this.targetClassId = targetClassId;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public String getScoreType() {
        return scoreType;
    }

    public void setScoreType(String scoreType) {
        this.scoreType = scoreType;
    }

    public Integer getGradeMonth() { return gradeMonth; }
    public void setGradeMonth(Integer gradeMonth) { this.gradeMonth = gradeMonth; }

    public boolean isPractice() {
        return practice;
    }

    public void setPractice(boolean practice) {
        this.practice = practice;
    }
}
