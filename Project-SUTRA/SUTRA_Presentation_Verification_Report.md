# SUTRA presentation verification report

Prepared for the Section 12–15 presentation. Claims below were checked against implementation files, configuration, migrations, frontend routes, source test declarations and the recorded acceptance report. No test suite was rerun while preparing this deck.

## Verified claims presented

| Claim | Verification source | Status and notes |
|---|---|---|
| Groq is the active language-model provider for quotation extraction and Sahayak. | `backend/extraction-service/src/main/java/com/sutra/extraction/client/GroqClient.java`; `backend/reporting-service/src/main/java/com/sutra/reporting/service/impl/AssistantServiceImpl.java`; `backend/extraction-service/src/main/resources/application.yml` | Implemented. `ExtractionSource.GEMINI` remains as a legacy enum for old quotation records (`backend/sutra-api/src/main/java/com/sutra/api/enums/ExtractionSource.java`); it is not an active extraction engine. |
| Default Groq model is `openai/gpt-oss-120b`; configured model fallbacks are `openai/gpt-oss-20b` and `qwen/qwen3.8-27b`. | `backend/extraction-service/src/main/resources/application.yml`; `GroqClient.java` | Implemented. `GroqClient` retries rate-limited requests and can move to the next configured model; AUTO extraction can then continue to the rules engine if a configured engine fails. |
| Java calls Groq; Python FastAPI parses files, exposes a rule-based extraction endpoint and a matching endpoint. | `GroqQuotationExtractor.java`; `DocumentProcessingClient.java`; `ai/app/main.py` | Implemented. Python is not the component that calls Groq. |
| Python parsing supports PDF, XLSX, XLS and DOCX using the project libraries. | `ai/app/parsers.py`; `ai/requirements.txt` | Implemented. PDF text extraction uses pdfplumber; spreadsheets use openpyxl/xlrd; Word files use python-docx. |
| Scanned PDF reading uses PDFBox to render pages and local Tesseract OCR. | `backend/extraction-service/src/main/java/com/sutra/extraction/service/internal/PdfOcrReader.java`; `GroqQuotationExtractor.java`; extraction `application.yml` | Implemented when Tesseract is enabled and available. Tesseract languages are configurable; the checked-in default is `eng`. OCR needs a local Tesseract installation and language data. |
| Groq receives a strict JSON schema and instructions to report printed values, flag uncertain values and ignore instructions embedded in vendor documents. | `GroqQuotationExtractor.java`; `ExtractionSchema.java`; `GroqClient.java` | Implemented as prompt and response-format controls. They reduce risk but do not guarantee resistance to prompt injection or extraction errors. |
| Java validates extracted quotation data, matches lines and recalculates all totals. | `ExtractionServiceImpl.java`; `HybridItemMatcher.java`; `backend/quotation-service/src/main/java/com/sutra/quotation/service/internal/QuotationCalculator.java`; `QuotationValidator.java` | Implemented. Groq is instructed not to calculate totals; the Java backend uses `BigDecimal` and compares calculated values with printed amounts. |
| Sahayak answers questions in Hindi, Hinglish or English using current context and returns only links that correspond to records included in that context. | `backend/reporting-service/src/main/java/com/sutra/reporting/service/impl/AssistantServiceImpl.java`; `frontend/src/components/Assistant.jsx` | Implemented. The assistant is read-only. Context is assembled by the reporting service, and response links are checked against known request and quotation IDs. |
| Sahayak can prepare an RFQ draft from pasted text, but a buyer or administrator must review and explicitly create it. | `AssistantServiceImpl.java` (`withRfqDraft`); `backend/extraction-service/src/main/java/com/sutra/extraction/service/impl/RfqDraftServiceImpl.java`; `frontend/src/components/Assistant.jsx` | Implemented. The draft is not saved by chat. Vendors and managers cannot create the draft through Sahayak. |
| When extraction fails, the original document is retained and a user can retry with another engine or enter data manually. | `backend/extraction-service/src/main/java/com/sutra/extraction/service/internal/ExtractionEngines.java`; quotation `QuotationServiceImpl.java`; `docs/ACCEPTANCE_RESULTS.md` | Implemented. The acceptance report checks a failed extraction, retained upload, retry and manual entry. |
| Passwords use BCrypt. User tokens use HS256 and include user ID, roles, optional vendor ID, token version and expiry. | `backend/auth-service/src/main/java/com/sutra/auth/config/AuthSecurityConfig.java`; `AuthServiceImpl.java`; `backend/sutra-common/src/main/java/com/sutra/common/security/JwtCodec.java` | Implemented. Checked-in auth configuration defaults user-token expiry to 480 minutes. Service tokens expire after five minutes. |
| The frontend stores the returned token in `sessionStorage` and sends it as a Bearer token. | `frontend/src/api/client.js`; `frontend/src/auth/AuthContext.jsx` | Implemented. |
| The gateway verifies tokens, blocks external `/internal/**` paths and checks account activity and token version. Services enforce route and method-level authorization; record-level checks protect ownership. | `backend/gateway/src/main/java/com/sutra/gateway/GatewayFilter.java`; `TokenStateCache.java`; `backend/sutra-common/src/main/java/com/sutra/common/security/CommonSecurityConfig.java`; `backend/rfq-service/.../RecordAccessPolicy.java`; `backend/quotation-service/.../RecordAccessPolicy.java`; `VendorPortalServiceImpl.java` | Implemented. The gateway caches active/version state for 15 seconds. Internal service tokens are separate from user tokens. Roles are `ADMIN`, `PROCUREMENT`, `VENDOR` and `APPROVER`. |
| Login and vendor signup are limited to 20 requests per minute per IP by default; repeated login failure locks a username after five failures for 15 minutes by default. | `backend/gateway/src/main/java/com/sutra/gateway/PublicEndpointRateLimiter.java`; `backend/auth-service/src/main/java/com/sutra/auth/service/internal/LoginAttemptService.java`; auth and gateway `application.yml` | Implemented. Login-attempt state and rate-limit windows are in memory, so they are single-instance controls and reset on restart. |
| Upload validation sanitises filenames, checks extensions and actual file signatures, validates Office package structure, and applies a 20 MB default file limit. | `backend/document-service/src/main/java/com/sutra/document/service/internal/FileContentValidator.java`; `DocumentServiceImpl.java`; document `application.yml` | Implemented. Allowed formats are PDF, XLS, XLSX and DOCX. |
| SUTRA calculates and stores SHA-256 digests for uploaded files. | `backend/document-service/src/main/java/com/sutra/document/service/impl/DocumentServiceImpl.java`; `backend/document-service/src/main/resources/db/migration/V1__document_schema.sql` | Implemented as a stored digest. The reviewed path does **not** reject duplicate file hashes. |
| The reviewed upload path does not provide antivirus or malware scanning. | `FileContentValidator.java`; `DocumentServiceImpl.java`; document dependencies/configuration | Deliberately called out. File format validation is not antivirus scanning. |
| Money uses `BigDecimal` in Java and `NUMERIC` in PostgreSQL; audit entries form a SHA-256 chain; a database trigger blocks update/delete for linked audit rows; an outbox relays audit events; quotation entity uses `@Version`. | `QuotationCalculator.java`; quotation `V1__quotation_schema.sql`; `AuditHashCalculator.java`; `AuditLogServiceImpl.java`; audit `V2__hash_chain_and_correlation_id.sql`; `AuditOutboxRelay.java`; `Quotation.java` | Implemented. The chain is tamper-evident and guarded by database rules. It is not described as impossible to bypass. |
| Current frontend routes include login, purchase requests, quotation review, vendor portal, awards and audit history. | `frontend/src/App.jsx`; `frontend/src/pages/PurchaseRequestsPage.jsx`; `PurchaseRequestDetailPage.jsx`; `QuotationReviewPage.jsx`; `VendorPortalPages.jsx` | Implemented. The Section 15 overview uses a source-based route diagram, not a fabricated screenshot. |

## Test results and discrepancies

| Presented result | Evidence | Qualification |
|---|---|---|
| Frontend: 52 passed across 10 files | Current `frontend/src/**/*.test.*` contains 52 `it(...)` test declarations across 10 files. | `README.md` says 54 frontend tests. The slide uses the count from the current checked-in source declarations and the report records the mismatch. No frontend tests were rerun. |
| Backend: 256 total, 0 failures, 0 errors, 1 skipped; build success | `docs/SPEC_COMPLIANCE.md` documents 256 backend tests; supplied verified results specify 0 failures, 0 errors, 1 skipped and BUILD SUCCESS. `PdfOcrReaderTest` explains its environment-based skip. | No backend tests were rerun. Existing checked-in `target/surefire-reports` XML files aggregate to a different count (307 tests), so those generated build outputs do not reconcile with the documented 256. The presentation uses the requested verified result and flags that it is not a fresh run. A clean test run should reconcile this before claiming the counts as current. |
| Python: 38 passed | `README.md`, `docs/SPEC_COMPLIANCE.md` and the supplied verified results. | No Pytest run was performed during this presentation work. |
| Acceptance: 75/75 passed on 10 November 2026 | `docs/ACCEPTANCE_RESULTS.md`, recorded run at 22:56 taking 68 seconds. | This is a previously recorded run, not a fresh execution during presentation preparation. |
| One OCR test skipped when Tesseract was unavailable | `backend/extraction-service/target/surefire-reports/com.sutra.extraction.service.internal.PdfOcrReaderTest.txt`; `PdfOcrReaderTest.java` | The skipped test is `readsTheTextOfAScannedPage`. This is not described as a passing OCR test. |

The figures are kept separate. They are not summed and are not presented as a coverage percentage.

## Features deliberately excluded or qualified

| Claim or feature | Treatment |
|---|---|
| Gemini as the active AI provider | Excluded. Gemini is a legacy extraction-source enum only; Groq is the configured active provider. |
| AI calculating totals or awarding vendors | Excluded. The backend recalculates totals; an authorised manager records an award or rejection. |
| Complete prompt-injection immunity | Excluded. Prompt instructions exist, but they cannot guarantee full resistance. |
| Duplicate-file rejection based on SHA-256 | Excluded. The digest is calculated and stored; no deduplication logic was verified. |
| Antivirus or malware scanning | Excluded. No scanning component was found in the reviewed upload path. |
| Audit chain is impossible to tamper with | Qualified as tamper-evident and database-guarded. Privileged database access remains outside this guarantee. |
| Automatic emails, purchase-order generation, mobile app, cloud deployment or Kubernetes | Excluded as implemented features. They are not established by the reviewed code paths for this presentation. |
| Actual UI screenshots | Excluded. The local frontend and API ports were not listening during preparation. Section 15 therefore labels its route map as an illustrative diagram, not an application capture. |
| Credentials, API keys, JWT secrets or environment values | Excluded from slides, notes and report. No demo credentials are invented. |

## Future work versus current implementation

The deck's future-work list is intentionally short and clearly proposed: evaluate extraction accuracy across a wider set of vendor layouts; consider malware scanning and stronger session controls; and package the services for monitored, backed-up deployment. These items are not presented as existing SUTRA features.

## Deliverable validation

- The PowerPoint contains 11 slides across Sections 12–15. Diagrams and text are editable PowerPoint elements; the uploaded logo screenshot crop is a raster image.
- Every slide has speaker notes with a natural explanation, term definitions, a SUTRA example, the point to emphasise, viva questions and source paths.
- The PowerPoint was rendered slide by slide and visually inspected. The PDF contains one rendered slide per page so its appearance matches the deck.
- The PDF and DOCX were generated from those reviewed slide contents and notes. They are not editable slide decks.
- No tests were run for this task. The live app was unavailable, so no actual app screenshots are included.
