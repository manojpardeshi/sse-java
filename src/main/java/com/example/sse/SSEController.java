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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;


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
    public SseEmitter clientConnect(@PathVariable("topic") String topic, @PathVariable("uid") String id) {
        SseEmitter emitter = new SseEmitter(360_000L);//keep connection open for 360 seconds


        if (this.topic.containsKey(topic)) {

            Map<String, SseEmitter> connectionPool;
            if (this.topic.get(topic) != null) {
                connectionPool = this.topic.get(topic);
                connectionPool.put(id, emitter);
                System.out.println("Connection count->" + connectionPool.size());
            } else {
                //first connection
                connectionPool = new ConcurrentHashMap<>();
                connectionPool.put(id, emitter);
                this.topic.put(topic, connectionPool);
            }

            Map<String, SseEmitter> finalConnectionPool = connectionPool;
            emitter.onCompletion(() -> {
                System.out.println("On Completion is called");
                finalConnectionPool.remove(id);
                System.out.println("Connection count->" + finalConnectionPool.size());
            });
            emitter.onTimeout(() -> {
                System.out.println("On Timeout is called");
                finalConnectionPool.remove(id);
                System.out.println("Connection count->" + finalConnectionPool.size());

            });
            Consumer<Throwable> consumer = t -> {
                System.out.println("On Error is called");
                finalConnectionPool.remove(id);
                System.out.println("Connection count->" + finalConnectionPool.size());
            };
            //OnError
            emitter.onError(consumer);

            return emitter;
        } else {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }


    }

    @RequestMapping(value = "/streams/push/{topic}/{uid}", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void postMessage(@RequestBody Notification notification, @PathVariable("topic") String topicName, @PathVariable("uid") String uid) {
        Map<String, SseEmitter> topic = this.topic.get(topicName);
        SseEmitter connection = topic.entrySet().stream()
                .filter(v -> v.getKey().equals(uid))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()))
                .values().stream().findFirst().orElse(null);
        try {
            connection.send(notification.getMessage());
        } catch (IOException e) {

            //remove the uid
          /*  topic.entrySet().stream()
                    .filter(v -> v.getKey().equals(uid))
                    .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()))
                    .entrySet().removeIf(map->map.getKey().equals(uid));*/
            //print the size
            int size = topic.entrySet().stream()
                    .filter(v -> v.getKey().equals(uid))
                    .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()))
                    .size();
            System.out.println("Connection count after Push message failed ->" + size);
        }

    }


}
