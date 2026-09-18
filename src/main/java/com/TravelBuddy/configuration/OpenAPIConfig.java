package com.TravelBuddy.configuration;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.servers.Server;

// Documentation of Swagger UI
@OpenAPIDefinition(
  info = @Info(
  title = "TravelBuddy API",
  version = "1.0.0",
  description = "API to manage TravelBuddy application",
  license = @License(
    name = "Apache License 2.0", 
    url = "NA")),
  servers = @Server(url = "http://localhost:8080")
)
public class OpenAPIConfig {
    
}