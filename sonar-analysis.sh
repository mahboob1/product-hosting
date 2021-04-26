#!/bin/bash -x

if [ "${sonar}" = "true" ] ; then
  ENFORCE=1
else
  ENFORCE=0
fi

#Set SonarQube Server
SOANR_URL="https://sonarqube.tools-k8s.a.intuit.com"

# Run sonar
#SONAR_STATUS_URL=$(mvn -s settings.xml --batch-mode org.sonarsource.scanner.maven:sonar-maven-plugin:3.6.0.1398:sonar | grep -o "$SOANR_URL.*" | tail -1)
mvn -s settings.xml --batch-mode org.sonarsource.scanner.maven:sonar-maven-plugin:3.6.0.1398:sonar > /tmp/sonar.log

echo "Sonar log:"
echo "##########################################"
cat /tmp/sonar.log
echo "##########################################"

SONAR_STATUS_URL=$(grep -o "$SOANR_URL.*" /tmp/sonar.log | tail -1)
if [[ ! "${SONAR_STATUS_URL}" =~ "${SOANR_URL}/api" ]] ; then 
  echo "ERROR: Can't get project URL" 
  echo "Exiting..."
  exit 1
fi

# Get project key
PROJECT_KEY=$(curl -s $SONAR_STATUS_URL | awk -F'"' '{print $18}')

sleep 5 

# Get analysis result
STATUS=$(curl -s $SOANR_URL/api/qualitygates/project_status?projectKey=$PROJECT_KEY | awk -F'"' '{print $6}')
if [ "$STATUS" = "OK" ] ; then
  echo "Quality Gate: $STATUS"
  echo "Results: $SOANR_URL/dashboard/index/$PROJECT_KEY"
else
  echo "Quality Gate: $STATUS"
  echo "Results: $SOANR_URL/dashboard/index/$PROJECT_KEY"
  exit $ENFORCE
fi
