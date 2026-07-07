package com.paytm.urlshortener.controller;

import com.paytm.urlshortener.dto.CreateShortUrlRequest;
import com.paytm.urlshortener.dto.CreateShortUrlResponse;
import com.paytm.urlshortener.dto.UrlStatsResponse;
import com.paytm.urlshortener.exception.ResourceNotFoundException;
import com.paytm.urlshortener.model.UrlMapping;
import com.paytm.urlshortener.repository.UrlRepository;
import com.paytm.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST controller exposing URL shortening endpoints.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "URL Shortener", description = "Create short URLs, redirect and view analytics")
public class UrlController {

    private final UrlService urlService;
    private final UrlRepository urlRepository;

    @Operation(summary = "Create a short URL", description = "Shortens an original URL; optional customAlias allowed")
    @ApiResponse(responseCode = "201", description = "Created", content = @Content(schema = @Schema(implementation = CreateShortUrlResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request")
    @ApiResponse(responseCode = "409", description = "Custom alias conflict")
    @PostMapping(path = "/shorten", consumes = "application/json", produces = "application/json")
    public ResponseEntity<CreateShortUrlResponse> shorten(
            @Valid
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Request to create a short URL",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CreateShortUrlRequest.class),
                            examples = @ExampleObject(name = "example",
                                    value = "{\"originalUrl\": \"https://www.paytm.com/offers/special?page=1\", \"customAlias\": \"paytm-offer\"}")))
            CreateShortUrlRequest request) {
        CreateShortUrlResponse resp = urlService.shortenUrl(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/{code}")
                .buildAndExpand(resp.shortCode())
                .toUri();
        return ResponseEntity.created(location).body(resp);
    }

    @Operation(summary = "Redirect to original URL", description = "Performs 302 redirect to the original URL for the given code")
    @ApiResponse(responseCode = "302", description = "Found / Redirect")
    @ApiResponse(responseCode = "404", description = "Short code not found")
    @GetMapping(path = "/{code}")
    public ResponseEntity<Void> redirect(@PathVariable("code") String code) {
        String target = urlService.redirect(code);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(target));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    
    @Operation(summary = "Resolve code without redirect", description = "Returns original URL and metadata without performing a redirect")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CreateShortUrlResponse.class)))
    @ApiResponse(responseCode = "404", description = "Short code not found")
    @GetMapping(path = "/resolve/{code}", produces = "application/json")
    public ResponseEntity<CreateShortUrlResponse> resolve(@PathVariable("code") String code) {
        UrlMapping mapping = urlRepository.findByShortCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Short code not found: " + code));
        CreateShortUrlResponse resp = new CreateShortUrlResponse(mapping.getShortCode(), mapping.getOriginalUrl(), mapping.getCreatedAt());
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Get analytics for a short code")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = UrlStatsResponse.class)))
    @ApiResponse(responseCode = "404", description = "Short code not found")
    @GetMapping(path = "/analytics/{code}", produces = "application/json")
    public ResponseEntity<UrlStatsResponse> analytics(@PathVariable("code") String code) {
        UrlStatsResponse stats = urlService.analytics(code);
        return ResponseEntity.ok(stats);
    }
}

