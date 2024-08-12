package be.kuleuven.dsgt4.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Stock {
    private final String symbol;
    private final String name;
    private final Double currentPrice;
    private final List<Tick> ticks;
    private final Double percentagePriceChange;

    @JsonCreator
    public Stock(
            @JsonProperty("symbol") String symbol,
            @JsonProperty("name") String name,
            @JsonProperty("currentPrice") Double currentPrice,
            @JsonProperty("percentagePriceChange") Double percentagePriceChange,
            @JsonProperty("dailyClosingPrice") List<Tick> ticks){
        this.symbol = symbol;
        this.name = name;
        this.currentPrice = currentPrice;
        this.percentagePriceChange = percentagePriceChange;
        this.ticks = ticks;
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

    public List<Tick> getDailyClosingPrice() {
        return ticks;
    }

    public Double getPercentagePriceChange() {
        return percentagePriceChange;
    }
}