#!/bin/bash
set -xe

#echo "Fetching sentry dsn"
#sentry_dsn=$(aws ssm get-parameter --name "sentryio.dw.eventingestor" | grep "Value" | awk -F\" '{print $4}')

mkdir -p /etc/presto-gateway

curl -s https://consul.moengage.com/v1/kv/dw-presto/presto-gateway/gateway-ha-config.yml | jq -r '.[] | .Value' | base64 --decode > /etc/presto-gateway/gateway-ha-config.yml

java -jar /app/gateway-ha-jar-with-dependencies.jar server /etc/presto-gateway/gateway-ha-config.yml

