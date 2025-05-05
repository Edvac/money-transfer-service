IMAGE_NAME          := money-transfer-pg
CONTAINER_NAME      := money-transfer-db
DATA_VOLUME         := pg_data_money_transfer
PG_PORT             := 5432
APP_PORT            := 8080
APP_JAR             := target/money-transfer-service-0.0.1-SNAPSHOT.jar


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


# Build FAT JAR ----------------------------------------------------------------
.PHONY: jar
jar:
	./mvnw clean package
	@echo "FAT JAR built at $(APP_JAR)"

# Run standalone JAR -----------------------------------------------------------
.PHONY: run-jar
run-jar: jar
	java -jar $(APP_JAR)

# Run Spring Boot application ----------------------------------------------------
.PHONY: run
run:
	./mvnw spring-boot:run

# Start the complete application (DB + App) ---------------------------------------
.PHONY: start
start: up
	@echo "Starting application..."
	@echo "Waiting for database to initialize..."
	@sleep 5
	$(MAKE) run-jar

# Clean up everything -----------------------------------------------------------
.PHONY: clean
clean:
	@echo "Stopping and removing containers..."
	-docker stop $(CONTAINER_NAME) 2>/dev/null || true
	-docker rm $(CONTAINER_NAME) 2>/dev/null || true
	@echo "Cleaning up Maven artifacts..."
	./mvnw clean

# Stop database container --------------------------------------------------------
.PHONY: down
down:
	@echo "Stopping database container..."
	-docker stop $(CONTAINER_NAME) 2>/dev/null || true
	-docker rm $(CONTAINER_NAME) 2>/dev/null || true
