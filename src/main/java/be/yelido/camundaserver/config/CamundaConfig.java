package be.yelido.camundaserver.config;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.spring.ProcessEngineFactoryBean;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;


@Configuration
public class CamundaConfig {

    @Bean
    public DataSource dataSource() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
//        dataSource.setDriverClass(org.h2.Driver.class);
//        dataSource.setUrl("jdbc:h2:mem:camunda;DB_CLOSE_DELAY=-1");
        dataSource.setDriverClass(com.mysql.cj.jdbc.Driver.class);
        dataSource.setUrl("jdbc:mysql://localhost:3306/camunda");
        dataSource.setUsername("root");
        dataSource.setPassword("admin");
        return dataSource;
    }

//    @Bean
//    public PlatformTransactionManager transactionManager() {
//        return new DataSourceTransactionManager(dataSource());
//    }
//
//    @Bean
//    public SpringProcessEngineConfiguration processEngineConfiguration() {
//        SpringProcessEngineConfiguration config = new SpringProcessEngineConfiguration();
//
//        config.setDataSource(dataSource());
//        config.setTransactionManager(transactionManager());
//
//        config.setDatabaseSchemaUpdate("true");
//        config.setHistory("full");
//        config.setJobExecutorActivate(true);
//
//        return config;
//    }
//
//    @Bean
//    public ProcessEngineFactoryBean processEngine() {
//        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
//        factoryBean.setProcessEngineConfiguration(processEngineConfiguration());
//        return factoryBean;
//    }

}
