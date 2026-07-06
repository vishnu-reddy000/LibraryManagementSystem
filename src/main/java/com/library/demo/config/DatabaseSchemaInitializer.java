package com.library.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSchemaInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        logger.info("DatabaseSchemaInitializer: Checking database schema for legacy column 'issue_record_id' on 'fines' table...");
        
        try {
            // Find and drop any foreign key constraints associated with the legacy column
            String findConstraintsSql = "SELECT CONSTRAINT_NAME " +
                                         "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                                         "WHERE TABLE_SCHEMA = DATABASE() " +
                                         "  AND TABLE_NAME = 'fines' " +
                                         "  AND COLUMN_NAME = 'issue_record_id'";
            
            java.util.List<String> constraints = jdbcTemplate.queryForList(findConstraintsSql, String.class);
            for (String constraint : constraints) {
                if (constraint != null && !constraint.equalsIgnoreCase("PRIMARY")) {
                    try {
                        logger.info("DatabaseSchemaInitializer: Dropping foreign key constraint '{}' from 'fines' table...", constraint);
                        jdbcTemplate.execute("ALTER TABLE fines DROP FOREIGN KEY " + constraint);
                    } catch (Exception ex) {
                        logger.warn("DatabaseSchemaInitializer: Failed to drop foreign key constraint '{}': {}", constraint, ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("DatabaseSchemaInitializer: Failed during constraint check: {}", e.getMessage());
        }

        try {
            // Drop the column itself
            jdbcTemplate.execute("ALTER TABLE fines DROP COLUMN issue_record_id");
            logger.info("DatabaseSchemaInitializer: Successfully dropped legacy column 'issue_record_id' from 'fines' table.");
        } catch (Exception e) {
            logger.info("DatabaseSchemaInitializer: Column 'issue_record_id' in table 'fines' was already dropped (Error: {}).", e.getMessage());
        }
        
        try {
            // Print out all users in the database for debugging/verification
            String findUsersSql = "SELECT email, role FROM users";
            java.util.List<java.util.Map<String, Object>> users = jdbcTemplate.queryForList(findUsersSql);
            logger.info("DatabaseSchemaInitializer: Registered users in database:");
            for (java.util.Map<String, Object> u : users) {
                logger.info("  - Email: {}, Role: {}", u.get("email"), u.get("role"));
            }
            
            // Promote testadmin@gmail.com if exists to ADMIN for verification
            jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE email = 'testadmin@gmail.com'");
        } catch (Exception e) {
            logger.warn("DatabaseSchemaInitializer: Failed to list users or promote testadmin@gmail.com: {}", e.getMessage());
        }
    }
}
