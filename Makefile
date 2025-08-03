# HomeBuddy Development & Production Makefile

.PHONY: help dev-up dev-down prod-up prod-down build test clean logs

# Default target
help:
	@echo "HomeBuddy Docker Commands:"
	@echo ""
	@echo "Development:"
	@echo "  make dev-up     - Start PostgreSQL for development"
	@echo "  make dev-down   - Stop development containers"
	@echo "  make dev-run    - Start app locally (after dev-up)"
	@echo "  make dev-logs   - Show PostgreSQL logs"
	@echo ""
	@echo "Production:"
	@echo "  make prod-up    - Start all services in production mode"
	@echo "  make prod-down  - Stop production containers"
	@echo "  make prod-logs  - Show production logs"
	@echo ""
	@echo "Utilities:"
	@echo "  make build      - Build application"
	@echo "  make test       - Run tests"
	@echo "  make clean-dev  - Clean development environment"
	@echo "  make clean-prod - Clean production environment"

# Development commands
dev-up:
	@echo "🚀 Starting PostgreSQL for development..."
	docker-compose -f docker-compose.dev.yml up -d
	@echo "✅ PostgreSQL is running on port 5432"
	@echo "📊 Connection details:"
	@echo "   Host: localhost"
	@echo "   Port: 5432"
	@echo "   Database: homebuddy"
	@echo "   Username: homebuddy"
	@echo "   Password: homebuddy"

dev-down:
	@echo "🛑 Stopping development containers..."
	docker-compose -f docker-compose.dev.yml down

dev-run: dev-up
	@echo "🚀 Starting auth-service locally..."
	cd auth-service && ./gradlew run -Dmicronaut.environments=dev

dev-logs:
	docker-compose -f docker-compose.dev.yml logs -f

# Production commands
prod-up:
	@if [ ! -f .env.production ]; then \
		echo "❌ Missing .env.production file!"; \
		echo "📋 Create it based on .env.example and set production passwords"; \
		exit 1; \
	fi
	@echo "🚀 Starting HomeBuddy in production mode..."
	docker-compose --env-file .env.production up --build -d
	@echo "✅ HomeBuddy started in production mode"
	@echo "🌐 Auth Service: http://localhost:8081"
	@echo "📊 Health check: http://localhost:8081/health"

prod-down:
	@echo "🛑 Stopping production containers..."
	docker-compose --env-file .env.production down

prod-logs:
	docker-compose --env-file .env.production logs -f

# Build and test
build:
	@echo "🔨 Building auth-service..."
	cd auth-service && ./gradlew build

test:
	@echo "🧪 Running tests..."
	cd auth-service && ./gradlew test

# Cleanup
clean-dev:
	@echo "🧹 Cleaning development environment..."
	docker-compose -f docker-compose.dev.yml down -v
	docker volume rm homebuddy_postgres_dev_data 2>/dev/null || true
	@echo "✅ Development environment cleaned"

clean-prod:
	@echo "🧹 Cleaning production environment..."
	docker-compose --env-file .env.production down -v 2>/dev/null || docker-compose down -v
	docker volume rm homebuddy_postgres_data 2>/dev/null || true
	@echo "✅ Production environment cleaned"

# Health checks
health:
	@echo "🏥 Checking service health..."
	@curl -f http://localhost:8081/health 2>/dev/null && echo "✅ Auth Service is healthy" || echo "❌ Auth Service is not responding"

# Database connection test
db-test:
	@echo "🗄️ Testing database connection..."
	@docker exec -it $(docker-compose -f docker-compose.dev.yml ps -q postgres) psql -U homebuddy -d homebuddy -c "SELECT version();" && echo "✅ Database connection OK" || echo "❌ Database connection failed"

# Database management
db-reset:
	@echo "🔄 Resetting development database..."
	@read -p "This will DELETE ALL DATA! Continue? (y/N): " -n 1 -r; \
	echo; \
	if [[ $REPLY =~ ^[Yy]$ ]]; then \
		docker exec -it $(docker-compose -f docker-compose.dev.yml ps -q postgres) psql -U homebuddy -d homebuddy -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"; \
		echo "✅ Database reset. Restart app to run migrations."; \
	else \
		echo "❌ Database reset cancelled"; \
	fi

db-migrate-info:
	@echo "📊 Flyway migration info:"
	@echo "Run 'cd auth-service && ./gradlew flywayInfo' for detailed migration status"