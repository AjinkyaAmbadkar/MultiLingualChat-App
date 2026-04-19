CREATE USER chatapp_user WITH PASSWORD 'Alpha#GAmma@123';
CREATE DATABASE chatapp OWNER chatapp_user;
GRANT ALL PRIVILEGES ON DATABASE chatapp TO chatapp_user;

CREATE TABLE test_table (
  id SERIAL PRIMARY KEY,
  name TEXT
);

INSERT INTO test_table (name) VALUES ('Ajinkya');
SELECT * FROM test_table;