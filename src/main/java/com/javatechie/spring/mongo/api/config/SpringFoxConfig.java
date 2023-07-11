package com.javatechie.spring.mongo.api.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static springfox.documentation.builders.PathSelectors.regex;


@Configuration
@EnableSwagger2
public class SpringFoxConfig {
    @Bean
    public Docket postsApi() {
        return new Docket(DocumentationType.SWAGGER_2).groupName("Java Techie").apiInfo(apiInfo()).select()
                .paths(regex("/trade.*")).build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("Trade Service")
                .description("Sample Documentation Generateed Using SWAGGER2 for our Book Rest API")
                .termsOfServiceUrl("")
                .license("")
                .licenseUrl("").version("1.0").build();
    }


}

