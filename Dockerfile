# Pull Base Image
FROM openjdk:11

# Set Maintainer Details
LABEL maintainer="fanglu@wish.com"

# Set Environment Variables
ENV GATEWAY_HOME=/presto-gateway
#Copy presto-gateway jars
COPY gateway-ha/target $GATEWAY_HOME/target
# For testing the docker image locally
COPY gateway-ha/gateway-ha-config.yml $GATEWAY_HOME/gateway-ha-config.yml
COPY bootstrap.sh $GATEWAY_HOME/bootstrap.sh

RUN chmod +x $GATEWAY_HOME/bootstrap.sh

WORKDIR $GATEWAY_HOME
ENTRYPOINT ["./bootstrap.sh"]
