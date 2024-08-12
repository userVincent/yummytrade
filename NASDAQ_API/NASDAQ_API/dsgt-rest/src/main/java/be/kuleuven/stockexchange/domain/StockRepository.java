package be.kuleuven.stockexchange.domain;

import java.util.*;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import be.kuleuven.stockexchange.domain.KafkaConfig;

@Component
public class StockRepository {
    private static final Map<String, Stock> stocks = new HashMap<>();
    private static final Map<UUID, Order> orders = new HashMap<>();

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public StockRepository(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostConstruct
    public void initData() {
        Stock a = new Stock("AMZN", "Amazon.com Inc.", 3500.00, kafkaTemplate);
        stocks.put(a.getSymbol(), a);

        Stock b = new Stock("FB", "Meta Platforms Inc.", 325.00, kafkaTemplate);
        stocks.put(b.getSymbol(), b);

        Stock c = new Stock("NFLX", "Netflix Inc.", 650.00, kafkaTemplate);
        stocks.put(c.getSymbol(), c);

        Stock d = new Stock("TSLA", "Tesla Inc.", 1100.00, kafkaTemplate);
        stocks.put(d.getSymbol(), d);

        Stock e = new Stock("NVDA", "NVIDIA Corporation", 300.00, kafkaTemplate);
        stocks.put(e.getSymbol(), e);

        Stock f = new Stock("PYPL", "PayPal Holdings Inc.", 190.00, kafkaTemplate);
        stocks.put(f.getSymbol(), f);

        Stock g = new Stock("ADBE", "Adobe Inc.", 620.00, kafkaTemplate);
        stocks.put(g.getSymbol(), g);

        Stock h = new Stock("INTC", "Intel Corporation", 55.00, kafkaTemplate);
        stocks.put(h.getSymbol(), h);

        Stock i = new Stock("CSCO", "Cisco Systems Inc.", 60.00, kafkaTemplate);
        stocks.put(i.getSymbol(), i);

        Stock j = new Stock("PEP", "PepsiCo Inc.", 170.00, kafkaTemplate);
        stocks.put(j.getSymbol(), j);
    }

    public Optional<Collection<Tick>> getTicks(String symbol) {
        Stock stock = stocks.get(symbol);
        return Optional.ofNullable(stock.getTicks());
    }

    public Collection<Stock> getAllStocks() {
        return stocks.values();
    }

    public Double getCurrentPrice(String symbol) {
        Stock stock = stocks.get(symbol);
        return stock.getCurrentPrice();
    }

    public boolean validSymbol(String symbol) {
        return stocks.get(symbol) != null;
    }

    public Stock getStock(String symbol) {
        return stocks.get(symbol);
    }

    public Order getOrder(UUID uuid){
        return orders.get(uuid);
    }

    public void addOrder(Order order){
        orders.put(order.getId(), order);
    }
}
