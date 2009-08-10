#!/bin/bash

if ./status.sh | grep -q "is running"; then
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
HOUNDER_DEPS=$LIBS/hounder-trunk-deps.jar
NUTCH_CHANGES=$NUTCH_PLUGIN_BASE/plugins/lib-http/lib-http.jar:$NUTCH_PLUGIN_BASE/plugins/protocol-httpclient/protocol-httpclient.jar
CLASSPATH=${CONF}:.:${HOUNDER}:${HOUNDER_DEPS}:${NUTCH_CHANGES}:${NUTCH_PLUGIN_BASE}
GET_CONF="java -cp ${CLASSPATH} com.flaptor.util.Config"
HOST=`${GET_CONF} common.properties rmiServer.host`

if [ ! -d ${LOG_DIR} ]; then
    mkdir -p ${LOG_DIR}
fi

echo Starting the crawler...
ARGS="-server -Xms512m -Xmx512m -Djava.rmi.server.hostname=${HOST}"
nohup java ${ARGS} -cp ${CLASSPATH} com.flaptor.hounder.crawler.Crawler >${LOUT} 2>${LERR} &
echo $! >pid

