package com.intuit.billingcomm.billing.qbeshostingabt.config;

import com.intuit.platform.webs.conversion.model.repository.ConversionAccountV2Repository;
import com.intuit.platform.webs.subscription.internal.model.repository.EntitledProductFeatureProcessingRepository;
import com.intuit.platform.webs.subscription.internal.model.repository.EntitledProductFeatureRepository;
import com.intuit.platform.webs.subscription.internal.model.repository.EntitledProductLicenseRepository;
import com.intuit.platform.webs.subscription.internal.model.repository.QbesHostingRequestStagingRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(
        basePackages = {
                "com.intuit.platform.webs.subscription.internal.model.repository",
                "com.intuit.platform.webs.conversion.model.repository"
        },
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        QbesHostingRequestStagingRepository.class,
                        EntitledProductFeatureRepository.class,
                        EntitledProductFeatureProcessingRepository.class,
                        EntitledProductLicenseRepository.class,
                        ConversionAccountV2Repository.class
                })
        }
)
public class TestDataConfig {

    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource,
                                                                       JpaVendorAdapter jpaVendorAdapter) {
        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactory.setDataSource(dataSource);
        entityManagerFactory.setJpaVendorAdapter(jpaVendorAdapter);
        entityManagerFactory.setPackagesToScan(
                "com.intuit.platform.webs.subscription.internal.model.entity",
                "com.intuit.platform.webs.conversion.model.entity"
        );
        return entityManagerFactory;
    }

    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:jdbc/schemas.sql")
                .addScript("classpath:jdbc/data.sql")
                .addScript("classpath:jdbc/hosting-features.sql")
                .build();
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        jpaVendorAdapter.setDatabase(Database.H2);
        jpaVendorAdapter.setDatabasePlatform("org.hibernate.dialect.Oracle10gDialect");
        return jpaVendorAdapter;
    }
}
