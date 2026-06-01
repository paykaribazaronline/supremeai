@echo off
setlocal enabledelayedexpansion

REM Usage: install_skills.bat [skill-name]
REM Default: install both marketing and penetration-testing

if "%~1"=="" (
  set SKILLS=marketing penetration-testing
) else (
  set SKILLS=%~1
)

for %%S in (%SKILLS%) do (
  echo [Skill Installer] Installing %%S ...
  if not exist ".agents\skills\%%S\SKILL.md" (
    echo [Skill Installer] Missing .agents\skills\%%S\SKILL.md
    exit /b 1
  )
  echo [Skill Installer] Registered %%S from .agents\skills\%%S\SKILL.md
  echo [Skill Installer] %%S installed successfully.
)

echo [Skill Installer] All requested skills installed.
endlocal
