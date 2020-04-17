0. Make sure you have both PSSQL clients installed locally.
1. mvn clean install (this will produce a benchmark JAR file at path "target/benchmarks.jar".)
1a. (do this only when setting up the database for the first time)
sudo docker ps (lists all containers, pick the id of the container with name "postresql")
sudo docker exec -it <postgresql_id> bash
(once in the container's terminal, execute)
psql -U postgres

CREATE TABLE dm_queue
(
id integer,
domainid integer,
command character varying(1024)
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
AFTER INSERT ON dm_queue
FOR EACH ROW EXECUTE PROCEDURE queue_event();

2. java -jar -Dcz.fi.muni.pa036.client=<CLIENT> target/benchmarks.jar
