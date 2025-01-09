#!/bin/bash
source setEnv.sh
"$EXEC_JAVA" -Xms128m -Xmx1024m  -cp "$START_HOME"/esProc/classes:"$RAQCLASSPATH"  -Duser.language="$language" -Dstart.home="$START_HOME"/esProc com.scudata.ide.spl.Esprocx $@
