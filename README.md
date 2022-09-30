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

### Build and run

Please note these steps have been verified with JDK 8 and 11. Higher versions of Java might run into unexpected issues. 

run `mvn clean install` to build `presto-gateway`

Edit the [config file](/gateway-ha/gateway-ha-config.yml) and update the mysql db information.

```
cd gateway-ha/target/
java -jar gateway-ha-{{VERSION}}-jar-with-dependencies.jar server ../gateway-ha-config.yml
```

If you encounter a `Failed to connect to JDBC URL` error, this may be due to newer versions of java disabling certain algorithms
when using SSL/TLS, in particular `TLSv1` and `TLSv1.1`. This will cause `Bad handshake` errors when connecting to the MySQL server.
To enable `TLSv1` and `TLSv1.1` open the following file in any editor (`sudo` access needed):
```
/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/jre/lib/security/java.security
```
Search for `jdk.tls.disabledAlgorithms`, it should look something like this:
```
jdk.tls.disabledAlgorithms=SSLv3, TLSv1, TLSv1.1, RC4, DES, MD5withRSA, \
    DH keySize < 1024, EC keySize < 224, 3DES_EDE_CBC, anon, NULL, \
    include jdk.disabled.namedCurves
```
Remove `TLSv1, TLSv1.1` and redo the above steps to build and run `presto-gateway`.

Now you can access load balanced presto at localhost:8080 port. We will refer to this as `prestogateway.lyft.com`

If you see test failures while building `presto-gateway` or in an IDE, please  run `mvn process-classes` to instrument javalite models
which are used by the tests . Ref [javalite-examples](https://github.com/javalite/javalite-examples/tree/master/simple-example#instrumentation) for more details.

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

## Routing Rules Engine
By default, presto-gateway reads the `X-Trino-Routing-Group` request header to route requests.
If this header is not specified, requests are sent to default routing group (adhoc).

The routing rules engine feature enables you to write custom logic to route requests based on the request info such as any of the [request headers](https://trino.io/docs/current/develop/client-protocol.html#client-request-headers).
Routing rules are separated from presto-gateway application code to a configuration file, allowing for dynamic rule changes.

### Defining your routing rules
To express and fire routing rules, we use the [easy-rules](https://github.com/j-easy/easy-rules) engine. These rules should be stored in a YAML file.
Rules consist of a name, description, condition, and list of actions. If the condition of a particular rule evaluates to true, its actions are fired.
```yaml
---
name: "airflow"
description: "if query from airflow, route to etl group"
condition: "request.getHeader(\"X-Trino-Source\") == \"airflow\""
actions:
  - "result.put(\"routingGroup\", \"etl\")"
---
name: "airflow special"
description: "if query from airflow with special label, route to etl-special group"
condition: "request.getHeader(\"X-Trino-Source\") == \"airflow\" && request.getHeader(\"X-Trino-Client-Tags\") contains \"label=special\""
actions:
  - "result.put(\"routingGroup\", \"etl-special\")"
```
In the condition, you can access the methods of a [HttpServletRequest](https://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletRequest.html) object called `request`.
There should be at least one action of the form `result.put(\"routingGroup\", \"foo\")` which says that if a request satisfies the condition, it should be routed to `foo`.

The condition and actions are written in [MVEL](http://mvel.documentnode.com/), an expression language with Java-like syntax.
In most cases, users can write their conditions/actions in Java syntax and expect it to work. There are some MVEL-specific operators that could be useful though.
For example, instead of doing a null-check before accessing the `String.contains` method like this:
```yaml
condition: "request.getHeader(\"X-Trino-Client-Tags\") != null && request.getHeader(\"X-Trino-Client-Tags\").contains(\"label=foo\")"
```
You can use the `contains` operator
```yaml
condition: "request.getHeader(\"X-Trino-Client-Tags\") contains \"label=foo\""
```
If no rules match, then request is routed to adhoc.

### Execution of Rules
All rules whose conditions are satisfied will fire. For example, in the "airflow" and "airflow special" example rules given above, a query with source `airflow` and label `special`
will satisfy both rules. The `routingGroup` is set to `etl` and then to `etl-special` because of the order in which the rules of defined.
If we swap the order of the rules, then we would possibly get `etl` instead, which is undesirable.

One could solve this by writing the rules such that they're atomic (any query will match exactly one rule). For example we can change the first rule to
```yaml
---
name: "airflow"
description: "if query from airflow, route to etl group"
condition: "request.getHeader(\"X-Trino-Source\") == \"airflow\" && request.getHeader(\"X-Trino-Client-Tags\") == null"
actions:
  - "result.put(\"routingGroup\", \"etl\")"
---
```
This could be hard to maintain as we add more rules. To have better control over the execution of rules, we could use rule priorities and composite rules.
Overall, with priorities, composite rules, and the constructs that MVEL support, you should likely be able to express your routing logic.

#### Rule Priority
We can assign an integer value `priority` to a rule. The lower this integer is, the earlier it will fire.
If the priority is not specified, the priority is defaulted to INT\_MAX.
We can add priorities to our airflow and airflow special rule like so:
```yaml
---
name: "airflow"
description: "if query from airflow, route to etl group"
priority: 0
condition: "request.getHeader(\"X-Trino-Source\") == \"airflow\""
actions:
  - "result.put(\"routingGroup\", \"etl\")"
---
name: "airflow special"
description: "if query from airflow with special label, route to etl-special group"
priority: 1
condition: "request.getHeader(\"X-Trino-Source\") == \"airflow\" && request.getHeader(\"X-Trino-Client-Tags\") contains \"label=special\""
actions:
  - "result.put(\"routingGroup\", \"etl-special\")"
```
Note that both rules will still fire. The difference is that we've guaranteed that the first rule (priority 0) is fired before the second rule (priority 1). Thus `routingGroup`
is set to `etl` and then to `etl-special`, so the `routingGroup` will always be `etl-special` in the end.

Above, the more specific rules have less priority since we want them to be the last to set `routingGroup`. This is a little counterintuitive.
To further control the execution of rules, for example to have only one rule fire, we can use composite rules.

##### Composite Rules
First, please refer to easy-rule composite rules docs: https://github.com/j-easy/easy-rules/wiki/defining-rules#composite-rules

Above, we saw how to control the order of rule execution using priorities. In addition to this, we could have only the first rule matched to be
fired (the highest priority one) and the rest ignored. We can use `ActivationRuleGroup` to achieve this.
```yaml
---
name: "airflow rule group"
description: "routing rules for query from airflow"
compositeRuleType: "ActivationRuleGroup"
composingRules:
  - name: "airflow special"
    description: "if query from airflow with special label, route to etl-special group"
    priority: 0
    condition: "request.getHeader(\"X-Trino-Source\") == \"airflow\" && request.getHeader(\"X-Trino-Client-Tags\") contains \"label=special\""
    actions:
      - "result.put(\"routingGroup\", \"etl-special\")"
  - name: "airflow"
    description: "if query from airflow, route to etl group"
    priority: 1
    condition: "request.getHeader(\"X-Trino-Source\") == \"airflow\""
    actions:
      - "result.put(\"routingGroup\", \"etl\")"
```
Note that the priorities have switched. The more specific rule has a higher priority, since we want it to be fired first.
A query coming from airflow with special label is matched to the "airflow special" rule first, since it's higher priority,
and the second rule is ignored. A query coming from airflow with no labels does not match the first rule, and is then tested and matched to the second rule.

We can also use `ConditionalRuleGroup` and `ActivationRuleGroup` to implement an if/else workflow.
The following logic in pseudocode:
```
if source == "airflow":
  if clientTags["label"] == "foo":
    return "etl-foo"
  else if clientTags["label"] = "bar":
    return "etl-bar"
  else
    return "etl"
```
Can be implemented with these rules:
```yaml
name: "airflow rule group"
description: "routing rules for query from airflow"
compositeRuleType: "ConditionalRuleGroup"
composingRules:
  - name: "main condition"
    description: "source is airflow"
    priority: 0 # rule with the highest priority acts as main condition
    condition: "request.getHeader(\"X-Trino-Source\") == \"airflow\""
    actions:
      - ""
  - name: "airflow subrules"
    compositeRuleType: "ActivationRuleGroup" # use ActivationRuleGroup to simulate if/else
    composingRules:
      - name: "label foo"
        description: "label client tag is foo"
        priority: 0
        condition: "request.getHeader(\"X-Trino-Client-Tags\") contains \"label=foo\""
        actions:
          - "result.put(\"routingGroup\", \"etl-foo\")"
      - name: "label bar"
        description: "label client tag is bar"
        priority: 0
        condition: "request.getHeader(\"X-Trino-Client-Tags\") contains \"label=bar\""
        actions:
          - "result.put(\"routingGroup\", \"etl-bar\")"
      - name: "airflow default"
        description: "airflow queries default to etl"
        condition: "true"
        actions:
          - "result.put(\"routingGroup\", \"etl\")"
```

##### If statements (MVEL Flow Control)
Above, we saw how we can use `ConditionalRuleGroup` and `ActivationRuleGroup` to implement and `if/else` workflow.
We could also take advantage of the fact that MVEL supports `if` statements and other flow control (loops, etc).
The following logic in pseudocode:
```
if source == "airflow":
  if clientTags["label"] == "foo":
    return "etl-foo"
  else if clientTags["label"] = "bar":
    return "etl-bar"
  else
    return "etl"
```
Can be implemented with these rules:
```yaml
---
name: "airflow rules"
description: "if query from airflow"
condition: "request.getHeader(\"X-Trino-Source\") == \"airflow\""
actions:
  - "if (request.getHeader(\"X-Trino-Client-Tags\") contains \"label=foo\") {
      result.put(\"routingGroup\", \"etl-foo\")
    }
    else "if (request.getHeader(\"X-Trino-Client-Tags\") contains \"label=bar\") {
      result.put(\"routingGroup\", \"etl-bar\")
    }
    else {
      result.put(\"routingGroup\", \"etl\")
    }"
```

### Enabling routing rules engine
To enable routing rules engine, find the following lines in `gateway-ha-config.yml`.
Set `rulesEngineEnabled` to True and `rulesConfigPath` to the path to your rules config file.
```
routingRules:
  rulesEngineEnabled: true
  rulesConfigPath: "src/test/resources/rules/routing_rules.yml" # replace with path to your rules config file
```


## Contributing

Want to help build Presto Gateway? Check out our [contributing documentation](CONTRIBUTING.md)

References :sparkles:
--------------------
[Lyft](https://eng.lyft.com/presto-infrastructure-at-lyft-b10adb9db01)

[Pinterest](https://medium.com/pinterest-engineering/presto-at-pinterest-a8bda7515e52)
    
[Zomato](https://www.zomato.com/blog/powering-data-analytics-with-trino)

[Shopify](https://shopify.engineering/faster-trino-query-execution-infrastructure)
    
{{Your org here}}
    


