# eyewear-backend

Backend REST API for an eyewear e-commerce website, built with Spring Boot.

## Tech Stack

- **Java 21**
- **Spring Boot 4.0.1** (Web MVC, Spring Data JPA, Spring Security)
- **MySQL 8.0**
- **JWT** (jjwt 0.11.5) for authentication
- **Swagger / OpenAPI 3** (springdoc-openapi 2.3.0) for API documentation
- **Docker & Docker Compose** for local development
- **Lombok** for boilerplate reduction

## Prerequisites

- [Git](https://git-scm.com/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop) (includes Docker Compose)

> Java and Maven do **not** need to be installed locally — everything runs inside Docker.

## Getting Started (Docker — recommended)

### 1. Clone the repository

```bash
git clone https://github.com/VEO-Global/eyewear-backend.git
cd eyewear-backend
```

### 2. Create your local environment file

```bash
cp .env.example .env
```

Edit `.env` if you need to change any credentials (the defaults work out of the box).

### 3. Start the application

```bash
docker compose up --build
```

This will:
- Start a **MySQL 8.0** container (accessible at `localhost:3307`)
- Build and start the **Spring Boot** application (accessible at `localhost:8080`)
- Automatically run `init.sql` to create the schema and seed initial data

> The first build may take a few minutes while Maven downloads dependencies.

### 4. Verify the application is running

Open your browser and navigate to:

```
http://localhost:8080/swagger-ui/index.html
```

You should see the Swagger UI listing all available API endpoints.

---

## Running Locally (without Docker)

**Extra prerequisites:** Java 21 JDK, Maven 3.9+, and a running MySQL instance.

1. Create the database:
   ```sql
   CREATE DATABASE eyewear_db;
   ```

2. Copy and edit the application properties:
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   # then update datasource credentials to match your local MySQL
   ```

3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

---

## Project Structure

```
src/
├── main/
│   ├── java/com/veo/backend/
│   │   ├── config/          # Security and data-initializer configuration
│   │   ├── controller/      # REST API controllers
│   │   ├── dto/             # Request and response DTOs
│   │   ├── entity/          # JPA entities
│   │   ├── exception/       # Custom exceptions
│   │   ├── repository/      # Spring Data JPA repositories
│   │   ├── security/        # JWT filter and UserDetailsService
│   │   ├── service/         # Business logic (interfaces + implementations)
│   │   └── utils/           # Utility classes
│   └── resources/
│       └── application.properties
└── test/
```

## API Endpoints

| Area             | Base Path                    |
|------------------|------------------------------|
| Authentication   | `/api/auth`                  |
| Users            | `/api/users`                 |
| Categories       | `/api/categories`            |
| Products         | `/api/products`              |
| Product Variants | `/api/products/{id}/variants`|

Full interactive documentation is available at `http://localhost:8080/swagger-ui/index.html` when the app is running.

## Database

The `init.sql` file at the project root creates all tables and seeds the following initial data:

- **Roles:** `CUSTOMER`, `SALES`, `OPERATIONS`, `MANAGER`, `ADMIN`
- **Categories:** `Kính râm` (Sunglasses), `Gọng kính` (Frames), `Kính trẻ em` (Kids)

> Database data is persisted in a Docker volume (`db_data`) so it survives container restarts.

---

## Contributing

1. Ask a project owner to add you as a collaborator on GitHub (Settings → Collaborators), or fork the repository.
2. Clone the repository:
   ```bash
   git clone https://github.com/VEO-Global/eyewear-backend.git
   cd eyewear-backend
   ```
3. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
4. Make your changes, then commit and push:
   ```bash
   git add .
   git commit -m "feat: describe your change"
   git push origin feature/your-feature-name
   ```
5. Open a **Pull Request** on GitHub for review.
