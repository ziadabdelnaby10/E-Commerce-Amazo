-- V1.1: Drop the unused database-backed distributed lock table.
--
-- Rationale: the table was never wired into the reservation path, and cross-instance mutual
-- exclusion for stock reservation is now handled by Redis (SET NX PX + Lua compare-and-delete),
-- which does not tie a lock's lifetime to a database connection or transaction.

DROP TABLE IF EXISTS distributed_locks;

