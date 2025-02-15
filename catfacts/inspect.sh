#!/usr/bin/env bash

# # Pass arguments only
# npx @modelcontextprotocol/inspector build/index.js arg1 arg2

# # Pass environment variables only
# npx @modelcontextprotocol/inspector -e KEY=value -e KEY2=$VALUE2 build/index.js

# # Pass both environment variables and arguments
# npx @modelcontextprotocol/inspector -e KEY=value -e KEY2=$VALUE2 build/index.js arg1 arg2

# # Use -- to separate inspector flags from server arguments
# npx @modelcontextprotocol/inspector -e KEY=$VALUE -- build/index.js -e server-flag

npx @modelcontextprotocol/inspector $1