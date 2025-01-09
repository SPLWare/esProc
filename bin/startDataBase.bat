@echo off
call "setEnv.bat"
%EXECJAVA% -Xms128m -Xmx9444m  -cp %START_HOME%\common\jdbc\hsqldb-2.7.3-jdk8.jar org.hsqldb.server.Server -database.0 file:%START_HOME%\esProc\database\demo\demo -dbname.0 demo
