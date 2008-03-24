LIB_DIR=../lib
CONF_DIR=conf
PROJECT_JAR=hounder-trunk.jar

java -cp ${CONF_DIR}:${LIB_DIR}/${PROJECT_JAR} com.flaptor.hounder.installer.CrawlerConfigurationWizard .
