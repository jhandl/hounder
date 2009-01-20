#!/bin/sh
#Parameters taken as environment variables:
#    GC_DEBUGGING: set to "true" to enable jvm's gc printing to std out.
#    CLEAN: set to "true" to erase all logfiles before starting the searcher
#    RESTART: set to "true" to execute the stop script before starting the searcher.


#export JAVA_HOME=some custom path
export PATH=${JAVA_HOME}/bin:$PATH



#Java version checks
if [ "$JAVA_HOME" == "" ]
then
    echo "JAVA_HOME not set."
    exit -1
fi

JAVA_VERSION=`java -version 2>&1|grep "java version" |cut -d \" -f2`
MAJOR_JAVA_VERSION=`echo $JAVA_VERSION | cut -d \. -f1,2`
if [ "$MAJOR_JAVA_VERSION" != "1.6" ]
then
    echo "Incorrect java version found (${JAVA_VERSION})"
    exit -1
fi
echo "Using javaVM ${JAVA_VERSION}"

if [ "$RESTART" == "true" ]
then
    ./stop.sh
fi

#Checking status
if ./status.sh
then
    echo The searcher is already running
    exit 1
fi



LOG_DIR=logs
LERR=${LOG_DIR}/searcher.err
LOUT=${LOG_DIR}/searcher.out

CONF=./conf
LIBS=../lib
WINK=$LIBS/wink.jar
CP=${CONF}:.:${WINK}
GET_PORT="java -cp ${CP} com.flaptor.util.PortUtil"
MAIN=com.wink.searcher.MultipleRpcSearcher

if [ "$CLEAN" == "true" ]
then
    echo "Removing old logfiles."
    rm -f logs/*
fi

if [ ! -d ${LOG_DIR} ]; then
    mkdir -p ${LOG_DIR}
fi

GET_CONF="java -cp ${CP} com.flaptor.util.Config"
HOST=`/sbin/ifconfig |grep eth0 -A 1 |grep inet | cut -d ":" -f 2 |cut -d " " -f 1`

echo Starting the searcher...

ARGUS="-server \
        -Xms300m \
        -Xmx3500m \
        -XX:+UseConcMarkSweepGC \
        -XX:+CMSIncrementalMode \
        -XX:+CMSIncrementalPacing \
        -XX:CMSIncrementalDutyCycleMin=0 \
        -XX:+UseMembar"

GC_DEBUG_OPTIONS="-verbose:gc \
        -XX:+PrintGCApplicationConcurrentTime \
        -XX:+PrintGCApplicationStoppedTime
        -XX:+PrintGCTimeStamps \
        -XX:+PrintGCDetails \
        -XX:+PrintGCTimeStamps \
        -XX:-TraceClassUnloading"

if [ "$GC_DEBUGGING" == "true" ]
then
    echo "Starting the jvm with gc debugging options enabled. Check ${LOUT}"
    ARGUS="$ARGUS $GC_DEBUG_OPTIONS"
else
    echo "Starting the jvm with gc debugging options disabled."
fi


CMND="java ${ARGUS} -cp ${CP} -Djava.rmi.server.hostname=${HOST} ${MAIN}"
nohup  ${CMND} > ${LOUT} 2> ${LERR} &
echo $! >pid


sleep 10s
if ./status.sh
then
        RMI=`${GET_PORT} getPort searcher.rmi`
        XMLRPC=`${GET_PORT} getPort searcher.xml`
        WEB=`${GET_PORT} getPort searcher.webOpenSearch`
        ONC=`${GET_PORT} getPort searcher.onc`


    echo
    echo    Searcher started, listening:
    echo        web:         http://localhost:${WEB}/websearch
    echo        opensearch:  http://localhost:${WEB}/opensearch
    echo        rmi          ${RMI}
    echo        xmlrpc       ${XMLRPC}
    echo        onc rpc      ${ONC}
    echo
else
    echo Searcher did not start correctly, check the logs
fi

