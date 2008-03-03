LIB_DIR=../lib
CONF_DIR=conf
PROJECT_JAR=search4j-trunk.jar

java -cp ${CONF_DIR}:${LIB_DIR}/${PROJECT_JAR} com.flaptor.search4j.installer.CrawlerConfigurationWizard -graphicMode .

