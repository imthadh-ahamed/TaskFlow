package com.taskflow.common.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * Sets up a SecurityContext with an {@link AuthenticatedUser} principal for controller tests.
 * Works in {@code @WebMvcTest} without needing FilterChainProxy — the TestExecutionListener
 * sets SecurityContextHolder before the test runs, and controllers read from it via
 * {@link AuthenticatedUser#currentUserId()}.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithAuthenticatedUser.Factory.class)
public @interface WithAuthenticatedUser {

    long userId() default 1L;

    class Factory implements WithSecurityContextFactory<WithAuthenticatedUser> {
        @Override
        public SecurityContext createSecurityContext(WithAuthenticatedUser annotation) {
            var ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(new UsernamePasswordAuthenticationToken(
                    new AuthenticatedUser(annotation.userId()), null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            return ctx;
        }
    }
}
