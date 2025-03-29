@echo off
call "setEnv.bat"
start "dm" %EXECJAVAW% %jvm_args% -cp %START_HOME%\esProc\classes;%RAQCLASSPATH% -Duser.language=%language% -Dstart.home=%START_HOME%\esProc   com.scudata.ide.spl.SPL
