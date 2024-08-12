package be.kuleuven.dsgt4.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.UUID;

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
    private final Integer amount;
    private Double price;
    private State state;
    private final Type type;
    private String email;
    private String dateTime;
    private String exchange;

    @JsonCreator
    public Order(
            @JsonProperty("id") UUID id,
            @JsonProperty("symbol") String symbol,
            @JsonProperty("amount") Integer amount,
            @JsonProperty("price") Double price,
            @JsonProperty("state") State state,
            @JsonProperty("type") Type type,
            @JsonProperty("dateTime") String dateTime) {
        this.amount = amount;
        this.id = id;
        this.symbol = symbol;
        this.price = price;
        this.state = state;
        this.type = type;
        this.email = null;
        this.dateTime = dateTime;
    }

    public Order(UUID id, String symbol, Integer amount, Double price, State state, Type type, String email, String dateTime, String exchange) {
        this.id = id;
        this.symbol = symbol;
        this.amount = amount;
        this.price = price;
        this.state = state;
        this.type = type;
        this.email = email;
        this.dateTime = dateTime;
        this.exchange = exchange;
    }

    public Map<String, Object> toDoc(){
        return Map.of(
                "id", this.id.toString(),
                "symbol", this.symbol,
                "amount", this.amount.toString(),
                "price", this.price,
                "state", this.state.toString(),
                "type", this.type.toString(),
                "email", this.email,
                "dateTime", this.dateTime,
                "exchange", this.exchange
        );
    }

    public static Order fromDoc(Map<String, Object> docData) {
        return new Order(
                UUID.fromString((String) docData.get("id")),
                (String) docData.get("symbol"),
                Integer.parseInt((String) docData.get("amount")),
                (Double) docData.get("price"),
                State.valueOf((String) docData.get("state")),
                Type.valueOf((String) docData.get("type")),
                (String) docData.get("email"),
                (String) docData.get("dateTime"),
                (String) docData.get("exchange")
        );
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

    public String getEmail() {
        return email;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getExchange() {
        return exchange;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setPrice(Double price){
        this.price = price;
    }

    public void setDateTime(String dateTime){
        this.dateTime = dateTime;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public void setExchange(String exchange){
        this.exchange = exchange;
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
                ", email=" + email +
                ", dateTime=" + dateTime +
                ", exchange='" + exchange +
                '}';
    }
}
