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
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.rememberme.InMemoryTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;


@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private MyUserDetailServiceImpl myUserDetailService;

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
                .failureHandler((request, response, exception) -> response.sendRedirect(request.getContextPath()
                        + (exception instanceof LockedException ? "/login?locked=true" : "/login?error=true")))
                .defaultSuccessUrl("/role", true)
                .and()
                .authorizeRequests()
                .antMatchers("/role", "/api/location/**").permitAll()
                .antMatchers("/chat/**", "/api/chat/**", "/ws", "/ws/**")
                    .hasAnyRole("ADMIN", "SCHOOL_ADMIN", "TEACHER")
                .antMatchers("/school-admin/**").hasAnyRole("ADMIN", "SCHOOL_ADMIN")
                .antMatchers("/user/create-teacher").hasAnyRole("ADMIN", "SCHOOL_ADMIN")
                .antMatchers("/user/delete/**").hasAnyRole("ADMIN", "SCHOOL_ADMIN")
                .antMatchers("/user/editMember/**", "/user/update").hasAnyRole("ADMIN", "SCHOOL_ADMIN")
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
