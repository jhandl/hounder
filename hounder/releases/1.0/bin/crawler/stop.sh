#!/bin/bash


pid=`cat pid`
if [ -z "$pid" ]; then
  echo The crawler is not running.
else
  kill $pid
  echo The crawler has stopped.
  rm pid
fi

exit
########### IGNORE CODE BELOW
### THE CRAWLER DOESNT STOP. It needs to be killed

MULTICRAWLER="no"

if [ $MULTICRAWLER = "yes" ]; then
    name=`pwd | awk '{n=split($0,a,"/"); print a[n]}'`
else
    name=""
fi

ok=0
if [ -f pid ]; then
  pid=`cat pid`
  if ps aux | grep -q "^$USER[[:space:]]*$pid[[:space:]]"; then
    echo -n Stopping the $name crawler...
    echo >stop  
    while ps aux | grep -q "^$USER[[:space:]]*$pid[[:space:]]"; do
      sleep 5
      echo >stop  
      echo -n .
    done
    ok=1
  fi
fi
if [ $ok = 1 ]; then
  echo stopped.
else
  echo The $name crawler was not running.
fi

