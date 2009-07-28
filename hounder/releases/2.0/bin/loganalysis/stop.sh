pid=`cat pid`
if [ -z "$pid" ]; then
  echo log analysis is not running.
else
  kill $pid
  echo log analysis has stopped.
  rm pid  
fi
