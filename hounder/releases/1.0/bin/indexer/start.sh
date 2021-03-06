#!/bin/sh

if ./status.sh | grep -q "is running"
then
    echo The indexer is already running
    exit 1
fi

rm /tmp/lucene*
rm -rf indexes/index

LOG_DIR=logs
LERR=${LOG_DIR}/indexer.err
LOUT=${LOG_DIR}/indexer.out

CONF=./conf
LIBS=../lib
HOUNDER=$LIBS/hounder-trunk.jar
DEPS=$LIBS/hounder-trunk-deps.jar

CP=${CONF}:.:${HOUNDER}:${DEPS}
MAIN=com.flaptor.hounder.indexer.MultipleRpcIndexer

if [ ! -d ${LOG_DIR} ]; then
    mkdir -p ${LOG_DIR}
fi

GET_CONF="java -cp ${CP} com.flaptor.util.Config"
# Read common.properties. If imported files appear, will be ignored. Use com.flaptor.util.Config to read files with imported properties instead.
HOST=`${GET_CONF} common.properties rmiServer.host`

echo Starting the indexer...

ARGUS="-server -Xmx256m -Djava.rmi.server.hostname=${HOST}"
CMND="java ${ARGUS} -cp ${CP} ${MAIN}"
#echo ${CMND}
nohup  ${CMND} > ${LOUT} 2> ${LERR} &

echo $! >pid
