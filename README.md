
# Yummytrade Broker Application

## Project Overview

Yummytrade Broker is a financial broker application developed using Java Spring Boot middleware, leveraging Firestore for storage, and Kafka for distributed messaging. The broker connects to two simulated exchanges—NYSE and NASDAQ—to provide real-time stock data and trading functionalities.

## Features

- **Stock Exchange Integration**: 
  - Generates real-time stock ticks every second.
  - Exposes HATEOAS REST API endpoints for broker operations.
  - Publishes stock ticks to a Kafka topic.
  
- **Live Ticker**:
  - Real-time updates for each stock's current price.
  - Tick data includes symbol, price, trade volume, and timestamp.
  - Ticks follow a random walk pattern for price simulation.
  - The broker subscribes to the Kafka topic to receive tick updates.
  - WebSocket connection provided to the frontend for real-time tick updates.

- **Order Transactions**:
  - Supports a two-phase commit protocol for buy/sell orders between the exchange and the broker.
  - Orders go through four states: INITIATED, PENDING, FILLED, KILLED.
  - Implements callback URLs for limit orders to notify the broker of state transitions.
 
<img src="https://github.com/user-attachments/assets/5fe26870-45e4-4155-befd-cb592a48a7e4" width=40% height=40%>

- **ETFs (Exchange-Traded Funds)**:
  - Offers composite products combining stocks from one or multiple exchanges.
  - Commits stock orders if all associated orders are successfully initiated, otherwise performs a rollback.
  - ETF blueprints are stored in Firestore by the broker.

- **Broker Middleware**:
  - Provides REST endpoints for fetching stock/ETF data and executing buy/sell operations.
  - Integrates with service providers: exchanges and Kafka.
  - Implements a security filter for user-specific endpoints using Firebase tokens.
  - Offers enhanced access controls for managers.
  - User, ETF, and order data are stored in Firestore.

## Technology Stack

- **Java Spring Boot**: Backend framework for creating the REST API and middleware.
- **Firestore**: Database for storing user, ETF, and order data.
- **Kafka**: Distributed messaging system used for pub/sub of stock ticks.
- **Firebase**: Authentication and security management for user-specific operations.
- **WebSocket**: Real-time communication between the broker and frontend for live stock updates.

## Demo Video

For a quick demonstration of the Yummytrade Broker application in action, please check out the [Demo Video](https://github.com/user-attachments/assets/d8a38a16-4ba8-4d18-90d5-e2fb603e9592).



