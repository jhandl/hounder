#!/bin/sh

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
S4J=$LIBS/search4j-trunk.jar
DEPS=$LIBS/search4j-trunk-deps.jar

CP=${CONF}:.:${S4J}:${DEPS}
MAIN=com.flaptor.search4j.cache.HttpCacheServer 


if [ ! -d ${LOG_DIR} ]; then
    mkdir -p ${LOG_DIR}
fi

echo Starting the cache server...

ARGUS="-server -Xms256m -Xmx256m -XX:-UseGCTimeLimit"
CMND="java ${ARGUS} -cp ${CP} ${MAIN}"
#echo ${CMND}
nohup  ${CMND} > ${LOUT} 2> ${LERR} &

echo $! >pid
