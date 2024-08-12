package be.kuleuven.stockexchange.domain;

public class Tick {
    private final String timestamp;
    private final double price;
    private final double volume;
    private String symbol;

    public Tick(String timestamp, double price, double volume, String symbol) {
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
}
