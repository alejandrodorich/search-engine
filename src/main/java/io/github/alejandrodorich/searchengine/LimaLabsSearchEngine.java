package io.github.alejandrodorich.searchengine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.github.alejandrodorich.searchengine.Crawler.PageRankNotCalculated;

/**
 * Main class of the search-engine project for the Lima-Labs company.
 * Crawl the entire Lima-Labs intranet and use ReverseIndex and VectorizedForwardIndex
 * in order to index all the retrieved website data. Allow users to perform regular and
 * weighted searches in the intranet. 
 */
public final class LimaLabsSearchEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(LimaLabsSearchEngine.class);
    private static List<JsonObject> testJSONs = new ArrayList<>();
    private static Scanner input = new Scanner(System.in);

    // Load the metadata from the JSON file
    private static void setUp() {
        try {
        testJSONs.add(Utils.parseJSONFile("intranet/lima-labs-customers.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/lima-labs-internal.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/lima-labs-investors.json"));
        testJSONs.add(Utils.parseJSONFile("intranet/lima-labs-suppliers.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Crawl all websites in the intranet and return all retrieved data
    private static List<Website> crawlAllWebsitesInProvidedNetwork() {

        Crawler crawler = new Crawler();
        
        for (JsonObject testJSON : testJSONs) {
            
            String jsonFile = new Gson().fromJson(testJSON.get("Net-Name"), String.class);
            String[] seedUrls = new Gson().fromJson(testJSON.get("Seed-URLs"), String[].class);
            
            //Crawl all seed-URLs for the current JSON file
            try {
                crawler.crawl(seedUrls, jsonFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        crawler.calculatePageRank();

        List<Website> crawledWebsites = new ArrayList<>();
        try {
            crawledWebsites = crawler.getCrawledSitesData();
        } catch (PageRankNotCalculated e) {
            System.out.println(e.getMessage());
        }
        
        return crawledWebsites;
    }

    // Build a weighted map from user input, that allows to perfom weighted searches in a VectorizedForwardIndex.
    private static Map<String, Double> buildWeightedMapWithUserInput() {
        Map<String, Double> weightedMap = new HashMap<>();
        String token;
        Double weight;
        boolean finished = false;

        System.out.println("In order to perform a search, you need to input each query token and weight separately.");
        System.out.println("To finish the search please write 'finished' as an input token.");
        System.out.println();

        do {
            System.out.print("Please enter a search token: ");
            token = input.nextLine();

            String regex = "[,\\.\\s]";
            String[] searchRequestTokens = token.split(regex);
            if (searchRequestTokens.length != 1) {
                System.out.println("Only one token per index of weightedQuery allowed.");
                continue;
            }
            if (!token.toLowerCase().equals("finished")) {
                System.out.print("Please assign a weight for the token (from 1 to 10): ");
                try {
                    weight = Double.parseDouble(input.nextLine());
                    if (weight < 1 || weight > 10 ) {
                        System.out.println("The weight has to be in the range of 1 to 10.");
                        continue;
                    }
                    weightedMap.put(token, weight);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. The weight has to be a number between 1 and 10.");
                    continue;
                }
            } else {
                System.out.println();
                finished = true;
            }
        } while (!finished);
        return weightedMap;
    }

    // Get the title of the given URL
    private static String getTitle(List<Website> websiteList, String url) {
        String title = "";
        for (Website site : websiteList) {
            if (site.url.equals(url)) return site.title;
        }
        return title;
    }

    // Allow users to decide if they want to perform normal or weighted queries
    private static String getQueryMethod() {
        boolean queryMethodDecided = false;
        String queryMethod;
        System.out.println();
        do {
            System.out.print("Please choose between a normal or weighted query (Input 'normal' or 'weighted'): ");
            queryMethod= input.nextLine().toLowerCase();
            if (queryMethod.equals("normal") || queryMethod.equals("weighted")) {
                queryMethodDecided = true;
            } else {
                System.out.println("Invalid input.");
            }
        } while (!queryMethodDecided);
        System.out.println();
        return queryMethod;
    }

    // Index all the given Data in a VectorizedForwardIndex
    private static VectorizedForwardIndex indexData(List<Website> sitesData) {
        ReverseIndex reverseIndex = new ReverseIndex();
        reverseIndex.setWebsiteContentList(sitesData);
        VectorizedForwardIndex vectorizedForwardIndex = new VectorizedForwardIndex();
        try {
            reverseIndex.createIndex();
            vectorizedForwardIndex.createIndex(reverseIndex);
        } catch (ReverseIndex.WebsiteContentListNotSet e) {
            e.printStackTrace();
        } catch (VectorizedForwardIndex.ReverseIndexNotCreated e) {
            e.printStackTrace();
        }
        return vectorizedForwardIndex;
    }

    private static void printResults(List<UrlCosineScore> results, List<Website> sitesData) {
        if (results.isEmpty()) {
            System.out.println("No relevant results found.");
        } else {
            for (UrlCosineScore document : results) {
                System.out.println("Url: " + document.url);
                System.out.println("Title: " + getTitle(sitesData, document.url));
                System.out.println();
            }
        }
    }
    
    /**
     * Main method: Entry point for the search engine.
     * 
     * Initialize the system, crawl the entire Lima-Labs network, index the data, handle user queries
     * and display the results in the console. Support both normal and weighted searches.
     */

    public static void main(String[] args) {

        // Print start message to logger
        LOGGER.info("Starting search-engine for Lima-Labs...");

        /*
         * Set the java.awt.headless property to true to prevent awt from opening windows.
         * If the property is not set to true, the program will throw an exception when trying to 
         * generate the graph visualizations in a headless environment.
         */
        System.setProperty("java.awt.headless", "true");
        LOGGER.info("Java awt GraphicsEnvironment headless: {}", java.awt.GraphicsEnvironment.isHeadless());

        // Load all seed-URLs into testJSONs
        setUp();

        List<Website> sitesData = crawlAllWebsitesInProvidedNetwork();
        VectorizedForwardIndex index = indexData(sitesData);
        
        String performSearches;
        do{
            String queryMethod = getQueryMethod();
            
            if (queryMethod.equals("normal")) {
              
                System.out.print("Search: ");
                String searchWords = input.nextLine();
                
                System.out.println();
                List<UrlCosineScore> searchResults = index.searchQuery(searchWords);
                printResults(searchResults, sitesData);
                
            } else {
                Map<String, Double> weightedQuery = buildWeightedMapWithUserInput();
                List<UrlCosineScore> searchWeightedResults = index.searchWeightedQuery(weightedQuery);
                printResults(searchWeightedResults, sitesData);
            }
            System.out.println();
            System.out.println("Do you want to perform more searches? (Enter 'y' for yes, any other key to exit");
            performSearches = input.nextLine().toLowerCase();

        } while (performSearches.equals("y"));
        input.close();
    }
}
