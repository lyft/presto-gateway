Gateway-HA
==========

How to setup dev environment
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
Once logged in to mysql console, please run `resources/gateway-ha-presistence.sql` to populate the tables.


Step 2: Edit the configuration `gateway-ha.yml`

Step 3: Add below program argument to class `HaGatewayLauncher` and debug in IDE 
```$xslt
server /path/to/gateway-ha/src/test/resources/config-template.yml
``` 
