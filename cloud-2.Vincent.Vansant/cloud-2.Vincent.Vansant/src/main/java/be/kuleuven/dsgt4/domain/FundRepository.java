package be.kuleuven.dsgt4.domain;

import java.util.*;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.cloud.firestore.Firestore;

import be.kuleuven.dsgt4.controller.NASDAQService;
import be.kuleuven.dsgt4.controller.NYSEService;

@Component
public class FundRepository {

    @Autowired
    Firestore db;

    @Autowired
    private NYSEService NYSEService;

    @Autowired
    private NASDAQService NASDAQService;


    @PostConstruct
    public void initData() {
        try {
            // Technology Index Fund
            List<String> techStocks = Arrays.asList("AAPL", "MSFT", "GOOGL");
            List<Double> techWeights = Arrays.asList(4.0, 3.0, 3.0);
            List<String> techExchanges = Arrays.asList("NYSE", "NYSE", "NYSE");
            Fund techIndexFund = new Fund("TECHIDX", "Technology Index Fund", techStocks, techWeights, techExchanges, NYSEService, NASDAQService);

            // Healthcare Index Fund
            // List<String> healthcareStocks = Arrays.asList("JNJ", "PFE", "MRK");
            // List<Integer> healthcareWeights = Arrays.asList(0.5, 0.25, 0.25);
            // List<String> healthcareExchanges = Arrays.asList("NYSE", "NYSE", "NYSE");
            // Fund healthcareIndexFund = new Fund("HEALTHIDX", "Healthcare Index Fund", healthcareStocks, healthcareWeights, healthcareExchanges);

            // Consumer Goods Index Fund
            List<String> consumerGoodsStocks = Arrays.asList("PG", "KO", "PEP");
            List<Double> consumerGoodsWeights = Arrays.asList(4.0, 3.0, 3.0);
            List<String> consumerGoodsExchanges = Arrays.asList("NYSE", "NYSE", "NASDAQ");
            Fund consumerGoodsIndexFund = new Fund("CONSUMERIDX", "Consumer Goods Index Fund", consumerGoodsStocks, consumerGoodsWeights, consumerGoodsExchanges, NYSEService, NASDAQService);

            // Display Fund details
            System.out.println("Fund details:");
            System.out.println(techIndexFund);
            // System.out.println(healthcareIndexFund);
            System.out.println(consumerGoodsIndexFund);

            // Save funds to Firestore
            db.collection("funds").document(techIndexFund.getSymbol()).set(techIndexFund.toDoc());
            // db.collection("funds").document(healthcareIndexFund.getSymbol()).set(healthcareIndexFund.toDoc());
            db.collection("funds").document(consumerGoodsIndexFund.getSymbol()).set(consumerGoodsIndexFund.toDoc());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
