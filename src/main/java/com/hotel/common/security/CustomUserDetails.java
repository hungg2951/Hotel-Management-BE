package com.hotel.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final Long hotelId;
    private final Long employeeId;
    private final String username;
    private final String password;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Long userId, Long hotelId, Long employeeId, String username, String password, boolean active, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.hotelId = hotelId;
        this.employeeId = employeeId;
        this.username = username;
        this.password = password;
        this.active = active;
        this.authorities = authorities;
    }

    public CustomUserDetails(Long userId, Long hotelId, String username, String password, boolean active, Collection<? extends GrantedAuthority> authorities) {
        this(userId, hotelId, null, username, password, active, authorities);
    }

    public Long getUserId() {
        return userId;
    }

    public Long getHotelId() {
        return hotelId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
