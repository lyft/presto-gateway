# presto-gateway

A load balancer / proxy / gateway for presto compute engine.

## Getting started


### Build and run
run `mvn clean install` to build `presto-gateway`

Edit the [config file](/gateway-ha/gateway-ha-config.yml) and update the mysql db information.

```
cd gateway-ha/target/
java -jar gateway-ha-{{VERSION}}-jar-with-dependencies.jar server ../gateway-ha-config.yml
```
Now you can access load balanced presto at localhost:8080 port. We will refer to this as `prestogateway.lyft.com`
 
### Query History UI - check query plans etc.
PrestoGateway records history of recent queries and displays links to check query details page in respective presto cluster.  
![prestogateway.lyft.com](/docs/assets/prestogateway_query_history.png) 

### Gateway Admin UI - add and modify backend information
The Gateway admin page is used to configure the gateway to multiple backends. Existing backend information can also be modified using the same.
![prestogateway.lyft.com/entity](/docs/assets/prestogateway_ha_admin.png) 

How to setup a dev environment
----------------------------

Step 1: setup mysql. Install docker and run the below command when setting up first time:
```$xslt
docker run -d -p 3306:3306  --name mysqldb -e MYSQL_ROOT_PASSWORD=root123 -e MYSQL_DATABASE=prestogateway -d mysql:5.7
```
Next time onwards, run the following commands to start mysqldb

```$xslt
docker start mysqldb
```
Now open mysql console and install the presto-gateway tables:
```$xslt
mysql -uroot -proot123 -h127.0.0.1 -Dprestogateway

```
Once logged in to mysql console, please run [gateway-ha-persistence.sql](/gateway-ha/src/main/resources/gateway-ha-persistence.sql) to populate the tables.


Step 2: Edit the configuration `gateway-ha-config.yml`

Step 3: Add below program argument to class `HaGatewayLauncher` and debug in IDE 
```$xslt
server /path/to/gateway-ha/src/test/resources/config-template.yml
``` 

## Gateway API

### Add or update a backend
```$xslt
curl -X POST http://localhost:8080/entity?entityType=GATEWAY_BACKEND \
 -d '{  "name": "presto1", \ 
        "proxyTo": "http://presto1.lyft.com",\
        "active": true, \
        "routingGroup": "adhoc" \
    }'

curl -X POST http://localhost:8080/entity?entityType=GATEWAY_BACKEND \
 -d '{  "name": "presto2", \ 
        "proxyTo": "http://presto2.lyft.com",\
        "active": true, \
        "routingGroup": "adhoc" \
    }'

```

### Get all backends behind the gateway
```$xslt
curl -X GET http://localhost:8080/entity/GATEWAY_BACKEND
[
    {
        "active": true,
        "name": "presto1",
        "proxyTo": "http://presto1.lyft.com",
        "routingGroup": "adhoc"
    },
    {
        "active": true,
        "name": "presto2",
        "proxyTo": "http://presto2.lyft.com",
        "routingGroup": "adhoc"
    }
]
```

### Deactivate a backend
```$xslt
curl -X POST http://localhost:8080/gateway/backend/deactivate/presto2
```

### Get all active backend behind the Gateway

`curl -X GET http://localhost:8080/gateway/backend/active | python -m json.tool`
```
    [{
        "active": true,
        "name": "presto1",
        "proxyTo": "http://presto1.lyft.com",
        "routingGroup": "adhoc"
    }]
```

### Activate a backend
`curl -X POST http://localhost:8080/gateway/backend/activate/presto2`


## Contributing

Want to help build Presto Gateway? Check out our [contributing documentation](CONTRIBUTING.md)
