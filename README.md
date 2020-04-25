#### 0. Prerequisites

Make sure you have both PSSQL Java clients ([blocking](https://github.com/zezulka/pssql-listen-notify-client-blocking),[nonblocking](https://not.available.yet)) installed locally in your Maven repository.

#### 1. Perftest JAR Build: 

`git clone https://github.com/mzezulka/pssql-listen-notify-perftests`

`cd https://github.com/mzezulka/pssql-listen-notify-perftests`

`mvn clean install` (this will produce a benchmark JAR file at path "target/benchmarks.jar")

#### 2. Database Start

`sudo systemctl start docker && sudo docker run -d --rm --name postgresql -v postgresql-data:/var/lib/postgresql/data --network host -e POSTGRES_PASSWORD="" -e POSTGRES_HOST_AUTH_METHOD=trust -p 5432:5432 postgres:latest`

#### 3. Database Setup

*Skip this once you've already set up the database.*

`sudo docker ps` (lists all containers, pick the id of the container with name "postresql")`
`sudo docker exec -it <postgresql_id> bash`

Once in the container's terminal, execute:

`psql -U postgres`
 
In the SQL shell, execute:
 
```sql
CREATE TABLE text
(
id integer NOT NULL,
message character varying(524288),
CONSTRAINT id_primary PRIMARY KEY (id)
);

CREATE TABLE bin
(
    id integer NOT NULL,
    img bytea NOT NULL,
    CONSTRAINT binary_pkey PRIMARY KEY (id)
);

CREATE OR REPLACE FUNCTION queue_event() RETURNS TRIGGER AS $$
 
DECLARE
data json;
notification json;
 
BEGIN
 
-- Convert the old or new row to JSON, based on the kind of action.
-- Action = DELETE? -&gt; OLD row
-- Action = INSERT or UPDATE? -&gt; NEW row
IF (TG_OP = 'DELETE') THEN
data = row_to_json(OLD);
ELSE
data = row_to_json(NEW);
END IF;
 
-- Contruct the notification as a JSON string.
notification = json_build_object(
'table',TG_TABLE_NAME,
'action', TG_OP,
'data', data);
 
-- Execute pg_notify(channel, notification)
PERFORM pg_notify('q_event',notification::text);
 
-- Result is ignored since this is an AFTER trigger
RETURN NULL;
END;
 
$$ LANGUAGE plpgsql;
	
CREATE TRIGGER queue_notify_event
AFTER INSERT ON text
FOR EACH ROW EXECUTE PROCEDURE queue_event();
```
#### 4. Run Performance Tests 

`java -jar -Dcz.fi.muni.pa036.client=<CLIENT> target/benchmarks.jar` (CLIENT = [blocking|nonblocking])
