IMAGE_NAME          := money-transfer-pg
CONTAINER_NAME      := money-transfer-db
DATA_VOLUME         := pg_data_money_transfer
PG_PORT             := 5432
APP_PORT            := 8080
APP_JAR             := target/money-transfer-service-0.0.1-SNAPSHOT.jar

# Check if Docker daemon is running ---------------------------------------------
.PHONY: check-docker
check-docker:
	@echo "Checking if Docker daemon is running..."
	@docker info > /dev/null 2>&1 || (echo "Error: Docker daemon is not running. Please start Docker first." && exit 1)

# Check if container exists and remove it if it does ---------------------------
.PHONY: check-container
check-container: check-docker
	@echo "Checking if container $(CONTAINER_NAME) already exists..."
	@if docker ps -a --format '{{.Names}}' | grep -q "^$(CONTAINER_NAME)$$"; then \
		echo "Container $(CONTAINER_NAME) already exists. Stopping and removing it..."; \
		docker stop $(CONTAINER_NAME) > /dev/null 2>&1 || true; \
		docker rm $(CONTAINER_NAME) > /dev/null 2>&1 || true; \
	fi

# Build image -----------------------------------------------------------------
.PHONY: build
build: check-docker
	docker build -t $(IMAGE_NAME) .


# Run container ---------------------------------------------------------------
.PHONY: up
up: check-docker build
	docker run -d \
	  --name $(CONTAINER_NAME) \
	  -p $(PG_PORT):5432 \
	  -v $(DATA_VOLUME):/var/lib/postgresql/data \
	  $(IMAGE_NAME)
	@echo "PostgreSQL available on localhost:$(PG_PORT)"

# Rebuild database from scratch ---------------------------------------------------
.PHONY: rebuild-db
rebuild-db: check-docker
	@echo "Stopping and removing existing container..."
	-docker stop $(CONTAINER_NAME) 2>/dev/null || true
	-docker rm $(CONTAINER_NAME) 2>/dev/null || true
	@echo "Removing volume to start with clean data..."
	-docker volume rm $(DATA_VOLUME) 2>/dev/null || true
	@echo "Building and starting database with fresh data..."
	$(MAKE) up

# Build FAT JAR ----------------------------------------------------------------
.PHONY: jar
jar:
	./mvnw clean package
	@echo "FAT JAR built at $(APP_JAR)"

# Find and kill process on port 8080 if needed ---------------------------------
.PHONY: free-port
free-port:
	@echo "Checking if port $(APP_PORT) is in use..."
	@if lsof -i :$(APP_PORT) > /dev/null; then \
		echo "Port $(APP_PORT) is in use. Attempting to free it..."; \
		PID=$$(lsof -i :$(APP_PORT) | grep LISTEN | awk '{print $$2}' | head -n 1); \
		if [ -n "$$PID" ]; then \
			echo "Killing process $$PID using port $(APP_PORT)"; \
			kill -9 $$PID || true; \
			sleep 2; \
		fi; \
	else \
		echo "Port $(APP_PORT) is available."; \
	fi


# Run standalone JAR with environment variables --------------------------------
.PHONY: run-jar
run-jar: jar
	@echo "Running application with PostgreSQL connection..."
	SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/money_transfer_db \
	SPRING_DATASOURCE_USERNAME=money_user \
	SPRING_DATASOURCE_PASSWORD=money_password \
	SPRING_JPA_HIBERNATE_DDL_AUTO=validate \
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
