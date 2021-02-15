package com.example.sse;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("topic")
@Component
public class ServiceConfigurations {

    private HashMap<String, String> timeouts;

    public HashMap<String, String> getTimeouts() {
        return timeouts;
    }

    public void setTimeouts(HashMap<String, String> timeouts) {
        this.timeouts = timeouts;
    }
}
