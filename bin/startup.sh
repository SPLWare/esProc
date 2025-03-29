#!/bin/bash
source setEnv.sh
"$EXEC_JAVA" $(jvm_args=$(sed -n 's/.*jvm_args=\(.*\).*/\1/p' "$START_HOME"/esProc/bin/config.txt)  
echo " $jvm_args ") -cp "$START_HOME"/esProc/classes:"$RAQCLASSPATH" -Duser.language="$language" -Dstart.home="$START_HOME"/esProc  com.scudata.ide.spl.SPL

