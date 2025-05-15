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
The `Account` class is a simple Java POJO located in the source package, reflecting the database schema with fields like
`id`, `ownerName`, and `balance`. It acts as a DTO for data transfer. Data is accessed via Spring's `JdbcTemplate`, 
which executes SQL queries and maps results manually to `Account` instances. This minimal approach avoids ORM overhead,
aligning with the project's focus on simplicity. Service classes use `Account` objects for fetching and updating data,
and controllers serialize these objects for API responses, maintaining a lightweight and straightforward architecture.

### 6. Data Access Evolution: Spring Data JDBC


Implemented Spring Data JDBC for Account model with validation and business logic for transfers. The model
includes validation annotations to ensure data integrity and business rules that prevent negative transfers
while allowing negative balances. 

#### 7. Models, Services, and Database Evolution
My work on this money transfer service evolved through several logical steps:
### Model Enhancement
Starting with a basic model, I recognized the need to track transfers between accounts. I expanded the model with additional fields (currency, timestamps) and created a new `Transaction` model to record money movements. `Account``Account`
### Service Layer Implementation
I created a dedicated to handle all money transfer business logic, properly implementing transaction management with Spring's annotation to ensure data consistency. This service was moved to the correct package to maintain proper separation of concerns. `TransferService``@Transactional`
### Database Schema Updates
The initial database schema was too limited with just a simple table. I enhanced it to include currency and timestamps, and added a new `transactions` table with proper foreign key relationships to the accounts table. `accounts`

### 8. Controller Layer Implementation

For the controller layer, I focused on creating a clean interface between clients and the service layer. I first
designed the `AccountController` with straightforward CRUD operations, making sure to return meaningful HTTP status
codes and manage error responses. Later, recognizing the need for transfer functionality, I separated concerns by
implementing a dedicated `TransferController`. This decision kept the code more maintainable as each controller handles
a specific domain concept. I chose to leverage Spring's automatic JSON serialization rather than creating custom DTOs
since our model objects were already well-structured for API responses.

Swagger UI choosed to use a static file. With this trade off
the file needs to be updated when there is an API change but the code remains more redable as the code already has many
annotations.

## Future enchancements

- Advanced error handling i.e. centralised exception handling and catching more cases
- Monitoring and observablity i.e. with tools like Prometheus
- Additional testing
  - Integration, using Spring tools or bash scripts
  - Load testing
  - Improve the CI/CD pipeline
  - Compliance and Auditing
    - Audit Trail and Financial regulations additions

## What I Chose Not to Implement

- **Authentication**: Not needed for an internal service
- **Complex API features**: Kept endpoints simple and focused
- **Advanced error handling**: Implemented only essential validations
- **UI components**: Pure REST API as required

This approach delivers a functional money transfer service while maintaining simplicity and focusing on the core
requirements.