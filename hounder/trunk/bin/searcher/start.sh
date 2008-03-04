#!/bin/sh

if ./status.sh | grep -q "is running"
then
    echo The searcher is already running
    exit 1
fi




LOG_DIR=logs
LERR=${LOG_DIR}/searcher.err
LOUT=${LOG_DIR}/searcher.out

CONF=./conf
NUTCH_PLUGIN_BASE=..
LIBS=../lib
HOUNDER=$LIBS/hounder-trunk.jar
DEPS=$LIBS/hounder-trunk-deps.jar
CP=${CONF}:.:${HOUNDER}:${DEPS}:${NUTCH_PLUGIN_BASE}
MAIN=com.flaptor.hounder.searcher.MultipleRpcSearcher

if [ ! -d ${LOG_DIR} ]; then
    mkdir -p ${LOG_DIR}
fi

GET_CONF="java -cp ${CP} com.flaptor.util.Config"
HOST=`${GET_CONF} common.properties rmiServer.host`

echo Starting the searcher...

ARGUS="-server -Xms256m -Xmx256m"
CMND="java ${ARGUS} -cp ${CP} -Djava.rmi.server.hostname=${HOST} ${MAIN}"
#echo ${CMND}
nohup  ${CMND} > ${LOUT} 2> ${LERR} &
echo $! >pid
