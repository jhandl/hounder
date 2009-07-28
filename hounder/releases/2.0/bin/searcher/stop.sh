pid=`cat pid`
if [ -z "$pid" ]; then
  echo "The searcher is not running."
else
  echo "Stopping the searcher..."
  kill $pid
  sleep 10s
  if [ -z "$pid" ]; then
    echo "The searcher has not stopped. Killing it..."
    kill -9 $pid
  fi
  echo The searcher has stopped.
  rm pid
fi
