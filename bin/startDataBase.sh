#!/bin/bash
source /Applications/esProc.app/Contents/raqsoft/esProc/bin/setEnv.sh
RAQCLASSPATH=$START_HOME/common/jdbc/hsqldb-2.2.8.jar:
"$EXEC_JAVA" -Xms128m -Xmx1024m  -cp "$RAQCLASSPATH" org.hsqldb.server.Server -database.0 file:"$START_HOME"/esProc/database/demo/demo -dbname.0 demo
