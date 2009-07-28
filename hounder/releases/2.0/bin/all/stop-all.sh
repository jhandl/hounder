#! /bin/bash

MULTICRAWLER=no

function addItem {
  if [ -d $1 ]; then
    cd $1
    st=`./status.sh 2>/dev/null`
    if echo $st | grep -q "is running"; then
      ./stop.sh
    else
      echo "$1 (not running)" >>$2
    fi
    cd - >/dev/null
  fi
}


opt=.start.opt
rm -f $opt

if [ $MULTICRAWLER = "yes" ]; then
    for d in `find crawler -type d -mindepth 1 -maxdepth 1 2>/dev/null`; do
        addItem $d $opt
    done
else
    addItem crawler $opt
fi

addItem indexer $opt
addItem searcher $opt
addItem cache-server $opt
addItem clustering-web $opt


