#!/bin/sh

if [ -f /etc/secrets/application.properties ]; then
  JAVA_OPTS="${JAVA_OPTS} -Dspring.config.location=/etc/secrets/application.properties"
fi

if [ -n "${APP_ENV}" ]; then
  JAVA_OPTS="${JAVA_OPTS} -Dspring.profiles.active=${APP_ENV}"
fi

if [ -n "${PFTS_KEY_NAME}" ]; then
  JAVA_OPTS="${JAVA_OPTS} -Dqbespfts.pftsKeyName=${PFTS_KEY_NAME}"
fi

if [ -n "${PFTS_REQ_FOLDER}" ]; then
  JAVA_OPTS="${JAVA_OPTS} -Dqbespfts.pftsReqFolder=${PFTS_REQ_FOLDER}"
fi

if [ -n "${PFTS_RES_FOLDER}" ]; then
  JAVA_OPTS="${JAVA_OPTS} -Dqbespfts.pftsResFolder=${PFTS_RES_FOLDER}"
fi

if [ -n "${PFTS_ACCT_NAME}" ]; then
  JAVA_OPTS="${JAVA_OPTS} -Dqbespfts.pftsAccountName=${PFTS_ACCT_NAME}"
fi

if [ -n "${PFTS_PORT}" ]; then
  JAVA_OPTS="${JAVA_OPTS} -Dqbespfts.pftsPort=${PFTS_PORT}"
fi

if [ -n "${LOGFILE_BASEPATH}" ]; then
  JAVA_OPTS="${JAVA_OPTS} -DLOGFILE_BASEPATH=${LOGFILE_BASEPATH}"
else
  JAVA_OPTS="${JAVA_OPTS} -DLOGFILE_BASEPATH=/usr/local/tomcat/logs"
fi

JAVA_OPTS="${JAVA_OPTS} -XX:+UseContainerSupport \
  -XX:+UseG1GC -XX:+UseStringDeduplication -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=\/usr\/local\/tomcat\/logs -XX:+DisableExplicitGC \
  -XX:+PrintTenuringDistribution -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+UseGCLogFileRotation \
  -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=10M -Xloggc:\/usr\/local\/tomcat\/logs\/verbosegc.log \
  -XshowSettings:vm"

# Java parameters for java8u192+
JAVA_OPTS="${JAVA_OPTS} -XX:+UnlockExperimentalVMOptions \
  -XX:+UseG1GC \
  -XX:+UseStringDeduplication \
  -XX:+UseCGroupMemoryLimitForHeap \
  -XX:MinRAMPercentage=50.0 \
  -XX:MaxRAMPercentage=80.0 \
  -XshowSettings:vm"

# use app dir for tmp dir
JAVA_OPTS="${JAVA_OPTS} -Djava.io.tmpdir=/app/tmp"

# Is contrast enabled, yes or no
contrastassess_enabled=yes

# ENV for contrast assessment
contrastassess_env=qal
contrastassess_jar="/app/contrast/javaagent/contrast.jar"
if [ "${contrastassess_enabled}" = "yes" ] && [ "${APP_ENV}" = "${contrastassess_env}" ]; then
  JAVA_OPTS="${JAVA_OPTS} -javaagent:${contrastassess_jar}"
  JAVA_OPTS="${JAVA_OPTS} -Dcontrast.dir=/app/contrast/javaagent"
  JAVA_OPTS="${JAVA_OPTS} -Dcontrast.application.code=2165123113335279749"
  JAVA_OPTS="${JAVA_OPTS} -Dcontrast.standalone.appname=Intuit.billingcomm.billing.qbeshosting"
  JAVA_OPTS="${JAVA_OPTS} -Dcontrast.application.name=Intuit.billingcomm.billing.qbeshosting"
  JAVA_OPTS="${JAVA_OPTS} -Dcontrast.inspect.allclasses=false -Dcontrast.process.codesources=false"
  JAVA_OPTS="${JAVA_OPTS} -Dcontrast.inventory.libraries=false"
fi

# Is appdynamics enabled, yes or no
appdynamics_enabled=no

appdynamics_jar="/app/appdynamics/javaagent.jar"
if [ "${appdynamics_enabled}" = "yes" ] && [ -r ${appdynamics_jar} ] && [ -f /etc/secrets/appd-account-access-key ]; then
    export APPDYNAMICS_CONTROLLER_PORT=443
    export APPDYNAMICS_CONTROLLER_SSL_ENABLED=true

    export APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY=`cat /etc/secrets/appd-account-access-key`

    JAVA_OPTS="$JAVA_OPTS -javaagent:${appdynamics_jar}"
    JAVA_OPTS="$JAVA_OPTS -Dappdynamics.agent.applicationName=${L1}-${L2}-${APP_NAME}-${APP_ENV}"
    JAVA_OPTS="$JAVA_OPTS -Dappdynamics.agent.tierName=${APPDYNAMICS_AGENT_TIER_NAME}"
    JAVA_OPTS="$JAVA_OPTS -Dappdynamics.agent.nodeName=${APPDYNAMICS_AGENT_TIER_NAME}_${HOSTNAME}"
fi

#When sidecar is injected, wait for sidecar to come up
if [[ "$MESH_ENABLED" == "true" ]]; then
until (echo >/dev/tcp/localhost/$MESH_SIDECAR_PORT) &>/dev/null ; do echo Waiting for Sidecar; sleep 3 ; done ; echo Sidecar available;
fi

exec java $JAVA_OPTS -jar /app/qbes-hosting-app.jar
