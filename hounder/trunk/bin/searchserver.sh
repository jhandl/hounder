#UPDATER=bin/syncDirectorySearcher.sh
#if [ ! -f "$UPDATER" ]; then
#    echo "The sync script could be not be found. Exiting"
#    exit
#fi
#bash $UPDATER &
##The updater script should give us its pid int the pid.tmp file
##Lets give it some time to write the file
#sleep 1
#if [ ! -f "pid.tmp" ]; then
#    echo "The sync script didn't return its pid. I will exit now, but you should kill that process manually"
#    exit
#else
#    PID=`cat pid.tmp`
#    rm pid.tmp
#fi

#Now that we have the syncer running we may run the main process.
$JAVA_HOME/bin/java -Xmx256m -cp lib/search4j.jar com.flaptor.util.XmlrpcServer com.flaptor.search4j.searcher.XmlSearcher 9013

#The main process has ended. There is no need for the syncer process to keep running.
echo "Killing the syncer process..."
kill $PID

