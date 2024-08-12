package be.kuleuven.dsgt4.controller;

import be.kuleuven.dsgt4.auth.WebSecurityConfig;
import be.kuleuven.dsgt4.domain.Fund;
import be.kuleuven.dsgt4.domain.Order;
import be.kuleuven.dsgt4.domain.Stock;
import be.kuleuven.dsgt4.domain.Tick;
import be.kuleuven.dsgt4.domain.User;
import be.kuleuven.dsgt4.domain.UserMessage;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

// Add the controller.
@RestController
class BrokerController {

    @Autowired
    Firestore db;

    @Autowired
    private NYSEService NYSEService;

    @Autowired
    private NASDAQService NASDAQService;

    @GetMapping("/api/hello")
    public void hello() {
        User user = WebSecurityConfig.getUser();
        this.db.collection("users").document(user.getEmail()).set(user.toDoc());
    }

    @GetMapping("/api/whoami")
    public User whoami() throws InterruptedException, ExecutionException {
        var user = WebSecurityConfig.getUser();
        // if (!user.isManager()) throw new AuthorizationServiceException("You are not a manager");

        UUID buuid = UUID.randomUUID();
        UserMessage b = new UserMessage(buuid, LocalDateTime.now(), user.getRole(), user.getEmail());
        this.db.collection("usermessages").document(b.getId().toString()).set(b.toDoc()).get();

        return user;
    }

    @GetMapping("/api/getAllCustomers")
    public Collection<User> getAllUsers() throws InterruptedException, ExecutionException {
        var usersCollection = db.collection("users").get().get();
        List<User> users = new ArrayList<>();
        for (QueryDocumentSnapshot document : usersCollection.getDocuments()) {
            users.add(User.fromDoc(document.getData()));
        }
        return users;
    }

    @GetMapping("/api/getAllOrders")
    public Collection<Order> getAllOrders() throws InterruptedException, ExecutionException {
        // Get all collections starting with "orders_"
        Collection<Collection<Order>> allOrders = new ArrayList<>();
        var collections = db.listCollections().iterator();

        while (collections.hasNext()) {
            var collection = collections.next();
            if (collection.getId().startsWith("orders_")) {
                var documents = collection.get().get().getDocuments();
                Collection<Order> orders = new ArrayList<>();
                for (QueryDocumentSnapshot document : documents) {
                    var data = document.getData();
                    Order order = Order.fromDoc(data);
                    orders.add(order);
                }
                allOrders.add(orders);
            }
        }

        // Flatten the list of lists into a single list of orders
        Collection<Order> result = new ArrayList<>();
        for (Collection<Order> orders : allOrders) {
            result.addAll(orders);
        }
        return result;
    }

    @GetMapping("/NYSEstocks")
    public Flux<Stock> getNYSEStocks() {
        return NYSEService.getAllStocks();
    }

    @GetMapping("/NASDAQstocks")
    public Flux<Stock> getNASDAQStocks() {
        return NASDAQService.getAllStocks();
    }

    @GetMapping("/indexFunds")
    public Collection<Fund> getIndexFunds() throws InterruptedException, ExecutionException {
        var documents = db.collection("funds").get().get().getDocuments();
        Collection<Fund> funds = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            try {
                var data = document.getData();
                Fund fund = Fund.fromDoc(data, NYSEService, NASDAQService);
                funds.add(fund);
            } catch (Exception e) {
                System.out.println("Failed to create fund: " + e.getMessage());
            }
        }
        return funds;
    }

    @GetMapping("/NYSEstocks/ticks/{symbol}")
    public Mono<Collection<Tick>> getNYSEStockTicks(@PathVariable String symbol) {
        return NYSEService.getStockTicks(symbol);
    }

    @GetMapping("/NASDAQstocks/ticks/{symbol}")
    public Mono<Collection<Tick>> getNASDAQStockTicks(@PathVariable String symbol) {
        return NASDAQService.getStockTicks(symbol);
    }

    @GetMapping("/indexFunds/ticks/{symbol}")
    public Collection<Tick> getIndexFundTicks(@PathVariable String symbol) throws InterruptedException, ExecutionException {
        Fund fund = getIndex(symbol);
        if (fund == null) {
            throw new RuntimeException("Fund not found: " + symbol);
        }
        return fund.getTicks();
    }

    @GetMapping("/indexFunds/{symbol}")
    public Fund getIndex(@PathVariable String symbol) throws InterruptedException, ExecutionException {
        var documents = db.collection("funds").get().get().getDocuments();
        for (QueryDocumentSnapshot document : documents) {
            try {
                var data = document.getData();
                Fund fund = Fund.fromDoc(data, NYSEService, NASDAQService);
                if (fund.getSymbol().equals(symbol)) {
                    return fund;
                }
            } catch (Exception e) {
                System.out.println("Failed to create fund: " + e.getMessage());
            }
        }
        return null;
    }

    @GetMapping("/api/NYSE/buy/{symbol}")
    public EntityModel<Order> executeNYSEBuyOrder(@PathVariable String symbol, @RequestParam(required = false) Double price, @RequestParam(required = false) Integer amount) {
        User user = WebSecurityConfig.getUser();
        return NYSEService.startTransaction(symbol, price, amount, "BUY")
                .flatMap(entityModel -> {
                    Order order = entityModel.getContent();
                    if (order != null) {
                        // Commit the transaction
                        return NYSEService.commitTransaction(order.getId())
                                .flatMap(committedOrder -> {
                                    // Save the committed order to the database
                                    Order commOrder = committedOrder.getContent();
                                    if(commOrder != null) {
                                        commOrder.setEmail(user.getEmail());
                                        commOrder.setExchange("NYSE");
                                        String path = "orders_" + commOrder.getEmail();
                                        this.db.collection(path).document(commOrder.getId().toString()).set(commOrder.toDoc());
                                    }
                                    return Mono.just(committedOrder);
                                });
                    }
                    return Mono.just(entityModel);
                })
                .block();
    }

    @GetMapping("/api/NASDAQ/buy/{symbol}")
    public EntityModel<Order> executeNASDAQBuyOrder(@PathVariable String symbol, @RequestParam(required = false) Double price, @RequestParam(required = false) Integer amount) {
        User user = WebSecurityConfig.getUser();
        return NASDAQService.startTransaction(symbol, price, amount, "BUY")
                .flatMap(entityModel -> {
                    Order order = entityModel.getContent();
                    if (order != null) {
                        // Commit the transaction
                        return NASDAQService.commitTransaction(order.getId())
                                .flatMap(committedOrder -> {
                                    Order commOrder = committedOrder.getContent();
                                    if(commOrder != null) {
                                        commOrder.setEmail(user.getEmail());
                                        commOrder.setExchange("NASDAQ");
                                        String path = "orders_" + commOrder.getEmail();
                                        this.db.collection(path).document(commOrder.getId().toString()).set(commOrder.toDoc());
                                    }
                                    return Mono.just(committedOrder);
                                });
                    }
                    return Mono.just(entityModel);
                })
                .block();
    }

    @GetMapping("/api/indexFund/buy/{symbol}")
    public Order executeIndexFundBuyOrder(@PathVariable String symbol, @RequestParam(required = false) Integer amount) throws InterruptedException, ExecutionException {
        Fund fund = getIndex(symbol);
        if (fund == null) {
            throw new RuntimeException("Fund not found: " + symbol);
        }

        User user = WebSecurityConfig.getUser();
        List<Order> nyseOrders = new ArrayList<>();
        List<Order> nasdaqOrders = new ArrayList<>();

        // Start transactions for all component stocks
        try {
            for (int i = 0; i < fund.getComponentStocks().size(); i++) {
                String componentStock = fund.getComponentStocks().get(i);
                String exchange = fund.getExchanges().get(i);
                Integer weight = fund.getWeights().get(i).intValue();
                Mono<EntityModel<Order>> startTransactionMono;

                if (exchange.equals("NYSE")) {
                    startTransactionMono = NYSEService.startTransaction(componentStock, null, amount * weight, "BUY");
                } else if (exchange.equals("NASDAQ")) {
                    startTransactionMono = NASDAQService.startTransaction(componentStock, null, amount * weight, "BUY");
                } else {
                    throw new RuntimeException("Unknown exchange: " + exchange);
                }

                EntityModel<Order> entityModel = startTransactionMono.block();
                Order order = entityModel.getContent();
                if (order == null) {
                    throw new RuntimeException("Failed to start transaction for stock: " + componentStock);
                }
                if (exchange.equals("NYSE")) {
                    nyseOrders.add(order);
                } else {
                    nasdaqOrders.add(order);
                }
            }

            // Commit all transactions
            for (Order order : nyseOrders) {
                NYSEService.commitTransaction(order.getId()).block();
            }
            for (Order order : nasdaqOrders) {
                NASDAQService.commitTransaction(order.getId()).block();
            }

        } catch (Exception e) {
            // Rollback all transactions if any fail
            for (Order order : nyseOrders) {
                if (order.getState() == Order.State.INITIATED) {
                    NYSEService.rollbackTransaction(order.getId()).block();
                }
            }
            for (Order order : nasdaqOrders) {
                if (order.getState() == Order.State.INITIATED) {
                    NASDAQService.rollbackTransaction(order.getId()).block();
                }
            }
            throw new RuntimeException("Failed to buy stocks for fund: " + symbol, e);
        }

        // Create a new order for the fund
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String dateTime = LocalDateTime.now().format(formatter);
        Order fundOrder = new Order(UUID.randomUUID(), symbol, amount, fund.getCurrentPrice(), Order.State.FILLED, Order.Type.BUY, WebSecurityConfig.getUser().getEmail(), dateTime, "");
        fundOrder.setState(Order.State.FILLED);
        fundOrder.setEmail(user.getEmail());

        String path = "orders_" + fundOrder.getEmail();
        this.db.collection(path).document(fundOrder.getId().toString()).set(fundOrder.toDoc());
        return fundOrder;
    }

    @GetMapping("/api/NYSE/sell/{symbol}")
    public EntityModel<Order> executeNYSESellOrder(@PathVariable String symbol, @RequestParam(required = false) Double price, @RequestParam(required = false) Integer amount) {
        User user = WebSecurityConfig.getUser();
        return NYSEService.startTransaction(symbol, price, amount, "SELL")
                .flatMap(entityModel -> {
                    Order order = entityModel.getContent();
                    if (order != null) {
                        // Commit the transaction
                        return NYSEService.commitTransaction(order.getId())
                                .flatMap(committedEntityModel -> {
                                    Order committedOrder = committedEntityModel.getContent();
                                    if (committedOrder != null) {
                                        // Save the committed order to the database
                                        committedOrder.setEmail(user.getEmail());
                                        committedOrder.setExchange("NYSE");
                                        String path = "orders_" + committedOrder.getEmail();
                                        this.db.collection(path).document(committedOrder.getId().toString()).set(committedOrder.toDoc());
                                    }
                                    return Mono.just(committedEntityModel);
                                });
                    }
                    return Mono.just(entityModel);
                })
                .block();
    }

    @GetMapping("/api/NASDAQ/sell/{symbol}")
    public EntityModel<Order> executeNASDAQSellOrder(@PathVariable String symbol, @RequestParam(required = false) Double price, @RequestParam(required = false) Integer amount) {
        User user = WebSecurityConfig.getUser();
        return NASDAQService.startTransaction(symbol, price, amount, "SELL")
                .flatMap(entityModel -> {
                    Order order = entityModel.getContent();
                    if (order != null) {
                        // Commit the transaction
                        return NASDAQService.commitTransaction(order.getId())
                                .flatMap(committedEntityModel -> {
                                    Order committedOrder = committedEntityModel.getContent();
                                    if (committedOrder != null) {
                                        // Save the committed order to the database
                                        committedOrder.setEmail(user.getEmail());
                                        committedOrder.setExchange("NASDAQ");
                                        String path = "orders_" + committedOrder.getEmail();
                                        this.db.collection(path).document(committedOrder.getId().toString()).set(committedOrder.toDoc());
                                    }
                                    return Mono.just(committedEntityModel);
                                });
                    }
                    return Mono.just(entityModel);
                })
                .block();
    }

    @GetMapping("/api/indexFund/sell/{symbol}")
    public Order executeIndexFundSellOrder(@PathVariable String symbol, @RequestParam(required = false) Integer amount) throws InterruptedException, ExecutionException {
        Fund fund = getIndex(symbol);
        if (fund == null) {
            throw new RuntimeException("Fund not found: " + symbol);
        }

        User user = WebSecurityConfig.getUser();
        List<Order> nyseOrders = new ArrayList<>();
        List<Order> nasdaqOrders = new ArrayList<>();

        // Start transactions for all component stocks
        try {
            for (int i = 0; i < fund.getComponentStocks().size(); i++) {
                String componentStock = fund.getComponentStocks().get(i);
                String exchange = fund.getExchanges().get(i);
                Integer weight = fund.getWeights().get(i).intValue();
                Mono<EntityModel<Order>> startTransactionMono;

                if (exchange.equals("NYSE")) {
                    startTransactionMono = NYSEService.startTransaction(componentStock, null, amount * weight, "SELL");
                } else if (exchange.equals("NASDAQ")) {
                    startTransactionMono = NASDAQService.startTransaction(componentStock, null, amount * weight, "SELL");
                } else {
                    throw new RuntimeException("Unknown exchange: " + exchange);
                }

                EntityModel<Order> entityModel = startTransactionMono.block();
                Order order = entityModel.getContent();
                if (order == null) {
                    throw new RuntimeException("Failed to start transaction for stock: " + componentStock);
                }
                if (exchange.equals("NYSE")) {
                    nyseOrders.add(order);
                } else {
                    nasdaqOrders.add(order);
                }
            }

            // Commit all transactions
            for (Order order : nyseOrders) {
                NYSEService.commitTransaction(order.getId()).block();
            }
            for (Order order : nasdaqOrders) {
                NASDAQService.commitTransaction(order.getId()).block();
            }

        } catch (Exception e) {
            // Rollback all transactions if any fail
            for (Order order : nyseOrders) {
                if (order.getState() == Order.State.INITIATED) {
                    NYSEService.rollbackTransaction(order.getId()).block();
                }
            }
            for (Order order : nasdaqOrders) {
                if (order.getState() == Order.State.INITIATED) {
                    NASDAQService.rollbackTransaction(order.getId()).block();
                }
            }
            throw new RuntimeException("Failed to sell stocks for fund: " + symbol, e);
        }

        // Create a new order for the fund
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String dateTime = LocalDateTime.now().format(formatter);
        Order fundOrder = new Order(UUID.randomUUID(), symbol, amount, fund.getCurrentPrice(), Order.State.FILLED, Order.Type.SELL, WebSecurityConfig.getUser().getEmail(), dateTime, "");
        fundOrder.setState(Order.State.FILLED);
        fundOrder.setEmail(user.getEmail());

        String path = "orders_" + fundOrder.getEmail();
        this.db.collection(path).document(fundOrder.getId().toString()).set(fundOrder.toDoc());
        return fundOrder;
    }

    @GetMapping("/api/order/cancel/{orderId}")
    public ResponseEntity<EntityModel<Order>> cancelOrder(@PathVariable UUID orderId) throws InterruptedException, ExecutionException {
        User user = WebSecurityConfig.getUser();
        String path = "orders_" + user.getEmail();
        var document = this.db.collection(path).document(orderId.toString()).get().get();

        if (!document.exists()) {
            return ResponseEntity.notFound().build();
        }

        Order order = Order.fromDoc(document.getData());

        if (order.getState() != Order.State.PENDING) {
            return ResponseEntity.status(HttpStatus.SC_CONFLICT).body(EntityModel.of(order));
        }

        // Call the rollback transaction
        EntityModel<Order> rolledBackOrder = null;
        try {
            if (order.getExchange().equals("NYSE")) {
                rolledBackOrder = NYSEService.rollbackTransaction(order.getId()).block();
            } else if (order.getExchange().equals("NASDAQ")) {
                rolledBackOrder = NASDAQService.rollbackTransaction(order.getId()).block();
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
        }

        // Update the order in Firestore
        Order cancelledOrder = rolledBackOrder.getContent();
        cancelledOrder.setEmail(user.getEmail());
        cancelledOrder.setExchange(order.getExchange());
        this.db.collection(path).document(cancelledOrder.getId().toString()).set(cancelledOrder.toDoc());

        return ResponseEntity.ok(rolledBackOrder);
    }

    @GetMapping("/api/orders")
    public Collection<Order> getOrders() throws InterruptedException, ExecutionException {
        User user = WebSecurityConfig.getUser();
        String path = "orders_" + user.getEmail();
        
        var documents = this.db.collection(path).get().get().getDocuments();
        Collection<Order> orders = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            var data = document.getData();
            Order order = Order.fromDoc(data);
            orders.add(order);
        }
        return orders;
    }

    @GetMapping("/ordercallback")
    public ResponseEntity<Void> orderCallback(@RequestParam String id, @RequestParam String price, @RequestParam String status, @RequestParam String datetime) throws ExecutionException, InterruptedException {
        String email = findOrderEmailById(id);
        if (email != null) {
            String path = "orders_" + email;
            var docRef = this.db.collection(path).document(id);
            var doc = docRef.get().get();
            if (doc.exists()) {
                Order order = Order.fromDoc(doc.getData());
                order.setPrice(Double.parseDouble(price));
                order.setState(Order.State.valueOf(status));
                order.setDateTime(datetime.replace("_", " "));
                this.db.collection(path).document(id).set(order.toDoc());
            }
        } else {
            System.out.println("Order not found: " + id);
        }
        return ResponseEntity.ok().build();
    }

    private String findOrderEmailById(String orderId) throws InterruptedException, ExecutionException {
        var collections = db.listCollections().iterator();
        while (collections.hasNext()) {
            var collection = collections.next();
            if (collection.getId().startsWith("orders_")) {
                var documents = collection.get().get().getDocuments();
                for (QueryDocumentSnapshot document : documents) {
                    if (document.getId().equals(orderId)) {
                        return collection.getId().substring(7); // Extract the email from the collection name
                    }
                }
            }
        }
        return null;
    }

}
