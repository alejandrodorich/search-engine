package io.github.alejandrodorich.searchengine;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


/**
 * Unit tests for the reverse index.
 */
class ReverseIndexTests {

    static List<JsonObject> testPages;
    static JsonObject expectedReverseIndex;
    static Map<String, List<UrlTokenScore>> actualReverseIndexMap;


    @BeforeAll
    static void setUp() throws IOException {

        testPages = Utils.parseAllJSONFiles(java.util.Optional.of("src/test/resources/tf-idf/pages"));
        expectedReverseIndex = Utils.parseJSONFile("src/test/resources/tf-idf/index.json");
         
        // Get all relevant data in order to create a reverse index
        List<Website> intranetSiteList = new ArrayList<>();
        for (JsonObject jsonObject : testPages) {
            String location = jsonObject.get("url").getAsString();
            String title = jsonObject.get("title").getAsString();
            String headings = jsonObject.get("headings").getAsString();
            String content = jsonObject.get("paragraphs").getAsString();

            Website website = new Website(location, title, headings, content, null);
            intranetSiteList.add(website);
        }

        // Create a reverse index out of all the test pages
        ReverseIndex actualReverseIndex = new ReverseIndex();
        actualReverseIndex.setWebsiteContentList(intranetSiteList);
        try {
            actualReverseIndex.createIndex();
            actualReverseIndexMap = actualReverseIndex.getIndexMap();
        } catch (ReverseIndex.WebsiteContentListNotSet e) {
            System.out.println(e.getMessage());
        }
    }
    
    @Test
    void correctTFIDFScoresForAllTokens() {

        for (Entry<String, JsonElement> entry : expectedReverseIndex.entrySet()) {
            String token = entry.getKey();
            JsonObject expectedValuesForToken = entry.getValue().getAsJsonObject();
            for (Entry<String, JsonElement> expectedValue : expectedValuesForToken.entrySet()) {

                String expectedUrlForToken = expectedValue.getKey();
                Double expectedTfidfForToken = expectedValue.getValue().getAsDouble();

                // Verify the actual reverse index contains the expected token
                assertTrue(actualReverseIndexMap.containsKey(token));

                // Flatten the actual results into a simple URL, TF-IDF map for easier comparison
                List<UrlTokenScore> actualUrlTokenScores = actualReverseIndexMap.get(token);
                Map<String, Double> actualValuesForToken = new HashMap<>();
                for (UrlTokenScore document : actualUrlTokenScores) {
                    String actualUrl= document.getUrl();
                    double actualTfidf = document.getTFIDF();
                    actualValuesForToken.put(actualUrl, actualTfidf);
                }

                // Verify that the expected URL is set as a value for this token in the actual reverse index
                assertTrue(actualValuesForToken.containsKey(expectedUrlForToken) );
                
                // Verify TF-IDF values match within floating point tolerance (0.0001)
                Double actualTfidfForToken = actualValuesForToken.get(expectedUrlForToken).doubleValue();
                assertTrue(Math.abs(expectedTfidfForToken - actualTfidfForToken) < 0.0001);
            }
        }
    }
}
