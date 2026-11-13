import json
from pathlib import Path

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.shared import Inches, Pt, RGBColor
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from reportlab.pdfgen import canvas
from reportlab.lib.utils import ImageReader

root = Path.cwd()
build = root / ".ppt-build"
notes = json.loads((build / "speaker-notes.json").read_text(encoding="utf-8"))

# Create a visual PDF version of the rendered slides so it exactly matches the deck.
pdf_path = root / "SUTRA_Final_Presentation_Sections_12-15.pdf"
page_w, page_h = 960, 540
c = canvas.Canvas(str(pdf_path), pagesize=(page_w, page_h), pageCompression=1)
c.setTitle("PROJECT SUTRA Sections 12–15")
c.setAuthor("PROJECT SUTRA")
for i in range(1, 12):
    img = ImageReader(str(build / "rendered" / f"slide-{i:02}.png"))
    c.drawImage(img, 0, 0, width=page_w, height=page_h, preserveAspectRatio=False, mask="auto")
    c.showPage()
c.save()

# Build a speaker notes document with a concise overview and the complete script.
doc = Document()
section = doc.sections[0]
section.top_margin = Inches(.72)
section.bottom_margin = Inches(.68)
section.left_margin = Inches(.82)
section.right_margin = Inches(.82)
styles = doc.styles
styles['Normal'].font.name = 'Arial'
styles['Normal'].font.size = Pt(10.5)
styles['Normal'].font.color.rgb = RGBColor(40, 43, 48)
styles['Normal'].paragraph_format.space_after = Pt(6)
for name, size, color in [('Title', 26, '17191D'), ('Heading 1', 18, 'BD1222'), ('Heading 2', 12, 'BD1222'), ('Heading 3', 10.5, '5F6670')]:
    st = styles[name]
    st.font.name = 'Arial'
    st.font.size = Pt(size)
    st.font.bold = True
    st.font.color.rgb = RGBColor.from_string(color)
    st.paragraph_format.space_before = Pt(8)
    st.paragraph_format.space_after = Pt(4)

title = doc.add_paragraph(style='Title')
title.add_run('PROJECT SUTRA\n')
subtitle = title.add_run('Speaker Notes for Sections 12–15')
subtitle.font.size = Pt(15)
subtitle.font.color.rgb = RGBColor.from_string('5F6670')
intro = doc.add_paragraph()
intro.add_run('Use these notes as a speaking guide, not a script to read word for word. ').bold = True
intro.add_run('The accompanying deck contains 11 editable slides. Its Section 15 diagrams are based on actual frontend routes because the local app was not running during preparation.')
doc.add_paragraph('Core idea: AI reads. The backend calculates. People decide.', style='Subtitle')
doc.add_page_break()

def add_page_field(paragraph):
    paragraph.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    r = paragraph.add_run('SUTRA  •  Presenter notes  |  Page ')
    r.font.name = 'Arial'; r.font.size = Pt(8); r.font.color.rgb = RGBColor.from_string('707780')
    fld = OxmlElement('w:fldSimple'); fld.set(qn('w:instr'), 'PAGE')
    paragraph._p.append(fld)

footer = section.footer.paragraphs[0]
add_page_field(footer)

for i, item in enumerate(notes):
    if i:
        doc.add_page_break()
    doc.add_heading(f"Section {item['section']}  |  Slide {i+1}: {item['title']}", level=1)
    raw = item['notes']
    blocks = {}
    for header in ['What this slide explains', 'Natural speaking script', 'Technical terms', 'SUTRA example', 'Point to emphasise', 'Likely viva questions', 'Sources']:
        marker = header + '\n'
        if marker in raw:
            blocks[header] = raw.split(marker, 1)[1].split('\n\n', 1)[0].strip()
    for header in ['What this slide explains', 'Natural speaking script', 'Technical terms', 'SUTRA example', 'Point to emphasise']:
        if header not in blocks:
            continue
        doc.add_heading(header, level=2)
        p = doc.add_paragraph(blocks[header])
        if header == 'Point to emphasise':
            p.paragraph_format.left_indent = Inches(.18)
            for run in p.runs:
                run.bold = True
                run.font.color.rgb = RGBColor.from_string('BD1222')
    doc.add_heading('Likely viva questions and answers', level=2)
    for line in blocks.get('Likely viva questions', '').splitlines():
        line = line.strip()
        if not line:
            continue
        p = doc.add_paragraph(style='List Bullet')
        p.paragraph_format.space_after = Pt(5)
        p.add_run(line)
    doc.add_heading('Source files', level=2)
    p = doc.add_paragraph(blocks.get('Sources', ''))
    p.paragraph_format.space_after = Pt(0)
    for run in p.runs:
        run.font.size = Pt(8)
        run.font.color.rgb = RGBColor.from_string('5F6670')

docx_path = root / 'SUTRA_Presentation_Speaker_Notes.docx'
doc.save(docx_path)
print(json.dumps({'pdf': str(pdf_path), 'docx': str(docx_path), 'slides': len(notes)}))
