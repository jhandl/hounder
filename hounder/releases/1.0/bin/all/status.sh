#! /bin/bash

MULTICRAWLER="no"

function addItem {
  if [ -d $1 ]; then
    cd $1
    st=`./status.sh 2>/dev/null`
    cd - >/dev/null
    if echo $st | grep -q "is running"; then
      echo "$1:  running" >>$2
    else
      echo "$1:  not running" >>$2
    fi
  fi
}


opt=.start.opt
rm -f $opt

if [ $MULTICRAWLER = "yes" ];then
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

echo
IFS='
'
n=0
for o in `cat $opt`; do
  n=$((n+1))
  echo "  $o"
done
IFS=' '
echo

