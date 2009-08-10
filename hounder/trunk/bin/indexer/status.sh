#! /bin/bash
#return 0 if the indexer is running, -1 if it's not

if [ -f pid ]; then
        PID=`cat pid`
        if ps -p $PID | grep -q $PID; then
                echo The indexer is running
                exit 0
        else
                echo The indexer is not running
                exit -1
        fi
else
        echo The indexer is not running
        exit -1
fi

