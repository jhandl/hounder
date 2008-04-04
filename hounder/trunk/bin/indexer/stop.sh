#!/bin/sh

pid=`cat pid`
if [ -z "$pid" ]; then
  echo The indexer is not running.
else
  kill $pid
  echo The indexer has stopped.
  rm pid  
fi
