# MongoDB Syphon Template Generator

## Introduction
mongosyphon is a migration tool(https://github.com/johnlpage/MongoSyphon).
In order to migrate data from source(oracle, mysql and other databases) to target(mostly mongodb), we need to define the table layout.
This java code will generate below sample templates

i.e.) species.json
```javascript
{
  "start": {
    "source": {
      "uri": "jdbc:mysql://localhost:3306/sdemo?useSSL=false",
      "user": "root",
      "password": "manager"
    },
    "target": {
      "mode": "insert",
      "uri": "mongodb://mongoadmin:passwordone@localhost:30000,localhost:30001,localhost:30002/test?authSource=admin&replicaSet=myRS",
      "namespace": "test.species"
    },
    "template": {
      "speciesid": "$speciesid",
      "species": "$species"
    },
    "query": {
      "sql": "SELECT * FROM species"
    }
  }
}
```

## configuration : application.xml
```
#source:
#  dbms: oracle
#  uri: jdbc:oracle:thin:@localhost:1521/orclpdb1.localdomain
#  username: test
#  passwd: test
#  owner: sqladm
#  tablename: table1, table2

source:
  dbms: mysql
  uri: jdbc:mysql://127.0.0.1:3306/ats?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Seoul
  username: test
  passwd: test
  dbname: test
  tablename: table1, table2, table3, table4

target:
  dbms: MongoDB
  uri: mongodb://user:password@localhost:30000,localhost:30001,localhost:30002/test?authSource=admin&replicaSet=myRS
  dbname: test

---
logging:
  level:
    root: error
    sun.rmi: error
    org.mongodb: info
    org.springframework: info
    com.mongodb.autotemplate: debug
    com.zaxxer.hikari: off
```

## Run
```console
mvn package
mvn compile package
java -jar target/syphonTemplate-0.0.1-SNAPSHOT.jar
```
## output : save them into filename (list)
```{"start": {"source": {"uri": "jdbc:mysql://127.0.0.1:3306/ats?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Seoul", "user": "test", "password": "test"}, "target": {"mode": "insert", "uri": "mongodb://localhost:30000,localhost:30001,localhost:30002/test?replicaSet=rs0", "namespace": "test.table1"}, "template": {"user_id": "$user_id", "jongmok_cd": "$jongmok_cd", "jongmok_name": "$jongmok_name", "priority": "$priority", "buy_amt": "$buy_amt", "buy_price": "$buy_price", "target_price": "$target_price", "cut_loss_price": "$cut_loss_price", "buy_trd_yn": "$buy_trd_yn", "sell_trd_yn": "$sell_trd_yn", "inst_id": "$inst_id", "inst_dtm": "$inst_dtm", "updt_id": "$updt_id", "updt_dtm": "$updt_dtm"}, "query": {"sql": "SELECT * FROM table1"}}}
{"start": {"source": {"uri": "jdbc:mysql://127.0.0.1:3306/ats?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Seoul", "user": "test", "password": "test"}, "target": {"mode": "insert", "uri": "mongodb://localhost:30000,localhost:30001,localhost:30002/test?replicaSet=rs0", "namespace": "test.table2"}, "template": {"msrl": "$msrl", "name": "$name", "password": "$password", "provider": "$provider", "uid": "$uid", "created_at": "$created_at", "modified_at": "$modified_at"}, "query": {"sql": "SELECT * FROM table2"}}}
{"start": {"source": {"uri": "jdbc:mysql://127.0.0.1:3306/ats?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Seoul", "user": "test", "password": "test"}, "target": {"mode": "insert", "uri": "mongodb://localhost:30000,localhost:30001,localhost:30002/test?replicaSet=rs0", "namespace": "test.table3"}, "template": {"user_msrl": "$user_msrl", "roles": "$roles"}, "query": {"sql": "SELECT * FROM table3"}}}
```

## split them into each separate file(x.json) 

```
# save to gen_script.sh
cnt=0
while read line
do
    echo  "$line" > $cnt.tmp
    cnt=`expr $cnt + 1`
    echo $cnt
done < list

cnt=0
for cur in `ls *.tmp`
do
  cat $cur | jq . > $cnt.json
  cnt=`expr $cnt + 1`
done
```



## execute migration via MongoSyphon
```
for cur in `ls *.json`
do
  nohup java -jar ~/MongoSyphon/bin/MongoSyphon.jar -c $cur &
done
```
