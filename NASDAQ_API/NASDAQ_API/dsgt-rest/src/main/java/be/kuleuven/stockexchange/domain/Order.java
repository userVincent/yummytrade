package be.kuleuven.stockexchange.domain;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Order {
    public enum State {
        INITIATED,
        PENDING,
        FILLED,
        KILLED
    }

    public enum Type {
        BUY,
        SELL
    }

    private final UUID id;
    private final String symbol;
    private final Stock stock;
    private final Integer amount;
    private Double price;
    private State state;
    private final Type type;
    private String dateTime;
    private String callbackURL;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public Order(String symbol, Stock stock, int amount, Double price, Type type, String callbackURL) {
        this.amount = amount;
        this.id = UUID.randomUUID();
        this.symbol = symbol;
        this.stock = stock;
        this.price = price;
        this.state = State.INITIATED;
        this.type = type;
        updateTime();
        this.callbackURL = callbackURL;
        scheduleAutoKill();
        System.out.println("Order initiated: " + this);
    }

    private void scheduleAutoKill() {
        scheduler.schedule(() -> {
            if (this.state == State.INITIATED) {
                this.state = State.KILLED;
                updateTime();
                System.out.println("Order auto-killed: " + this);
            }
        }, 30, TimeUnit.SECONDS);
    }

    private void updateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.dateTime = LocalDateTime.now().format(formatter);
    }

    public Integer getAmount() {
        return amount;
    }

    public UUID getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public Double getPrice() {
        return price;
    }

    public State getState() {
        return state;
    }

    public Type getType() {
        return type;
    }

    public Stock getStock() {
        return stock;
    }

    public String getDateTime(){
        return dateTime;
    }

    public boolean commit() {
        if (this.state == State.INITIATED) {
            if (this.price == null){
                this.price = stock.getCurrentPrice();
            }
            updateTime();
            if (this.type == Type.BUY && this.price >= this.stock.getCurrentPrice() ||
                    this.type == Type.SELL && this.price <= this.stock.getCurrentPrice()) {
                this.state = State.FILLED;
            } else {
                this.state = State.PENDING;
                stock.addListener(this);
            }
            System.out.println("Order committed: " + this);
            return true;
        } else {
            return false;
        }
    }

    public boolean rollback() {
        if (this.state != State.INITIATED && this.state != State.PENDING) {
            return false;
        }
        if (this.state == State.PENDING) {
            stock.removeListener(this);
        } else if (this.state == State.INITIATED) {
            scheduler.shutdownNow();
        }
        this.state = State.KILLED;
        updateTime();
        System.out.println("Order killed: " + this);
        return true;
    }

    public void fillOrder() {
        if ((type == Type.BUY && stock.getCurrentPrice() <= price) ||
                (type == Type.SELL && stock.getCurrentPrice() >= price)) {
            state = State.FILLED;
            updateTime();
            stock.removeListener(this);
            notifyBroker();
        }
    }

    private void notifyBroker() {
        if (callbackURL != null && !callbackURL.isEmpty()) {
            try {
                String urlString = String.format("%s?id=%s&price=%s&status=%s&datetime=%s",
                        callbackURL, id.toString(), price.toString(), state.toString(), dateTime.replace(" ", "_"));
                System.out.println(urlString);
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int code = connection.getResponseCode();
                System.out.println("Callback sent successfully. Response code: " + code);
            } catch (Exception e) {
                System.err.println("Error sending callback: " + e.getMessage());
            }
        }
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", symbol='" + symbol + '\'' +
                ", amount=" + amount +
                ", price=" + price +
                ", state=" + state +
                ", type=" + type +
                ", date time=" + dateTime +
                '}';
    }
}
