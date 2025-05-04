# ──────────────────────────────────────────────────────────────────────────────
# Money-Transfer ‑ PostgreSQL image
# ──────────────────────────────────────────────────────────────────────────────
FROM postgres:16-alpine

# ── Configuration ------------------------------------------------------------
ENV POSTGRES_DB=money_transfer_db
ENV POSTGRES_USER=money_user
ENV POSTGRES_PASSWORD=money_password

# Seed schema for initial testing
# will be executed exactly once on first container start-up.
COPY db/init.sql /docker-entrypoint-initdb.d/

# Expose default Postgres port
EXPOSE 5432