package byzas.libs.flow.manager.async.config.jpa.base;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

public abstract class JpaBase implements DatasourceConstants  {
    @Autowired
    private JpaHelper jpaHelper;

    public HikariDataSource jpaDatasource(String configPrefix, String hikariPoolName) {
        //        DataSource dataSource = sardisJpaHelper.createDatasource(configPrefix);
        HikariConfig hikariConfig = jpaHelper.createHikariConfig(configPrefix, hikariPoolName);

        return new HikariDataSource(hikariConfig);
    }

    public LocalContainerEntityManagerFactoryBean entityManagerFactoryBean(DataSource dataSource, String configPrefix, String persistenceUnitName) {
        return jpaHelper.createLocalEntityManagerFactoryBean(configPrefix, dataSource, persistenceUnitName);
    }

    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory, DataSource jpaDatasource) {
        return jpaHelper.createTransactionManager(entityManagerFactory, jpaDatasource);
    }
}

interface DatasourceConstants {
    String ENTITY_MANAGER_STR = "EntityManager";
    String TRANSACTION_MANAGER_STR = "TransactionManager";
    String DATASOURCE_STR = "Datasource";
    String PERSISTENCE_UNIT_STR = "PersistenceUnit";
    String HIKARI_POOL_STR = "PostgreHikariPool";

}