package io.github.alejandrodorich.searchengine;
import java.util.ArrayList;
import java.util.List;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;

import com.google.gson.JsonObject;
/**
 * Unit tests for the crawler.
 */
class CrawlerTests {
    static List<JsonObject> testJSONs = new ArrayList<>();
    static Crawler crawler = new Crawler();

    @BeforeAll
    static void setUp() throws IOException {
        // Load the metadata from the JSON file
        testJSONs.add(Utils.parseJSONFile("intranet/lima-labs-customers.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/lima-labs-internal.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/lima-labs-investors.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/lima-labs-suppliers.json"));

        // Crawl the entire Lima-Labs intranet
        for (JsonObject testJSON : testJSONs) {
            String[] seedUrls = new Gson().fromJson(testJSON.get("Seed-URLs"), String[].class);
            String jsonFile = new Gson().fromJson(testJSON.get("Net-Name"), String.class);
            crawler.crawl(seedUrls, jsonFile);
        }
    }

    @Test
    void crawlAllWebsitesInProvidedNetwork() {
        
        for (JsonObject testJSON : testJSONs) {

            String jsonFile = new Gson().fromJson(testJSON.get("Net-Name"), String.class);
            int numPagesCrawled = crawler.getNumberOfCrawledPages(jsonFile);
            
            // Verify that the number of crawled pages is correct, i.e. the same as stated in the JSON file
            assertEquals(testJSON.get("Num-Websites").getAsInt(), numPagesCrawled);
        }
    }

    @Test
    void findCorrectNumberOfLinks() {

        for (JsonObject testJSON : testJSONs) {
        
            String jsonFile = new Gson().fromJson(testJSON.get("Net-Name"), String.class);
            int numLinks = crawler.getNumberOfLinks(jsonFile);

            // Verify that the number of links is correct, i.e. the same as stated in the JSON file
            assertEquals(testJSON.get("Num-Links").getAsInt(), numLinks);
        }
    }        
}
