#!/bin/bash

name=`pwd | awk '{n=split($0,a,"/"); print a[n]}'`
running=0
if [ -f pid ]; then
  PID=`cat pid`
  if ps -p $PID | grep -q $PID
  then
    running=1
  fi
fi
if [ $running = 1 ]; then
  echo The $name crawler is running.
else
  echo The $name crawler is not running.
fi

