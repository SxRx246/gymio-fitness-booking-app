package com.ga.gymio.authentication;

import com.ga.gymio.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapts a Gymio User entity to Spring Security's UserDetails interface.
 * Provides the user's credentials, role, and account status for authentication.
 */
@AllArgsConstructor
@NoArgsConstructor
public class MyUserDetails implements UserDetails {
    @Getter
    private User user;

    /**
     * Returns the authorities granted to the user based on their role.
     *
     * @return a collection containing the user's role as a granted authority
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    /**
     * Returns the user's stored password.
     *
     * @return the user's password
     */
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * Returns the user's email address as their login username.
     *
     * @return the user's email address
     */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    /**
     * Checks whether the user's account is enabled.
     * An account is enabled only when the email is verified and the user is active.
     *
     * @return true if the email is verified and the account is active;
     *         false otherwise
     */
    @Override
    public boolean isEnabled() {
        return user.isEmailVerified() && user.getStatus() == User.Status.ACTIVE;
    }

    /**
     * Checks whether the user's credentials are still valid.
     *
     * @return true because credential expiration is not currently enforced
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Checks whether the user's account is locked.
     *
     * @return true because account locking is not currently enforced
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Checks whether the user's account has expired.
     *
     * @return true because account expiration is not currently enforced
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
}