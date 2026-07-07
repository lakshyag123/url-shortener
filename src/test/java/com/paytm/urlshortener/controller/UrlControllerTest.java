package com.paytm.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paytm.urlshortener.dto.CreateShortUrlRequest;
import com.paytm.urlshortener.dto.CreateShortUrlResponse;
import com.paytm.urlshortener.dto.UrlStatsResponse;
import com.paytm.urlshortener.exception.ResourceNotFoundException;
import com.paytm.urlshortener.service.UrlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = UrlController.class)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UrlService urlService;

    @Test
    void shorten_returnsCreated() throws Exception {
        CreateShortUrlRequest req = new CreateShortUrlRequest("https://example.com", null);
        CreateShortUrlResponse resp = new CreateShortUrlResponse("abc","/abc", Instant.now());
        Mockito.when(urlService.shortenUrl(any())).thenReturn(resp);

        mockMvc.perform(post("/shorten").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.shortCode").value("abc"));
    }

    @Test
    void shorten_invalidUrl_returnsBadRequest() throws Exception {
        // missing originalUrl
        String body = "{\"originalUrl\": \"not-a-url\"}";
        mockMvc.perform(post("/shorten").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void redirect_success_returns302() throws Exception {
        Mockito.when(urlService.redirect(eq("abc"))).thenReturn("https://example.com");
        mockMvc.perform(get("/abc"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"));
    }

    @Test
    void redirect_notFound_returns404() throws Exception {
        Mockito.when(urlService.redirect(eq("nope"))).thenThrow(new ResourceNotFoundException("nope"));
        mockMvc.perform(get("/nope")).andExpect(status().isNotFound());
    }

    @Test
    void analytics_returns200() throws Exception {
        UrlStatsResponse stats = new UrlStatsResponse("abc", 10L, Instant.now(), Map.of());
        Mockito.when(urlService.analytics(eq("abc"))).thenReturn(stats);
        mockMvc.perform(get("/analytics/abc")).andExpect(status().isOk()).andExpect(jsonPath("$.totalClicks").value(10));
    }
}

