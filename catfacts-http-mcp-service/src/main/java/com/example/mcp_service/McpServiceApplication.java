package com.example.mcp_service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.mcp.server.McpServer;
import org.springframework.ai.mcp.server.McpSyncServer;
import org.springframework.ai.mcp.server.transport.WebMvcSseServerTransport;
import org.springframework.ai.mcp.spec.McpSchema;
import org.springframework.ai.mcp.spec.ServerMcpTransport;
import org.springframework.ai.mcp.spring.ToolHelper;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@SpringBootApplication
@RegisterReflectionForBinding(Fact.class)
public class McpServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServiceApplication.class, args);
    }


    @Bean
    WebMvcSseServerTransport webMvcSseServerTransport(ObjectMapper objectMapper) {
        return new WebMvcSseServerTransport(objectMapper, "/mcp/message");
    }

    @Bean
    RouterFunction<ServerResponse> routerFunction(WebMvcSseServerTransport transport) {
        return transport.getRouterFunction();
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
        this.http = client.baseUrl("https://catfact.ninja").build();
    }

    Fact randomFact() {
        var random = this.http.get().uri("/fact").retrieve().body(Fact.class);
        System.out.println("got [" + random + ']');
        return random;
    }
}


record Fact(@JsonProperty("max_length") int length, String fact) {
}
