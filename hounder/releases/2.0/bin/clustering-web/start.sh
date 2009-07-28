#!/bin/bash

if ./status.sh | grep -q "is running"
then
    echo the clustering web is already running
    exit 1
fi

LOG_DIR=logs
LERR=${LOG_DIR}/clustering-web.err
LOUT=${LOG_DIR}/clustering-web.out

CONF=./conf
NUTCH_PLUGIN_BASE=..
LIBS=../lib
HOUNDER=$LIBS/hounder-trunk.jar
DEPS=$LIBS/hounder-trunk-deps.jar

CP=${CONF}:.:${HOUNDER}:${DEPS}
MAIN=com.flaptor.clusterfest.HTTPClusterfestServer

if [ ! -d ${LOG_DIR} ]; then
    mkdir -p ${LOG_DIR}
fi

GET_PORT="java -cp ${CP} com.flaptor.util.PortUtil"
PORT=`${GET_PORT} getPort clustering.web`
echo Starting the clustering web...

ARGUS="-server"
CMND="java ${ARGUS} -cp ${CP} ${MAIN} ${PORT}"
#echo ${CMND}
nohup  ${CMND} > ${LOUT} 2> ${LERR} &
echo $! >pid
if ./status.sh | grep -q "is running"
then
    echo
    echo    Access the admin webapp at http://localhost:${PORT}/
    echo
else
    echo The clustering web did not start correctly, check logs
fi

