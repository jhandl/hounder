#!/bin/bash

running=0
if [ -f pid ]; then
  PID=`cat pid`
  if ps -p $PID | grep -q $PID; then
    running=1
  fi
fi
if [ $running = 1 ]; then
  echo The crawler is running.
else
  echo The crawler is not running.
fi

