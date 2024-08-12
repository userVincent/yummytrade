package be.kuleuven.dsgt4.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Tick {
    private final String timestamp;
    private final double price;
    private final double volume;
    private final String symbol;

    @JsonCreator
    public Tick(
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("price") double price, 
        @JsonProperty("volume") double volume,
        @JsonProperty("symbol") String symbol){
        this.timestamp = timestamp;
        this.price = price;
        this.volume = volume;
        this.symbol = symbol;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public double getPrice() {
        return price;
    }

    public double getVolume() {
        return volume;
    }

    public String getSymbol() { return symbol; }

    @Override
    public String toString() {
        return "Tick{" +
                "symbol='" + symbol + '\'' +
                ", price=" + price +
                ", volume=" + volume +
                ", date time=" + timestamp +
                '}';
    }
}
