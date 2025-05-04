# Money-Transfer Service

A small Spring Boot application backed by PostgreSQL.  
Use it as a starting point for experimenting with basic account/transfer functionality.

## Prerequisites
* Java 21 or newer (build-tested on JDK 24)
* Maven 3.9+ (`./mvnw` wrapper included)
* Docker 20.10+ (for the local PostgreSQL container)
* GNU Make (used to build and run the database image)
* `psql` (optionally, to inspect the data)


## Quick start

1. Build and run the database:

   ```bash
   make up
   ```

   When the command completes, PostgreSQL is available on `localhost:5432`.

2. Verify the seed data (optional):

   ```bash
   PGPASSWORD=money_password \
   psql -h localhost -U money_user -d money_transfer_db \
        -c "SELECT * FROM accounts;"
   ```
