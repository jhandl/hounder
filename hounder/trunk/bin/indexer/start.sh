#!/bin/bash

if ./status.sh
then
    echo The indexer is already running
    exit 1
fi

rm -rf indexes/index

LOG_DIR=logs
LERR=${LOG_DIR}/indexer.err
LOUT=${LOG_DIR}/indexer.out

CONF=./conf
LIBS=../lib
HOUNDER=$LIBS/hounder-trunk.jar
HOUNDER_DEPS=$LIBS/hounder-trunk-deps.jar
CLASSPATH=${CONF}:.:${HOUNDER}:${HOUNDER_DEPS}
GET_CONF="java -cp ${CLASSPATH} com.flaptor.util.Config"
HOST=`${GET_CONF} common.properties rmiServer.host`

if [ ! -d ${LOG_DIR} ]; then
    mkdir -p ${LOG_DIR}
fi

echo Starting the indexer...
ARGS="-server -Xmx256m -Djava.rmi.server.hostname=${HOST}"
nohup java ${ARGS} -cp ${CLASSPATH} com.flaptor.hounder.indexer.MultipleRpcIndexer >${LOUT} 2>${LERR} &
echo $! >pid

