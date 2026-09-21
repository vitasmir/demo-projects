package com.angular.backend;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class BackendApplicationTests extends AbstractIntegrationTest {

    // This class is used to test if the Spring application context loads correctly
    // and if the main application bean is available in the context.
    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    void applicationBeanLoads() {
        assertThat(context.getBean(BackendApplication.class)).isNotNull();
    }
}
