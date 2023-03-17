DROP TABLE IF EXISTS cities;
CREATE TABLE cities (
  id integer NOT NULL,
  name VARCHAR(20),
  country VARCHAR(20),
  PRIMARY KEY  (id)
);

INSERT INTO cities VALUES (1, 'Beijing', 'China');
INSERT INTO cities VALUES (2, 'London', 'United Kingdom');
INSERT INTO cities VALUES (3, 'Brno', 'Czechia');


