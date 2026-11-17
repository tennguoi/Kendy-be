# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

**Build and Test**
- `mvn compile` - Compile the application
- `mvn test` - Run all tests
- `mvn package -DskipTests` - Package the application (skip tests for faster builds)
- `mvn spring-boot:run` - Run the application in development mode

**Test Specifics**
- `mvn test -Dtest=<TestClassName>` - Run a specific test class
- `mvn test -Dtest=<TestClassName>#<testMethod>` - Run a specific test method

**Code Quality**
- The codebase uses Spring Boot 4.0.6 with Java 17
- Lombok is used for reducing boilerplate code
- No separate linting/formatting tools are configured in this repository

## Project Structure

**Main Application**
- Entry point: `src/main/java/com/example/KendyDigital/KendyDigitalApplication.java`
- Uses Spring Boot's auto-configuration with `@SpringBootApplication`

**Service Layer Organization**
The service directory has been reorganized into logical groupings:
- `analytics_finance` - Analytics and financial reporting services
- `customer_service` - Ticket and warranty management services
- `file_integrations` - File storage and webhook handling services
- `product_inventory` - Coupon, entitlement, and inventory services
- `user_management` - User, role, and preference services
- `system_infrastructure` - Monitoring, job queue, and system configuration services
- Other standalone services: `auth`, `bank`, `catalog`, `checkout`, `deposit`, `notification`, `order`, `security`, `wallet`

**Key Architectural Components**
- **Controllers**: REST endpoints in `src/main/java/com/example/KendyDigital/controller/`
- **Services**: Business logic in `src/main/java/com/example/KendyDigital/service/{package}/`
- **Repositories**: Data access layer using Spring Data JPA/JDBC
- **Models**: Entity classes in `src/main/java/com/example/KendyDigital/model/`
- **DTOs**: Data transfer objects in `src/main/java/com/example/KendyDigital/dto/`
- **Configuration**: Beans and properties in `src/main/java/com/example/KendyDigital/config/`
- **Common Utilities**: Filters, helpers, and shared code in `src/main/java/com/example/KendyDigital/common/`

**Security Architecture**
- OAuth2 authentication for `/oauth2/**` and `/login/oauth2/**` endpoints
- Stateless JWT/bearer token authentication for API endpoints
- Role-based authorization with `ADMIN` and `SUPER_ADMIN` roles
- Custom filters for request ID, rate limiting, maintenance mode, and server time
- CORS configuration for cross-origin requests

**External Integrations**
- Database: Supports PostgreSQL (primary) and H2 (testing)
- Redis: For caching and session storage
- Cloudinary: For media/file storage
- SePay: Payment webhook handling
- Email: Notification services

**Database Schema**
- Uses JPA/Hibernate for ORM
- Tables follow Spring Data naming conventions
- Migration handled through application startup (no Flyway/Liquibase visible)

## Common Development Patterns

**Service Dependencies**
- Services are injected via constructor injection
- Circular dependencies avoided through proper layering
- Repository interfaces extend Spring Data repositories

**DTO Usage**
- Request/response objects organized by feature in dto/ packages
- Validation annotations used on request DTOs
- Separate DTOs for internal vs external APIs when needed

**Error Handling**
- Centralized exception handling via `@ControllerAdvice` (ApiExceptionHandler)
- Custom error codes and messages defined in common/error/
- Standard HTTP status codes used appropriately

**Testing Approach**
- Integration tests using `@SpringBootTest`
- Test profiles configured in src/test/resources/
- Mockito used for mocking dependencies
- Testcontainers or similar not visible in current configuration

This structure provides a clean separation of concerns with services organized by business domain rather than technical concern, making it easier to locate related functionality.