package com.sutra.approval.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sutra.api.decision.DecisionResponses.ApprovalResponse;
import com.sutra.api.enums.ApprovalDecision;
import com.sutra.api.quotation.QuotationRequests.ApprovalRequest;
import com.sutra.approval.service.ApprovalService;
import com.sutra.common.error.BusinessRuleException;
import com.sutra.common.error.ForbiddenOperationException;
import com.sutra.common.error.GlobalExceptionHandler;
import com.sutra.common.security.CommonSecurityConfig;
import com.sutra.common.security.JwtCodec;
import com.sutra.common.security.RoleName;
import com.sutra.common.security.SutraUserPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Award / reject decisions over HTTP: who may decide (managers and administrators only), request validation
 * (selection required to award, reason required to reject) and business-rule errors from the mocked service.
 */
@WebMvcTest(ApprovalController.class)
@Import({CommonSecurityConfig.class, GlobalExceptionHandler.class})
class ApprovalControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JwtCodec jwt;
    @MockitoBean ApprovalService approvals;

    private static final ApprovalResponse AWARD = new ApprovalResponse(1L, 7L, 3L, 2, 41L, "Brakeline Industries",
            ApprovalDecision.APPROVED, null, "ravi.manager", Instant.parse("2026-11-01T09:30:00Z"));

    private MockHttpServletRequestBuilder as(RoleName role, MockHttpServletRequestBuilder request) {
        String token = jwt.issue(new SutraUserPrincipal(20L, "user-" + role, null, true, Set.of(role)));
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void managerAwardsTheRfqToTheSelectedQuotation() throws Exception {
        when(approvals.decide(eq(7L), any())).thenReturn(AWARD);

        mvc.perform(as(RoleName.APPROVER, post("/api/purchase-requests/7/approvals"))
                        .content("{\"comparisonReportId\":3,\"decision\":\"APPROVED\",\"selectedQuotationId\":41}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decision").value("APPROVED"))
                .andExpect(jsonPath("$.selectedVendorName").value("Brakeline Industries"))
                .andExpect(jsonPath("$.comparisonReportVersion").value(2));

        ArgumentCaptor<ApprovalRequest> sent = ArgumentCaptor.forClass(ApprovalRequest.class);
        verify(approvals, times(1)).decide(eq(7L), sent.capture());
        assertThat(sent.getValue().comparisonReportId()).isEqualTo(3L);
        assertThat(sent.getValue().selectedQuotationId()).isEqualTo(41L);
    }

    @Test
    void awardWithoutASelectedQuotationIsInvalid() throws Exception {
        mvc.perform(as(RoleName.APPROVER, post("/api/purchase-requests/7/approvals"))
                        .content("{\"comparisonReportId\":3,\"decision\":\"APPROVED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("selectedQuotationId is required when approving"));

        verify(approvals, never()).decide(anyLong(), any());
    }

    @Test
    void rejectionWithoutAReasonIsInvalid() throws Exception {
        mvc.perform(as(RoleName.APPROVER, post("/api/purchase-requests/7/approvals"))
                        .content("{\"comparisonReportId\":3,\"decision\":\"REJECTED\",\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].message").value("reason is required when rejecting"));

        verify(approvals, never()).decide(anyLong(), any());
    }

    @Test
    void unknownDecisionValueIsAMalformedRequest() throws Exception {
        mvc.perform(as(RoleName.APPROVER, post("/api/purchase-requests/7/approvals"))
                        .content("{\"comparisonReportId\":3,\"decision\":\"MAYBE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void buyersCannotRecordDecisions() throws Exception {
        mvc.perform(as(RoleName.PROCUREMENT, post("/api/purchase-requests/7/approvals"))
                        .content("{\"comparisonReportId\":3,\"decision\":\"APPROVED\",\"selectedQuotationId\":41}"))
                .andExpect(status().isForbidden());

        verify(approvals, never()).decide(anyLong(), any());
    }

    @Test
    void vendorsCannotEvenListDecisions() throws Exception {
        mvc.perform(as(RoleName.VENDOR, get("/api/purchase-requests/7/approvals")))
                .andExpect(status().isForbidden());

        verify(approvals, never()).list(anyLong());
    }

    @Test
    void managerCannotAwardAnRfqTheyRaised() throws Exception {
        when(approvals.decide(eq(7L), any()))
                .thenThrow(new ForbiddenOperationException("You cannot approve a purchase request you created"));

        mvc.perform(as(RoleName.APPROVER, post("/api/purchase-requests/7/approvals"))
                        .content("{\"comparisonReportId\":3,\"decision\":\"APPROVED\",\"selectedQuotationId\":41}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot approve a purchase request you created"));
    }

    @Test
    void outdatedComparisonIsABusinessRuleViolation() throws Exception {
        when(approvals.decide(eq(7L), any()))
                .thenThrow(new BusinessRuleException("REPORT_OUTDATED", "A newer comparison exists"));

        mvc.perform(as(RoleName.ADMIN, post("/api/purchase-requests/7/approvals"))
                        .content("{\"comparisonReportId\":3,\"decision\":\"APPROVED\",\"selectedQuotationId\":41}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("REPORT_OUTDATED"));
    }

    @Test
    void staffCanListTheDecisionHistory() throws Exception {
        when(approvals.list(7L)).thenReturn(List.of(AWARD));

        mvc.perform(as(RoleName.PROCUREMENT, get("/api/purchase-requests/7/approvals")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].decidedBy").value("ravi.manager"));
    }
}
