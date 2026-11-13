import path from 'node:path';
import { pathToFileURL } from 'node:url';
const skill='C:/Users/harsh/.codex/plugins/cache/openai-primary-runtime/presentations/26.1111.10930/skills/presentations';
const {finalizePresentation}=await import(pathToFileURL(path.join(skill,'container_tools/artifact_tool_utils.mjs')).href);
const workspaceDir=path.resolve('.');
const result=await finalizePresentation({
  explicitTotalSlideCount:11,
  requiredNativeTableOwnerSlides:[],
  requiredNativeChartOwnerSlides:[],
  workspaceDir,
  candidatePath:path.join(workspaceDir,'.ppt-build/SUTRA_Final_Presentation_Sections_12-15.draft.pptx'),
  finalPath:path.join(workspaceDir,'.ppt-output/SUTRA_Final_Presentation_Sections_12-15.pptx'),
  pythonExecutable:'C:/Program Files/WindowsApps/OpenAI.CodexPrimaryRuntime.v26-1111-570-0_26.1111.570.0_x64__3k8sg7r9htsxt/dependencies/python/python.exe',
  integrityValidatorPath:path.join(skill,'container_tools/inspect_presentation_package_integrity.py'),
  layoutValidatorPath:path.join(skill,'container_tools/inspect_presentation_layout_geometry.py'),
  layoutArgs:['--expected-slide-size-emu','12192000,6858000','--validate-bullet-geometry','--validate-heading-fit'],
  fontPolicy:{basis:'design',families:['Arial']},
  verifyArtifactToolImport:true,
  receiptPath:path.join(workspaceDir,'.ppt-build/final-validation.json'),
});
console.log(JSON.stringify(result));
