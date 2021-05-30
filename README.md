# MongoDB Syphon Template Generator

## Introduction
mongosyphon is a migration tool(https://github.com/johnlpage/MongoSyphon).
In order to migrate data from source(oracle, mysql and other databases) to target(mostly mongodb), we need to define the table layout.
This java code will generate the sample templates

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
      "uri": "mongodb://mongoadmin:passwordone@localhost:30000,localhost:30001,localhost:30002/",
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

## Run
```console
mvn package
mvn compile package
java -jar target/syphonTemplate-0.0.1-SNAPSHOT.jar
```
