import json
from pathlib import Path
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from reportlab.pdfgen import canvas
from reportlab.lib.utils import ImageReader

root=Path.cwd(); out=root/'SUTRA_Final_Deliverables'; build=root/'.ppt-build'/'build20'
notes=json.loads((build/'notes.json').read_text(encoding='utf-8'))
RED=RGBColor(229,9,32); DARK=RGBColor(25,25,25); GRAY=RGBColor(90,90,90)

def setup(doc,title,subtitle):
    sec=doc.sections[0]; sec.top_margin=Inches(.72); sec.bottom_margin=Inches(.68); sec.left_margin=Inches(.78); sec.right_margin=Inches(.78)
    styles=doc.styles
    styles['Normal'].font.name='Arial'; styles['Normal'].font.size=Pt(10.5); styles['Normal'].font.color.rgb=DARK
    styles['Normal'].paragraph_format.space_after=Pt(5)
    for n,size in [('Title',26),('Heading 1',17),('Heading 2',12)]:
        st=styles[n]; st.font.name='Arial'; st.font.size=Pt(size); st.font.bold=True; st.font.color.rgb=RED if n!='Title' else DARK
    p=doc.add_paragraph(style='Title');p.alignment=WD_ALIGN_PARAGRAPH.LEFT;p.add_run(title)
    p=doc.add_paragraph();p.paragraph_format.space_after=Pt(16);r=p.add_run(subtitle);r.italic=True;r.font.color.rgb=GRAY;r.font.size=Pt(11)
    f=sec.footer.paragraphs[0];f.alignment=WD_ALIGN_PARAGRAPH.RIGHT;r=f.add_run('PROJECT SUTRA  •  GROUP 12');r.font.name='Arial';r.font.size=Pt(8);r.font.color.rgb=GRAY

def label(doc,txt):
    p=doc.add_paragraph();p.paragraph_format.space_before=Pt(5);p.paragraph_format.space_after=Pt(2);r=p.add_run(txt);r.bold=True;r.font.color.rgb=RED;r.font.size=Pt(10)

doc=Document();setup(doc,'SUTRA Speaker Notes','Slide-by-slide speaking script and viva prompts for the 20-slide presentation')
p=doc.add_paragraph('Use these notes as a guide rather than a script to read word for word. Explain the workflow in your own words, distinguish current features from future work, and describe the testing numbers as recorded results rather than a fresh run.');p.paragraph_format.space_after=Pt(14)
for i,n in enumerate(notes,1):
    doc.add_heading('Slide '+str(i)+' — '+n['title'],level=1)
    label(doc,'Speaking script');doc.add_paragraph(n['script'])
    label(doc,'Technical details');doc.add_paragraph(n['terms'])
    label(doc,'SUTRA example');doc.add_paragraph(n['example'])
    label(doc,'Likely viva question');doc.add_paragraph(n['viva'])
    if n.get('sources'):
        p=doc.add_paragraph();p.paragraph_format.space_before=Pt(1);r=p.add_run('Source trail: ');r.bold=True;r.font.color.rgb=GRAY
        r=p.add_run(n['sources']);r.font.size=Pt(8);r.font.color.rgb=GRAY
    if i in (5,10,15):doc.add_page_break()
doc.save(out/'SUTRA_Speaker_Notes.docx')

qas=[
('What does SUTRA stand for?','Supplier Unified Tender & Request Automation. It connects supplier onboarding, RFQs, quotations, comparison and a recorded decision.','What does “sutra” mean here?','A thread or rule that connects steps; it is the project’s organizing metaphor.'),
('What problem does SUTRA solve?','It reduces manual handling of vendor quotations in different formats and centralizes extraction, validation, comparison and audit history.','Does it remove procurement judgment?','No. The manager remains responsible for the award or rejection.'),
('Why use a microservices architecture?','The project separates identity, master data, RFQ, quotation, documents, extraction, comparison, approval, audit and reporting responsibilities.','How many Spring services are shown?','Ten business services plus the API Gateway, eleven Spring Boot deployables.'),
('Is Python the component that calls Groq?','No. Java’s extraction service calls Groq. Python FastAPI parses documents and provides rule-based extraction and matching helpers.','What is the purpose of FastAPI?','It exposes the Python document processing endpoints.'),
('Which AI provider is active?','Groq is configured as the active provider. The default model is openai/gpt-oss-120b, with openai/gpt-oss-20b and qwen/qwen3.8-27b as configured fallbacks.','Is Gemini active?','No. A legacy enum value remains, but Gemini is not an active integration.'),
('How does quotation extraction work?','The system validates and stores the file, extracts text or tables, uses OCR for scanned PDFs when configured, asks Groq for structured fields, then validates and recalculates in Java.','What happens if model output is uncertain?','The system can flag values, allow retry with rules, or permit manual entry.'),
('Why use strict JSON output?','A schema constrains the response to expected fields and types, making validation and downstream processing more predictable.','Does a schema guarantee correct values?','No. Java validation, matching, recalculation and human review are still required.'),
('How does SUTRA handle prompt injection in vendor files?','The model instructions say to treat uploaded content as data and ignore embedded commands. This is a defense in depth measure, not a guarantee of complete protection.','Can an uploaded file still cause errors?','Yes. The content may be misleading, malformed or unclear, so uncertain values need review.'),
('What can Sahayak do?','It answers staff questions in Hindi, Hinglish or English using current SUTRA context and can suggest RFQ drafts from pasted text.','Can it create or approve an RFQ automatically?','No. The user must review and explicitly create a draft; Sahayak cannot approve or award.'),
('What are the four user roles?','ADMIN, PROCUREMENT, VENDOR and APPROVER. Each has different responsibilities and access.','How is vendor isolation enforced?','Authorization checks include vendor association and record-level access, not just the visible page.'),
('Are JWTs encrypted?','No. SUTRA uses signed HS256 JWTs. Signing lets the system verify integrity; it does not encrypt the token contents.','How long do tokens last?','User tokens default to eight hours; internal service tokens last five minutes.'),
('What sign-in protections are implemented?','BCrypt password verification, active-account and token-version checks, role authorization, a five-failure 15-minute lockout, and a default 20 login/signup requests per minute per IP.','What is a limitation of the lockout and limiter?','Their state is in memory, so they are single-instance controls and reset on restart.'),
('How are uploads validated?','SUTRA sanitizes filenames, checks extension and file signatures, validates Office package structure and enforces a default 20 MB limit.','Does that mean it has antivirus?','No. No established antivirus or malware scanning was verified.'),
('Does the SHA-256 file digest reject duplicate uploads?','No. The digest is stored, but the reviewed upload flow does not use it to reject duplicate files.','What does the digest help with?','It provides a recorded fingerprint for comparison or later integrity checks.'),
('How are monetary totals kept accurate?','Java uses BigDecimal, PostgreSQL stores NUMERIC values, and the quotation calculator recalculates line and grand totals with HALF_UP rounding.','Does Groq calculate totals?','No. Model-provided totals are not authoritative.'),
('Is the audit log tamper-proof?','It is tamper-evident: events are linked by a SHA-256 hash chain, protected from normal updates/deletes, and relayed through an outbox. It is not impossible for a privileged operator to bypass controls.','Why use the term tamper-evident?','It accurately describes that unauthorized changes can be detected, without promising absolute immunity.'),
('Who decides which vendor wins?','An authorized manager records an award or rejection. The comparison report provides advisory ranking and flags.','What if the manager does not pick the lowest complete offer?','A reason is required under the decision rules.'),
('What are the quotation and RFQ states?','RFQs move through states such as DRAFT, OPEN, UNDER_REVIEW, APPROVED, REJECTED or CANCELLED. Quotations can be UPLOADED, PROCESSING, READY_FOR_COMPARISON, NEEDS_REVIEW or FAILED.','Can an RFQ be approved by setting a status manually?','The decision is intended to go through the manager approval action and its backend rules.'),
('What test results are reported?','Frontend: 52 source test declarations across 10 files; backend: documented 256, zero failures, zero errors, one skipped; Python: 38; acceptance: 75/75 in the recorded 10 Nov 2026 run.','Were these all rerun for this presentation?','No. They are recorded/documented results, not a fresh run. The README and generated reports contain count discrepancies that should be reconciled.'),
('Why was an OCR test skipped?','PdfOcrReaderTest.readsTheTextOfAScannedPage was skipped because Tesseract was unavailable in that recorded test environment.','Does the skip mean OCR works?','It means this specific test did not validate OCR behavior in that environment.'),
('What is needed for a live demo?','PostgreSQL, Spring services, Python FastAPI, a configured Groq key and network access; Tesseract is needed for scanned-PDF OCR.','What if Groq is unavailable?','The quotation workflow may try the rules engine; Sahayak needs Groq, so skip that feature and use prepared evidence.'),
('What are the main current limitations?','No MFA or refresh-token flow, sessionStorage token storage, no established antivirus scanning, in-memory rate limiting, OCR uncertainty, and no implemented email or purchase-order generation.','Which one would you address first?','A reasonable first improvement is stronger session and MFA support, followed by malware scanning and distributed rate limiting.'),
('What is the future scope?','Possible work includes MFA, malware scanning, improved OCR, notifications, purchase-order generation, ERP integration, cloud deployment, CI/CD and analytics.','Are these features already available?','No. They are proposed future enhancements, not current implementation.'),
('How would you explain the sample acceptance result?','The recorded acceptance run checked a buyer RFQ and three vendor quotations through extraction, comparison, a warranty warning, manager approval, award PDF and audit history. It passed 75 of 75 checks on 10 November 2026.','Does that prove production readiness?','No. It is useful end-to-end evidence for the tested scenario, not a production readiness guarantee.'),
('What is the main design principle of SUTRA?','AI reads candidate values, the backend recalculates exact amounts, and people make accountable approval decisions.','Why split these responsibilities?','It uses AI for flexible document reading while keeping money calculations deterministic and decisions auditable.'),
]
doc=Document();setup(doc,'SUTRA Viva Questions and Answers','Likely technical questions with concise answers and follow-up prompts')
p=doc.add_paragraph('Use these answers as a technical guide. When asked about results, identify their source and date. When asked about planned work, say clearly that it is future scope. Avoid claiming features that were not verified in the repository.');p.paragraph_format.space_after=Pt(14)
for i,(q,a,fq,fa) in enumerate(qas,1):
    doc.add_heading(str(i)+'. '+q,level=1)
    p=doc.add_paragraph();r=p.add_run('Answer  ');r.bold=True;r.font.color.rgb=RED;p.add_run(a)
    p=doc.add_paragraph();p.paragraph_format.left_indent=Inches(.22);r=p.add_run('Follow-up  ');r.bold=True;r.font.color.rgb=GRAY;p.add_run(fq+' '+fa)
doc.save(out/'SUTRA_Viva_Questions_and_Answers.docx')

pdf=out/'SUTRA_Final_20_Slide_Presentation.pdf'; c=canvas.Canvas(str(pdf),pagesize=(960,540))
for i in range(1,21):
    im=ImageReader(str(build/'rendered'/('slide-'+str(i).zfill(2)+'.png')))
    c.drawImage(im,0,0,width=960,height=540,preserveAspectRatio=False,mask='auto');c.showPage()
c.save()
(out/'SUTRA_Assets'/'README.txt').write_text('The SUTRA emblem is the original project logo copied from docs/brand/sutra-logo-512.png. Diagrams, tables, cards and text are editable PowerPoint objects; the logo is the only raster asset embedded in the deck.\\n',encoding='utf-8')
print('created',len(notes),'notes and',len(qas),'viva questions')
