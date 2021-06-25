# presto-gateway

A load balancer / proxy / gateway for presto compute engine.

How to setup a dev environment
------------------------------
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
### Build and run
run `mvn clean install` to build `presto-gateway`

Edit the [config file](/gateway-ha/gateway-ha-config.yml) and update the mysql db information.

```
cd gateway-ha/target/
java -jar gateway-ha-{{VERSION}}-jar-with-dependencies.jar server ../gateway-ha-config.yml
```
Now you can access load balanced presto at localhost:8080 port. We will refer to this as `prestogateway.lyft.com`

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
If the backend URL is different from the `proxyTo` URL (for example if they are internal vs. external hostnames). You can use the optional `externalUrl` field to override the link in the Active Backends page.
```$xslt
curl -X POST http://localhost:8080/entity?entityType=GATEWAY_BACKEND \
 -d '{  "name": "presto1", \ 
        "proxyTo": "http://presto1.lyft.com",\
        "active": true, \
        "routingGroup": "adhoc" \
        "externalUrl": "http://presto1-external.lyft.com",\
    }'

curl -X POST http://localhost:8080/entity?entityType=GATEWAY_BACKEND \
 -d '{  "name": "presto2", \ 
        "proxyTo": "http://presto2.lyft.com",\
        "active": true, \
        "routingGroup": "adhoc" \
        "externalUrl": "http://presto2-external.lyft.com",\
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

### Delete a backend from the gateway

```$xslt
curl -X POST -d "presto3" http://localhost:8080/gateway/backend/modify/delete
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


### Query History UI - check query plans etc.
PrestoGateway records history of recent queries and displays links to check query details page in respective presto cluster.
![prestogateway.lyft.com](/docs/assets/prestogateway_query_history.png)

### Gateway Admin UI - add and modify backend information
The Gateway admin page is used to configure the gateway to multiple backends. Existing backend information can also be modified using the same.
![prestogateway.lyft.com/entity](/docs/assets/prestogateway_ha_admin.png)

## Resource Groups API

For resource group and selector apis, we can now specify a query parameter with the request supporting multiple presto databases for different presto backends. This allows a user to configure a db for every presto backend with their own resource groups and selector tables. To use this, just specify the query parameter ?useSchema=<schemaname> to the request. Example, to list all resource groups,
 ```$xslt
curl -X GET http://localhost:8080/presto/resourcegroup/read/{INSERT_ID_HERE}?useSchema=newdatabasename
```
 
### Add a resource group
To add a single resource group, specify all relevant fields in the body. Resource group id should not be specified since the database should autoincrement it.
```$xslt
curl -X POST http://localhost:8080/presto/resourcegroup/create \
 -d '{  
        "name": "resourcegroup1", \
        "softMemoryLimit": "100%", \
        "maxQueued": 100, \
        "softConcurrencyLimit": 100, \
        "hardConcurrencyLimit": 100, \
        "environment": "test", \
        "schedulingPolicy": null, \
        "schedulingWeight": null, \
        "jmxExport": null, \
        "softCpuLimit": null, \
        "hardCpuLimit": null, \
        "parent": null, \
        "environment": "test" \
    }'
```

### Get existing resource group(s)
If no resourceGroupId (type long) is specified, then all existing resource groups are fetched. 
```$xslt
curl -X GET http://localhost:8080/presto/resourcegroup/read/{INSERT_ID_HERE}
```

### Update a resource group
Specify all columns in the body, which will overwrite properties for the resource group with that specific resourceGroupId.
```$xslt
curl -X POST http://localhost:8080/presto/resourcegroup/update \
 -d '{  "resourceGroupId": 1, \
        "name": "resourcegroup_updated", \
        "softMemoryLimit": "80%", \
        "maxQueued": 50, \
        "softConcurrencyLimit": 40, \
        "hardConcurrencyLimit": 60, \
        "environment": "test", \
        "schedulingPolicy": null, \
        "schedulingWeight": null, \
        "jmxExport": null, \
        "softCpuLimit": null, \
        "hardCpuLimit": null, \
        "parent": null, \
        "environment": "test" \
    }'
```

### Delete a resource group
To delete a resource group, specify the corresponding resourceGroupId (type long).
```$xslt
curl -X POST http://localhost:8080/presto/resourcegroup/delete/{INSERT_ID_HERE}
```

### Add a selector
To add a single selector, specify all relevant fields in the body. Resource group id should not be specified since the database should autoincrement it.
```$xslt
curl -X POST http://localhost:8080/presto/selector/create \
 -d '{  
        "priority": 1, \
        "userRegex": "selector1", \
        "sourceRegex": "resourcegroup1", \
        "queryType": "insert" \
     }'
```

### Get existing selectors(s)
If no resourceGroupId (type long) is specified, then all existing selectors are fetched. 
```$xslt
curl -X GET http://localhost:8080/presto/selector/read/{INSERT_ID_HERE}
```

### Update a selector
To update a selector, the existing selector must be specified with all relevant fields under "current". The updated version of that selector is specified under "update", with all relevant fields included. If the selector under "current" does not exist, a new selector will be created with the details under "update". Both "current" and "update" must be included to update a selector. 
```$xslt
curl -X POST http://localhost:8080/presto/selector/update \
 -d '{  "current": {
            "resourceGroupId": 1, \
            "priority": 1, \
            "userRegex": "selector1", \
            "sourceRegex": "resourcegroup1", \
            "queryType": "insert" \
        },
        "update":  {
            "resourceGroupId": 1, \
            "priority": 2, \
            "userRegex": "selector1_updated", \
            "sourceRegex": "resourcegroup1", \
            "queryType": null \
        }
}'
```

### Delete a selector
To delete a selector, specify all relevant fields in the body.
```$xslt
curl -X POST http://localhost:8080/presto/selector/delete \
 -d '{  "resourceGroupId": 1, \
        "priority": 2, \
        "userRegex": "selector1_updated", \
        "sourceRegex": "resourcegroup1", \
        "queryType": null \
     }'
```

### Add a global property
To add a single global property, specify all relevant fields in the body.
```$xslt
curl -X POST http://localhost:8080/presto/globalproperty/create \
 -d '{
        "name": "cpu_quota_period", \
        "value": "1h" \
     }'
```

### Get existing global properties
If no name (type String) is specified, then all existing global properties are fetched. 
```$xslt
curl -X GET http://localhost:8080/presto/globalproperty/read/{INSERT_NAME_HERE}
```

### Update a global property
Specify all columns in the body, which will overwrite properties for the global property with that specific name.
```$xslt
curl -X POST http://localhost:8080/presto/globalproperty/update \
 -d '{
        "name": "cpu_quota_period", \
        "value": "2h" \
     }'
```

### Delete a global property
To delete a global property, specify the corresponding name (type String).
```$xslt
curl -X POST http://localhost:8080/presto/globalproperty/delete/{INSERT_NAME_HERE}
```

## Graceful shutdown
Presto gateway supports graceful shutdown of Presto clusters. Even when a cluster is deactivated, any submitted query states can still be retrieved based on the Query ID.

To graceful shutdown a Presto cluster without query losses, the steps are:
1. Set the backend to deactivate state, this prevents any new incoming queries from getting assigned to the backend.
2. Poll the Presto backend coorinator URL until the queued query count and the running query count both hit 0.
3. Terminate the Presto Coordinator & Worker Java process.


To gracefully shutdown a single worker process, see [this](https://trino.io/docs/current/admin/graceful-shutdown.html) for the operations.


## Contributing

Want to help build Presto Gateway? Check out our [contributing documentation](CONTRIBUTING.md)

References :sparkles:
--------------------
[Scaling Presto Infra with gateway at Lyft](https://eng.lyft.com/presto-infrastructure-at-lyft-b10adb9db01)

[Presto-gateway at Pinterest](https://medium.com/pinterest-engineering/presto-at-pinterest-a8bda7515e52)

