package be.kuleuven.dsgt4.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import be.kuleuven.dsgt4.controller.NASDAQService;
import be.kuleuven.dsgt4.controller.NYSEService;

public class Fund {
    private final NYSEService nyseService;
    private final NASDAQService nasdaqService;

    private String symbol;
    private String name;
    private Double currentPrice;
    private List<Tick> ticks = new ArrayList<>();
    private Double percentagePriceChange;
    private List<String> componentStocks;
    private List<Double> weights;
    private List<String> exchanges;

    public Fund(String symbol, String name, List<String> componentStocks, List<Double> weights, List<String> exchanges,
                NYSEService nyseService, NASDAQService nasdaqService) {
        this.symbol = symbol;
        this.name = name;
        this.componentStocks = componentStocks;
        this.weights = weights;
        this.exchanges = exchanges;
        this.nyseService = nyseService;
        this.nasdaqService = nasdaqService;

        // check if the number of component stocks, weights and exchanges are the same
        if (componentStocks.size() != weights.size() || componentStocks.size() != exchanges.size()) {
            throw new RuntimeException("The number of component stocks, weights and exchanges should be the same");
        }
        calculateDailyClosingPrice();
        calculatePercentagePriceChange();
    }

    private void calculateDailyClosingPrice() {
        List<Collection<Tick>> allTicks = new ArrayList<>();
        for (int i = 0; i < componentStocks.size(); i++) {
            // get daily closing prices for each stock
            if (exchanges.get(i).equals("NYSE")) {
                allTicks.add(i, nyseService.getStockTicks(componentStocks.get(i)).block());
            } else if (exchanges.get(i).equals("NASDAQ")) {
                allTicks.add(i, nasdaqService.getStockTicks(componentStocks.get(i)).block());
            }
        }

        // check if all daily closing prices are available
        for (int i = 0; i < componentStocks.size(); i++) {
            if (allTicks.get(i) == null) {
                throw new RuntimeException("Daily closing prices for stock " + componentStocks.get(i) + " are not available");
            }
        }

        for (int i = 0; i < allTicks.get(0).size(); i++) {
            Double sumPrice = 0.0;
            String timestamp = null;
            Double sumVolume = 0.0;

            for (int j = 0; j < componentStocks.size(); j++) {
                Collection<Tick> ticks = allTicks.get(j);
                ArrayList<Tick> ticksList = new ArrayList<>(ticks);  
                Tick currentTick = ticksList.get(i);
                sumPrice += currentTick.getPrice() * weights.get(j);
                sumVolume += currentTick.getVolume() * weights.get(j);
                timestamp = currentTick.getTimestamp();
            }
            Tick newTick = new Tick(timestamp, sumPrice, sumVolume, this.symbol);
            ticks.add(newTick);
        }

        // calculate current price
        currentPrice = ticks.get(ticks.size() - 1).getPrice();
    }

    private void calculatePercentagePriceChange() {
        percentagePriceChange = (currentPrice - ticks.get(0).getPrice()) / ticks.get(0).getPrice() * 100;
    }

    public Map<String, Object> toDoc() {
        return Map.of(
                "symbol", this.symbol,
                "name", this.name,
                "componentStocks", this.componentStocks,
                "weights", this.weights,
                "exchanges", this.exchanges
        );
    }

    public static Fund fromDoc(Map<String, Object> docData, NYSEService nyseService, NASDAQService nasdaqService) {
        return new Fund(
                (String) docData.get("symbol"),
                (String) docData.get("name"),
                (List<String>) docData.get("componentStocks"),
                (List<Double>) docData.get("weights"),
                (List<String>) docData.get("exchanges"),
                nyseService,
                nasdaqService
        );
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public List<Tick> getTicks() {
        return ticks;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public Double getPercentagePriceChange() {
        return percentagePriceChange;
    }

    public List<String> getComponentStocks() {
        return componentStocks;
    }

    public List<Double> getWeights() {
        return weights;
    }

    public List<String> getExchanges() {
        return exchanges;
    }

    @Override
    public String toString() {
        return "Fund{" +
                "symbol='" + symbol + '\'' +
                ", name='" + name + '\'' +
                ", componentStocks=" + componentStocks +
                ", weights=" + weights +
                ", exchanges=" + exchanges +
                '}';
    }
}
