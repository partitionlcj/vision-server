#!/bin/sh

# user dir
cd "$( dirname "${BASH_SOURCE[0]}" )"
cd ..
SHDIR=$(pwd)

echo current path:$SHDIR

echo "arguments: first:${1}, second:${2}"

# if the server is already running
PIDFILE="./start.pid"
if [ -f $PIDFILE ]; then
    if kill -9 `cat $PIDFILE` > /dev/null 2>&1; then
        echo server already running as process `cat $PIDFILE`. 
        exit 0
    fi
fi

if [[ $# == 0 ]]; then
    echo "no arguments specified, skip deploying..."
    exit 1
fi

# prepare config file and log conf file
# generate env by hostgroup

if [[ -z "$ENV" ]]; then
    ENV=${1}
fi


confFile="conf/server-conf/config.$ENV.json"
if [ -f $confFile ]; then
    cp $confFile conf/config.json
fi
confFileLog="conf/server-conf/log4j2.$ENV.xml"
if [ -f $confFileLog ]; then
    cp $confFileLog conf/log4j2.xml
fi

JVM_OPTS="$JVM_OPTS -XX:+PrintGCDetails"
JVM_OPTS="$JVM_OPTS -XX:+PrintGCDateStamps"
JVM_OPTS="$JVM_OPTS -Xloggc:gc.log"
JVM_OPTS="$JVM_OPTS -XX:+UseGCLogFileRotation"
JVM_OPTS="$JVM_OPTS -XX:NumberOfGCLogFiles=10"
JVM_OPTS="$JVM_OPTS -XX:GCLogFileSize=10M"

JVM_OPTS="$JVM_OPTS -XX:+UseG1GC"
JVM_OPTS="$JVM_OPTS -XX:MaxGCPauseMillis=100"
JVM_OPTS="$JVM_OPTS -XX:G1RSetUpdatingPauseTimePercent=5"

JVM_OPTS="$JVM_OPTS -XX:ErrorFile=/data/app/online-ds/logs/hs_error%p.log"

JVM_OPTS="$JVM_OPTS -XX:+HeapDumpOnOutOfMemoryError"
JVM_OPTS="$JVM_OPTS -XX:HeapDumpPath=$SHDIR/mem.dump"
JVM_OPTS="$JVM_OPTS -XX:-OmitStackTraceInFastThrow"

JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote.port=7601 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"


# wirte pid to file
if [ $? -eq 0 ] 
then
    if /bin/echo -n $! > "$PIDFILE"
    then
        sleep 1
        echo STARTED SUCCESS
    else
        echo FAILED TO WRITE PID
        exit 1
    fi
else
    echo SERVER DID NOT START
    exit 1
fi

