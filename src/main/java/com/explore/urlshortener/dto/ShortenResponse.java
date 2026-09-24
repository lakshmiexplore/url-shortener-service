package com.explore.urlshortener.dto;

public record ShortenResponse(
    String shortCode,
    String shortUrl,
    String originalUrl
) {}