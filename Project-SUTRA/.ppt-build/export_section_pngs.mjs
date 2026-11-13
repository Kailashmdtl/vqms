import fs from 'node:fs/promises';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
const runtime='C:/Program Files/WindowsApps/OpenAI.CodexPrimaryRuntime.v26-1111-570-0_26.1111.570.0_x64__3k8sg7r9htsxt/dependencies/node/node_modules/@oai/artifact-tool/dist/artifact_tool.mjs';
const {PresentationFile,FileBlob}=await import(pathToFileURL(runtime).href);
const root=process.cwd();
const input=path.join(root,'SUTRA_Final_Deliverables','SUTRA_Final_20_Slide_Presentation.pptx');
const out=path.join(root,'SUTRA_Final_Deliverables','SUTRA_Sections_12-15_PNG');
await fs.mkdir(out,{recursive:true});
const ppt=await PresentationFile.importPptx(await FileBlob.load(input));
const files=[
 ['12_AI_Integration_and_Safety.png',12],
 ['13_Security_and_Data_Integrity.png',13],
 ['14_Testing_Validation_and_Results.png',14],
 ['15_Live_System_Demonstration.png',15],
];
for(const [name,index] of files){
 const blob=await ppt.export({slide:ppt.slides.items[index],format:'png',scale:2});
 await fs.writeFile(path.join(out,name),new Uint8Array(await blob.arrayBuffer()));
}
console.log(JSON.stringify({count:files.length,files:files.map(x=>path.join(out,x[0]))}));
