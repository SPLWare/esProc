#!/bin/bash
source /Applications/esProc.app/Contents/raqsoft/esProc/bin/setEnv.sh
"$EXEC_JAVA" -Xms128m -Xmx1024m  -cp "$START_HOME"/esProc/classes:"$START_HOME"/esProc/lib/*:"$START_HOME"/common/jdbc/*  -Duser.language=en -Dstart.home="$START_HOME"/esProc com.scudata.ide.spl.Esproc $@
