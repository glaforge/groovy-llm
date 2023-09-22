package com.google.cloud.devrel.docchat;

import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;

@Controller("/query")
public class StatelessDocchatController {
    private final LLMQueryService queryService;

    public StatelessDocchatController(LLMQueryService queryService) {
        this.queryService = queryService;
    }

    @Post
    @Consumes("application/json")
    @Produces("application/json")
    String query(String query, String chatId) {
        return queryService.executeWithMemory(query, chatId);
    }
}
