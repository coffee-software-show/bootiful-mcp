package com.example.ls;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

@SpringBootApplication
public class LsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LsApplication.class, args);
    }
}


// 1.
@Configuration
@Profile("one")
class LocalToolsAutoConfiguration {

    @Component
    @Profile("one")
    static class FileToolsWithComponentModel {

        @Tool(description = "returns all the files a folder, /Users/jlong/Desktop/user-context")
        String[] localListFile () {
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
@Profile("two")
class ThirdPartyManuallyConfiguredMcpServerConfiguration {

    @Bean
    McpSyncClient mcpClient(@Value("${HOME}/Desktop/user-context") File dbPath) {
        var stdioParams = ServerParameters.builder("npx")
                .args("-y", "@modelcontextprotocol/server-filesystem", dbPath.getAbsolutePath())
                .build();
        var mcpClient = McpClient//
                .sync(new StdioClientTransport(stdioParams))//
                .requestTimeout(Duration.ofSeconds(10))//
                .build();
        mcpClient.initialize();
        return mcpClient;
    }

    @Bean
    NamedMcpClientRunner thirdPartyManuallyConfiguredMcpServer(McpSyncClient mcpClient, ChatClient.Builder builder) {
        var mcpToolCallbackProvider = new SyncMcpToolCallbackProvider(mcpClient);
        return new NamedMcpClientRunner(builder.defaultTools(mcpToolCallbackProvider));
    }

}

@Configuration
class ThirdPartyAutoConfiguredMcpServerConfiguration {

    // 3.
    @Bean
    @Profile("three")
    NamedMcpClientRunner thirdPartyAutoConfiguredMcpServer(ChatClient.Builder builder, ToolCallbackProvider provider) {
        return new NamedMcpClientRunner(builder.defaultTools(provider));
    }
}


// 4.
@Configuration
//    @Profile("four")
class BootifulAutoConfiguredMcpServerConfiguration {

    @Bean
    McpSyncClient mcpClient() {
        var mcpClient = McpClient
                .sync(new HttpClientSseClientTransport("http://localhost:8080"))
                .build();
        mcpClient.initialize();
        return mcpClient;
    }

    @Bean
    NamedMcpClientRunner bootifulAutoConfiguredMcpServer(McpSyncClient mcpSyncClient, ChatClient.Builder builder) {
        var mcpToolCallbackProvider = new SyncMcpToolCallbackProvider(mcpSyncClient);
        return new NamedMcpClientRunner(builder.defaultTools(mcpToolCallbackProvider));
    }
}

record ChineseFile(String fileName, String word) {
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
        var prompt = "what files are in the user-context folder and of those files," +
                " which have names that correspond to words in Chinese?";
        var entity = this.builder.build().prompt(prompt).call().entity(ChineseFile.class);
        System.out.println(this.beanName.get() + ": " + entity);
    }
}
