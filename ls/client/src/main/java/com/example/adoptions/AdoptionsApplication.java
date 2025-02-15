package com.example.adoptions;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@SpringBootApplication
public class AdoptionsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdoptionsApplication.class, args);
    }


    // 1.
    @Configuration
    static class LocalToolsAutoConfiguration {

        @Component
        static class FileToolsWithComponentModel {

            @Tool(description = "returns all the files a folder, /Users/jlong/Desktop/user-context")
            String[] listFileNames() {
                var folder = new File("/Users/jlong/Desktop/user-context");
//                System.out.println("   > returning the files in [" + folder.getAbsolutePath() + "]");
                return folder.list();
            }

        }

        @Bean
        NamedMcpClientRunner localToolsRunner(ChatClient.Builder builder, FileToolsWithComponentModel tools) {
            return new NamedMcpClientRunner(builder.defaultTools(tools));
        }

    }

    // 2.
    @Configuration
    static class ThirdPartyManuallyConfiguredMcpServerConfiguration {

        @Bean 
        McpSyncClient mcpClient(@Value("${HOME}/Desktop/user-context") File dbPath) {
            var stdioParams = ServerParameters.builder("npx")
                    .args("-y", "@modelcontextprotocol/server-filesystem",  dbPath.getAbsolutePath())
                    .build();
            var mcpClient = McpClient//
                    .sync(new StdioClientTransport(stdioParams))//
                    .requestTimeout(Duration.ofSeconds(10))//
                    .build();
            mcpClient.initialize();
            return mcpClient;
        }

        @Bean
        NamedMcpClientRunner thirdPartyManuallyConfiguredMcpServer(McpSyncClient mcpClient , ChatClient.Builder builder) {
            var mcpToolCallbackProvider = new SyncMcpToolCallbackProvider(mcpClient) ;
            return new NamedMcpClientRunner(builder.defaultTools(mcpToolCallbackProvider));
        }

    }


    // 3.
    @Bean
    NamedMcpClientRunner thirdPartyAutoConfiguredMcpServer(ChatClient.Builder builder, ToolCallbackProvider provider) {
        return new NamedMcpClientRunner(builder.defaultTools(provider));
    }

}

class NamedMcpClientRunner implements ApplicationRunner, BeanNameAware {

    private final AtomicReference<String> beanName = new AtomicReference<>();

    private final ChatClient.Builder builder;

    NamedMcpClientRunner(ChatClient.Builder builder) {
        this.builder = builder;
    }

    @Override
    public void setBeanName(String name) {
        this.beanName.set(name);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var prompt = "what files are in the user-context folder and of those files, which have names that correspond to words in Chinese?";
        var entity = this.builder.build().prompt(prompt).call().entity(ChineseFile.class);
        System.out.println(this.beanName.get() + ": " + entity);
    }
}

record ChineseFile(String fileName, String word) {
}