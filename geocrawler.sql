DROP TABLE IF EXISTS venue;

CREATE TABLE venue (
       id BIGINT NOT NULL AUTO_INCREMENT
     , name VARCHAR(255) NOT NULL
     , address VARCHAR(255) NOT NULL
     , zip VARCHAR(255)
     , latitude DOUBLE NOT NULL
     , longitude DOUBLE NOT NULL
     , woeid INT
     , url VARCHAR(255) NOT NULL
     , type VARCHAR(255) NOT NULL
     , dateCreated DATETIME
     , lastUpdated DATETIME
     , PRIMARY KEY (id)
) ENGINE=InnoDB;