rm -f /tmp/lucene-*
for i in `ls lib/*jar`; do P=$P:$i; done
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/home/diego/local/lib
$JAVA_HOME/bin/java -Xmx512m  -XX:+UseParallelGC -cp $P com.flaptor.util.XmlrpcServer com.flaptor.search4j.indexer.Indexer 9003
#$JAVA_HOME/bin/java -Xrunjmp -Xmx256m -cp $P com.flaptor.util.XmlrpcServer com.flaptor.search4j.indexer.Indexer 9003
#$JAVA_HOME/bin/java -Xrunhprof:heap=sites,depth=5 -verbose:gc -Xmx256m -cp $P com.flaptor..util.XmlrpcServer com.flaptor.search4j.indexer.Indexer 9003

