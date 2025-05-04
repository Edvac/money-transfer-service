CREATE TABLE accounts (
                          id          SERIAL PRIMARY KEY,
                          owner_name  TEXT    NOT NULL,
                          balance     NUMERIC(14,2) NOT NULL
);

INSERT INTO accounts(owner_name, balance)
VALUES ('Alice', 100.00);