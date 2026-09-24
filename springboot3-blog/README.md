# springboot3-blog

A Spring Boot 3 backend for the blog system, focused on authentication, user data, and content-domain APIs.

## 1. Overview

`springboot3-blog` is a Java backend service built with Spring Boot 3 and MyBatis-Plus. It provides RESTful APIs for user and auth features, with JWT-based authentication and RSA-assisted login payload decryption.

## 2. Tech Stack

- Java 17
- Spring Boot 3.2.x
- Spring Security
- MyBatis-Plus
- MySQL 8
- Druid
- JWT (`jjwt`)
- Maven

## 3. Project Structure

```text
src/main/java/com/culciful
├─ common/        # shared response models, enums, common utilities
├─ config/        # Spring/Security/MyBatis related configurations
├─ controller/    # HTTP layer (request/response mapping only)
├─ dto/           # request/response transfer objects
├─ mapper/        # MyBatis mapper layer
├─ pojo/          # persistence entities
├─ security/      # auth filters, token helpers, crypto-related modules
├─ service/       # business logic abstractions
│  └─ impl/       # business logic implementations
└─ Main.java      # application entry point
```

## 4. Runtime Requirements

- JDK 17
- Maven 3.8+
- MySQL 8.x

## 5. Configuration

Main config files:

- `src/main/resources/application.yaml`
- `src/main/resources/application-dev.yaml`
- `src/main/resources/application-prod.yaml`

Common environment variables:

- `SERVER_PORT`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_TOKEN_SIGN_KEY`
- `JWT_TOKEN_EXPIRATION`

## 6. Database Setup

1. Create database (default: `culciful_blog`).
2. Import SQL scripts from the repository database folder.
3. Verify DB user privileges and connectivity.

## 7. Local Development

```bash
# compile
mvn clean compile

# run tests
mvn test

# start app
cd springboot3-blog

mvn spring-boot:run
```

If local test dependencies are not ready, temporary startup fallback:

```bash
mvn spring-boot:run "-Dmaven.test.skip=true"
```

## 8. Auth and Security Notes

- Login endpoint accepts RSA-encrypted payload from frontend.
- Server decrypts payload with RSA private key helper.
- JWT is issued after successful authentication.
- Security filter chain validates token for protected routes.
- Public routes should be explicitly whitelisted in security configuration.

## 9. API Conventions

- Unified response envelope is used across controllers.
- Error codes/messages follow shared enum conventions.
- RESTful naming is preferred for new endpoints.

## 10. Troubleshooting

### Port already in use

If startup fails with `Port 8080 was already in use`:

- stop the existing process using port 8080, or
- run with another port (for example `--server.port=8081`).



