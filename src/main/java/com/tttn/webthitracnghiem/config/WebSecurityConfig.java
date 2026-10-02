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
                .formLogin().loginPage("/login").failureUrl("/login?error=true")//
                .defaultSuccessUrl("/role", true)
                .and()
                .authorizeRequests().antMatchers("/role").permitAll()
                .antMatchers("/user/**", "/exam/**", "/question/**", "/news/**", "/document/**", "/class/**",
                        "/lesson/**","/chapter/**").hasRole("ADMIN")
                .and()
                .authorizeRequests().antMatchers("/quiz1/**", "/subject/**", "/score/**", "/history/**"
                        ,"/submit1/**","/view","/editMember","/editPass")
                .access("hasAnyRole('ROLE_USER', 'ROLE_ADMIN')");
        ;
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
