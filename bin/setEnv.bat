set START_HOME="..\..\..\scudata"
set JAVA_HOME="%JAVA_HOME%"
set EXECJAVA=%JAVA_HOME%\jre\bin\java
set EXECJAVAW=%JAVA_HOME%\jre\bin\javaw
set RAQCLASSPATH=%START_HOME%\esProc\lib\*;%START_HOME%\common\jdbc\*
set language=en
set "configFile=%START_HOME%\esProc\bin\config.txt"
setlocal enabledelayedexpansion
for /f "usebackq delims=" %%a in (`powershell -Command "$content = Get-Content -Path 'configFile'; ($content -split '=')[-1] -split ';' | Select-Object -First 1"`) do (
    set "jvm_args=%%a"
)
endlocal
