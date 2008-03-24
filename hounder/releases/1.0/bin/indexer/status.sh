#! /bin/bash

if [ -f pid ]; then
	PID=`cat pid`
	if ps -p $PID | grep -q $PID
	then
		echo The indexer is running
	else
		echo The indexer is not running
	fi
else
	echo The indexer is not running
fi

