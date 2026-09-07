# GitHub Actions Setup

This document explains how to enable CI and Docker Hub publishing for this repository.

## Workflows

- `unit-tests-pr.yml`
  - Trigger: pull requests to `main`
  - Behavior: runs `./mvnw clean test` for changed services only
  - Shared file changes (`.github/`, `docs/`, `docker-compose.yml`, `README.md`, `ports.txt`) run all services

- `verify-main.yml`
  - Trigger: pushes to `main`
  - Behavior: runs `./mvnw clean verify` for all services

- `docker-publish.yml`
  - Trigger: pushes to `main` and tags matching `v*`
  - Behavior: builds service jars and publishes Docker images

## Required GitHub Secrets

Add these in repository settings:

1. `DOCKERHUB_USERNAME` = `ziadabdelnaby`
2. `DOCKERHUB_TOKEN` = Docker Hub access token

## Docker Image Naming

Docker repository: `ziadabdelnaby/amazo-ecommerce`

Published tags:

- On `main`:
  - `[service]-latest`
  - `[service]-sha-<shortSha>`
- On `v*` tag:
  - `[service]-vX.Y.Z`
  - `[service]-sha-<shortSha>`

Examples:

- `ziadabdelnaby/amazo-ecommerce:gateway-service-latest`
- `ziadabdelnaby/amazo-ecommerce:customer-service-v1.0.0`

## Services Included

- `config-service`
- `customer-service`
- `discovery-service`
- `gateway-service`
- `inventory-service`
- `notification-service`
- `order-service`
- `payment-service`

## First Run Checklist

1. Push branch with workflow files.
2. Open PR to `main` and validate `Unit Tests (PR)`.
3. Merge PR.
4. Validate `Verify Main` on `main`.
5. Create a tag (example `v1.0.0`) and push it to trigger Docker publish.

## Common Troubleshooting

- If Docker publish fails with auth error:
  - Recreate `DOCKERHUB_TOKEN` and update GitHub secret.
- If Maven wrapper fails in CI:
  - Ensure each service has `mvnw`, `mvnw.cmd`, and `.mvn/wrapper/maven-wrapper.properties`.
- If local wrapper fails on Windows:
  - Install local Maven or re-generate wrapper files with a working Maven installation.

