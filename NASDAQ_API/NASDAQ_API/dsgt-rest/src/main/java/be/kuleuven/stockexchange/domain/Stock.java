package be.kuleuven.stockexchange.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Stock {
    private final String symbol;
    private final String name;
    private double currentPrice;
    private double percentagePriceChange;
    private final List<Tick> ticks;
    private final Random rand = new Random();
    private final List<Order> listeners = new ArrayList<>();
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Stock(String symbol, String name, Double initPrice, KafkaTemplate<String, Object> kafkaTemplate) {
        this.symbol = symbol;
        this.name = name;
        this.kafkaTemplate = kafkaTemplate;
        this.ticks = new ArrayList<>();
        this.currentPrice = initPrice;
        generateInitialTicks();
        schedulePriceUpdate();
    }

    private void generateInitialTicks() {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 1000; i++) {
            now = now.minusSeconds(1);
            double price = currentPrice + (rand.nextDouble() - 0.5) * currentPrice * 0.01;
            if (price <= 0) {
                price = 0;
            }
            double volume = rand.nextDouble() * 1000;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            Tick tick = new Tick(now.format(formatter), price, volume, this.symbol);
            ticks.add(0, tick); // Add to the beginning to keep the order correct
            currentPrice = price;
        }
    }

    public void updatePrice() {
        LocalDateTime now = LocalDateTime.now();
        double price = currentPrice + (rand.nextDouble() - 0.5) * currentPrice * 0.01;
        double volume = rand.nextDouble() * 1000;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Tick tick = new Tick(now.format(formatter), price, volume, this.symbol);
        ticks.add(tick);
        if (ticks.size() > 1000) {
            ticks.remove(0);
        }
        currentPrice = price;
        calculatePercentageChange();
        System.out.println("Updated price for " + symbol + ": " + currentPrice + ", " + tick.getVolume() + ", " + tick.getTimestamp());

        // Publish to Kafka
        kafkaTemplate.send("stock-ticks", tick);

        List<Order> listenersCopy;
        synchronized (listeners) {
            listenersCopy = new ArrayList<>(listeners);
        }
        for (Order listener : listenersCopy) {
            listener.fillOrder();
        }
    }

    private void schedulePriceUpdate() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updatePrice();
            } catch (Exception e) {
                System.err.println("Scheduled task failed for " + symbol + ": " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void calculatePercentageChange() {
        if (ticks.size() < 2) {
            this.percentagePriceChange = 0.0;
            return;
        }
        double today = ticks.get(ticks.size() - 1).getPrice();
        double start = ticks.get(0).getPrice();
        this.percentagePriceChange = ((today - start) / start) * 100;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public List<Tick> getTicks() {
        return ticks;
    }

    public double getPercentagePriceChange() {
        return percentagePriceChange;
    }

    public void addListener(Order order) {
        listeners.add(order);
    }

    public void removeListener(Order order) {
        listeners.remove(order);
    }
}
