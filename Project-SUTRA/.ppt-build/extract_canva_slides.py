from zipfile import ZipFile, ZIP_DEFLATED
from pathlib import Path
from xml.etree import ElementTree as ET
import re
import posixpath
src=Path('SUTRA_Final_Deliverables/SUTRA_Final_20_Slide_Presentation.pptx')
dst=Path('SUTRA_Final_Deliverables/SUTRA_Sections_12-15_Canva_Import.pptx')
keep_indices={12,13,14,15}
P='http://schemas.openxmlformats.org/presentationml/2006/main'
R='http://schemas.openxmlformats.org/officeDocument/2006/relationships'
REL='http://schemas.openxmlformats.org/package/2006/relationships'
CT='http://schemas.openxmlformats.org/package/2006/content-types'
ET.register_namespace('p',P); ET.register_namespace('r',R); ET.register_namespace('',REL); ET.register_namespace('',CT)
with ZipFile(src,'r') as zin:
    items={n:zin.read(n) for n in zin.namelist()}
pres=ET.fromstring(items['ppt/presentation.xml'])
rels=ET.fromstring(items['ppt/_rels/presentation.xml.rels'])
relmap={r.attrib['Id']:r.attrib.get('Target','') for r in rels}
sldlist=pres.find('{%s}sldIdLst'%P)
ids=list(sldlist)
selected=[node for i,node in enumerate(ids) if i in keep_indices]
keep_rids={node.attrib['{%s}id'%R] for node in selected}
for node in list(sldlist):
    if node.attrib['{%s}id'%R] not in keep_rids:sldlist.remove(node)
kept_slides=set()
for rid in keep_rids:
    target=relmap[rid].replace('\\\\','/')
    kept_slides.add('ppt/'+target.lstrip('/'))
# Existing targets are relative to ppt/, e.g. slides/slide13.xml
kept_slides={'ppt/'+relmap[rid].replace('\\\\','').lstrip('/') for rid in keep_rids}
kept_notes=set()
for slidepath in kept_slides:
    base=slidepath.rsplit('/',1)[-1]
    rp='ppt/slides/_rels/'+base+'.rels'
    if rp in items:
        root=ET.fromstring(items[rp])
        for rel in root:
            if rel.attrib.get('Type','').endswith('/notesSlide'):
                kept_notes.add(posixpath.normpath(posixpath.join('ppt/slides',rel.attrib['Target'])))
# Normalize kept notes from slide relationships, resolve ../
normalized_notes=set()
for slidepath in kept_slides:
    base=slidepath.rsplit('/',1)[-1]; rp='ppt/slides/_rels/'+base+'.rels'
    if rp in items:
        root=ET.fromstring(items[rp])
        for rel in root:
            if rel.attrib.get('Type','').endswith('/notesSlide'):
                target=rel.attrib['Target']
                parent=Path('ppt/slides')
                normalized_notes.add(posixpath.normpath(posixpath.join('ppt/slides',target)))
kept_notes=normalized_notes
# Keep only the presentation relationships for selected slides; preserve master, theme, notes master, etc.
for rel in list(rels):
    if rel.attrib.get('Type','').endswith('/slide') and rel.attrib.get('Id') not in keep_rids:
        rels.remove(rel)
items['ppt/presentation.xml']=ET.tostring(pres,encoding='utf-8',xml_declaration=True)
items['ppt/_rels/presentation.xml.rels']=ET.tostring(rels,encoding='utf-8',xml_declaration=True)
ct=ET.fromstring(items['[Content_Types].xml'])
for override in list(ct):
    part=override.attrib.get('PartName','').lstrip('/')
    if part.startswith('ppt/slides/slide') and part.endswith('.xml') and part not in kept_slides:
        ct.remove(override)
    if part.startswith('ppt/notesSlides/notesSlide') and part.endswith('.xml') and part not in kept_notes:
        ct.remove(override)
items['[Content_Types].xml']=ET.tostring(ct,encoding='utf-8',xml_declaration=True)
# Remove unused slide and note-slide parts and their relationship files.
for name in list(items):
    if name.startswith('ppt/slides/slide') and name.endswith('.xml') and '/_rels/' not in name and name not in kept_slides: del items[name]
    if name.startswith('ppt/slides/_rels/slide') and name.endswith('.xml.rels'):
        slide='ppt/slides/'+name.split('/')[-1][:-5]
        if slide not in kept_slides: del items[name]
    if name.startswith('ppt/notesSlides/notesSlide') and name.endswith('.xml') and '/_rels/' not in name and name not in kept_notes: del items[name]
    if name.startswith('ppt/notesSlides/_rels/notesSlide') and name.endswith('.xml.rels'):
        note='ppt/notesSlides/'+name.split('/')[-1][:-5]
        if note not in kept_notes: del items[name]
if 'docProps/app.xml' in items:
    app=items['docProps/app.xml'].decode('utf-8')
    app=re.sub(r'<Slides>\\d+</Slides>','<Slides>4</Slides>',app)
    items['docProps/app.xml']=app.encode('utf-8')
with ZipFile(dst,'w',ZIP_DEFLATED) as zout:
    for name,data in items.items():zout.writestr(name,data)
print('created',dst,'slides',len(selected),'notes',len(kept_notes),'slide_parts',sorted(kept_slides))
