# Money-Transfer Service: Design Decisions

## Project Approach

I've designed a simple money transfer service that builds on the existing project structure:

- Using the provided **PostgreSQL Docker setup** for data storage
- Building on the minimalist **Spring Boot application** already in place
- Adding basic REST endpoints for account management and transfers
- Packaging as a **standalone executable JAR**

## Key Design Decisions

### 1. Simple Database Schema
- Added a `transactions` table to complement the existing `accounts` table
- Used straightforward JDBC with Spring's JdbcTemplate for data access
- Implemented transaction management for safe money transfers

### 2. Lightweight REST API
- Built minimal endpoints for:
    - Viewing accounts
    - Transferring money
    - Viewing transaction history
- Focused on core functionality without complex features

### 3. Containerization Strategy
- Leveraged existing Dockerfile for PostgreSQL
- Created a simple Java application Dockerfile for the service
- Ensured the app can connect to the database when running in Docker

### 4. Testing Approach
- Documented curl commands for manual API testing
- Included basic unit tests for the service layer
- Focused on demonstrating the core functionality works

### 5. Data Model and Integration
The `Account` class is a simple Java POJO located in the source package, reflecting the database schema with fields like `id`, `ownerName`, and `balance`. It acts as a DTO for data transfer.
Data is accessed via Spring's `JdbcTemplate`, which executes SQL queries and maps results manually to `Account` instances. This minimal approach avoids ORM overhead, aligning with the project's focus on simplicity.
Service classes use `Account` objects for fetching and updating data, and controllers serialize these objects for API responses, maintaining a lightweight and straightforward architecture.


- Implemented consistent error handling patterns
- Added comprehensive JavaDoc documentation
- Used consistent code formatting and naming conventions
- Followed SOLID principles in service design

## What I Chose Not to Implement

- **Authentication**: Not needed for an internal service
- **Complex API features**: Kept endpoints simple and focused
- **Advanced error handling**: Implemented only essential validations
- **UI components**: Pure REST API as required

This approach delivers a functional money transfer service while maintaining simplicity and focusing on the core requirements.