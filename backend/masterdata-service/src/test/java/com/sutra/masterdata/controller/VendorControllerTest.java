package com.sutra.masterdata.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sutra.common.error.DuplicateResourceException;
import com.sutra.common.error.GlobalExceptionHandler;
import com.sutra.common.security.CommonSecurityConfig;
import com.sutra.common.security.JwtCodec;
import com.sutra.common.security.RoleName;
import com.sutra.common.security.SutraUserPrincipal;
import com.sutra.common.web.PageResponse;
import com.sutra.masterdata.dto.request.MasterDataRequests.VendorRequest;
import com.sutra.masterdata.dto.response.MasterDataResponses.VendorResponse;
import com.sutra.masterdata.exception.VendorNotFoundException;
import com.sutra.masterdata.service.VendorService;
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

/**
 * The vendor REST API through the real Spring MVC stack and the real JWT security (signed tokens,
 * {@code @PreAuthorize} role rules, the shared error handler); only {@link VendorService} is a Mockito mock.
 */
@WebMvcTest(VendorController.class)
@Import({CommonSecurityConfig.class, GlobalExceptionHandler.class})
class VendorControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JwtCodec jwt;
    @MockitoBean VendorService vendors;

    private static final VendorResponse BRAKELINE = new VendorResponse(5L, "VEN-005", "Brakeline Industries",
            "R. Mehta", "sales@brakeline.example", "+91 98100 00000", "Pune", "27ABCDE1234F1Z5", null, true,
            Instant.parse("2026-10-01T10:00:00Z"), Instant.parse("2026-10-01T10:00:00Z"));

    private String token(RoleName role) {
        return "Bearer " + jwt.issue(new SutraUserPrincipal(1L, "user-" + role, null, true, Set.of(role)));
    }

    @Test
    void staffCanSearchVendors() throws Exception {
        when(vendors.search(eq("brake"), eq(0), eq(20))).thenReturn(new PageResponse<>(List.of(BRAKELINE), 0, 20, 1, 1));

        mvc.perform(get("/api/vendors").param("q", "brake").header(HttpHeaders.AUTHORIZATION, token(RoleName.APPROVER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].code").value("VEN-005"))
                .andExpect(jsonPath("$.content[0].name").value("Brakeline Industries"));
    }

    @Test
    void getUnknownVendorReturns404InTheStandardErrorShape() throws Exception {
        when(vendors.get(99L)).thenThrow(new VendorNotFoundException(99L));

        mvc.perform(get("/api/vendors/99").header(HttpHeaders.AUTHORIZATION, token(RoleName.PROCUREMENT)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VENDOR_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/vendors/99"))
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void administratorCreatesAVendorAndTheBodyReachesTheService() throws Exception {
        when(vendors.create(any())).thenReturn(BRAKELINE);

        mvc.perform(post("/api/vendors").header(HttpHeaders.AUTHORIZATION, token(RoleName.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"VEN-005","name":"Brakeline Industries","email":"sales@brakeline.example",
                                 "phone":"+91 98100 00000"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));

        ArgumentCaptor<VendorRequest> sent = ArgumentCaptor.forClass(VendorRequest.class);
        verify(vendors).create(sent.capture());
        assertThat(sent.getValue().code()).isEqualTo("VEN-005");
        assertThat(sent.getValue().email()).isEqualTo("sales@brakeline.example");
    }

    @Test
    void invalidVendorIsRejectedWith400BeforeReachingTheService() throws Exception {
        mvc.perform(post("/api/vendors").header(HttpHeaders.AUTHORIZATION, token(RoleName.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\",\"name\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists());

        verifyNoInteractions(vendors);
    }

    @Test
    void malformedJsonIsRejectedWith400() throws Exception {
        mvc.perform(post("/api/vendors").header(HttpHeaders.AUTHORIZATION, token(RoleName.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void duplicateCodeIsReportedAs409() throws Exception {
        when(vendors.create(any())).thenThrow(new DuplicateResourceException("Vendor code VEN-005 already exists"));

        mvc.perform(post("/api/vendors").header(HttpHeaders.AUTHORIZATION, token(RoleName.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"VEN-005\",\"name\":\"Dup\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void buyerMayNotCreateVendorsOnlyAdministratorsMay() throws Exception {
        mvc.perform(post("/api/vendors").header(HttpHeaders.AUTHORIZATION, token(RoleName.PROCUREMENT))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"VEN-009\",\"name\":\"New\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(vendors, never()).create(any());
    }

    @Test
    void vendorUsersCannotReadTheVendorMasterAtAll() throws Exception {
        mvc.perform(get("/api/vendors/5").header(HttpHeaders.AUTHORIZATION, token(RoleName.VENDOR)))
                .andExpect(status().isForbidden());

        verify(vendors, never()).get(anyLong());
    }

    @Test
    void requestsWithoutAValidTokenAreUnauthenticated() throws Exception {
        mvc.perform(get("/api/vendors")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get("/api/vendors").header(HttpHeaders.AUTHORIZATION, "Bearer tampered.token.value"))
                .andExpect(status().isUnauthorized());

        verify(vendors, never()).search(isNull(), eq(0), eq(20));
    }

    @Test
    void buyerDeletesAVendorAndGets204() throws Exception {
        mvc.perform(delete("/api/vendors/5").header(HttpHeaders.AUTHORIZATION, token(RoleName.PROCUREMENT)))
                .andExpect(status().isNoContent());

        verify(vendors).delete(5L);
    }

    @Test
    void deletingAVendorStillInUseIsRefused() throws Exception {
        doThrow(new com.sutra.common.error.BusinessRuleException("VENDOR_IN_USE", "Vendor has quotations"))
                .when(vendors).delete(5L);

        mvc.perform(delete("/api/vendors/5").header(HttpHeaders.AUTHORIZATION, token(RoleName.ADMIN)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VENDOR_IN_USE"));
    }

    @Test
    void approverMayNotDeleteVendors() throws Exception {
        mvc.perform(delete("/api/vendors/5").header(HttpHeaders.AUTHORIZATION, token(RoleName.APPROVER)))
                .andExpect(status().isForbidden());

        verify(vendors, never()).delete(anyLong());
    }
}
