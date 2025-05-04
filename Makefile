IMAGE_NAME          := money-transfer-pg
CONTAINER_NAME      := money-transfer-db
DATA_VOLUME         := pg_data_money_transfer
PG_PORT             := 5432

# Build image -----------------------------------------------------------------
.PHONY: build
build:
	docker build -t $(IMAGE_NAME) .

# Run container ---------------------------------------------------------------
.PHONY: up
up: build
	docker run -d \
	  --name $(CONTAINER_NAME) \
	  -p $(PG_PORT):5432 \
	  -v $(DATA_VOLUME):/var/lib/postgresql/data \
	  $(IMAGE_NAME)
	@echo "PostgreSQL available on localhost:$(PG_PORT)"


# Run Spring Boot application ----------------------------------------------------
.PHONY: run
run:
	./mvnw spring-boot:run
