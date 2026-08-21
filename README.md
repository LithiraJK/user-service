# Tripvisito - User Service

## Mandatory Student & GCP Information
- **Student Name:** Lithira Jayanaka
- **Student Number:** 241722002
- **GCP Project ID:** project-a4f7bad0-3923-4cdb-b9b

---

## Project Description
The **User Service** manages user authentication, authorization, and profile settings for the Tripvisito platform. It issues JWT tokens for session validation, supports traditional email/password registration, integrates Google OAuth login, handles profile picture uploads to Google Cloud Storage (GCS) buckets, and aggregates dashboard stats by calling other services.

## Database & Cloud Storage
- **Relational Database:** MySQL (`tripvisito_users` schema) - satisfies the ECA Relational DB requirement.
- **Cloud Storage:** Google Cloud Storage bucket (`tripvisito-trip-images`) - satisfies the ECA GCP Bucket integration requirement.

## Technology Stack
- **Runtime:** Java 25
- **Framework:** Spring Boot (v3+)
- **ORM:** Spring Data JPA & Hibernate
- **Security:** Spring Security & JWT Token Generator
- **Storage:** Google Cloud Storage Client SDK
- **Build Tool:** Maven

## Setup / Getting Started Instructions

### Prerequisites
- JDK 25 installed
- MySQL Database running on port `3308` (configurable via environment variables)
- GCP Service Account Key for GCS bucket storage

### Local Setup
1. Navigate to the service folder:
   ```bash
   cd tripvisito-springboot/business-services/user-service
   ```
2. Set up environment variables in your local environment or `.env` file:
   ```env
   DB_HOST=127.0.0.1
   DB_PORT=3308
   DB_USERNAME=root
   DB_PASSWORD=mysql
   JWT_SECRET=your-super-secret-key-32-chars-long
   GCP_BUCKET_NAME=your-gcp-bucket-name
   ```
3. Run the service:
   ```bash
   mvn spring-boot:run
   ```
   The service runs on port `8081`.

### PM2 Deployment
On the GCP VM (IaaS):
```bash
pm2 start ecosystem.config.js --only user-service
```
Processes automatically write output logs and restart if the VM restarts.
