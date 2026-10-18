package com.TravelBuddy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SabreToken(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") Long expiresIn
) {}