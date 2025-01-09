@echo off
call "setEnv.bat"
start "dm" %EXECJAVA% -Xms128m -Xmx9444m  -cp %START_HOME%\esProc\classes;%RAQCLASSPATH% -Duser.language=%language% -Dstart.home=%START_HOME%\esProc com.scudata.ide.spl.ServerConsole %1 %2 
