with open('/home/pintu/projects/microservices/docker-compose.yml', 'r') as f:
    c = f.read()
services = [
    ('accounts_db', 'mysql-accounts'),
    ('customer_db', 'mysql-customer'),
    ('transaction_db', 'mysql-transaction'),
    ('notification_db', 'mysql-notification'),
]
for db, host in services:
    old_url = 'DB_URL: jdbc:mysql://' + host + ':3306/' + db
    spring = 'SPRING_DATASOURCE_URL: jdbc:mysql://' + host + ':3306/' + db + '?useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true'
    if old_url in c:
        c = c.replace(old_url, old_url + '\n      ' + spring)
        print('Patched: ' + db)
    else:
        print('Already patched or not found: ' + db)
with open('/home/pintu/projects/microservices/docker-compose.yml', 'w') as f:
    f.write(c)
print('Done')
