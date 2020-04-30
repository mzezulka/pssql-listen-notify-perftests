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
CREATE TABLE IF NOT EXISTS text
(
    id integer NOT NULL,
    value character varying(524288),
    CONSTRAINT id_primary PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS bin
(
    id integer NOT NULL,
    value bytea NOT NULL,
    CONSTRAINT binary_pkey PRIMARY KEY (id),
    CONSTRAINT fk_id FOREIGN KEY (id),
    REFERENCES text (id) MATCH SIMPLE
        ON UPDATE CASCADE
        ON DELETE CASCADE
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
ELSIF (TG_OP = 'INSERT') THEN
  data = row_to_json(NEW);
ELSIF (TG_OP = 'UPDATE') THEN
  data = row_to_json(NEW);
END IF;
 
-- Contruct the notification as a JSON string.
-- String data is truncated so that it fits to the NOTIFY payload
notification = json_build_object(
'table',TG_TABLE_NAME,
'action', TG_OP,
'data', json_build_object('id', data->>'id', 'value', SUBSTRING(data->>'value', 1, 7500)));
 
-- Execute pg_notify(channel, notification)
PERFORM pg_notify('q_event', notification::text);
 
-- Result is ignored since this is an AFTER trigger
RETURN NULL;
END;
 
$$ LANGUAGE plpgsql;
	
CREATE TRIGGER check_insert_text
AFTER INSERT ON text
FOR EACH ROW EXECUTE PROCEDURE queue_event();

-- Let us do the same thing for the bin table
CREATE OR REPLACE FUNCTION queue_event_bin() RETURNS TRIGGER AS $$
 
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
-- Please note that BLOB data is too long for a NOTIFY payload
---    what we do instead is to send just id of the row which 
notification = json_build_object(
'table',TG_TABLE_NAME,
'action', TG_OP,
'data', json_build_object('id', NEW.id, 'value', ''));
 
-- Execute pg_notify(channel, notification)
PERFORM pg_notify('q_event_bin', notification::text);
 
-- Result is ignored since this is an AFTER trigger
RETURN NULL;
END;
 
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_insert_bin
AFTER INSERT ON bin
FOR EACH ROW EXECUTE PROCEDURE queue_event_bin();

-- Last but not least, let's create UPDATE and DELETE triggers
CREATE TRIGGER check_update_text
    AFTER UPDATE ON text
    FOR EACH ROW
    EXECUTE PROCEDURE queue_event();

CREATE TRIGGER check_update_bin
    AFTER UPDATE ON bin
    FOR EACH ROW
    EXECUTE PROCEDURE queue_event_bin();

CREATE TRIGGER check_delete_text
    AFTER DELETE ON text
    EXECUTE PROCEDURE queue_event();

CREATE TRIGGER check_delete_bin
    AFTER DELETE ON bin
    EXECUTE PROCEDURE queue_event_bin();
```
#### 4. Run Performance Tests 

either `./run.sh` or manually with your own parameters `java -jar -Dcz.fi.muni.pa036.client=<CLIENT> target/benchmarks.jar <JMH_FLAGS>` (CLIENT = [blocking|nonblocking], for JMH_FLAGS, see the shell script)

### 5. Visualize Output CSV Data

`./csv_to_graph.py`
