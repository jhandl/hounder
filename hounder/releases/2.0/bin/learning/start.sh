#!/bin/bash
#This script is used in the learning app to start it
#

LOG_DIR=logs
LERR=${LOG_DIR}/learning-web.err
LOUT=${LOG_DIR}/learning-web.out

CONF=./conf
NUTCH_PLUGIN_BASE=..
LIBS=../lib
HOUNDER=$LIBS/hounder-trunk.jar
DEPS=$LIBS/hounder-trunk-deps.jar

CP=${CONF}:.:${NUTCH_PLUGIN_BASE}:${HOUNDER}:${DEPS}:${MYSQL}
MAIN=com.flaptor.hounder.classifier.HTTPLearningServer

if [ ! -d ${LOG_DIR} ]; then
    mkdir -p ${LOG_DIR}
fi

GET_PORT="java -cp ${CONF_DIR}:${HOUNDER}:${DEPS} com.flaptor.util.PortUtil"
PORT=`${GET_PORT} getPort learning.web`

echo Starting the learning web...

CMND="java -cp ${CP} ${MAIN} ${PORT}" 
#echo ${CMND}
nohup  ${CMND} > ${LOUT} 2> ${LERR} &

echo $! >pid

