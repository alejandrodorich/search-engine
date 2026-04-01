package io.github.alejandrodorich.searchengine;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Crawl websites and all external URLs found within them, gathering relevant information such as
 * URLs, titles, headers and content..
 * 
 * Supports Page Rank calculation and tracking of crawled sites.
 * 
 * @see PageRankCalculator      Used to calculate the PageRank values for crawled websites.
 * @see Queue                   Manages URLs to be crawled.
 * @see ReverseIndex            Creates indices based on crawled data.
 */
public class Crawler {
    
    private List<Website> crawledSitesData = new ArrayList<>();
    
    private Map<String, Integer> numCrawledPagesPerFile = new HashMap<>();
    private Map<String, Integer> numUrlsPerFile = new HashMap<>();

    // Set a maximum amount of pages that should be crawled
    private boolean networkSizeRestricted;
    private int maxCrawledPages;

    private Queue queue = new Queue();    
    private boolean pageRankCalculated;

    /**
     * Exception indicating that PageRank has not been calculated or is outdated.
     */
    public class PageRankNotCalculated extends Exception {
        /**
         * Construct a new 'PageRankNotCalculated' exception with the given message.
         * @param message   The message explaining why the exception was triggered.
         */
        PageRankNotCalculated(String message) {
            super(message);
        }
    }

    /**
     * Crawl all provided seed-URLs and any external URLs found on these sites.
     * Store all relevant information from crawled sites in 'crawledSitesData'.
     * 
     * @param seedUrls      Array of initial seed URLs to start the crawling process.
     * @param jsonFile      Key used to track crawled pages and URLs.
     * @throws IOException  If the connection to a URL cannot be established.
     */
    public void crawl(String[] seedUrls, String jsonFile) throws IOException {

        // Mark PageRank as outdated
        pageRankCalculated = false;

        int counterCrawled = 0;
        int counterUrls = 0;

        for (String seed : seedUrls) {
            queue.add(seed);
        }

        //  Crawl all seed-URLs from the queue, and add any newly discovered URLs
        //  back to the queue for further crawling.
        while (queue.getSize() != 0) {

            counterCrawled++;

            Document site = Jsoup.connect(queue.poll()).get();
            
            String location = site.location();
            String title = site.title();
            String header = site.select("h1").get(0).text();
            Elements completeContent = site.select("p");
            String content = completeContent.first().ownText();
                
            // Extract all external URLs from the current site
            Elements links = site.select("a[href]");
            List<String> urlsList = new ArrayList<>();
            for (Element link : links ) {
                String url = link.attr("href");
                urlsList.add(url);
            }

            crawledSitesData.add(new Website(location, title, header, content, urlsList));

            // Add all gathered, non-duplicated external URLs to the queue
            for (String url : urlsList) {
                queue.add(url);
                counterUrls++;
            }

            //Stop the crawling if the maximum number of pages has been crawled
            if (networkSizeRestricted && counterCrawled == maxCrawledPages) break;
        }
        
        numCrawledPagesPerFile.put(jsonFile, counterCrawled);
        numUrlsPerFile.put(jsonFile, counterUrls);
    }

    /**
     * Set a restriction on the maximum number of pages that can be crawled.
     * 
     * @param maxCrawledPages An integer value that indicates the maximum number of pages that can be crawled.
     */
    public void setMaximalCrawledPages(int maxCrawledPages) {
        networkSizeRestricted = true;
        this.maxCrawledPages = maxCrawledPages;
    }

    /**
     * Calculate the PageRank for all crawled sites and update their values in 'crawledSitesData'.
     * Uses the PageRankCalculator to perform the calculation.
     */
    public void calculatePageRank() {   
        PageRankCalculator pageRankCalculator = new PageRankCalculator(crawledSitesData);
        pageRankCalculator.calculatePageRank();
        pageRankCalculated = true;
    }

    /**
     * Return an unmodifiable map containing all crawled URLs and their PageRank values.
     * 
     * @return                          An unmodifiable map with URLs as keys and their PageRank values.
     * @throws PageRankNotCalculated    If the PageRank values have not been calculated or are outdated.
     */
    public Map<String, Double> getPageRankMap() throws PageRankNotCalculated {
        if (!pageRankCalculated) throw new PageRankNotCalculated("PageRank has not been calculated or is outdated.");
        Map<String, Double> pageRankMap = new HashMap<>();
        for (Website site : crawledSitesData) {
            pageRankMap.put(site.url, site.getPageRank());
        }
        return Collections.unmodifiableMap(pageRankMap);
    }

    /**
     * Get the number of crawled pages for the given JSON file.
     * 
     * @param jsonFile  The name of the JSON file that represents an index in 'crawledSitesData'.
     * @return          The number of crawled sites corresponding to the given JSON file.
     * @throws IllegalArgumentException     If the JSON file does not correspond to an index in 'crawledSitesData'.
     */
    public int getNumberOfCrawledPages(String jsonFile) throws IllegalArgumentException {
        Integer numCrawledPages = numCrawledPagesPerFile.get(jsonFile);
        if (numCrawledPages == null) throw new IllegalArgumentException("JSON file has not been crawled through.");
        return (numCrawledPages);
    }

    /**
     * Return the number of all external URLs corresponding to the given JSON file.
     * 
     * @param jsonFile  The name of the JSON file that represents an index in 'crawledSitesData'.
     * @return          The total number of external URLs corresponding to the given JSON file.
     * @throws IllegalArgumentException    If the JSON file does not correspond to an index in 'crawledSitesData'.
     */
    public int getNumberOfLinks(String jsonFile) throws IllegalArgumentException {
        Integer numberOfUrls = numUrlsPerFile.get(jsonFile);
        if (numberOfUrls == null) throw new IllegalArgumentException("JSON file has not been crawled through.");
        return (numberOfUrls);
    }

    /**
     * Return an unmodifiable view of the list containing all crawled sites and their relevant information.
     * 
     * @return                          An unmodifiable list of Website objects with gathered data.
     * @throws PageRankNotCalculated    If the PageRank values have not been calculated or are outdated.
     */
    public List<Website> getCrawledSitesData() throws PageRankNotCalculated {
        if (!pageRankCalculated) throw new PageRankNotCalculated("PageRank has not been calculated or is outdated.");
        return Collections.unmodifiableList(crawledSitesData);
    } 
}

