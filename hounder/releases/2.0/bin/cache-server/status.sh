#! /bin/bash

if [ -f pid ]; then
	PID=`cat pid`
	if ps -p $PID | grep -q $PID
	then
		echo The cache-server is running
	else
		echo The cache-server is not running
	fi
else
	echo The cache-server is not running
fi

