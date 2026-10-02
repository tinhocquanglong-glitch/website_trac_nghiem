package com.tttn.webthitracnghiem.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.print.Doc;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.sql.Date;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "user")
public class User {
    @Id
    @Column(unique = true)
    @NotBlank(message = "Tài khoản không được để trống.")
    private String id;

    @Length(min = 6,message = "Mật khẩu không được ít hơn 6 kí tự.")
    @NotBlank(message = "Mật khẩu không được để trống.")
    private String passWord;

    @Length(min = 6,message = "Mật khẩu không được ít hơn 6 kí tự.")
    @NotBlank(message = "Mật khẩu không được để trống.")
    private String rePassWord;

    @NotBlank(message = "Tên không được để trống.")
    private String fullName;

    @Pattern(regexp = "^[a-z][a-z0-9_\\.]*@[a-z0-9]{2,}(\\.[a-z0-9]{2,4}){1,2}$", message = "Sai định dạng email")
    @NotBlank(message = " Email không được để trống.")
    private String email;

    @NotBlank(message = "Địa chỉ  không được để trống")
    private String address;

    @Pattern(regexp = "^((\\(84\\)\\+)|(0))[1-9][\\d]{8}$", message = "Sai định dạng số điện thoại")
    @NotBlank(message = "Số điện thoại không được để trống")
    private String phoneNumber;

    private String img;

    @Column(name = "create_date")
    private Date createDate;

    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "username", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
    @JsonBackReference
    private Set<Role> roles;

    @OneToMany(mappedBy = "users")
    @JsonManagedReference
    private List<Exam> exams;
    @OneToMany(mappedBy = "users")
    @JsonManagedReference
    private Set<Document> documents;
    @OneToOne(mappedBy = "users", cascade = CascadeType.REMOVE)
    @JsonManagedReference
    private Result result;
    @OneToOne(mappedBy = "users", cascade = CascadeType.REMOVE)
    @JsonManagedReference
    private IQ iq;
    @OneToMany(mappedBy = "users")
    @JsonManagedReference
    private Set<News> news;


    public User() {
    }

    public User(String id, String passWord, String rePassWord, String fullName, String email, String address,
                String phoneNumber, String img, Date createDate, Set<Role> roles, List<Exam> exams,
                Set<Document> documents, Result result, Set<News> news) {
        this.id = id;
        this.passWord = passWord;
        this.rePassWord = rePassWord;
        this.fullName = fullName;
        this.email = email;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.img = img;
        this.createDate = createDate;
        this.roles = roles;
        this.exams = exams;
        this.documents = documents;
        this.result = result;
        this.news = news;
    }

    public IQ getIq() {
        return iq;
    }

    public void setIq(IQ iq) {
        this.iq = iq;
    }

    public String getId() {
        return id;
    }

    public void setId(String user) {
        this.id = user;
    }

    public String getPassWord() {
        return passWord;
    }

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    public String getRePassWord() {
        return rePassWord;
    }

    public void setRePassWord(String rePassWord) {
        this.rePassWord = rePassWord;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public List<Exam> getExams() {
        return exams;
    }

    public void setExams(List<Exam> exams) {
        this.exams = exams;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Set<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(Set<Document> documents) {
        this.documents = documents;
    }

    public Set<News> getNews() {
        return news;
    }

    public void setNews(Set<News> news) {
        this.news = news;
    }
}
