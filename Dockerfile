FROM docker.artifactory.a.intuit.com/billingcomm/billing/qbes-hosting/service/qbes-hosting:cocoon AS cocoon
FROM docker.intuit.com/oicp/standard/maven/amzn-maven-corretto8:1.0.0 AS build
COPY --from=cocoon /root/.m2 /root/.m2

# USER root needed for build, since CPD does not allow root user for gold images.
# intermediate containers will be discarded at final stage, runtime image will be executed with non root user
USER root

# The following ARG and 2 LABEL are used by Jenkinsfile command
# to identify this intermediate container, for extraction of 
# code coverage and other reported values.
ARG build
LABEL build=${build}
LABEL image=build
ARG MVN_SETTINGS=settings.xml
COPY package /usr/src/package
COPY project.properties /usr/src/project.properties
COPY app/pom.xml /usr/src/app/pom.xml
COPY pom.xml /usr/src/pom.xml
COPY ${MVN_SETTINGS} /usr/src/settings.xml
COPY app /usr/src/app
RUN mvn -f /usr/src/pom.xml -s /usr/src/settings.xml clean install

FROM docker.intuit.com/oicp/standard/java/amzn-corretto-jdk8:1.1.5
ARG BUILD_TAG=latest
# ARG JIRA_PROJECT=https://jira.intuit.com/projects/<CHANGE_ME>
ARG DOCKER_IMAGE_NAME=docker.artifactory.a.intuit.com/billingcomm/billing/qbes-hosting/service/qbes-hosting:${BUILD_TAG}
ARG SERVICE_LINK=https://devportal.intuit.com/app/dp/resource/2165123113335279749

LABEL maintainer=some_email@intuit.com \
      app=qbes-hosting \
      app-scope=runtime \
      build=${build}
      
# Switch to root for installation and some other operations
USER root

COPY --from=build /usr/src/app/target/qbes-hosting-app.jar /app/qbes-hosting-app.jar
RUN chmod 644 /app/qbes-hosting-app.jar

# Download latest contrast.jar
RUN curl -o /app/contrast/javaagent/contrast.jar https://artifact.intuit.com/artifactory/generic-local/dev/security/ssdlc/contrast/java/latest/contrast.jar

COPY --from=build /usr/src/package/target/build.params.json /build.params.json
RUN chmod 644 /build.params.json

COPY --from=build /usr/src/package/entry.sh /app/entry.sh
RUN chmod 644 /app/entry.sh

RUN mkdir /app/tmp
RUN chown appuser:appuser /app -R

# Install Oracle RDS cert
ADD https://s3.amazonaws.com/rds-downloads/rds-ca-2019-root.pem /app/conf/
RUN /usr/lib/jvm/java-8-amazon-corretto/bin/keytool -import -keystore /usr/lib/jvm/java-8-amazon-corretto/jre/lib/security/cacerts -storepass changeit -noprompt -alias 'Amazon RDS Root CA 2019' -file /app/conf/rds-ca-2019-root.pem

# Remove unnecessary tools
RUN ["/home/appuser/post_harden.sh"]

USER appuser
CMD ["/bin/sh", "/app/entry.sh"]
