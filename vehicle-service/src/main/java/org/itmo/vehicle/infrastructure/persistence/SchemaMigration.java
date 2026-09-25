package org.itmo.vehicle.infrastructure.persistence;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

@ApplicationScoped
public class SchemaMigration {

    @Resource(lookup = "jdbc/vehicles")
    private DataSource dataSource;

    @Inject
    @ConfigProperty(name = "soa.db-schema")
    private String schema;

    void migrate(@Observes @Initialized(ApplicationScoped.class) Object applicationStarted) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .table("soa_flyway_history")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }
}
