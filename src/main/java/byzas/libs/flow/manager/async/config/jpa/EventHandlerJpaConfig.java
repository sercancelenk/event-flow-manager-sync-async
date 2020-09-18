package byzas.libs.flow.manager.async.config.jpa;

import byzas.libs.flow.manager.async.config.jpa.base.JpaBase;
import byzas.libs.flow.manager.util.YamlPropertySourceFactory;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@Log4j2
@EnableJpaRepositories(
        entityManagerFactoryRef = EventHandlerJpaConfig.ENTITY_MANAGER_FACTORY_REF,
        transactionManagerRef = EventHandlerJpaConfig.TRANSACTION_MANAGER_REF,
        basePackages = {EventHandlerJpaConfig.JPA_REPOSITORY_BASE_PACKAGES}
)
@RequiredArgsConstructor
@EnableJpaAuditing
@PropertySource(value = "classpath:event-handler-postgre-support.yml", factory = YamlPropertySourceFactory.class)
public class EventHandlerJpaConfig extends JpaBase {
    /**
     * Jpa Configuration Properties
     * Please type a new name and config prefix. Mandatory fields that you must be type,
     * CONFIG_PREFIX -> db properties start key prefix in yml file
     * BEAN_NAME_PREFIX
     */
    public static final String CONFIG_PREFIX = "event-handler";
    public static final String BEAN_NAME_PREFIX = "eventHandler";
    public static final String JPA_REPOSITORY_BASE_PACKAGES = "byzas.libs.flow.manager.async.config.jpa.repository";

    public static final String ENTITY_MANAGER_FACTORY_REF = BEAN_NAME_PREFIX + ENTITY_MANAGER_STR;
    public static final String TRANSACTION_MANAGER_REF = BEAN_NAME_PREFIX + TRANSACTION_MANAGER_STR;
    public static final String DATASOURCE_BEAN_NAME = BEAN_NAME_PREFIX + DATASOURCE_STR;
    public static final String PERSISTENCE_UNIT_NAME = BEAN_NAME_PREFIX + PERSISTENCE_UNIT_STR;
    public static final String HIKARI_POOL_NAME = BEAN_NAME_PREFIX + HIKARI_POOL_STR;

    @PostConstruct
    public void init(){
        log.info("JPA COnfig initialized");
    }

    @Bean(destroyMethod = "close", name = DATASOURCE_BEAN_NAME)
    public HikariDataSource dataSource() {
        return super.jpaDatasource(CONFIG_PREFIX, HIKARI_POOL_NAME);
    }

    @Bean(name = ENTITY_MANAGER_FACTORY_REF)
    public LocalContainerEntityManagerFactoryBean entityManagerFactoryBean(@Qualifier(DATASOURCE_BEAN_NAME) DataSource dataSource) {
        return super.entityManagerFactoryBean(dataSource, CONFIG_PREFIX, PERSISTENCE_UNIT_NAME);
    }

    @Primary
    @Bean(name = TRANSACTION_MANAGER_REF)
    public PlatformTransactionManager transactionManager(@Qualifier(ENTITY_MANAGER_FACTORY_REF) EntityManagerFactory entityManagerFactory,
                                                         @Qualifier(DATASOURCE_BEAN_NAME) DataSource centralConfigDatasource) {
        return super.transactionManager(entityManagerFactory, centralConfigDatasource);
    }
}