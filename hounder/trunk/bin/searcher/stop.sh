pid=`cat pid`
if [ -z "$pid" ]; then
  echo The searcher is not running.
else
  kill $pid
  echo The searcher has stopped.
  rm pid
fi
