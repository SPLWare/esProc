@echo off
call "setEnv.bat"
%EXECJAVA% -Xms128m -Xmx4818m  -cp %START_HOME%\esProc\classes;%RAQCLASSPATH% -Duser.language=%language% -Dstart.home=%START_HOME%\esProc   com.scudata.ide.spl.Esprocx  %1 %2 
