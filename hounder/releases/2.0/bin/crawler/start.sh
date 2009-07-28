#!/bin/bash

if ./status.sh | grep -q "is running"
then
    echo The crawler is already running
    exit 1
fi

LOG_DIR=logs
LERR=${LOG_DIR}/crawler.err
LOUT=${LOG_DIR}/crawler.out

CONF=./conf
NUTCH_PLUGIN_BASE=..
LIBS=../lib
HOUNDER=$LIBS/hounder-trunk.jar
DEPS=$LIBS/hounder-trunk-deps.jar

if [ ! -d ${LOG_DIR} ]; then
    mkdir -p ${LOG_DIR}
fi

CP=${CONF}:.:${HOUNDER}:${DEPS}:../plugins/lib-http/lib-http.jar:../plugins/protocol-httpclient/protocol-httpclient.jar:${NUTCH_PLUGIN_BASE}
MAIN=com.flaptor.hounder.crawler.Crawler

GET_CONF="java -cp ${CP} com.flaptor.util.Config"
HOST=`${GET_CONF} common.properties rmiServer.host`

name=`pwd | awk '{n=split($0,a,"/"); print a[n]}'`
echo Starting the $name crawler...

ARGUS="-server -Xms512m -Xmx512m -Djava.rmi.server.hostname=${HOST}"
CMND="java ${ARGUS} -cp ${CP} ${MAIN}"
#echo ${CMND}
nohup  ${CMND} > ${LOUT} 2> ${LERR} &

echo $! >pid

