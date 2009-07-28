#!/bin/bash
#This script is used in the learning app, to fetch all the urls to the cache
# in BG.
# Usage:
#   cd conf
#   ../bin/learningCacheUrls.sh cycling ~/var/local/learning/cache/ ~/var/local/learning/ urls  
#
#
#

CONF=./conf
NUTCH_PLUGIN_BASE=..
LIBS=../lib
HOUNDER=$LIBS/hounder-trunk.jar
DEPS=$LIBS/hounder-trunk-deps.jar

CP=${CONF}:.:${HOUNDER}:${DEPS}
MAIN=com.flaptor.hounder.classifier.UrlsBean


CATEGORY=$1
CACHE_DIR=$2
BASE_DIR=$3
URLS_FILE=$4
REFETCH="refetch"

CMND="java -cp $CP $MAIN  $CATEGORY $CACHE_DIR $BASE_DIR $URLS_FILE $REFETCH"
echo $CMND
$CMND

echo $! >pid_cache_urls


