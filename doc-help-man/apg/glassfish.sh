#!/bin/sh
#
# glassfish        Startup script for glassfish
#
# chkconfig: - 99 01
# processname: glassfish
# config: /etc/sysconfig/glassfish
# pidfile: /var/run/glassfish.pid
# description: glassfish is a JavaEE Application Server
#
### BEGIN INIT INFO
# Provides: glassfish
# Required-Start: $local_fs $remote_fs $network
# Required-Stop: $local_fs $remote_fs $network
# Default-Start:3 4 5
# Default-Stop: 0 1 2 6
# Short-Description: start and stop glassfish
### END INIT INFO

# Source function library.
. /etc/rc.d/init.d/functions

if [ -f /etc/sysconfig/glassfish ]; then
    . /etc/sysconfig/glassfish
fi

prog=glassfish
domain=domain1
ASADMIN="/opt/glassfish/glassfish/bin/asadmin"
START_OP="start-domain"
STOP_OP="stop-domain"
RESTART_OP="restart-domain"
DOMAIN_DIR="/opt/glassfish/glassfish/domains"
DOMAIN_ARGS="--domaindir ${DOMAIN_DIR} ${domain}"
lockfile=${LOCKFILE-/var/lock/subsys/glassfish}
pidfile=${PIDFILE-/var/run/glassfish.pid}
SLEEPMSEC=100000
RETVAL=0

start() {
    echo -n $"Starting $prog: "
    daemon --user glassfish --pidfile=${pidfile} ${ASADMIN} ${START_OP} ${DOMAIN_ARGS}
    RETVAL=$?
    echo
    [ $RETVAL = 0 ] && touch ${lockfile} && cp ${DOMAIN_DIR}/${domain}/config/pid ${pidfile}
    return $RETVAL
}

stop() {
    echo -n $"Stopping $prog: "
    ${ASADMIN} ${STOP_OP} ${DOMAIN_ARGS}
    RETVAL=$?
    echo
    [ $RETVAL = 0 ] && rm -f ${lockfile} ${pidfile}
}

restart() {
    echo -n $"Restarting $prog: "
    ${ASADMIN} ${RESTART_OP} ${DOMAIN_ARGS}
    RETVAL=$?
    echo
}


rh_status() {
    status -p ${pidfile} ${prog}
}

case "$1" in
    start)
        rh_status >/dev/null 2>&1 && exit 0
        start
        ;;
    stop)
        stop
        ;;
    status)
        rh_status
        RETVAL=$?
        ;;
    restart)
        restart
        ;;
    *)
        echo $"Usage: $prog {start|stop|restart|status|help}"
        RETVAL=2
esac

exit $RETVAL
