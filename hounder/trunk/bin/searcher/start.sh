#!/bin/bash
#Parameters taken as environment variables:
#    GC_DEBUGGING: set to "true" to enable jvm's gc printing to std out.
#    CLEAN: set to "true" to erase all logfiles before starting the searcher
#    RESTART: set to "true" to execute the stop script before starting the searcher.


JAVA_VERSION=`java -version 2>&1 | grep "java version" | cut -d \" -f2`
MAJOR_JAVA_VERSION=`echo $JAVA_VERSION | cut -d \. -f1,2`
if [ "$MAJOR_JAVA_VERSION" != "1.6" ]; then
    echo "Incorrect java version found (${JAVA_VERSION})"
    exit -1
fi

if ./status.sh; then
    echo The searcher is already running
    exit 1
fi

LOG_DIR=logs
LERR=${LOG_DIR}/searcher.err
LOUT=${LOG_DIR}/searcher.out

CONF=./conf
LIBS=../lib
HOUNDER=$LIBS/hounder-trunk.jar
HOUNDER_DEPS=$LIBS/hounder-trunk-deps.jar
CLASSPATH=${CONF}:.:${HOUNDER}:${HOUNDER_DEPS}
GET_PORT="java -cp ${CLASSPATH} com.flaptor.util.PortUtil"
MAIN=com.flaptor.hounder.searcher.MultipleRpcSearcher

if [ ! -d ${LOG_DIR} ]; then
    mkdir -p ${LOG_DIR}
fi

GET_CONF="java -cp ${CLASSPATH} com.flaptor.util.Config"
HOST=`/sbin/ifconfig | grep eth0 -A 1 | grep inet | cut -d ":" -f 2 | cut -d " " -f 1`

echo Starting the searcher...

ARGS="-server \
      -Xms300m \
      -Xmx800m \
      -XX:+UseConcMarkSweepGC \
      -XX:+CMSIncrementalMode \
      -XX:+CMSIncrementalPacing \
      -XX:CMSIncrementalDutyCycleMin=0 \
      -XX:+UseMembar \
      -Djava.rmi.server.hostname=${HOST}"

nohup java ${ARGS} -cp ${CLASSPATH} com.flaptor.hounder.searcher.MultipleRpcSearcher >${LOUT} 2>${LERR} &
echo $! >pid

