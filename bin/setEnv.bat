set START_HOME="..\..\..\scudata"
set JAVA_HOME="%JAVA_HOME%"
set EXECJAVA=%JAVA_HOME%\jre\bin\java
set EXECJAVAW=%JAVA_HOME%\jre\bin\javaw
set RAQCLASSPATH=%START_HOME%\esProc\lib\*;%START_HOME%\common\jdbc\*
set language=en
set "configFile=%START_HOME%\esProc\bin\config.txt"
set "targetKey=jvm_args"
for /f "usebackq delims=" %%i in (`powershell -Command "Get-Content '%configFile%' | Where-Object { $_ -match '^%targetKey%=.*' } | ForEach-Object { $_.Trim() -replace '^%targetKey%=|;$' }"`) do (
    set "value=%%i"
    goto :breakLoop
)
:breakLoop
if defined value (
    set "value = !valueWithKey:%targetKey% =!"
) else (
	echo ""
)
set v1=%value% 
set "jvm_args=%v1:~9%"
echo %jvm_args%
