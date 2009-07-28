pid=`cat pid`
if [ -z "$pid" ]; then
  echo The crawler is not running.
else
  kill $pid
  echo The crawler is stopped.
fi
