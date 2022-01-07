@echo off
call "D:\raqsoft\esProc\bin\setEnv.bat"
%EXECJAVA% -Xms128m -Xmx9663m  -cp %START_HOME%\common\jdbc\hsqldb-2.2.8.jar org.hsqldb.server.Server -database.0 file:%START_HOME%\esProc\database\demo\demo -dbname.0 demo
