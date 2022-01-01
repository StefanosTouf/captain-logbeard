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
Using the optional `filters` configuration, logbeard can ignore any logs you dont want passing to your database. A predicate can be difined for each syslog field and/or each custom field (see the custom fields section). If all predicates pass, then the log is not ignored and continues through the pipeline

*example*
```json
...
"filters": {
  "priority": {
    "gt": 10
  },
  "app_name": { 
    "not_one_of": ["unwanted_app", "unwanted_app_2"]
  },
  "event": { 
    "one_of": ["Warning", "Info"]
  }
...
```

Available operators:
* **eq**: checks for equality between values of the same type
* **not_eq**: checks for inequality between values of the same type
* **gte**: greater or equal (used for numbers)
* **lte**: lesser or equal (used for numbers)
* **gt**: greater (used for numbers)
* **lt**: lesser (used for numbers)
* **one_of**: checks if the field value is one of the specified values (matches values of the same type)
* **not_one_of**: checks if the field value is not one of the specified values (matches values of the same type)


## Outputs
Currently, only postgres is supported. You can customize the table where the logs will be stored in via the configuration file. Log beard will create the configured table on startup if it doesnt already exist.

The default config stores syslog fields to columns of the same name in a one-to-one fashion on a table named "LOGS".

*default config file:*
```json
{
...
  "table-name":"LOGS",
  "columns":{
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
...
}
```
*A useful breakdown of the syslog RFC 5424 format can be found in this article https://blog.datalust.co/seq-input-syslog/*

You can customize the table name, the names of each column, or ignore some fields all together, the captain wont ask any questions, he trusts your judgement. Each entry of the fields map represents the name of the column you wish to use (the key) along with the syslog field you want stored in it (the value).

*sample custom config*
```json
{
...
  "table-name":"BETTER_TABLE_NAME",
  "columns":{
    "priority":"priority",
    "time":"timestamp",
    "hostname": "hostname",
    "mid": "message_id",
    "message": "message"
    }
...
}
```

## Custom fields
A lot of information can be found in the actual message of each log. This is something the syslog format alone cannot account for. Logbeard lets you extract parts of the message and then use them in any other parts of the filtering/processing pipeline individually by defining `custom_fields`. This unlocks the full power of the captain, for which he is known across the seven seas!

Lets say that all our apps follow the same log format:
```
Event: message message message message message message message -- number

```

*example*
```
Warning: there is something going on that is suspicious, yet not entirely dangerous -- 1234
```

We can tell the captain to extract the event, the message body and the message number so we can use them in the following pipeline and/or store them in individual columns.

```json
{
  ...
  "event": {
    "regex": "^[^:]+",
    "type": "varchar"
    },
  "message_body": {
    "regex": "[^:]+$",
    "type": "varchar"
  }
  ...
}
```
Here, we are extracting the `event` and the `message_body` with regural expressions from the message and telling logbeard its type.

Currently, every `custom field` extracts its info exclusively from the `message` part of the syslog standard. This is probably enough for most use cases. If not, the captain shall revise his plans. 

**Okay, we have the event and the message body. What can we do with them?**

Well, for starters, we can now store them in their own columns
```json
"columns": {
  ...
  "container_name": "app_name",
  "event": "event",
  ...
},
```

But we can also do much cooler things than that...

### Using filters with the custom fields
Having extracted the event from the message, we can now use predicates directly on it in the filters section
```json
"event": { 
  "one_of": ["Warning", "Error"]
},
```
A custom field, just like the already available syslog fields, doesnt need to be stored in the database. That means we can extract it for the sole purpose of using it in filters. 

Lets suppose that in our previous examle log format,
```
Event: message message message message message message message -- number
```
, the number's significance can be determined from the last digit. We can extract it like so:
```json
...
"custom_fields":{
  ...
  "last_digit": {
    "regex": "[0-9]$",
    "type": "int"
  },
  ...
}
...
```

Then we can use that in our filters
```json
...
"filters": {
...
  "last_digit": { 
    "gt": 3 
    },
...
}
...
```
Now, only logs with an enging number greater than 3 will pass. Great!

Notice, we dont have to store the `last_digit` field we created, but we could if we wanted to. Lets also extract the entire number its self and place that instead in its own column.
```json
...
"filters": {
  ...
  "num": {
    "regex": "[^ ]+$",
    "type": "int"
  },
  ...
}
...
"columns": {
...
  "message_number": "num"
...
}
```

Combining this with some of our previous examples, we might get a configuration file that looks like this:
```json
{
"table-name":"LOGS",
"custom_fields":{
  "event": {
    "regex": "^[^:]+",
    "type": "varchar"
  },
  "body": {
    "regex": "[^:]+$",
    "type": "varchar"
  },
  "num": {
    "regex": "[^ ]+$",
    "type": "int"
  },
  "last_digit":{
    "regex": "[0-9]$",
    "type": "int"
  }
},
"filters": {
  "event": { 
    "one_of": ["Warning", "Info"]
  },
  "last_digit": { 
    "gt": 3 
  }
},
"columns":{
  "priority":"priority",
  "timestamp":"timestamp",
  "container_name": "app_name",
  "event": "event",
  "message_body": "body",
  "message_number": "num"
  }
}

```

**Note:**

If the match of a regural expression is an empty string or cant be converted to the specified type, then the predicate associated with it will always return true. This makes sure that a log that might be of a different format doesnt get thrown away.
