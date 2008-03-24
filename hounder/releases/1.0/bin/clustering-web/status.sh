#! /bin/bash

if [ -f pid ]; then
	PID=`cat pid`
	if ps -p $PID | grep -q $PID
	then
		echo Clustering-web is running
	else
		echo Clustering-web is not running
	fi
else
	echo Clustering-web is not running
fi

