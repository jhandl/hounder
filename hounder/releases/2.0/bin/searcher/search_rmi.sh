LIB_DIR=../lib
LOG_DIR=logs
CONF_DIR=conf
PROJECT_JAR=hounder-trunk.jar
GET_CONF="java -cp ${CONF_DIR}:${LIB_DIR}/${PROJECT_JAR} com.flaptor.util.Config"

PORT=`${GET_CONF} common.properties port.base`

java -cp ${CONF_DIR}:${LIB_DIR}/${PROJECT_JAR} com.flaptor.hounder.searcher.RmiSearcherStub localhost ${PORT} $1 0 10
