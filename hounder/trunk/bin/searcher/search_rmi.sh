LIB_DIR=../lib
LOG_DIR=logs
CONF_DIR=conf
PROJECT_JAR=search4j-trunk.jar
GET_CONF="java -cp ${CONF_DIR}:${LIB_DIR}/${PROJECT_JAR} com.flaptor.util.Config"

PORT=`${GET_CONF} common.properties port.base`

java -cp ${CONF_DIR}:${LIB_DIR}/${PROJECT_JAR} com.flaptor.search4j.searcher.RmiSearcherStub localhost ${PORT} $1 0 10
