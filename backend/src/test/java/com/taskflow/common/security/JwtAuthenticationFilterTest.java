package com.taskflow.common.security;

import com.taskflow.auth.application.JwtService;
import com.taskflow.auth.application.TokenValidationException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifies that JwtAuthenticationFilter populates the SecurityContext on a valid access
 * token and leaves it empty — never throws — when the token is missing or invalid.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    JwtService jwtService;

    @InjectMocks
    JwtAuthenticationFilter filter;

    @BeforeEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validAccessToken_populatesSecurityContext() throws Exception {
        when(jwtService.validateAccessToken("valid-token"))
                .thenReturn(new JwtService.ParsedClaims(42L, "some-jti"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("tf_access", "valid-token"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        assertThat(((AuthenticatedUser) auth.getPrincipal()).userId()).isEqualTo(42L);
    }

    @Test
    void invalidToken_leavesSecurityContextEmpty() throws Exception {
        when(jwtService.validateAccessToken(anyString()))
                .thenThrow(new TokenValidationException("Invalid token"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("tf_access", "bad-token"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void expiredToken_leavesSecurityContextEmpty() throws Exception {
        when(jwtService.validateAccessToken(anyString()))
                .thenThrow(new TokenValidationException("Token has expired"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("tf_access", "expired-token"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void noCookie_leavesSecurityContextEmpty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // No cookies set

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void otherCookiePresent_leavesSecurityContextEmpty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("some_other_cookie", "value"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void filterChainAlwaysCalled_evenOnInvalidToken() throws Exception {
        when(jwtService.validateAccessToken(anyString()))
                .thenThrow(new TokenValidationException("Invalid"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("tf_access", "bad-token"));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // The chain must be called so Spring Security can return 401 downstream
        assertThat(chain.getRequest()).isNotNull();
    }
}
