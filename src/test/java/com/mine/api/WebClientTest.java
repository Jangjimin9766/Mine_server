package com.mine.api;

import org.springframework.web.reactive.function.client.WebClient;

public class WebClientTest {
    public static void main(String[] args) {
        try {
            WebClient.create().post().uri("http://localhost:12345").retrieve().bodyToMono(String.class).block();
        } catch (Exception e) {
            System.out.println("Exception: " + e.getClass().getName());
            System.out.println("Message: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("Cause: " + e.getCause().getClass().getName());
                System.out.println("Cause Message: " + e.getCause().getMessage());
            }
        }
    }
}
