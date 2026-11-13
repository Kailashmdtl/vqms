package com.sutra.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** One id follows a user action through every service and every log line. */
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    void wellFormedIncomingIdIsKeptEchoedAndVisibleDuringTheRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/rfqs");
        request.addHeader(CorrelationIdFilter.HEADER, "gw-1234abcd");
        AtomicReference<String> seen = new AtomicReference<>();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            seen.set(CorrelationIdFilter.current());
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        assertThat(seen.get()).isEqualTo("gw-1234abcd");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("gw-1234abcd");
        assertThat(CorrelationIdFilter.current()).as("cleared after the request").isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void legacyHeaderIsAccepted() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/rfqs");
        request.addHeader(CorrelationIdFilter.LEGACY_HEADER, "legacy-0001");

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("legacy-0001");
    }

    @Test
    void unsafeIncomingIdIsReplacedWithANewOne() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/rfqs");
        request.addHeader(CorrelationIdFilter.HEADER, "bad\r\nInjected: header");

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getHeader(CorrelationIdFilter.HEADER))
                .isNotEqualTo("bad\r\nInjected: header")
                .matches("[0-9a-f-]{36}");
    }

    @Test
    void currentOrNewAlwaysGivesAnIdOutsideARequest() {
        assertThat(CorrelationIdFilter.current()).isNull();
        assertThat(CorrelationIdFilter.currentOrNew()).isNotBlank();
    }
}
