package com.hotel.common.security;

import com.hotel.auth.entity.User;
import com.hotel.auth.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản người dùng: " + username));

        var authorities = user.getRole() != null
                ? List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getRoleCode()))
                : Collections.<SimpleGrantedAuthority>emptyList();

        return new CustomUserDetails(
                user.getUserId(),
                null,
                user.getEmployee() != null ? user.getEmployee().getEmployeeId() : null,
                user.getUsername(),
                user.getPasswordHash(),
                Boolean.TRUE.equals(user.getIsActive()),
                authorities
        );
    }
}
