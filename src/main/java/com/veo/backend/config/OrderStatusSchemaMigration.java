package com.veo.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        migrateOrdersStatusColumn();
        migrateOrderStatusHistoryColumn();
    }

    private void migrateOrdersStatusColumn() {
        String columnType = getColumnType("orders", "status");
        if (columnType == null) {
            return;
        }

        if (isEnumColumn(columnType)) {
            jdbcTemplate.execute("""
                    ALTER TABLE orders
                    MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'PENDING_PAYMENT'
                    """);
            log.info("Migrated orders.status from {} to VARCHAR(50)", columnType);
        }
    }

    private void migrateOrderStatusHistoryColumn() {
        String columnType = getColumnType("order_status_history", "status");
        if (columnType == null) {
            return;
        }

        if (isEnumColumn(columnType) || !supportsOperationStatuses(columnType)) {
            jdbcTemplate.execute("""
                    ALTER TABLE order_status_history
                    MODIFY COLUMN status VARCHAR(50) NOT NULL
                    """);
            log.info("Migrated order_status_history.status from {} to VARCHAR(50)", columnType);
        }
    }

    private String getColumnType(String tableName, String columnName) {
        return jdbcTemplate.query(
                """
                SELECT COLUMN_TYPE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """,
                ps -> {
                    ps.setString(1, tableName);
                    ps.setString(2, columnName);
                },
                rs -> rs.next() ? rs.getString("COLUMN_TYPE") : null
        );
    }

    private boolean isEnumColumn(String columnType) {
        return columnType != null && columnType.trim().toLowerCase().startsWith("enum(");
    }

    private boolean supportsOperationStatuses(String columnType) {
        if (columnType == null) {
            return false;
        }

        String normalized = columnType.toUpperCase();
        return normalized.contains("PACKING")
                && normalized.contains("READY_TO_SHIP")
                && normalized.contains("MANUFACTURING");
    }
}
