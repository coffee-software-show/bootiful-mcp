package com.example.mcp_service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.mcp.server.McpServer;
import org.springframework.ai.mcp.server.McpSyncServer;
import org.springframework.ai.mcp.server.transport.StdioServerTransport;
import org.springframework.ai.mcp.spec.McpSchema;
import org.springframework.ai.mcp.spec.ServerMcpTransport;
import org.springframework.ai.mcp.spring.ToolHelper;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.io.FileWriter;

@SpringBootApplication
@RegisterReflectionForBinding(Fact.class)
public class McpServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServiceApplication.class, args);
    }

   /* @Bean
    ApplicationRunner pidListenerApplicationRunner(@Value("file://${HOME}/Desktop/pid") File pid) {
        return _ -> {
            var ap = new ApplicationPid().toLong();
            try (var out = new FileWriter(pid)) {
                out.write(ap.toString());
            }
        };
    }*/


    @Bean
    StdioServerTransport stdioServerTransport() {
        return new StdioServerTransport();
    }

    @Bean
    McpSyncServer mcpServer(
            ServerMcpTransport transport,
            CatFactClient catFactClient) {

        var capabilities = McpSchema.ServerCapabilities
                .builder()
                .resources(false, true)
                .tools(true)
                .prompts(true)
                .logging()
                .build();

        var tool = FunctionCallback
                .builder()
                .function("fact", catFactClient::randomFact)
                .description("returns a random fact about cats")
                .build();

        var toolRegistration = ToolHelper.toSyncToolRegistration(tool);

        return McpServer
                .sync(transport)
                .serverInfo("MCP Cat Facts Server", "1.0.0")
                .capabilities(capabilities)
                .tools(toolRegistration)
                .build();
    }
}

@Component
class CatFactClient {

    private final RestClient http;

    CatFactClient(RestClient.Builder client) {
        this.http = client .baseUrl("https://catfact.ninja").build();
    }

    Fact randomFact() {
        return this.http.get().uri("/fact").retrieve().body(Fact.class);
    }
}

record Fact(@JsonProperty("max_length") int length, String fact) {
}


 