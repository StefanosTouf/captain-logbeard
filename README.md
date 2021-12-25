# Captain logbeard the great arrives! 

Captain logbeard is at your service to aggregate, process and forward your application logs at their destination. 

## Meet the captain

Logbeard is a simple man, living a simple life, and he is proud of it! He consists of a very small codebase that enables him to do one thing, and do it well; get your logs from your application to a relational database (or burry them in a deserted island, whatever floats your boat. See what i did there?)

## Sample Use
```
docker run -it \
  --name logbeard \
  --rm \
  --network mock-net \
  -e PORT=5000 \
  -e NULL_RETRIES=20 \
  -e LOGS_PER_WRITE=100 \
  -e DB_PORT=5432 \
  -e DB_TABLE_NAME=logs \
  -e DB_NAME=postgres \
  -e DB_USER=postgres \
  -e DB_PASSWORD=postgres \
  -e DB_HOST=postgres \
  logbeard
```

## Environment Variables
```java
PORT=5000 \\what port logbeard exposes a tcp socket
NULL_RETRIES=20 \\how many times logbeard will accept failing to read a log before restarting and waiting for a new client
LOGS_PER_WRITE=100 \\how many writes to the database will happen at a time
DB_PORT=5432 \\what port to search for the database
DB_TABLE_NAME=logs \\where to store the logs
DB_NAME=postgres
DB_USER=postgres
DB_PASSWORD=postgres
DB_HOST=postgres
```

## Inputs

Logbead can currently only receive logs from a tcp socket. He likes the syslog format (rfc5424) and so, currently, thats all he supports.

*example sending logs through tcp to logbeard with logspout*
```
docker run --name logspout -t --network mock-net --rm --env DEBUG=1 \
  --volume=/var/run/docker.sock:/var/run/docker.sock \
  --publish 8000:80 \
  gliderlabs/logspout:latest \
  syslog+tcp://logbeard:5000
```

## Processors
* ToDo

## Filters
* ToDo

## Outputs
Currently, only postgres is supported and no customization of the table is provided (other than the name). The logs will be stored in a table thats a one-to-one mapping of the syslog rfc5424 format's fields

```sql
  create table logs(
    priority        integer,
    version         integer,
    timestamp       timestamp,
    hostname        varchar,
    app_name        varchar,
    process_id      integer,
    message_id      varchar,
    structured_data varchar,
    message         varchar);
```

