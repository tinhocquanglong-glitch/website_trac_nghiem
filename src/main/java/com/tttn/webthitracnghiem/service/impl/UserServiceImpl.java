package com.tttn.webthitracnghiem.service.impl;

import com.tttn.webthitracnghiem.model.Role;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.model.UserRequest;
import com.tttn.webthitracnghiem.repository.LessonPlanRepository;
import com.tttn.webthitracnghiem.repository.RoleRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import com.tttn.webthitracnghiem.service.IFileService;
import com.tttn.webthitracnghiem.service.IUserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.sql.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserServiceImpl implements IUserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private LessonPlanRepository lessonPlanRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private IFileService fileService;

    @Override
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAllActive(pageable);
    }

    @Override
    public Page<User> search(String name, Pageable pageable) {
        return userRepository.search(name,pageable);
    }

    @Override
    public Iterable<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User findById(String id) {
        return userRepository.findById(id).orElse(null);
    }
    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Registers a user with a server-controlled role. The admin role row is
     * locked for the duration of this transaction so concurrent registrations
     * cannot both become the initial administrator.
     */
    @Override
    @Transactional
    public User register(User user) {
        Role adminRole = roleRepository.findByRoleNameForUpdate("ROLE_ADMIN")
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN is not configured"));
        Role defaultRole = roleRepository.findByRoleName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("ROLE_USER is not configured"));

        Set<Role> roles = new HashSet<>();
        Integer adminCount = userRepository.countUsersByRoleName("ROLE_ADMIN");
        roles.add(adminCount != null && adminCount > 0 ? defaultRole : adminRole);
        user.setRoles(roles);
        return userRepository.save(user);
    }
    @Override
    @Transactional
    public void delete(User user) {
        String userId = user.getId();
        user.setStatus(0);
        userRepository.clearHomeroomTeacher(userId);
        userRepository.clearTrackingClass(userId);
        userRepository.deleteTeacherAssignments(userId);
        userRepository.save(user);
    }
    @Override
    public  Integer findByTotalUser(){return userRepository.findByTotalUser();}

    @Override
    public String findByNewUser(){return userRepository.findByNewUser();}

    @Override
    public boolean userExists(String username){
        return userRepository.findById(username).isPresent();
    }

    @Override
    public boolean userExistss(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Override
    public String findByPass(String id) {
        return userRepository.findByPass(id);
    }

    @Override
    public List<User> getNewMember() {
        Date date = new Date(System.currentTimeMillis());
        return userRepository.newMember(date);
    }

    @Override
    public int getTotalMember() {
        return userRepository.getMember().size();
    }

    @Override
    public User save(UserRequest userRequest) {
        String avatarPath;
        String oldUrl;
        User user;
        if(userRequest.getId() == null){
            user = modelMapper.map(userRequest,User.class);
            oldUrl = "/img/avatar/default.jpg";
        } else {
            Optional<User> findUser = userRepository.findById(userRequest.getId());
            user = modelMapper.map(userRequest,User.class);
            oldUrl = findUser.get().getImg();
        }
        String realUrl = new File(".").getAbsolutePath().replace('\\', File.separatorChar).replace('/', File.separatorChar);
        if (userRequest.getImg() != null && userRequest.getImg().getOriginalFilename() != null && !userRequest.getImg().getOriginalFilename().isEmpty()) {
            if (oldUrl != null && !oldUrl.equals("/img/avatar/default.jpg")) {
                try {
                    String oldFileName = oldUrl.substring(oldUrl.lastIndexOf("/") + 1);
                    File file = new File(realUrl + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "static" + File.separator + "img" + File.separator + "avatar" + File.separator + oldFileName);
                    File target = new File(realUrl + File.separator + "target" + File.separator + "classes" + File.separator + "static" + File.separator + "img" + File.separator + "avatar" + File.separator + oldFileName);
                    file.delete();
                    target.delete();
                } catch (Exception ignored) {}
            }
            try {
                String pathFile = realUrl + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "static" + File.separator + "img" + File.separator + "avatar";
                File fileSaved = fileService.uploadFile(userRequest.getImg(), pathFile);
                avatarPath = "/img/avatar/" + fileSaved.getName();
                File targetAvatar = new File(realUrl + File.separator + "target" + File.separator + "classes" + File.separator + "static" + File.separator + "img" + File.separator + "avatar" + File.separator + fileSaved.getName());
                fileService.copyFile(fileSaved, targetAvatar);
            } catch (Exception e) {
                avatarPath = oldUrl;
            }
        } else {
            avatarPath = oldUrl;
        }
        user.setImg(avatarPath);
        user = userRepository.save(user);
        return user;
    }

}

