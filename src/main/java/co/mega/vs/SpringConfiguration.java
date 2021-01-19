package co.mega.vs;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
