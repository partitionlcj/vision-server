package co.mega.vs;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SpringConfiguration {
    @Bean
    @ConfigurationProperties("app.datasource.local")
    public HikariDataSource localDataSource() {
        HikariDataSource dataSource = DataSourceBuilder.create().type(HikariDataSource.class).build();
        dataSource.addDataSourceProperty("useUnicode", "true");
        dataSource.addDataSourceProperty("characterEncoding", "utf8");
        return dataSource;
    }

    @Bean
    @ConfigurationProperties("app.datasource.test")
    public HikariDataSource testDataSource() {
        HikariDataSource dataSource = DataSourceBuilder.create().type(HikariDataSource.class).build();
        dataSource.addDataSourceProperty("useUnicode", "true");
        dataSource.addDataSourceProperty("characterEncoding", "utf8");
        return dataSource;
    }

    @Bean
    @ConfigurationProperties("app.datasource.stg")
    public HikariDataSource stgDataSource() {
        HikariDataSource dataSource = DataSourceBuilder.create().type(HikariDataSource.class).build();
        dataSource.addDataSourceProperty("useUnicode", "true");
        dataSource.addDataSourceProperty("characterEncoding", "utf8");
        return dataSource;
    }

    @Bean
    @ConfigurationProperties("app.datasource.gn-test")
    public HikariDataSource gnTestDataSource() {
        HikariDataSource dataSource = DataSourceBuilder.create().type(HikariDataSource.class).build();
        dataSource.addDataSourceProperty("useUnicode", "true");
        dataSource.addDataSourceProperty("characterEncoding", "utf8");
        return dataSource;
    }

    @Bean
    @ConfigurationProperties("app.datasource.gn-stg")
    public HikariDataSource gnStgDataSource() {
        HikariDataSource dataSource = DataSourceBuilder.create().type(HikariDataSource.class).build();
        dataSource.addDataSourceProperty("useUnicode", "true");
        dataSource.addDataSourceProperty("characterEncoding", "utf8");
        return dataSource;
    }

    @Bean
    public JdbcTemplate getDefaultJdbcTemplate() {
        return new JdbcTemplate(testDataSource());
    }

    @Profile({"local"})
    @Bean(name = "mainJdbcTemplate")
    public JdbcTemplate getLocalJdbcTemplate() {
        return new JdbcTemplate(localDataSource());
    }

    @Profile({"ds-mars-dev", "ds-mars-dev1", "ds-mars-qe", "ds-mars-int", "ds-mars-uee"})
    @Bean(name = "mainJdbcTemplate")
    public JdbcTemplate getTestJdbcTemplate() {
        return new JdbcTemplate(testDataSource());
    }

    @Profile({"ds-mars-stg", "ds-mars-prod"})
    @Bean(name = "mainJdbcTemplate")
    public JdbcTemplate getStgJdbcTemplate() {
        return new JdbcTemplate(stgDataSource());
    }

    @Profile({"ds-gn-test"})
    @Bean(name = "mainJdbcTemplate")
    public JdbcTemplate getGnTestJdbcTemplate() {
        return new JdbcTemplate(gnTestDataSource());
    }

    @Profile({"ds-gn-stg", "ds-gn-prod"})
    @Bean(name = "mainJdbcTemplate")
    public JdbcTemplate getGnStgJdbcTemplate() {
        return new JdbcTemplate(gnStgDataSource());
    }

}
