#!/bin/bash

CONF=./conf
LIBS=../lib
HOUNDER=$LIBS/hounder-trunk.jar
DEPS=$LIBS/hounder-trunk-deps.jar
CP=${CONF}:.:${HOUNDER}:${DEPS}

java -cp ${CP} com.flaptor.hounder.crawler.pagedb.PageDB create pagedb pagedb.seeds
