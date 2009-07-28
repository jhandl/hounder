#!/bin/bash

#this command synchronizes the wink flaptor tree
#($WINKHOME/src/com/flaptor) with the changes made in our master
#pass as parameter the path to the wink checkout

if [ "" == "$*" ]; then
    echo "You didn't supply the path to wink."
    exit
fi
if [ ! -d "$*" ]; then
    echo "The supplied path does not exists."
    exit
fi

rsync -av --delete --exclude *.svn* src/com/flaptor/ ${*}/src/com/flaptor
