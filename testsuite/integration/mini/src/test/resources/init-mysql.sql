DROP TABLE IF EXISTS cities;
CREATE TABLE cities (
  id int(10) unsigned NOT NULL auto_increment,
  name VARCHAR(20),
  country VARCHAR(20),
  PRIMARY KEY  (id)
)
ENGINE=INNODB;
INSERT INTO cities(name, country) VALUES ('Beijing', 'China');
INSERT INTO cities(name, country) VALUES ('London', 'United Kingdom');
INSERT INTO cities(name, country) VALUES ('Brno', 'Czechia');


