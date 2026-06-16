CREATE ALIAS IF NOT EXISTS UTC_TIMESTAMP AS $$
java.sql.Timestamp utcTimestamp(int precision) {
    return java.sql.Timestamp.from(java.time.Instant.now());
}
$$;
