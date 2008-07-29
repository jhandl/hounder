#!/bin/bash

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
GET_PORT="java -cp ${CP} com.flaptor.util.PortUtil"
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

if ./status.sh | grep -q "is running"
then
	RMI=`${GET_PORT} getPort searcher.rmi`
	XMLRPC=`${GET_PORT} getPort searcher.xml`
	WEB=`${GET_PORT} getPort searcher.webOpenSearch`

    echo
    echo    Searcher started, listening:
    echo        web:         http://localhost:${WEB}/websearch  
    echo        opensearch:  http://localhost:${WEB}/opensearch
    echo        rmi          ${RMI}
    echo        xmlrpc       ${XMLRPC}
    echo
else
    echo Searcher did not start correctly, check logs
fi

