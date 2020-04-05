CREATE TABLE gatlingPocCheck (
   traceId VARCHAR(200) UNIQUE NOT NULL,
   personId VARCHAR(200),
   created_on TIMESTAMP NOT NULL
)