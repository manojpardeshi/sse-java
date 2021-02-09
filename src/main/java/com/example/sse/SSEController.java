package com.example.sse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.yaml.snakeyaml.emitter.Emitter;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import static com.google.common.collect.MoreCollectors.onlyElement;

@RestController
@RequestMapping("/sse/mvc")
public class SSEController {

    //private Map<String, SseEmitter> map = new HashMap<>();
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

    @GetMapping(value = "/streams/client/{topic}/{uid}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getEvents(@PathVariable("topic") String topic, @PathVariable("uid") String id) {
        if (this.topic.containsKey(topic)) {
            SseEmitter emitter = new SseEmitter(360_000L);//keep connection open for 360 seconds
            Map<String, SseEmitter> connectionPool = new HashMap<>();
            connectionPool.put(id, emitter);
            if (this.topic.get(topic) != null) {
                connectionPool = this.topic.get(topic);
                connectionPool.put(id, emitter);
            } else {
                //first connection
                connectionPool.put(id, emitter);
                this.topic.put(topic, connectionPool);
            }

            Map<String, SseEmitter> finalConnectionPool = connectionPool;
            emitter.onCompletion(() -> finalConnectionPool.remove(id));
            emitter.onTimeout(() -> finalConnectionPool.remove(id));
            //OnError
           // emitter.onError(()->finalConnectionPool.remove(id));
            return emitter;
        }else{
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

    }

    @RequestMapping(value = "/streams/push", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void postMessage(@RequestBody Notification notification) {
        Map<String, SseEmitter> topic = this.topic.get(notification.getTopic());
        SseEmitter connection = topic.entrySet().stream()
                .filter(v -> v.getKey().equals(notification.getUid()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()))
                .values().stream().findFirst().orElse(null);

        /*SseEmitter connectionUsingGuava = topic.entrySet().stream()
                .filter(v -> v.getKey().equals(notification.getUid()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()))
                .values()
                .stream()
                .collect(onlyElement());*/


        try {
            connection.send(notification.getMessage());
        } catch (IOException e) {
            // Remove from connection pool
            //topic.remove(notification.getUid());

        }

    }


}
