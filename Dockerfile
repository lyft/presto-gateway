# Pull Base Image
FROM openjdk:11

# Set Maintainer Details
MAINTAINER Fang Lu <fanglu@wish.com>

# Set Environment Variables
ENV GATEWAY_HOME=/presto-gateway
#Copy presto-gateway jars
COPY gateway-ha/target $GATEWAY_HOME/target
COPY gateway-ha/gateway-ha-config.yml $GATEWAY_HOME/gateway-ha-config.yml
COPY bootstrap.sh $GATEWAY_HOME/bootstrap.sh
# COPY plugin/kafka/* $PRESTO_HOME/plugin/kafka/

RUN chmod +x $GATEWAY_HOME/bootstrap.sh
#VOLUME ["$PRESTO_HOME/data"]

WORKDIR $GATEWAY_HOME
ENTRYPOINT ["./bootstrap.sh"]
