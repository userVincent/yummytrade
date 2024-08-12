package be.kuleuven.dsgt4.controller;

import be.kuleuven.dsgt4.domain.Order;
import be.kuleuven.dsgt4.domain.Stock;
import be.kuleuven.dsgt4.domain.Tick;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.stereotype.Service;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Collection;
import java.util.UUID;

@Service
public class NYSEService {

    private final WebClient webClient;
    private final String apiKey = "Iw8zeveVyaPNWonPNaU0213uw3g6Ei";

    @Autowired
    public NYSEService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8088").build();
    }

    public Flux<Stock> getAllStocks() {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/stockexchange/stocks")
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<EntityModel<Stock>>>() {})
                .flatMapMany(collectionModel -> Flux.fromIterable(collectionModel.getContent())
                        .map(EntityModel::getContent));
    }

    public Mono<Collection<Tick>> getStockTicks(String symbol) {
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/stockexchange/ticks/{symbol}")
                        .queryParam("key", apiKey)
                        .build(symbol))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<Tick>>() {})
                .map(CollectionModel::getContent);
    }

    public Mono<EntityModel<Order>> startTransaction(String symbol, Double price, Integer amount, String type) {
        String callbackURL = getCallbackUrl();
        return this.webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/stockexchange/transaction/start")
                        .queryParam("symbol", symbol)
                        .queryParam("price", price)
                        .queryParam("amount", amount)
                        .queryParam("type", type)
                        .queryParam("key", apiKey)
                        .queryParam("callbackURL", callbackURL)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<EntityModel<Order>>() {});
    }

    public Mono<EntityModel<Order>> commitTransaction(UUID orderId) {
        return this.webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/stockexchange/transaction/commit")
                        .queryParam("orderId", orderId)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<EntityModel<Order>>() {});
    }

    public Mono<EntityModel<Order>> rollbackTransaction(UUID orderId) {
        return this.webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/stockexchange/transaction/rollback")
                        .queryParam("orderId", orderId)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<EntityModel<Order>>() {});
    }

    private String getCallbackUrl() {
        URI uri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/ordercallback")
                .build()
                .toUri();
        return uri.toString();
    }
}
