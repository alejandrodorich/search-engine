package io.github.alejandrodorich.searchengine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import java.util.logging.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.github.alejandrodorich.searchengine.Crawler.PageRankNotCalculated;

/**
 * Unit tests for the page rank algorithm.
 */
class PageRankTests {

    private static final Logger logger = Logger.getLogger(PageRankTests.class.getName());
    static List<JsonObject> testJSONs = new ArrayList<>();
    static List<Map<String, Double>> pageRankForAllIntranets;


    // Crawl the given seed URLs and compute PageRank for each page
    // Return a map containing each URL and its corresponding PageRank value.
    private Map<String,Double> calculatePageRank(String[] seedUrls, String jsonFile) {
        Crawler crawler = new Crawler();
        try {
            crawler.crawl(seedUrls, jsonFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        crawler.calculatePageRank();
        Map<String,Double> pageRankMap = new HashMap<>();

        try {
            pageRankMap = crawler.getPageRankMap();
        } catch (PageRankNotCalculated e) {
            e.printStackTrace();
        }
        return pageRankMap;
    }

    @BeforeAll
    static void setUp() throws IOException, PageRankNotCalculated {
        // Load the metadata from the JSON file
        testJSONs.add(Utils.parseJSONFile("intranet/lima-labs-customers.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/lima-labs-internal.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/lima-labs-investors.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/lima-labs-suppliers.json"));

        // Crawl each Lima-Labs network separately and store its PageRank results
        pageRankForAllIntranets = new ArrayList<>();
        for (JsonObject testJSON : testJSONs) {
            String[] seedUrls = new Gson().fromJson(testJSON.get("Seed-URLs"), String[].class);
            String jsonFile = new Gson().fromJson(testJSON.get("Net-Name"), String.class);
            
            Crawler crawler = new Crawler();
            crawler.crawl(seedUrls, jsonFile);
            crawler.calculatePageRank();
            pageRankForAllIntranets.add(crawler.getPageRankMap());
        }
    }

    @Test
    void sumOfPageRank() {
        for (Map<String, Double> pageRank : pageRankForAllIntranets) {

            double pageRankSum = pageRank.values().stream().mapToDouble(Double::doubleValue).sum();
            logger.log(Level.INFO, "Sum of PageRank: {0}", pageRankSum);
            
            // PageRank values should form a probability distribution (sum ≈ 1)
            assertTrue(Math.abs(pageRankSum - 1.0) < 0.001);
        }
    }

    @Test
    void correctPageRankScores() throws IOException{
        // Create a map with URLs and the correct rounded PageRank values
        // These scores will be used to verify the correctness of the PageRank algorithm
        Map<String, Double> correctPageRankScores = Map.of(
            "http://localhost:8080/pages/investors/index.html", 0.26044,
            "http://localhost:8080/pages/investors/funding.html", 0.22641,
            "http://localhost:8080/pages/investors/board.html", 0.14745,
            "http://localhost:8080/pages/investors/financials.html", 0.2116,
            "http://localhost:8080/pages/investors/pitch.html", 0.1541);
        
        JsonObject testJSON = Utils.parseJSONFile("intranet/lima-labs-investors.json");
        String jsonFile = new Gson().fromJson(testJSON.get("Net-Name"), String.class);

        String[] seedUrls = new Gson().fromJson(testJSON.get("Seed-URLs"), String[].class);
        

        Map<String,Double> pageRank = calculatePageRank(seedUrls, jsonFile);

        // Validate PageRank implementation against independently computed reference values (Spreadsheet, 11 iterations)
        for (Map.Entry<String, Double> entry : correctPageRankScores.entrySet()) {
            String url = entry.getKey();
            double correctPageRank = entry.getValue();

            double actualPageRank = pageRank.get(url);

            assertTrue(Math.abs(actualPageRank - correctPageRank) < 0.00001);
        }
    }
}
