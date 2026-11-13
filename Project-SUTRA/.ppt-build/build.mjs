import fs from 'node:fs/promises';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const runtime = 'C:/Program Files/WindowsApps/OpenAI.CodexPrimaryRuntime.v26-1111-570-0_26.1111.570.0_x64__3k8sg7r9htsxt/dependencies/node/node_modules/@oai/artifact-tool/dist/artifact_tool.mjs';
const { Presentation, PresentationFile } = await import(pathToFileURL(runtime).href);
const root = path.resolve('.');
const build = path.join(root, '.ppt-build');
const out = path.join(root, '.ppt-output');
const W = 1280, H = 720;
const C = { red:'#E50922', deep:'#B70F20', ink:'#17191D', muted:'#606773', paper:'#F7F7F4', white:'#FFFFFF', pale:'#F0F1F2', teal:'#31A5A1', green:'#25856A', gold:'#E6AE36', line:'#D9DCE0', dark:'#22252B' };
const FONT = 'Arial';
const ppt = Presentation.create({slideSize:{width:W,height:H}});

function shape(slide, geometry, x,y,w,h, fill='none', lineFill='none', lineWidth=0, radius='rounded-none') {
  const config={geometry, position:{left:x,top:y,width:w,height:h}, fill,
    line:{style:'solid',fill:lineFill,width:lineWidth}};
  if(['rect','textbox','roundRect'].includes(geometry)) config.borderRadius=radius;
  return slide.shapes.add(config);
}
function txt(slide, text, x,y,w,h, size=20, color=C.ink, bold=false, opts={}) {
  const s=shape(slide,'textbox',x,y,w,h);
  s.text=text;
  s.text.style={fontFamily:FONT,fontSize:size,color,bold,alignment:opts.align||'left',verticalAlignment:opts.valign||'middle',wrap:true,...opts.style};
  s.text.insets=0;
  return s;
}
function base(n, title, sub='') {
  const s=ppt.slides.add(); s.background.fill=C.paper;
  shape(s,'rect',0,0,W,10,C.red);
  txt(s,'PROJECT SUTRA',64,34,220,24,14,C.red,true);
  txt(s,`${String(n).padStart(2,'0')}  /  18`,1100,34,120,24,13,C.muted,true,{align:'right'});
  txt(s,title,64,78,1120,66,38,C.ink,true);
  if(sub) txt(s,sub,66,143,1100,34,17,C.muted,false);
  shape(s,'rect',64,674,1152,1,C.line);
  txt(s,'SUTRA  •  Vendor quotation management',64,682,550,20,11,C.muted,false);
  return s;
}
function pill(slide,label,x,y,w,fill=C.red,color=C.white){
  shape(slide,'roundRect',x,y,w,30,fill,'none',0,'rounded-full');
  txt(slide,label,x+10,y+1,w-20,26,12,color,true,{align:'center'});
}
function card(slide,x,y,w,h,{fill=C.white,stroke=C.line}={}){
  shape(slide,'roundRect',x,y,w,h,fill,stroke,1,'rounded-xl');
}
function bullet(slide,text,x,y,w,size=16,color=C.ink){
  shape(slide,'ellipse',x,y+9,8,8,C.red);
  txt(slide,text,x+20,y,w-20,48,size,color,false,{valign:'top'});
}
function notes(slide, citations, extra=''){
  slide.speakerNotes.text=`Sources: ${citations.join('; ')}${extra?`\n${extra}`:''}`;
}

// 12 — AI integration and guardrails
{
 const s=base(12,'AI reads documents. People make the call.','A bounded extraction workflow keeps AI away from pricing decisions and approvals.');
 const steps=[
  {n:'01',title:'READ',body:'PDF, Excel and Word\nScanned PDFs use local OCR',c:C.red},
  {n:'02',title:'EXTRACT',body:'Groq reads structured fields\nRules provide a fallback',c:C.teal},
  {n:'03',title:'CHECK',body:'Java matches items, recalculates\namounts and flags uncertainty',c:C.gold},
  {n:'04',title:'DECIDE',body:'Buyer compares\nManager awards or rejects',c:C.ink},
 ];
 const xs=[64,360,656,952];
 for(let i=0;i<4;i++){
  const a=steps[i], x=xs[i]; card(s,x,236,250,226);
  shape(s,'ellipse',x+22,257,42,42,a.c);
  txt(s,a.n,x+22,257,42,42,13,C.white,true,{align:'center'});
  txt(s,a.title,x+22,315,208,32,21,C.ink,true);
  txt(s,a.body,x+22,356,208,72,15,C.muted,false,{valign:'top'});
  if(i<3){ shape(s,'rightArrow',x+260,326,34,20,C.line); }
 }
 shape(s,'roundRect',64,500,1152,94,C.dark,'none',0,'rounded-xl');
 txt(s,'AI extracts fields',92,522,260,30,21,C.white,true);
 txt(s,'Backend owns the maths',488,522,300,30,21,C.white,true);
 txt(s,'Manager owns the decision',890,522,290,30,21,C.white,true);
 txt(s,'No automatic award. Uncertain terms remain visible for human review.',92,560,1080,22,14,'#D8DCE2',false);
 notes(s,['README.md','docs/SUTRA_PROJECT_GUIDE.md','docs/SPEC_COMPLIANCE.md']);
}
// 13 — security
{
 const s=base(13,'Security follows the user, request and record','Authentication, role checks and audit history work across service boundaries.');
 const xs=[64,356,648,940];
 const entries=[
  ['IDENTITY','JWT sign-in','Sessions stop working after account deactivation.'],
  ['PERMISSIONS','Four roles','Backend enforces role and record-level access.'],
  ['PUBLIC ENDPOINTS','20 requests / min','IP rate limit plus lockout after five failed sign-ins.'],
  ['TRACEABILITY','Append-only audit','Hash-chained events can be checked with /verify.'],
 ];
 entries.forEach((e,i)=>{
  const x=xs[i]; shape(s,'rect',x,236,250,7,i===0?C.red:i===1?C.teal:i===2?C.gold:C.ink);
  txt(s,e[0],x,259,250,20,12,C.muted,true);
  txt(s,e[1],x,292,250,38,23,C.ink,true);
  txt(s,e[2],x,342,240,76,15,C.muted,false,{valign:'top'});
 });
 shape(s,'roundRect',64,466,1152,138,'#FCE8EA','none',0,'rounded-xl');
 txt(s,'Vendor isolation',92,490,240,28,18,C.red,true);
 txt(s,'Vendor accounts are linked to one vendor record. The server checks ownership before showing RFQs, quotations or files.',92,528,1040,54,18,C.ink,false,{valign:'top'});
 txt(s,'Four roles',92,426,150,20,12,C.muted,true);
 pill(s,'ADMIN',250,421,88,C.ink); pill(s,'BUYER',348,421,88,C.teal); pill(s,'VENDOR',446,421,96,C.gold,C.ink); pill(s,'MANAGER',552,421,108,C.red);
 notes(s,['README.md','docs/SPEC_COMPLIANCE.md','docs/diagrams/system-architecture.mmd']);
}
// 14 — data integrity
{
 const s=base(14,'Data integrity is enforced at more than one layer','Each service owns its schema. Constraints catch invalid or conflicting records.');
 // central visual database layers
 const layers=[['APPLICATION RULES','role checks • validation • recalculation',C.red],['DATABASE RULES','42 checks • 18 unique constraints',C.teal],['RELATIONSHIPS','21 cross-schema foreign keys',C.gold],['TRACE','versioned comparisons • hash-chained audit',C.ink]];
 layers.forEach((a,i)=>{
  let y=220+i*82;
  shape(s,'roundRect',70,y,650,64,C.white,C.line,1,'rounded-lg');
  shape(s,'rect',70,y,11,64,a[2]);
  txt(s,a[0],98,y+8,240,20,12,C.muted,true);
  txt(s,a[1],98,y+29,570,26,18,C.ink,true);
 });
 shape(s,'roundRect',776,220,430,310,C.dark,'none',0,'rounded-xl');
 txt(s,'POSTGRESQL',810,250,350,32,17,'#BAC2CC',true);
 txt(s,'9 service schemas',810,298,350,51,31,C.white,true);
 txt(s,'Money uses NUMERIC values. Original uploads are stored byte-for-byte with a SHA-256 digest.',810,364,344,90,17,'#D8DCE2',false,{valign:'top'});
 shape(s,'rect',810,472,340,1,'#555B65');
 txt(s,'A comparison records the quotation versions it used.',810,486,344,30,14,'#D8DCE2',false);
 txt(s,'The database protects consistency; the audit trail preserves who changed what.',70,567,1100,34,19,C.ink,true);
 notes(s,['docs/SPEC_COMPLIANCE.md','docs/SUTRA_PROJECT_GUIDE.md','README.md']);
}
// 15 — quality assurance
{
 const s=base(15,'Quality checks cover code and the full workflow','Automated checks test business rules, service contracts and an end-to-end procurement run.');
 const metrics=[['256','backend tests','JUnit + Mockito'],['38','Python tests','parsers, matcher, API'],['54','frontend tests','Vitest + React Testing Library'],['75 / 75','acceptance checks','full stack workflow']];
 metrics.forEach((m,i)=>{
  const x=64+i*292; card(s,x,230,264,180);
  txt(s,m[0],x+20,252,224,54,i===3?35:38,i===3?C.red:C.ink,true);
  txt(s,m[1],x+20,314,224,30,17,C.ink,true);
  txt(s,m[2],x+20,354,224,32,14,C.muted,false);
 });
 txt(s,'What the acceptance run exercised',64,450,470,28,18,C.ink,true);
 const checks=['PDF + Excel uploads stayed linked to each vendor','Extracted prices matched the backend totals','Incomplete warranty terms appeared as a review flag','Role restrictions and the manager award were enforced'];
 checks.forEach((t,i)=>bullet(s,t,72,492+i*36,1080,15));
 notes(s,['docs/ACCEPTANCE_RESULTS.md','README.md']);
}
// 16 — real scenario
{
 const s=base(16,'One RFQ. Three bids. A decision with context.','Automotive demo pack: five production materials for Axiom Auto Components.');
 const rows=[
  ['1','Vertex Auto','₹36,22,788.80','Warranty incomplete • 50% advance',C.red],
  ['2','Shakti Metals','₹37,02,250.00','No missing information',C.teal],
  ['3','Pioneer Industrial','₹37,23,962.00','No missing information',C.gold],
 ];
 txt(s,'RANK',74,223,90,22,12,C.muted,true); txt(s,'VENDOR',170,223,250,22,12,C.muted,true); txt(s,'GRAND TOTAL',483,223,230,22,12,C.muted,true); txt(s,'REVIEW NOTES',760,223,400,22,12,C.muted,true);
 rows.forEach((r,i)=>{
   const y=259+i*75; if(i===0) shape(s,'roundRect',64,y-8,1152,64,'#FCE8EA','none',0,'rounded-lg');
   txt(s,r[0],84,y,45,34,18,r[4],true); txt(s,r[1],170,y,280,34,19,C.ink,true); txt(s,r[2],483,y,240,34,19,C.ink,true); txt(s,r[3],760,y,420,38,15,i===0?C.deep:C.muted,false);
 });
 shape(s,'roundRect',64,510,1152,94,C.dark,'none',0,'rounded-xl');
 txt(s,'System result',90,530,180,26,13,'#B8C0CA',true);
 txt(s,'Lowest complete bid ranked first',90,557,430,30,20,C.white,true);
 txt(s,'Manager recorded the award and reason',668,557,490,30,18,C.white,true);
 notes(s,['ai/test-data/README.txt','docs/ACCEPTANCE_RESULTS.md'], 'Demo-only figures. The acceptance run records a manager award to Vertex with a note that the bearing warranty must be fixed in the purchase order.');
}
// 17 — architecture and demo flow
{
 const s=base(17,'The architecture supports a clear live demo','Follow one request from the buyer screen through vendor submission to a recorded decision.');
 const blocks=[
  {x:64,w:232,t:'USERS',b:'Buyer • vendor\nManager • admin',c:C.ink},
  {x:354,w:232,t:'WEB APP',b:'React + Vite\nScreens and workflows',c:C.red},
  {x:644,w:232,t:'API GATEWAY',b:'JWT checks\nRoutes /api requests',c:C.teal},
  {x:934,w:282,t:'SPRING SERVICES',b:'RFQ • quotation • comparison\napproval • audit • reporting',c:C.ink},
 ];
 blocks.forEach(b=>{
  shape(s,'roundRect',b.x,250,b.w,142,C.white,C.line,1,'rounded-xl');
  shape(s,'rect',b.x,250,b.w,8,b.c);
  txt(s,b.t,b.x+18,274,b.w-36,24,13,C.muted,true);
  txt(s,b.b,b.x+18,315,b.w-36,57,17,C.ink,true,{valign:'top'});
 });
 [309,599,889].forEach(x=>shape(s,'rightArrow',x,310,34,22,C.line));
 shape(s,'roundRect',354,440,560,145,'#E8F3F2','none',0,'rounded-xl');
 txt(s,'DOCUMENT READING',380,460,250,22,12,C.teal,true);
 txt(s,'Python parses files • Tesseract reads scans • Groq extracts fields',380,492,500,54,17,C.ink,true);
 shape(s,'roundRect',944,440,272,145,'#FCE8EA','none',0,'rounded-xl');
 txt(s,'POSTGRESQL',970,460,220,22,12,C.red,true);
 txt(s,'One schema per service',970,494,220,50,19,C.ink,true);
 txt(s,'DEMO RUN OF SHOW   1  Create RFQ     2  Submit quotation     3  Compare bids     4  Record award',64,615,1140,28,15,C.muted,true);
 notes(s,['docs/diagrams/system-architecture.mmd','README.md']);
}
// 18 — future and conclusion
{
 const s=base(18,'A clear base for the next release','SUTRA already supports the core RFQ-to-award workflow. These are candidate improvements.');
 const futures=[
  ['EXTRACTION','Measure accuracy on more vendor layouts and improve handling of irregular tables.'],
  ['SECURITY','Add stronger session protections and malware scanning for uploaded documents.'],
  ['OPERATIONS','Package services for cloud deployment, monitoring and backup.'],
 ];
 futures.forEach((f,i)=>{
  const x=64+i*390; card(s,x,235,354,150);
  pill(s,f[0],x+20,253,118,i===0?C.teal:i===1?C.red:C.ink);
  txt(s,f[1],x+20,300,314,68,16,C.ink,false,{valign:'top'});
 });
 shape(s,'roundRect',64,430,1152,160,C.dark,'none',0,'rounded-xl');
 txt(s,'SUTRA brings procurement evidence into one workflow.',94,458,1080,40,27,C.white,true);
 txt(s,'Documents are read. Amounts are recalculated. A manager remains accountable for the award.',94,510,1080,42,19,'#D8DCE2',false);
 pill(s,'QUESTIONS',94,558,126,C.red);
 notes(s,['docs/SUTRA_PROJECT_GUIDE.md','docs/SPEC_COMPLIANCE.md'], 'Roadmap topics are suggestions, not implemented features.');
}

await fs.mkdir(build,{recursive:true}); await fs.mkdir(out,{recursive:true});
for(let i=0;i<ppt.slides.items.length;i++){
 const blob=await ppt.export({slide:ppt.slides.items[i],format:'png',scale:1});
 await fs.writeFile(path.join(build,`slide-${String(i+12).padStart(2,'0')}.png`),new Uint8Array(await blob.arrayBuffer()));
}
const draft=path.join(build,'sutra-slides-12-18-draft.pptx');
await (await PresentationFile.exportPptx(ppt)).save(draft);
console.log(JSON.stringify({draft,slides:ppt.slides.items.length}));
