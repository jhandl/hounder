pid=`cat pid`
if [ -z "$pid" ]; then
  echo The cache-server is not running.
else
  kill $pid
  echo The cache-server has stopped.
  rm pid  
fi
