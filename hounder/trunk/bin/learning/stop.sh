pid=`cat pid`
if [ -z "$pid" ]; then
  echo The learning webapp is not running.
else
  kill $pid
  echo The learning webapp has stopped
  rm pid  
fi

pid=`cat pid_cache_urls`
if [ -z "$pid" ]; then
  echo The learning BG url fetcher is not running.
else
  kill $pid
  echo The learning BG url fetcher has stopped
  rm pid_cache_urls
fi
