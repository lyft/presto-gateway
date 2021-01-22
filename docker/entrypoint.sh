#!/usr/bin/bash
set -eux

initialize() {
    echo "Setting up config"
    sed -i.u "s|DB_HOST|${DB_HOST}|g" gateway-ha/gateway-ha-config.yml
    sed -i.u "s|DB_PORT|${DB_PORT}|g" gateway-ha/gateway-ha-config.yml
    sed -i.u "s|DB_USER|${DB_USER}|g" gateway-ha/gateway-ha-config.yml
    sed -i.u "s|DB_PASS|${DB_PASS}|g" gateway-ha/gateway-ha-config.yml
}

check_mysql_connection()
{
    connected=0
    counter=0

    echo "Wait 60 seconds for connection to MySQL"
    while [[ ${counter} -lt 60 ]]; do
        {
            /usr/bin/mysql -u"${DB_USER}" -p"${DB_PASS}" -h "${DB_HOST}" --port="${DB_HOST}" -e "SELECT 1" \
            | echo "Connecting to MySQL" &&
            connected=1

        } || {
            let counter=$counter+3
            sleep 3
        }
        if [[ ${connected} -eq 1 ]]; then
            echo "Connected"
            break;
        fi
    done

    if [[ ${connected} -eq 0 ]]; then
        echo "MySQL process failed."
        exit;
    fi
}

setup_mysql_dev_schema()
{
    check_mysql_connection

    echo "Setting up DB: mysql"
    /usr/bin/mysql -u"${DB_USER}" -p"${DB_PASS}" -h "${DB_HOST}" --port="${DB_HOST}" < gateway-ha/src/main/resources/gateway-ha-persistence.sql
}

initiliaze()
check_mysql_connection()
setup_mysql_dev_schema()
java -jar gateway-ha/target/gateway-ha-{{VERSION}}-jar-with-dependencies.jar server gateway-ha/gateway-ha-config.yml
