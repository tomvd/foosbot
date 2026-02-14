package com.foosbot.db;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.core.order.Ordered;
import io.micronaut.runtime.event.ApplicationStartupEvent;
import jakarta.inject.Singleton;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

@Singleton
public class DatabaseMigration implements ApplicationEventListener<ApplicationStartupEvent>, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseMigration.class);

    private final DataSource dataSource;

    public DatabaseMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void onApplicationEvent(ApplicationStartupEvent event) {
        LOG.info("Running database migrations...");
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
        LOG.info("Database migrations complete.");
    }

    @Override
    public int getOrder() {
        return -100; // Run before other startup listeners
    }
}
