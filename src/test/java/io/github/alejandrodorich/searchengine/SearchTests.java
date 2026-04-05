package io.github.alejandrodorich.searchengine;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.github.alejandrodorich.searchengine.Crawler.PageRankNotCalculated;
import io.github.alejandrodorich.searchengine.ReverseIndex.WebsiteContentListNotSet;
import io.github.alejandrodorich.searchengine.VectorizedForwardIndex.ReverseIndexNotCreated;;
/**
 * Unit tests for the search.
 */
class SearchTests {

    static List<JsonObject> testJSONs = new ArrayList<>();

    static private List<String> getListOfResults(List<? extends UrlData> indexSearchResults) {
        List<String> resultingUrls = new ArrayList<>();

        for (UrlData urlElement : indexSearchResults ) resultingUrls.add(urlElement.getUrl());
        return resultingUrls;
    }
    
    @BeforeAll
    static void setUp() throws IOException {
        // Load the metadata from the JSON file
        testJSONs.add(Utils.parseJSONFile("intranet/lima-labs-customers.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/lima-labs-internal.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/lima-labs-investors.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/lima-labs-suppliers.json"));
    }

    @Test
    void checkResults() throws IOException, PageRankNotCalculated, WebsiteContentListNotSet, ReverseIndexNotCreated {

        for (JsonObject testJSON : testJSONs) {

    
            String jsonFile = new Gson().fromJson(testJSON.get("Net-Name"), String.class);
            String[] seedUrls = new Gson().fromJson(testJSON.get("Seed-URLs"), String[].class);
            String[] queryArray = new Gson().fromJson(testJSON.get("Query-Token"), String[].class);
            List<String> expectedResults =  Arrays.asList(new Gson().fromJson(testJSON.get("Query-URLs"), String[].class));

            // Transform the query into a single String
            String query = String.join(" ", queryArray);
            
            // Crawl all seedURLs and any external URLs found on these sites.
            Crawler crawler = new Crawler();
            crawler.crawl(seedUrls, jsonFile);
            crawler.calculatePageRank();
            List<Website> crawledWebsites = new ArrayList<>();
            crawledWebsites = crawler.getCrawledSitesData();

            ReverseIndex rIndex = new ReverseIndex();
            rIndex.setWebsiteContentList(crawledWebsites);
            rIndex.createIndex();

            // Check that the results in the reverse index match the expected results
            List<String> resultsRi = getListOfResults(rIndex.searchQuery(query));
            assertEquals(resultsRi.size(), expectedResults.size());
            assertTrue(resultsRi.containsAll(expectedResults));
            
            VectorizedForwardIndex vectorizedForwardIndex = new VectorizedForwardIndex();
            vectorizedForwardIndex.createIndex(rIndex);

            // Check that the results in the vectorized forward index match the expected results
            List<String> resultsVfi = getListOfResults(vectorizedForwardIndex.searchQuery(query));
            assertEquals(resultsVfi.size(), expectedResults.size());
            assertTrue(resultsVfi.containsAll(expectedResults));
        }
    }        
}
