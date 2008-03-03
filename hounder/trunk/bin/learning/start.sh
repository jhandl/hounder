#!/bin/sh
#This script is used in the learning app to start it
#

LOG_DIR=logs
LERR=${LOG_DIR}/learning-web.err
LOUT=${LOG_DIR}/learning-web.out

CONF=./conf
NUTCH_PLUGIN_BASE=..
LIBS=../lib
S4J=$LIBS/search4j-trunk.jar
DEPS=$LIBS/search4j-trunk-deps.jar

CP=${CONF}:.:${NUTCH_PLUGIN_BASE}:${S4J}:${DEPS}:${MYSQL}
MAIN=com.flaptor.search4j.classifier.HTTPLearningServer

if [ ! -d ${LOG_DIR} ]; then
    mkdir -p ${LOG_DIR}
fi

GET_PORT="java -cp ${CONF_DIR}:${S4J}:${DEPS} com.flaptor.util.PortUtil"
PORT=`${GET_PORT} getPort learning.web`

echo Starting the learning web...

CMND="java -cp ${CP} ${MAIN} ${PORT}" 
#echo ${CMND}
nohup  ${CMND} > ${LOUT} 2> ${LERR} &

echo $! >pid

