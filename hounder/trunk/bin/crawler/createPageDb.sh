#!/bin/sh

CONF=./conf
LIBS=../lib
S4J=$LIBS/search4j-trunk.jar
DEPS=$LIBS/search4j-trunk-deps.jar

CP=${CONF}:.:${S4J}:${DEPS}
MAIN=com.flaptor.search4j.crawler.pagedb.PageDB

java -cp ${CP} $MAIN create pagedb pagedb.seeds
