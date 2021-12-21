#!/bin/bash
source /raqsoft/esProc/bin/setEnv.sh
"$EXEC_JAVA" -Xms128m -Xmx1024m  -cp "$START_HOME"/esProc/classes:"$START_HOME"/esProc/lib/*:"$START_HOME"/common/jdbc/* -Duser.language="$language" -Dstart.home="$START_HOME"/esProc -javaagent:"$START_HOME"/esProc/lib/rqagent-1.0.1.jar com.scudata.ide.spl.SPL
