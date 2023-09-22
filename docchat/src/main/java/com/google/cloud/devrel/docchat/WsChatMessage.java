package com.google.cloud.devrel.docchat;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record WsChatMessage(String message) {
    public String toString() {
        return message;
    }
}