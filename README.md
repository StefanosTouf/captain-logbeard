# Captain logbeard "the great" arrives! 

Captain logbeard is at your service to aggregate, process and forward your application logs to their destination. 

## Meet the captain

Logbeard is a simple man, living a simple life, and he is proud of it! He consists of a very small codebase that enables him to do one thing, and do it well; get your logs from your application to a relational database (or burry them in a deserted island, whatever floats your boat. See what i did there?).

## Sample Use
```
docker run -it \
  --name logbeard \
  --rm \
  -v ${PWD}/config.json:/opt/logbeard/config.json \
  -e PORT=5000 \
  -e DB_PORT=5432 \
  -e DB_NAME=postgres \
  -e DB_USER=postgres \
  -e DB_PASSWORD=postgres \
  -e DB_HOST=postgres
  logbeard
```

## Environment variables with their default values
```
PORT=5000 //what port logbeard exposes a tcp socket on
NULL_RETRIES=20 //how many times logbeard will accept failing to read a log before restarting and waiting for a new client
LOGS_PER_WRITE=50 //how many writes to the database will happen at a time
DB_PORT=5432 //what port to search for the database
DB_NAME=postgres
DB_USER=postgres
DB_PASSWORD=postgres
DB_HOST=postgres
CONFIG_PATH=/opt/logbeard/config.json //location inside of container where the configuration file is
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
Currently, only postgres is supported. You can customize the table where the logs will be stored in via the configuration file. Log beard will create the configured table on startup if it doesnt already exist.

The default config stores syslog fields to columns of the same name in a one-to-one fashion on a table named "LOGS".

*default config file:*
```json
{
  "table":{ 
    "name":"LOGS",
    "fields":{
      "priority":"priority",
      "version": "version",
      "timestamp":"timestamp",
      "hostname": "hostname",
      "app_name": "app_name",
      "process_id": "process_id",
      "message_id": "message_id",
      "structured_data": "structured_data",
      "message": "message"
    }
  }
}
```
*A useful breakdown of the syslog RFC 5424 format can be found in this article https://blog.datalust.co/seq-input-syslog/*

### Customized table and columns

#### Simple customization
You can customize the table name, the names of each column, or ignore some fields all together, the captain wont ask any questions, he trusts your judgement. Each entry of the fields map represents the name of the column you wish to use (the key) along with the syslog field you want stored in it (the value).

*sample custom config file:*
```json
{
  "table":{ 
    "name":"BETTER_TABLE_NAME",
    "fields":{
      "priority":"priority",
      "time":"timestamp",
      "hostname": "hostname",
      "mid": "message_id",
      "message": "message"
    }
  }
}
```
#### Complex customization
A lot of information can be found in the actual message of each log. This is something the syslog format alone cannot account for. 

Lets say that all our apps follow the same log format:
```
Event: message message message message message message message
```
*this text will be found at the end of each syslog message, according to the rfc 5424 standard*

We can tell the captain to extract each part of our logs and store them in their own columns using the optional `custom_fields` configuration option along with the normal `fields` configuration.

```json
{
  "table":{ 
    "name":"LOGS",
    "custom_fields":{
      "event": {
        "regex": "^[^:]+"
      },
      "actual_msg": {
        "regex": "[^:]+$"
      }
    },
    "fields":{
      "priority":"priority",
      "version": "version",
      "timestamp":"timestamp",
      "hostname": "hostname",
      "app_name": "app_name",
      "process_id": "process_id",
      "message_id": "message_id",
      "structured_data": "structured_data",
      "actual_message": "actual_msg",
      "event": "event"
    }
  }
}
```
Here, we are extracting the `Event` with regural expressions from the message and placing it in its own column. Then we are extracting the actual message in the same fashion. Every `custom field` needs to be also configured and given a name on the `fields` map. This is of course a very simple log format, but using regural expressions and as many custom fields as possible, along with the already existing syslog fields, im sure you can achieve nothing less than greatness.

Currently, every `custom field` extracts its info exclusively from the `message` part of the syslog standard. This is probably enough for most use cases. If not, the captain shall revise his plans. 

