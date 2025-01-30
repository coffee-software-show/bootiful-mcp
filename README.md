# Bootiful MCP 


MCP is a protocol by which to extend a software clients available "tools." Think of it like a plugin model, but for smart clients, like LLMs. 

In this repository we look at several scenarios. 

For these examples, I'll be assuming the final client is Claude Desktop, the macOS and Windows desktop application for Claude. 

All modules will use Java 21's virtual threads or reactive programming to ensure the easiest time scaling the system. 

All modules will use GraalVM native images. To make this trivial, I've exctracted out some Spring AOT hints to help the GraalVM native image compilation along into a module called `mcp-aot`. 

Some of these examples assume Redis is present, so there's a Docker Compose `compose.yml` in the root of this project. Run `docker compose up -d ` in the root.

## Simple STDIO MCP Service

The first example, `catfacts-stdio-mcp-service`, is a simple service that can be used with GraalVm and registered with Claude Desktop directly. It in turn acts as a client to another [webservice called CatFacts](https://catfacts.ninja).


## Simple HTTP MCP Service 

This next example, `catfacts-stdio-mcp-service`, is built using HTTP. 

The MCP protocol supports HTTP, but Claude Desktop itself does not. So, we'll need a bridge, to act as a client to our HTTP MCP service, and a service for the Claude Desktop client, adapting STDIO to HTTP. That's `catfacts-mcp-proxy`. It'll generate a native binary that can be registerd with Claude Desktop. To use it, you'll need to export an environment variable with a username and a password on your desktop environment. if you're connecting to the HTTP MCP service directly, you'll need to specify that by overriding the `catfacts.mcp.host` property.

Our HTTP service will no doubt be overrun with eager clients and ussers, so we'll need to design the system in such a way as to be more resilient, and to reduce redundant configuration. This is the `catfacts-mcp-gateway`, which is an instance of Spring Cloud Gateway designed to centrally support Eureka-based load balancing, Redis-based rate limiting, and authentication.

