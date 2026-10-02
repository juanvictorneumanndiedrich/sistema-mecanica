@echo off
setlocal enableextensions
chcp 65001 >nul
rem ==== Genera TallerJB.exe (con Java incluido) y crea el acceso directo en el Escritorio ====
cd /d "%~dp0.."
set "RAIZ=%CD%"
echo.
echo === Taller JB: generando ejecutable ===
echo.

rem ---- 1) Buscar un JDK 21 (necesita javac y jpackage) ----
set "JDK="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jpackage.exe" set "JDK=%JAVA_HOME%"
if not defined JDK for /d %%D in ("%ProgramFiles%\Java\jdk-2*" "%ProgramFiles%\Eclipse Adoptium\jdk-2*" "%ProgramFiles%\Microsoft\jdk-2*" "%ProgramFiles%\Zulu\zulu-2*" "%ProgramFiles%\BellSoft\LibericaJDK-2*" "%ProgramFiles%\Amazon Corretto\jdk2*") do if exist "%%~D\bin\jpackage.exe" set "JDK=%%~D"
if not defined JDK for /f "delims=" %%J in ('where jpackage 2^>nul') do if not defined JDK for %%K in ("%%~dpJ..") do set "JDK=%%~fK"
if not defined JDK (
  echo ERROR: no encontre un JDK 21 instalado ^(hace falta el JDK, no solo el JRE^).
  echo Instale "Temurin JDK 21" desde https://adoptium.net y vuelva a ejecutar este archivo.
  pause & exit /b 1
)
set "JAVA_HOME=%JDK%"
echo JDK: %JDK%

rem ---- 2) Buscar Maven; si no existe lo descarga a %LOCALAPPDATA% ----
set "MVN="
for /f "delims=" %%M in ('where mvn.cmd 2^>nul') do if not defined MVN set "MVN=%%M"
if not defined MVN if exist "%LOCALAPPDATA%\TallerJB-build\maven\bin\mvn.cmd" set "MVN=%LOCALAPPDATA%\TallerJB-build\maven\bin\mvn.cmd"
if not defined MVN (
  echo Maven no encontrado, descargando una copia portatil...
  mkdir "%LOCALAPPDATA%\TallerJB-build" 2>nul
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; $z=\"$env:LOCALAPPDATA\TallerJB-build\maven.zip\"; Invoke-WebRequest 'https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip' -OutFile $z; Expand-Archive $z \"$env:LOCALAPPDATA\TallerJB-build\" -Force; Move-Item \"$env:LOCALAPPDATA\TallerJB-build\apache-maven-3.9.9\" \"$env:LOCALAPPDATA\TallerJB-build\maven\" -Force"
  set "MVN=%LOCALAPPDATA%\TallerJB-build\maven\bin\mvn.cmd"
)
if not exist "%MVN%" (
  echo ERROR: no se pudo obtener Maven. Revise la conexion a internet.
  pause & exit /b 1
)
echo Maven: %MVN%

rem ---- 3) Compilar y empaquetar (TallerJB.jar + lib\) ----
call "%MVN%" -q clean package -DskipTests
if errorlevel 1 ( echo ERROR en la compilacion. & pause & exit /b 1 )

rem ---- 4) Armar el .exe con jpackage (incluye su propio Java) ----
if exist "%RAIZ%\dist" rmdir /s /q "%RAIZ%\dist"
mkdir "%RAIZ%\dist"
mkdir "%RAIZ%\target\app"
copy /y "%RAIZ%\target\TallerJB.jar" "%RAIZ%\target\app\" >nul
xcopy /e /i /y /q "%RAIZ%\target\lib" "%RAIZ%\target\app\lib" >nul
"%JDK%\bin\jpackage.exe" --type app-image --name TallerJB --input "%RAIZ%\target\app" --main-jar TallerJB.jar --main-class com.mecanica.view.Main --icon "%RAIZ%\exe\taller.ico" --dest "%RAIZ%\dist" --java-options "-Dfile.encoding=UTF-8"
if errorlevel 1 ( echo ERROR en jpackage. & pause & exit /b 1 )
if not exist "%RAIZ%\dist\TallerJB\TallerJB.exe" ( echo ERROR: no se genero TallerJB.exe & pause & exit /b 1 )

rem ---- 5) Acceso directo en el Escritorio ----
powershell -NoProfile -ExecutionPolicy Bypass -Command "$s=(New-Object -ComObject WScript.Shell).CreateShortcut([Environment]::GetFolderPath('Desktop')+'\Taller JB.lnk'); $s.TargetPath='%RAIZ%\dist\TallerJB\TallerJB.exe'; $s.WorkingDirectory='%RAIZ%\dist\TallerJB'; $s.IconLocation='%RAIZ%\dist\TallerJB\TallerJB.exe'; $s.Save()"

rem ---- refrescar cache de iconos de Windows ----
ie4uinit.exe -show >nul 2>&1

echo.
echo LISTO: %RAIZ%\dist\TallerJB\TallerJB.exe
echo Se creo el acceso directo "Taller JB" en el Escritorio.
echo Para anclarlo a la barra de tareas: clic derecho en el acceso directo ^> "Anclar a la barra de tareas".
echo Recuerde: PostgreSQL debe estar iniciado para que el sistema abra.
echo.
pause
