#! /bin/bash

if [ -f pid ]; then
	PID=`cat pid`
	if ps -p $PID | grep -q $PID
	then
		echo The searcher is running
	else
		echo The searcher is not running
	fi
else
	echo The searcher is not running
fi
