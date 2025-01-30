package com.example.mcp_service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.mcp.client.McpClient;
import org.springframework.ai.mcp.client.McpSyncClient;
import org.springframework.ai.mcp.client.transport.WebFluxSseClientTransport;
import org.springframework.ai.mcp.server.McpServer;
import org.springframework.ai.mcp.server.McpSyncServer;
import org.springframework.ai.mcp.server.transport.StdioServerTransport;
import org.springframework.ai.mcp.spec.ClientMcpTransport;
import org.springframework.ai.mcp.spec.McpSchema;
import org.springframework.ai.mcp.spec.ServerMcpTransport;
import org.springframework.ai.mcp.spring.ToolHelper;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * this code acts as a bridge between the HTTP MCP service and the STDIO MCP client
 * 
 * @author Josh Long
 */
@SpringBootApplication
@RegisterReflectionForBinding(Fact.class)
public class McpServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServiceApplication.class, args);
    }

    @Bean
    StdioServerTransport stdioServerTransport() {
        return new StdioServerTransport();
    }
    
    @Bean
    WebFluxSseClientTransport sseClientTransport(WebClient.Builder webClient, ObjectMapper objectMapper) {
        return new WebFluxSseClientTransport(
                webClient.baseUrl("http://localhost:3001"), objectMapper);
    }

    @Bean
    McpSyncServer mcpServer(
            CatFactMcpClient catFactClient,
            ServerMcpTransport transport) {

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


record Fact(@JsonProperty("max_length") int length, String fact) {
}


@Service
class CatFactMcpClient {

    private final ClientMcpTransport transport;

    private final ObjectMapper objectMapper;

    CatFactMcpClient(ClientMcpTransport transport, ObjectMapper objectMapper) {
        this.transport = transport;
        this.objectMapper = objectMapper;
    }

    Fact randomFact() {
        try (var client = McpClient.sync(this.transport).build()) {
            client.initialize();
            client.ping();
            return this.callTool(client, "fact", Map.of(), Fact.class);
        }
    }

    private <T> T callTool(McpSyncClient client, String toolName, Map<String, Object> args,
                           Class<T> tClass) {
        try {
            var tools = client.listTools();
            Assert.state(
                    tools.tools().stream().anyMatch(tool -> tool.name().contains(toolName)),
                    "there is no tool called '" + toolName + "' on this MCP server.");
            var callToolRequest = new McpSchema.CallToolRequest(toolName, args);
            var result = client.callTool(callToolRequest);
            if (result.content().getFirst() instanceof McpSchema.TextContent textContent) {
                var json = textContent.text();
                return objectMapper.readValue(json, tClass);
            }
        }//
        catch (Throwable throwable) {
            throw new IllegalStateException(throwable);
        }
        return null;
    }
}