#!/bin/bash

if ./status.sh | grep -q "is running"
then
    echo The cache server is already running
    exit 1
fi




LOG_DIR=logs
LERR=${LOG_DIR}/cache-server.err
LOUT=${LOG_DIR}/cache-server.out 

CONF=./conf
LIBS=../lib
HOUNDER=$LIBS/hounder-trunk.jar
DEPS=$LIBS/hounder-trunk-deps.jar

CP=${CONF}:.:${HOUNDER}:${DEPS}
MAIN=com.flaptor.hounder.cache.HttpCacheServer 


if [ ! -d ${LOG_DIR} ]; then
    mkdir -p ${LOG_DIR}
fi

echo Starting the cache server...

ARGUS="-server -Xms256m -Xmx256m -XX:-UseGCTimeLimit"
CMND="java ${ARGUS} -cp ${CP} ${MAIN}"
#echo ${CMND}
nohup  ${CMND} > ${LOUT} 2> ${LERR} &

echo $! >pid
