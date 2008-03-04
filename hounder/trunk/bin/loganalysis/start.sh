#!/bin/sh

if ./status.sh | grep -q "is running"
then
    echo log analysis is already running
    exit 1
fi


LOG_DIR=logs
LERR=${LOG_DIR}/loganalysis.err
LOUT=${LOG_DIR}/loganalysis.out

CONF=./conf
LIBS=../lib
HOUNDER=$LIBS/hounder-trunk.jar
DEPS=$LIBS/hounder-trunk-deps.jar

if [ ! -d ${LOG_DIR} ]; then
    mkdir -p ${LOG_DIR}
fi

CP=${CONF}:.:${HOUNDER}:${DEPS}
MAIN=com.flaptor.hounder.loganalysis.HTTPLogAnalysisServer

echo Starting the log analysis web...

ARGUS="-server"
CMND="java ${ARGUS} -cp ${CP} ${MAIN}"
#echo ${CMND}
nohup  ${CMND} > ${LOUT} 2> ${LERR} &

echo $! >pid
