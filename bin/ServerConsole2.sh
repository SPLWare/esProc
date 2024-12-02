#!/bin/bash 
source /app/bin/setEnv2.sh
"$EXEC_JAVA" -Xms128m -Xmx1024m  -cp "$START_HOME"/app/target/classes:"$START_HOME"/app/lib/*:"$START_HOME"/jdbc/* -Duser.language="$language" -Dstart.home="$START_HOME"/app com.scudata.ide.spl.ServerConsole $1 $2
