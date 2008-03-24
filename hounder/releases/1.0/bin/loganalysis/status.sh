#! /bin/bash

if [ -f pid ]; then
	PID=`cat pid`
	if ps -p $PID | grep -q $PID
	then
		echo log analysis is running
	else
		echo log analysis is not running
	fi
else
	echo log analysis is not running
fi

