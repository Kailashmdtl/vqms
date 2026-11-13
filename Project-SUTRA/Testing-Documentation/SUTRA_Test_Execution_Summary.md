# SUTRA – Test Execution Summary

Final run finished **2026-11-12T10:10:43+05:30** · build **SUCCESS** · Maven total time **01:54 min**.  
All figures are taken from the Surefire XML and JaCoCo CSV reports in `evidence/`.

## Result

| Total | Passed | Failed | Errors | Skipped |
|---:|---:|---:|---:|---:|
| 307 | 306 | 0 | 0 | 1 |

51 of these test cases (7 test classes) were added in this cycle; the existing suite had 256 (baseline run: 256 run, 251 passed, 5 skipped).

## Commands executed

```powershell
cd backend; mvn -B test -fae                          # baseline (before changes)
cd backend; mvn -B test -pl <module> -Dtest=<Class>  # each new test class
cd backend; mvn -B install -DskipTests               # service jars for the live run
cd ai;      .\.venv\Scripts\python -m uvicorn app.main:app --port 8000   # Python document service
cd backend; mvn -B test -fae                          # FINAL run with JaCoCo (results below)
cd ai;      .\.venv\Scripts\python -m pytest -q        # 38 passed
cd frontend; npx vitest run                          # 52 passed (10 files)
```

## Module-wise results

| Module | Classes | Total | Passed | Failed | Skipped | Line cov. | Branch cov. |
|---|---:|---:|---:|---:|---:|---:|---:|
| sutra-common | 7 | 37 | 37 | 0 | 0 | 58.5% | 53.5% |
| gateway | 2 | 12 | 12 | 0 | 0 | 32.6% | 38.5% |
| auth-service | 5 | 26 | 26 | 0 | 0 | 66.9% | 65.7% |
| masterdata-service | 4 | 36 | 36 | 0 | 0 | 58.4% | 48.9% |
| document-service | 2 | 15 | 15 | 0 | 0 | 68.4% | 52.1% |
| rfq-service | 3 | 22 | 22 | 0 | 0 | 63.0% | 49.6% |
| extraction-service | 6 | 34 | 33 | 0 | 1 | 58.1% | 55.3% |
| quotation-service | 7 | 62 | 62 | 0 | 0 | 71.3% | 64.5% |
| comparison-service | 3 | 21 | 21 | 0 | 0 | 77.1% | 72.4% |
| approval-service | 2 | 18 | 18 | 0 | 0 | 60.0% | 60.3% |
| reporting-service | 3 | 16 | 16 | 0 | 0 | 67.1% | 67.3% |
| audit-service | 1 | 8 | 8 | 0 | 0 | 67.1% | 53.6% |
| **Total** | **45** | **307** | **306** | **0** | **1** | **64.1%** | **59.7%** |

## Failures and skips

- **Failures: none** in the final run.
- **SKIPPED** `PdfOcrReaderTest.readsTheTextOfAScannedPage` – Not executed – assumption not met: Assumption failed: Tesseract OCR is not installed
- During development one new assertion in `MoneyTest` failed because the test expectation was wrong (the 0.1% relative tolerance applies). The test input was corrected; production code was not changed.

## New tests added in this cycle

| Module | Test class | Tests | What it covers |
|---|---|---:|---|
| sutra-common | `GlobalExceptionHandlerTest` | 11 | every exception → HTTP status, error code, no leakage of internals (Mockito-mocked request) |
| sutra-common | `JwtAuthenticationFilterTest` | 3 | JWT filter with mocked JwtCodec/FilterChain: valid, forged and missing tokens |
| sutra-common | `MoneyTest` | 10 | BigDecimal rounding, GST percentage, tolerance and percent-difference rules |
| sutra-common | `TextTest` | 2 | blank-to-null normalisation |
| sutra-common | `CorrelationIdFilterTest` | 4 | correlation id propagation, legacy header, header-injection protection |
| masterdata-service | `VendorControllerTest` | 12 | @WebMvcTest + real JWT security: CRUD, validation, 401/403/404/409/422, ArgumentCaptor |
| approval-service | `ApprovalControllerTest` | 9 | @WebMvcTest: award/reject rules, @AssertTrue validation, role restrictions |

Build change: JaCoCo 0.8.13 (`prepare-agent` + `report`) added to `backend/pom.xml`. No production source file was modified.

## Live run

All 10 domain services, the gateway and the Python service were started (isolated `sutra_test` database) and reported `UP`. Smoke checks: unauthenticated `/api/vendors` → 401 `UNAUTHENTICATED`; `/internal/**` without service token → 401; forged JWT → 401; wrong password → 401 `AUTHENTICATION_FAILED` with a generic message. See `evidence/logs/live-smoke-check.log`.

## Coverage (JaCoCo 0.8.13)

Overall: instruction 62.1%, branch 59.7%, line 64.1%, method 53.6%. Each module counts only its own classes exercised by its own tests. Lowest-covered classes (≥25 lines, <40% lines):

- `com.sutra.extraction.service.impl.ExtractionServiceImpl` (extraction-service) – 0.0% of 98 lines
- `com.sutra.gateway.ProxyController` (gateway) – 0.0% of 66 lines
- `com.sutra.common.audit.AuditOutboxRelay` (sutra-common) – 0.0% of 34 lines
- `com.sutra.common.security.CommonSecurityConfig` (sutra-common) – 0.0% of 30 lines
- `com.sutra.gateway.ServiceStatusController` (gateway) – 0.0% of 26 lines
- `com.sutra.rfq.client.MasterDataClient` (rfq-service) – 0.0% of 26 lines
- `com.sutra.quotation.client.MasterDataClient` (quotation-service) – 0.0% of 26 lines
- `com.sutra.reporting.client.MasterDataClient` (reporting-service) – 0.0% of 26 lines

## Known limitations

- End-to-end acceptance script (75 checks) not executed: port 9080 is reserved on this machine, and the gateway on an alternative port could not share the running services' JWT secret.
- OCR test skipped – Tesseract not installed.
- Only 2 controllers have dedicated MockMvc tests; no `@DataJpaTest` repository tests; integration tests cover RFQ and quotation services only.
- Groq AI API always mocked; no performance/load, DAST or browser end-to-end tests.
- Runtime differs from VERSIONS.md (JDK 23.0.2, Maven 3.9.16, Python 3.13.3); Mockito warns about dynamic agent loading on JDK 23.

## Deliverables

- `SUTRA_Software_Testing_Report.docx` / `.pdf` – full report
- `SUTRA_Test_Cases.xlsx` – all test cases, one sheet per module
- `evidence/` – Maven logs, Surefire reports, JaCoCo reports, screenshots
