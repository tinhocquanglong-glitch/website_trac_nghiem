package com.tttn.webthitracnghiem.config;

import com.tttn.webthitracnghiem.service.impl.MyUserDetailServiceImpl;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import com.tttn.webthitracnghiem.repository.UserRepository;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.model.School;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.rememberme.InMemoryTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;


@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private MyUserDetailServiceImpl myUserDetailService;

    @Autowired
    private UserRepository userRepository;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(myUserDetailService).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .formLogin().loginPage("/login")
                .failureHandler((request, response, exception) -> {
                    if (exception instanceof DisabledException) {
                        response.sendRedirect(request.getContextPath() + "/login?deleted=true");
                    } else if (exception instanceof LockedException) {
                        response.sendRedirect(request.getContextPath() + "/login?locked=true");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/login?error=true");
                    }
                })
                .defaultSuccessUrl("/role", true)
                .and()
                .addFilterAfter(new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                            throws ServletException, IOException {
                        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
                            if (!isAdmin) {
                                User u = userRepository.findById(auth.getName()).orElse(null);
                                if (u != null) {
                                    School s = u.getTrackingClass() != null && u.getTrackingClass().getSchool() != null
                                            ? u.getTrackingClass().getSchool()
                                            : u.getSchool();
                                    if (s != null) {
                                        if (s.isDeleted()) {
                                            org.springframework.security.core.context.SecurityContextHolder.clearContext();
                                            HttpSession session = request.getSession(false);
                                            if (session != null) session.invalidate();
                                            response.sendRedirect(request.getContextPath() + "/login?deleted=true");
                                            return;
                                        } else if (!s.isActive()) {
                                            org.springframework.security.core.context.SecurityContextHolder.clearContext();
                                            HttpSession session = request.getSession(false);
                                            if (session != null) session.invalidate();
                                            response.sendRedirect(request.getContextPath() + "/login?locked=true");
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                        filterChain.doFilter(request, response);
                    }
                }, SecurityContextPersistenceFilter.class)
                .authorizeRequests()
                .antMatchers("/role", "/api/location/**").permitAll()
                .antMatchers("/chat/**", "/api/chat/**", "/ws", "/ws/**")
                    .hasAnyRole("ADMIN", "SCHOOL_ADMIN", "TEACHER")
                .antMatchers("/school-admin/**").hasAnyRole("ADMIN", "SCHOOL_ADMIN")
                .antMatchers("/user/create-teacher").hasAnyRole("ADMIN", "SCHOOL_ADMIN")
                .antMatchers("/user/delete/**").hasAnyRole("ADMIN", "SCHOOL_ADMIN")
                .antMatchers("/user/editMember/**", "/user/update").hasAnyRole("ADMIN", "SCHOOL_ADMIN")
                .antMatchers("/user/reset-password", "/user/editPass/**", "/user/updatePass")
                    .hasAnyRole("ADMIN", "SCHOOL_ADMIN", "TEACHER")
                .antMatchers("/user/update-role").hasRole("ADMIN")
                .antMatchers("/user/**", "/school/**").hasRole("ADMIN")
                .antMatchers("/subject/create", "/subject/edit/**", "/subject/delete/**")
                    .hasAnyRole("ADMIN", "SCHOOL_ADMIN")
                .antMatchers("/api/subject/**").hasAnyRole("ADMIN", "SCHOOL_ADMIN")
                .antMatchers("/api/subjectClasses/**", "/api/chapter/**", "/api/lesson/**", "/api/document/**")
                    .hasAnyRole("ADMIN", "SCHOOL_ADMIN", "TEACHER")
                .antMatchers("/lesson-plans/**", "/curriculum", "/curriculum/**", "/api/curriculum", "/api/curriculum/**")
                    .hasAnyRole("ADMIN", "SCHOOL_ADMIN", "TEACHER")
                // Question and exam APIs expose answers; only staff pages call them.
                .antMatchers("/api/question", "/api/question/**", "/api/exam", "/api/exam/**")
                    .hasAnyRole("ADMIN", "SCHOOL_ADMIN", "TEACHER")
                .antMatchers("/exam/**", "/question/**", "/news/**", "/document/**", "/class/**",
                        "/lesson/**", "/chapter/**", "/student-tracking/**", "/teacher/**").hasAnyRole("ADMIN", "SCHOOL_ADMIN", "TEACHER")
                .antMatchers("/practice/**").hasAnyRole("USER", "TEACHER", "SCHOOL_ADMIN", "ADMIN")
                .antMatchers("/student/exams", "/student/statistics").hasAnyRole("USER", "TEACHER", "SCHOOL_ADMIN", "ADMIN")
                .antMatchers("/quiz1/**", "/subject/**", "/score/**", "/history/**",
                        "/submit1/**", "/view", "/editMember", "/editPass").hasAnyRole("USER", "TEACHER", "SCHOOL_ADMIN", "ADMIN");
//                .anyRequest().authenticated();
        http.authorizeRequests().and().rememberMe().tokenRepository(this.persistentTokenRepository()).
                tokenValiditySeconds(60 * 60 * 24 * 30); // 30 ngày
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        InMemoryTokenRepositoryImpl inMemoryTokenRepository = new InMemoryTokenRepositoryImpl();
        return inMemoryTokenRepository;
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

}
