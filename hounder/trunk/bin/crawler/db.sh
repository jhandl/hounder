#!/bin/sh

CONF=./conf
LIBS=../lib
HOUNDER=$LIBS/hounder-trunk.jar
DEPS=$LIBS/hounder-trunk-deps.jar

CP=${CONF}:.:${HOUNDER}:${DEPS}
MAIN=com.flaptor.hounder.crawler.pagedb.PageDB

java -cp ${CP} $MAIN $*

