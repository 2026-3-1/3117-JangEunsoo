package com.jes.devlearn.global.security;

import com.jes.devlearn.domain.user.entity.Role;
import com.jes.devlearn.domain.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {
    private final User user;
    private final Role role;

    public UserPrincipal(User user) {
        this.user = user;
        this.role = user.getRole();
    }

    public UserPrincipal(User user, Role role) {
        this.user = user;
        this.role = role == null ? user.getRole() : role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Role effective = role != null ? role : Role.STUDENT;
        return List.of(new SimpleGrantedAuthority("ROLE_" + effective.name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    public Long getUserId() {
        return user.getId();
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return user.isActive(); }
}