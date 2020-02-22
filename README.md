# presto-gateway

A load balancer / proxy / gateway for presto compute engine.


Totvs - Local Debug

Spin up dataproc

```
gcloud beta dataproc clusters create presto-poc3 --single-node --enable-component-gateway --region us-central1 --subnet default --zone us-central1-b --master-machine-type n1-standard-4 --master-boot-disk-size 500 --image-version 1.3-deb9 --max-age 7600s --scopes 'https://www.googleapis.com/auth/cloud-platform' --project labs-poc --scopes sql-admin --initialization-actions=gs://goog-dataproc-initialization-actions-us-central1/presto/presto.sh,gs://goog-dataproc-initialization-actions-us-central1/cloud-sql-proxy/cloud-sql-proxy.sh --properties hive:hive.metastore.warehouse.dir=gs://drew-totvs-tools/hivetest  --metadata "hive-metastore-instance=labs-poc:us-central1:mdm-rdms-poc"

gcloud compute ssh presto-poc3-m   --project=labs-poc   --zone=us-central1-b -- -L 8080:localhost:8080 -N &

mvn process-classes
```
If you want you can run mysql locally. In my case I'm hitting a cloudsql instance which must be proxied:

```
cd ~/Downloads
curl -o cloud_sql_proxy https://dl.google.com/cloudsql/cloud_sql_proxy.darwin.amd64
chmod +x cloud_sql_proxy
~/Downloads/cloud_sql_proxy -instances=labs-poc:us-central1:mdm-rdms-poc=tcp:3306
```

If rolling your own the MySQL instance create table statements are found
 
```
create database prestogateway
use database prestogateway
/gateway-ha/src/main/resources/gateway-ha-persistence.sql
```

after running some queries, they should show up when you

```
select * from query_history
```

Run it in eclipse

```
com.lyft.data.gateway.ha.HaGatewayLauncher server ../gateway_local.yaml
```

Now you need to register and activate a backend

```
POST http://localhost:2233/entity?entityType=GATEWAY_BACKEND
{  "name": "presto1",  
        "proxyTo": "http://localhost:8080",
        "active": true, 
        "routingGroup": "adhoc" 
    }
POST http://localhost:2233/gateway/backend/activate/presto1
{}

```

Now we can query it

```

wget https://repo1.maven.org/maven2/com/facebook/presto/presto-cli/0.230/presto-cli-0.230-executable.jar
mv presto-cli-0.230-executable.jar presto-cli
chmod +x presto-cli
./presto-cli --server http://localhost:2233 --user 623cdc6dd7d343168805a47435d063e2_3bd7f8ae22b34adb94bf69e29806856d   --catalog hive --schema default

presto:default> show tables;
                  Table                   
------------------------------------------
 8cd6e43115e9416eb23609486fa053e3_recipts 

select * from receipts limit 1
```
Proxy server should on the fly swap out receipts for 8cd6e43115e9416eb23609486fa053e3_recipts based on the tenant Id lookup



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


### Query History UI - check query plans etc.
PrestoGateway records history of recent queries and displays links to check query details page in respective presto cluster.  
![prestogateway.lyft.com](/docs/assets/prestogateway_query_history.png) 

### Gateway Admin UI - add and modify backend information
The Gateway admin page is used to configure the gateway to multiple backends. Existing backend information can also be modified using the same.
![prestogateway.lyft.com/entity](/docs/assets/prestogateway_ha_admin.png) 

## Contributing

Want to help build Presto Gateway? Check out our [contributing documentation](CONTRIBUTING.md)

References :sparkles:
--------------------
[Scaling Presto Infra with gateway at Lyft](https://eng.lyft.com/presto-infrastructure-at-lyft-b10adb9db01)

[Presto-gateway at Pinterest](https://medium.com/pinterest-engineering/presto-at-pinterest-a8bda7515e52)

