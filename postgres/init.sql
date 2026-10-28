-- Create separate databases for each microservice
CREATE DATABASE user_db;
CREATE DATABASE order_db;
CREATE DATABASE inventory_db;
CREATE DATABASE payment_db;
CREATE DATABASE notification_db;

-- Note: Each service will create its own tables via Hibernate DDL
-- This script ensures databases exist before services start
