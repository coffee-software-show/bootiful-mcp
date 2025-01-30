package com.example.mcp_service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.mcp.server.McpServer;
import org.springframework.ai.mcp.server.McpSyncServer;
import org.springframework.ai.mcp.server.transport.StdioServerTransport;
import org.springframework.ai.mcp.spec.McpSchema;
import org.springframework.ai.mcp.spec.ServerMcpTransport;
import org.springframework.ai.mcp.spring.ToolHelper;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@ImportRuntimeHints(McpHints.class)
@SpringBootApplication
public class McpServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServiceApplication.class, args);
    }

    @Bean
    ApplicationRunner pidListenerApplicationRunner (@Value("file://${HOME}/Desktop/pid") File pid) {
        return _ -> {
            var ap = new ApplicationPid() .toLong();
             try (var out = new FileWriter(pid)){
                 out.write(ap.toString());
             }
        };
    }


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
        this.http = client.build();
    }

    Fact randomFact() {
        var random = this.http.get().uri("/fact").retrieve().body(Fact.class);
        System.out.println("got [" + random + ']');
        return random;
    }
}


record Fact(@JsonProperty("max_length") int length, String fact) {
}

// for AOT
class ClasseHints {


}


class McpHints implements RuntimeHintsRegistrar {


    private static Set<TypeReference> innerClasses(Class<?> clzz) {
        var indent = new HashSet<String>();
        findNestedClasses(clzz, indent);
        return indent.stream().map(TypeReference::of).collect(Collectors.toSet());
    }


    private static void findNestedClasses(Class<?> clazz, Set<String> indent) {
        var classes = new ArrayList<Class<?>>();
        classes.addAll(Arrays.asList(clazz.getDeclaredClasses()));
        classes.addAll(Arrays.asList(clazz.getClasses()));
        for (var nestedClass : classes) {
            findNestedClasses(nestedClass, indent);
        }
        indent.addAll(classes.stream().map(Class::getName).toList());
    }

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

        var mcs = MemberCategory.values();

        var extra = Set
                .of(Fact.class)
                .stream()
                .map(TypeReference::of)
                .toList();

        var register = new HashSet<TypeReference>();
        register.addAll(innerClasses(McpSchema.class));
        register.addAll(extra);

        for (var tr : register)
            hints.reflection().registerType(tr, mcs);


    }
}