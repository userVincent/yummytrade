package be.kuleuven.stockexchange.controllers;

import be.kuleuven.stockexchange.domain.Order;
import be.kuleuven.stockexchange.domain.Stock;
import be.kuleuven.stockexchange.domain.StockRepository;
import be.kuleuven.stockexchange.domain.Tick;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
public class StockExchangeController {

    private final StockRepository stockRepository;
    private final String apiKey;

    @Autowired
    public StockExchangeController(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
        this.apiKey = "Iw8zeveVyaPNWonPNaU0213uw3g6Ei";
    }

    @PostMapping("/stockexchange/transaction/start")
    public ResponseEntity<EntityModel<Order>> startTransaction(@RequestParam String symbol,
                                                  @RequestParam(required = false) Double price,
                                                  @RequestParam Integer amount,
                                                  @RequestParam String type,
                                                  @RequestParam(required = false) String callbackURL,
                                                  @RequestParam String key) {
        if (!isValidApiKey(key)) {
            throw new UnauthorizedException();
        }
        Stock stock = stockRepository.getStock(symbol);
        if (stock == null){
            throw new NotFoundException();
        }
        Order order = new Order(symbol, stock, amount, price, Order.Type.valueOf(type.toUpperCase()), callbackURL);
        stockRepository.addOrder(order);
        EntityModel<Order> orderModel = EntityModel.of(order,
                linkTo(methodOn(StockExchangeController.class).commitTransaction(order.getId(), key)).withRel("commit"),
                linkTo(methodOn(StockExchangeController.class).rollbackTransaction(order.getId(), key)).withRel("rollback"),
                linkTo(methodOn(StockExchangeController.class).getAllStocks(key)).withRel("stocks"),
                linkTo(methodOn(StockExchangeController.class).getTicks(symbol, key)).withRel("dailyClosingPrice")
        );
        return ResponseEntity.ok(orderModel);
    }

    @PostMapping("/stockexchange/transaction/commit")
    public ResponseEntity<EntityModel<Order>> commitTransaction(@RequestParam UUID orderId, @RequestParam String key) {
        if (!isValidApiKey(key)) {
            throw new UnauthorizedException();
        }
        Order order = stockRepository.getOrder(orderId);
        if (order == null) {
            throw new NotFoundException();
        }
        boolean success = order.commit();
        if (!success) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
        EntityModel<Order> orderModel = EntityModel.of(order,
                linkTo(methodOn(StockExchangeController.class).commitTransaction(order.getId(), key)).withSelfRel(),
                linkTo(methodOn(StockExchangeController.class).rollbackTransaction(order.getId(), key)).withRel("rollback"),
                linkTo(methodOn(StockExchangeController.class).getAllStocks(key)).withRel("stocks")
        );
        return ResponseEntity.ok(orderModel);
    }

    @PostMapping("/stockexchange/transaction/rollback")
    public ResponseEntity<EntityModel<Order>> rollbackTransaction(@RequestParam UUID orderId, @RequestParam String key) {
        if (!isValidApiKey(key)) {
            throw new UnauthorizedException();
        }
        Order order = stockRepository.getOrder(orderId);
        if (order == null) {
            throw new NotFoundException();
        }
        boolean success = order.rollback();
        if (!success) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
        EntityModel<Order> orderModel = EntityModel.of(order,
                linkTo(methodOn(StockExchangeController.class).commitTransaction(order.getId(), key)).withRel("commit"),
                linkTo(methodOn(StockExchangeController.class).rollbackTransaction(order.getId(), key)).withSelfRel(),
                linkTo(methodOn(StockExchangeController.class).getAllStocks(key)).withRel("stocks")
        );
        return ResponseEntity.ok(orderModel);
    }

    @GetMapping("/stockexchange/ticks/{symbol}")
    public CollectionModel<Tick> getTicks(@PathVariable String symbol, @RequestParam String key) {
        if (!isValidApiKey(key)) {
            throw new UnauthorizedException();
        }
        Stock stock = stockRepository.getStock(symbol);
        if (stock == null){
            throw new NotFoundException();
        }
        Collection<Tick> prices = stockRepository.getTicks(symbol).orElseThrow();
        return CollectionModel.of(prices,
                linkTo(methodOn(StockExchangeController.class).getTicks(symbol, key)).withSelfRel(),
                linkTo(methodOn(StockExchangeController.class).getAllStocks(key)).withRel("stocks"));
    }

    @GetMapping("/stockexchange/stocks")
    public CollectionModel<EntityModel<Stock>> getAllStocks(@RequestParam String key) {
        if (!isValidApiKey(key)) {
            throw new UnauthorizedException();
        }
        Collection<EntityModel<Stock>> stocks = stockRepository.getAllStocks().stream()
                .map(stock -> EntityModel.of(stock,
                        linkTo(methodOn(StockExchangeController.class).getTicks(stock.getSymbol(), key)).withRel("ticks")
                ))
                .collect(Collectors.toList());
        return CollectionModel.of(stocks,
                linkTo(methodOn(StockExchangeController.class).getAllStocks(key)).withSelfRel());
    }


    private boolean isValidApiKey(String key) {
        return apiKey.equals(key);
    }
}

@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "Unauthorized")
class UnauthorizedException extends RuntimeException {}

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Symbol not available")
class NotFoundException extends RuntimeException {}

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Invalid state transition")
class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
