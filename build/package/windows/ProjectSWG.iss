;This file will be executed next to the application bundle image
;I.e. current directory will contain folder ProjectSWG with application files
[Setup]
AppId={{fxApplication}}
AppName=ProjectSWG
AppVersion=0.9.1
AppVerName=ProjectSWG 0.9.1
AppPublisher=projectswg.com
AppComments=ProjectSWG
AppCopyright=Copyright (C) 2015
AppPublisherURL=http://projectswg.com/
;AppSupportURL=http://java.com/
;AppUpdatesURL=http://java.com/
DefaultDirName={pf32}\ProjectSWG
DisableStartupPrompt=Yes
DisableDirPage=No
DisableProgramGroupPage=No
DisableReadyPage=No
DisableFinishedPage=No
DisableWelcomePage=No
DefaultGroupName=ProjectSWG
;Optional License
LicenseFile=LICENSE.txt
;WinXP or above
MinVersion=0,5.1 
OutputBaseFilename=ProjectSWG
Compression=lzma
SolidCompression=yes
PrivilegesRequired=admin
SetupIconFile=ProjectSWG\ProjectSWG.ico
UninstallDisplayIcon={app}\ProjectSWG.ico
UninstallDisplayName=ProjectSWG
WizardImageStretch=No
WizardSmallImageFile=ProjectSWG-setup-icon.bmp   
ArchitecturesInstallIn64BitMode=


[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "ProjectSWG\ProjectSWG.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "ProjectSWG\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "ProjectSWG\app\ProjectSWG.exe.manifest"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\ProjectSWG"; Filename: "{app}\ProjectSWG.exe"; IconFilename: "{app}\ProjectSWG.ico"; Check: returnTrue()
Name: "{commondesktop}\ProjectSWG"; Filename: "{app}\ProjectSWG.exe";  IconFilename: "{app}\ProjectSWG.ico"; Check: returnFalse()

[Run]
Filename: "{app}\ProjectSWG.exe"; Parameters: "-install -svcName ""ProjectSWG"" -svcDesc ""ProjectSWG"" -mainExe ""ProjectSWG.exe""  "; Check: returnFalse()

[UninstallRun]
Filename: "{app}\ProjectSWG.exe "; Parameters: "-uninstall -svcName ProjectSWG -stopOnUninstall"; Check: returnFalse()

[Code]
function returnTrue(): Boolean;
begin
  Result := True;
end;

function returnFalse(): Boolean;
begin
  Result := False;
end;

function InitializeSetup(): Boolean;
begin
// Possible future improvements:
//   if version less or same => just launch app
//   if upgrade => check if same app is running and wait for it to exit
//   Add pack200/unpack200 support? 
  Result := True;
end;  
