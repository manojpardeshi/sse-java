package com.example.sse;

import org.springframework.beans.factory.annotation.Value;

public class Notification {
    String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
