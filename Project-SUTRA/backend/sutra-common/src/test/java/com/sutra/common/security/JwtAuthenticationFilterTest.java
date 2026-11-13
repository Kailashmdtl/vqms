package com.sutra.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** The filter every service puts in front of its controllers; the token codec is mocked. */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtCodec jwt;
    @Mock FilterChain chain;
    @InjectMocks JwtAuthenticationFilter filter;

    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/vendors");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenAuthenticatesTheRequestWithItsRoles() throws Exception {
        var buyer = new SutraUserPrincipal(10L, "asha.buyer", null, true, Set.of(RoleName.PROCUREMENT));
        when(jwt.verify("good-token")).thenReturn(Optional.of(new JwtCodec.Token(JwtCodec.TYPE_USER, buyer, Instant.now())));
        request.addHeader("Authorization", "Bearer good-token");

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isSameAs(buyer);
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_PROCUREMENT");
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void invalidTokenLeavesTheRequestAnonymousButStillContinues() throws Exception {
        when(jwt.verify("forged")).thenReturn(Optional.empty());
        request.addHeader("Authorization", "Bearer forged");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void missingOrNonBearerHeaderIsNeverVerified() throws Exception {
        request.addHeader("Authorization", "Basic YWRtaW46QWRtaW5AMTIz");

        filter.doFilter(request, response, chain);
        filter.doFilter(new MockHttpServletRequest("GET", "/api/vendors"), new MockHttpServletResponse(), chain);

        verify(jwt, never()).verify(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
