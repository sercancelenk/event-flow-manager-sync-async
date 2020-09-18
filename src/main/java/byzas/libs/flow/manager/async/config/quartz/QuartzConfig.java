package byzas.libs.flow.manager.async.config.quartz;

import lombok.extern.log4j.Log4j2;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

@Log4j2
@Configuration
public class QuartzConfig {

    @Autowired
    private QuartzPropertyList quartzPropertyList;

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

    public final class AutoWiringSpringBeanJobFactory extends SpringBeanJobFactory implements ApplicationContextAware {

        private transient AutowireCapableBeanFactory beanFactory;

        @Override
        public void setApplicationContext(final ApplicationContext applicationContext) {
            beanFactory = applicationContext.getAutowireCapableBeanFactory();
        }

        @Override
        protected Object createJobInstance(final TriggerFiredBundle bundle) throws Exception {
            final Object job = super.createJobInstance(bundle);
            beanFactory.autowireBean(job);
            return job;
        }
    }

    @PostConstruct
    public void init(){
        log.info("Quartz COnfig initialized");
    }

    @Bean
    public JobFactory jobFactory(ApplicationContext applicationContext) {
        AutoWiringSpringBeanJobFactory jobFactory = new AutoWiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }


    @Bean(name = "clusteredScheduler")
    public SchedulerFactoryBean clusteredScheduler(JobFactory jobFactory) throws IOException {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setQuartzProperties(quartzProperties());
        factory.setJobFactory(jobFactory);
        return factory;
    }

    @Bean
    public Properties quartzProperties() throws IOException {
        PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
        Properties properties = new Properties();
        if (Objects.nonNull(quartzPropertyList.getProperties())) {
            quartzPropertyList
                    .getProperties()
                    .entrySet()
                    .stream()
                    .forEach((entry) -> {
                        properties.setProperty(entry.getKey(), entry.getValue());
                    });
        }
        propertiesFactoryBean.setProperties(properties);
        propertiesFactoryBean.afterPropertiesSet();
        return propertiesFactoryBean.getObject();
    }

    private void shutdownScheduler(SchedulerFactoryBean clusteredFactoryBean) {
        try {
            clusteredFactoryBean.getScheduler().shutdown(true);
        } catch (Exception e) {
            try {
                clusteredFactoryBean.getScheduler().shutdown(false);
            } catch (Exception e2) {
                log.error("Can not shutdown quartz scheduler", e2);
            }
        }
    }

    @PreDestroy
    private void shutdownScheduler() {
        shutdownScheduler((SchedulerFactoryBean) applicationContext.getBean("clusteredScheduler"));
    }

}
