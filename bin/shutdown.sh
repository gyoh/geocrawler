#!/bin/sh

PIDFILE=geocrawler.pid

if [ -f $PIDFILE ]; then
  PID=`cat $PIDFILE`
  kill -TERM $PID
  rm $PIDFILE
fi
