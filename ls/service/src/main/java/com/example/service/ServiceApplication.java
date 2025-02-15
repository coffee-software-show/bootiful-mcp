package com.example.service;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.File;

@SpringBootApplication
public class ServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }

}

@Configuration
class FileToolsConfiguration {

    @Bean
    ToolCallbackProvider weatherTools(FileTools fileTools) {
        return MethodToolCallbackProvider.builder().toolObjects(fileTools).build();
    }

    @Component
    static class FileTools {

        @Tool(description = "returns all the files a folder, /Users/jlong/Desktop/user-context")
        String[] listFileNames() {
            System.out.println("listing the file names...");
            return new File("/Users/jlong/Desktop/user-context").list();
        }
    }
}
