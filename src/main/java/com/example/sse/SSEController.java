package com.example.sse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sse/mvc")
public class SSEController {

    private Map<String, SseEmitter> map = new HashMap<>();
    private Map<String, Map<String, SseEmitter>> topic = new HashMap<>();

    @Value("${topic.list}")
    private String[] validTopics;


    // private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

    @PostConstruct
    void initiate() {
        for (String m : validTopics) {
            topic.put(m, null);
        }
    }

    @GetMapping(value = "/streams/client/{topic}/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getEvents(@PathVariable("topic") String topic, @PathVariable("id") String id) {
        if (this.topic.containsKey(topic)) {
            SseEmitter emitter = new SseEmitter(360_000L);//keep connection open for 360 seconds
            Map<String, SseEmitter> inner = new HashMap<>();
            inner.put(id, emitter);
            if (this.topic.get(topic) != null) {
                inner = this.topic.get(topic);
                inner.put(id, emitter);
            } else {
                //first connection
                inner.put(id, emitter);
                this.topic.put(topic, inner);
            }
            emitter.onCompletion(() -> map.remove(id));
            emitter.onTimeout(() -> map.remove(id));
            return emitter;
        }
        return null;
    }

    @RequestMapping(value = "/streams/push", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void postMessage(@RequestBody Notification notification) {
        Map<String, SseEmitter> temp = this.topic.get(notification.getTopic());
        Map<String, SseEmitter> sseMap = temp.entrySet().stream()
                .filter(v -> v.getKey().equals(notification.getUid()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        sseMap.values().forEach(v -> {
            try {
                v.send(notification.getMessage());
            } catch (IOException e) {
                map.remove(notification.getUid());
            }
        });
    }


}
