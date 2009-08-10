#!/bin/bash

ok=0
if [ -f pid ]; then
  pid=`cat pid`
  if ps aux | grep -q "^$USER[[:space:]]*$pid[[:space:]]"; then
    echo -n Stopping the crawler...
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
  echo The crawler was not running.
fi

