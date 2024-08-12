package be.kuleuven.dsgt4.controller;

import be.kuleuven.dsgt4.domain.Tick;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class TickListener {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "stock-ticks", groupId = "tick-consumers")
    public void listen(String message) {
        try {
            Tick tick = objectMapper.readValue(message, Tick.class);
            // Send the tick data to the WebSocket topic for the specific symbol
            messagingTemplate.convertAndSend("/topic/ticks/" + tick.getSymbol(), tick);
        } catch (Exception e) {
            System.err.println("Failed to parse tick: " + e.getMessage());
        }
    }
}

