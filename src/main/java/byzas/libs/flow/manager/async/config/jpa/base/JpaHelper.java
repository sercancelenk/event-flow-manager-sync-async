package byzas.libs.flow.manager.async.config.jpa.base;

import com.zaxxer.hikari.HikariConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

@Component
@RequiredArgsConstructor
public class JpaHelper {
    private final Environment env;
    public static final String DEFAULT_ENTITY_SCAN_PACKAGES = "byzas.libs.flow.manager.config.jpa.entity";

    public DataSource createDatasource(String configPrefix) {
        return DataSourceBuilder.create()
                .url(env.getProperty(configPrefix + ".datasource.url", "Please provide mysql datasource url"))
                .username(env.getProperty(configPrefix + ".datasource.username", "Please provide mysql username"))
                .password(env.getProperty(configPrefix + ".datasource.password", "Please provide mysql password"))
                .driverClassName(env.getProperty(configPrefix + ".datasource.driver-class-name", "Please provide driver class name"))
                .build();
    }

    public HikariConfig createHikariConfig(String configPrefix, String hikariPoolName) {
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setDriverClassName(env.getProperty(configPrefix + ".datasource.driver-class-name", "Please provide driver class name"));
        hikariConfig.setJdbcUrl(env.getProperty(configPrefix + ".datasource.url", "Please provide mysql datasource url"));
        hikariConfig.setUsername(env.getProperty(configPrefix + ".datasource.username", "Please provide mysql username"));
        hikariConfig.setPassword(env.getProperty(configPrefix + ".datasource.password", "Please provide mysql password"));
        hikariConfig.setPoolName(env.getProperty(configPrefix + ".datasource.hikari.pool-name", hikariPoolName));
        hikariConfig.setAutoCommit(env.getProperty(configPrefix + ".datasource.hikari.auto-commit", Boolean.class, false));
        hikariConfig.setMaximumPoolSize(env.getProperty(configPrefix + ".datasource.hikari.maximum-pool-size", Integer.class, 10));
        hikariConfig.setMinimumIdle(env.getProperty(configPrefix + ".datasource.hikari.minimum-idle", Integer.class, 3));
        hikariConfig.setConnectionTimeout(env.getProperty(configPrefix + ".datasource.hikari.connectionTimeout", Integer.class, 10000));
        hikariConfig.setIdleTimeout(env.getProperty(configPrefix + ".datasource.hikari.idle-timeout", Integer.class, 60000));
        hikariConfig.addDataSourceProperty("dataSource.cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("dataSource.prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("dataSource.prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("dataSource.useServerPrepStmts", "true");
        return hikariConfig;
    }

    private Properties additionalJpaProperties(String configPrefix) {
        Properties map = new Properties();
        map.put("hibernate.show-sql", env.getProperty(configPrefix + ".jpa.properties.hibernate.show-sql"));
        map.put("hibernate.ddl-auto", env.getProperty(configPrefix + ".jpa.properties.hibernate.ddl-auto"));
        map.put("hibernate.hbm2ddl.auto", env.getProperty(configPrefix + ".jpa.properties.hibernate.ddl-auto"));
        map.put("hibernate.jdbc.batch_size", env.getProperty(configPrefix + ".jpa.properties.hibernate.jdbc.batch_size"));
        map.put("hibernate.order_inserts", env.getProperty(configPrefix + ".jpa.properties.hibernate.order_inserts"));
        map.put("hibernate.order_updates", env.getProperty(configPrefix + ".jpa.properties.hibernate.order_updates"));
        map.put("hibernate.generate_statistics", env.getProperty(configPrefix + ".jpa.properties.hibernate.generate_statistics"));
        map.put("hibernate.dialect", env.getProperty(configPrefix + ".jpa.properties.hibernate.dialect"));
        map.put("hibernate.connection.provider_disables_autocommit", env.getProperty(configPrefix + ".jpa.properties.hibernate.connection.provider_disables_autocommit"));

        return map;
    }

    public LocalContainerEntityManagerFactoryBean createLocalEntityManagerFactoryBean(String configPrefix, DataSource dataSource, String persistenceUnitName) {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setDataSource(dataSource);
        entityManagerFactoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        entityManagerFactoryBean.setPackagesToScan((DEFAULT_ENTITY_SCAN_PACKAGES + "," + env.getProperty(configPrefix + ".jpa.properties.hibernate.package_to_scan")).split(","));
        entityManagerFactoryBean.setPersistenceUnitName(persistenceUnitName);
        entityManagerFactoryBean.setJpaProperties(additionalJpaProperties(configPrefix));
        return entityManagerFactoryBean;
    }

    public PlatformTransactionManager createTransactionManager(EntityManagerFactory entityManagerFactory, DataSource jpaDatasource) {
        JpaTransactionManager txManager = new JpaTransactionManager();
        txManager.setEntityManagerFactory(entityManagerFactory);
        txManager.setDataSource(jpaDatasource);
        return txManager;
    }
}