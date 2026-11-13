package com.sutra.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sutra.common.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Every exception a service can throw becomes the same {@link ApiError} shape with the right HTTP status and a stable
 * code, and nothing internal (SQL, stack traces, credentials) reaches the caller. The servlet request is a Mockito mock.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock HttpServletRequest request;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/vendors/5");
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void notFoundBecomes404WithTheResourceCode() {
        ResponseEntity<ApiError> res = handler.handleSutra(new ResourceNotFoundException("Vendor", 5L), request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody().code()).isEqualTo("VENDOR_NOT_FOUND");
        assertThat(res.getBody().message()).isEqualTo("Vendor with id 5 was not found");
        assertThat(res.getBody().path()).isEqualTo("/api/vendors/5");
        assertThat(res.getBody().fieldErrors()).isNull();
    }

    @Test
    void businessRuleBecomes422() {
        ResponseEntity<ApiError> res = handler.handleSutra(
                new BusinessRuleException("RFQ_NOT_OPEN", "The RFQ is not open for quotations"), request);

        assertThat(res.getStatusCode().value()).isEqualTo(422);
        assertThat(res.getBody().code()).isEqualTo("RFQ_NOT_OPEN");
    }

    @Test
    void duplicateAndForbiddenAndUnavailableKeepTheirStatus() {
        assertThat(handler.handleSutra(new DuplicateResourceException("Code exists"), request).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(handler.handleSutra(new ForbiddenOperationException("Not yours"), request).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<ApiError> unavailable = handler.handleSutra(
                new ServiceUnavailableException("RFQ service", new RuntimeException("connection refused")), request);
        assertThat(unavailable.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(unavailable.getBody().message()).doesNotContain("connection refused");
    }

    @Test
    void beanValidationErrorsAreListedPerField() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "vendorRequest");
        binding.reject("AssertTrue", "reason is required when rejecting"); // object-level error, e.g. @AssertTrue
        binding.addError(new org.springframework.validation.FieldError("vendorRequest", "name", "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mock(MethodParameter.class), binding);

        ResponseEntity<ApiError> res = handler.handleValidation(ex, request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(res.getBody().fieldErrors())
                .extracting(ApiError.FieldError::field)
                .containsExactly("name", "vendorRequest");
    }

    @Test
    void missingParameterNamesTheParameter() {
        ResponseEntity<ApiError> res = handler.handleBadRequest(
                new MissingServletRequestParameterException("rfqId", "Long"), request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().message()).isEqualTo("Missing required parameter 'rfqId'");
    }

    @Test
    void badCredentialsNeverRevealWhichPartWasWrong() {
        ResponseEntity<ApiError> res = handler.handleAuth(new BadCredentialsException("user admin: wrong password"), request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody().code()).isEqualTo("AUTHENTICATION_FAILED");
        assertThat(res.getBody().message()).isEqualTo("Invalid username or password");
    }

    @Test
    void accessDeniedBecomes403() {
        ResponseEntity<ApiError> res = handler.handleDenied(new AccessDeniedException("denied"), request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody().code()).isEqualTo("FORBIDDEN");
    }

    @Test
    void optimisticLockAndIntegrityViolationsBecome409WithoutSqlDetails() {
        ResponseEntity<ApiError> lock = handler.handleOptimisticLock(
                new ObjectOptimisticLockingFailureException(Object.class, 9L), request);
        ResponseEntity<ApiError> integrity = handler.handleIntegrity(new DataIntegrityViolationException("x",
                new RuntimeException("duplicate key value violates unique constraint \"uk_vendor_code\"")), request);

        assertThat(lock.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(lock.getBody().code()).isEqualTo("CONCURRENT_MODIFICATION");
        assertThat(integrity.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(integrity.getBody().message()).doesNotContain("uk_vendor_code");
    }

    @Test
    void oversizedUploadBecomes413() {
        ResponseEntity<ApiError> res = handler.handleTooLarge(new MaxUploadSizeExceededException(20_000_000L), request);

        assertThat(res.getStatusCode().value()).isEqualTo(413);
        assertThat(res.getBody().code()).isEqualTo("FILE_TOO_LARGE");
    }

    @Test
    void unexpectedErrorsBecome500WithAGenericMessage() {
        ResponseEntity<ApiError> res = handler.handleUnexpected(
                new IllegalStateException("password=Admin@123 leaked"), request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(res.getBody().message()).isEqualTo("An unexpected error occurred").doesNotContain("Admin@123");
    }

    @Test
    void errorBodyCarriesTheRequestsCorrelationId() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "corr-12345678");

        ResponseEntity<ApiError> res = handler.handleSutra(new DuplicateResourceException("dup"), request);

        assertThat(res.getBody().correlationId()).isEqualTo("corr-12345678");
        assertThat(res.getBody().timestamp()).isNotNull();
    }
}
