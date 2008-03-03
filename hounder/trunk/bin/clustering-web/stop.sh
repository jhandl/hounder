pid=`cat pid`
if [ -z "$pid" ]; then
  echo Clustering-web is not running.
else
  kill $pid
  echo Clustering-web has stopped.
  rm pid  
fi
