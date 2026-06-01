# 09. 빌드 & 배포 — DevLearn P2

## 1. 로컬 개발 실행 (Docker 없이)

### Backend

```bash
cd P2/backend

# 환경변수 (.env 또는 IDE Run Config)
JWT_SECRET_KEY=<base64_256bit_이상>
DB_HOST=localhost
DB_PORT=3306
DB_NAME=devlearn_p2
DB_USERNAME=root
DB_PASSWORD=...
PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:5173
SWAGGER_ENABLED=true   # 개발 중에는 true 권장

./gradlew bootRun
```

### Frontend

```bash
cd P2/frontend

# .env.development
# VITE_API_URL=http://localhost:8080

npm install
npm run dev   # http://localhost:5173
```

### DB 부트스트랩

```bash
mysql -u root -p -e "CREATE DATABASE devlearn_p2 CHARACTER SET utf8mb4;"
# (앱 1회 기동으로 테이블 자동 생성 후)
mysql -u root -p devlearn_p2 < backend/src/main/resources/db/schema-p2-migration.sql
mysql -u root -p devlearn_p2 < backend/src/main/resources/db/seed.sql
```

## 2. Docker Compose 실행

```bash
# .env (compose 디렉토리 = 프로젝트 루트)
JWT_SECRET_KEY=<base64_256bit_이상>
DB_PASSWORD=...
CORS_ALLOWED_ORIGINS=http://localhost
VITE_API_URL=http://localhost:8080

docker compose up -d
```

서비스:

| 서비스 | 포트 | 비고 |
|--------|------|------|
| mysql | 3306 | 컨테이너 내부. 호스트 노출은 선택 |
| backend | 8080 | Spring Boot 4.0.2 |
| frontend | 80 → 4173 | `vite preview` 구동 (정적 서빙) |

`docker compose up` 한 줄로 mysql → backend(헬스체크 대기) → frontend 순서로 기동된다.

## 3. `docker-compose.yml` 핵심

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: devlearn_p2
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 5s
      timeout: 5s
      retries: 20

  backend:
    build:
      context: ./backend
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: devlearn_p2
      DB_USERNAME: root
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET_KEY: ${JWT_SECRET_KEY}
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS}
      PORT: 8080
      SWAGGER_ENABLED: "false"
      JAVA_OPTS: "-Xms256m -Xmx512m"
    ports:
      - "8080:8080"

  frontend:
    build:
      context: ./frontend
      args:
        VITE_API_URL: ${VITE_API_URL}
    depends_on:
      - backend
    ports:
      - "80:4173"

volumes:
  mysql-data:
```

## 4. Backend Dockerfile

```dockerfile
# 1단계: Gradle 8.14 + JDK 21로 빌드
FROM gradle:8.14-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle build.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true
COPY src ./src
RUN gradle bootJar --no-daemon -x test

# 2단계: JRE 21만 담는 실행 이미지
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
RUN groupadd -r app && useradd -r -g app app
COPY --from=build /workspace/build/libs/*.jar /app/app.jar
RUN chown -R app:app /app
USER app
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
```

## 5. Frontend Dockerfile

```dockerfile
# 1단계: 빌드
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG VITE_API_URL
ENV VITE_API_URL=${VITE_API_URL}
RUN npm run build

# 2단계: Vite preview 정적 서버 (학습용 단순화 — nginx 대신)
FROM node:20-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/package.json /app/package-lock.json ./
COPY --from=build /app/dist ./dist
COPY --from=build /app/vite.config.ts ./
COPY --from=build /app/tsconfig*.json ./
COPY --from=build /app/node_modules ./node_modules
EXPOSE 4173
CMD ["npx", "vite", "preview", "--host", "0.0.0.0", "--port", "4173"]
```

## 6. `application.properties` (Backend)

```properties
spring.application.name=dev-learn

spring.config.import=optional:file:.env[.properties]

server.port=${PORT}

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect

# DataSource
spring.datasource.url=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JWT
spring.jwt.secret=${JWT_SECRET_KEY}

# CORS
app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:5173}

# Swagger
springdoc.api-docs.enabled=${SWAGGER_ENABLED:false}
springdoc.swagger-ui.csrf.enabled=${SWAGGER_ENABLED:false}
```

## 7. 환경변수 일람

| 변수 | 백엔드 | 프론트 | 비고 |
|------|:-----:|:-----:|------|
| `JWT_SECRET_KEY` | ✅ | — | Base64, 256bit 이상 |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | ✅ | — | MySQL 접속 |
| `PORT` | ✅ | — | 기본 8080 |
| `CORS_ALLOWED_ORIGINS` | ✅ | — | 콤마 구분, 기본 `http://localhost:5173` |
| `SWAGGER_ENABLED` | ✅ | — | 운영에서는 false |
| `JAVA_OPTS` | ✅ | — | JVM 메모리 등 |
| `VITE_API_URL` | — | ✅ | 빌드 시점 주입 |

## 8. 헬스체크

`/health` 엔드포인트 (permitAll). Compose의 backend depends_on에서 활용 가능.

## 9. EC2 등 단일 호스트 배포

저장소 루트에 `docker-compose.yml`만 있으면 다음 절차로 충분:

```bash
# 첫 배포
git clone <repo> && cd <repo>
cp .env.example .env  # 값 채우기
docker compose pull
docker compose up -d

# 코드 변경 후 재배포
git pull
docker compose build
docker compose up -d
```

## 10. 운영 시 권장 (이 프로젝트 범위 외)

- nginx 리버스 프록시 + HTTPS (Let's Encrypt)
- MySQL은 RDS 등 매니지드로 분리
- 로그는 stdout → 컨테이너 로그 드라이버
- 모니터링은 Spring Boot Actuator `+ /actuator/health` 활성화

학습 프로젝트 범위에서는 위 모두 생략한다.
