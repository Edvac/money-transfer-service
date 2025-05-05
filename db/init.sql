-- Drop existing tables if they exist (for clean restart)
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS accounts;

-- Create accounts table with all required fields
CREATE TABLE accounts (
                          id          SERIAL PRIMARY KEY,
                          owner_name  TEXT NOT NULL,
                          balance     NUMERIC(14,2) NOT NULL,
                          currency    VARCHAR(3) DEFAULT 'USD',
                          created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create transactions table
CREATE TABLE transactions (
                              id              SERIAL PRIMARY KEY,
                              from_account_id BIGINT NOT NULL,
                              to_account_id   BIGINT NOT NULL,
                              amount          NUMERIC(14,2) NOT NULL,
                              currency        VARCHAR(3) NOT NULL,
                              timestamp       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              status          VARCHAR(20) NOT NULL,
                              FOREIGN KEY (from_account_id) REFERENCES accounts(id),
                              FOREIGN KEY (to_account_id) REFERENCES accounts(id)
);

-- Insert sample data
INSERT INTO accounts(owner_name, balance, currency)
VALUES
    ('Alice', 100.00, 'USD'),
    ('Bob', 50.00, 'USD');