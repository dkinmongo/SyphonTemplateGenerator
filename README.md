# MongoDB Syphon Template Generator

## Introduction
mongosyphon is a migration tool(https://github.com/johnlpage/MongoSyphon).
In order to migrate data from source(oracle, mysql and other databases) to target(mostly mongodb), we need to define the table layout.
This java code will generate the template 
```console

```

## Dependencies
Package Name|Version
-----|-----
spring-boot-starter|2.2.9.BUILD-SNAPSHOT
mongodb-driver|3.11.2
lombok|1.18.12
logback-classic:1.2.3

## Run

### Create MongoDB Collection on ReplicaSet

```javascript
use plm
db.createCollection("trends")
db.createCollection("defects")

db.getSiblingDB("plm").runCommand(
  {
    createIndexes: "trends",
    indexes:[
        {
            key: {
                'taskId':1,
                'updateDate':-1,
                'devModelCode':1,
                'verifier':1,
                'status':1
            },
            name:"c_idx_trends_01"
        }
    ]
  }
);

```

```console
mvn package
mvn compile package
java -jar target/
```
