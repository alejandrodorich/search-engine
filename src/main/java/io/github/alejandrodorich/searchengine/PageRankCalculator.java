package io.github.alejandrodorich.searchengine;

import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Calculate the PageRank value for each website in a given list of Website objects.
 * Used by the crawler to sort results by relevance in VectorizedForwardIndex.
 * 
 * @see Crawler                 Calculates the PageRank after finishing the crawling process by implementing
 *                              the PageRankCalculator.
 * @see VectorizedForwardIndex  An index that uses the PageRank values in order to properly sort all search results.
 */
public class PageRankCalculator {
    
    private List<Website> websiteList;
    private double numOfWebsites;
    private static final double DAMPING_FACTOR = 0.85;
    private double rankSource;

    /**
     * Construct a PageRankCalculator with the given list of websites.
     * Initialize the PageRank for each website to 1/numOfWebsites.
     * 
     * @param websiteList   The list of websites gathered by the crawler.
     */
    public PageRankCalculator(List<Website> websiteList) {
        this.websiteList = websiteList;
        this.numOfWebsites = websiteList.size();

        // Set the initial PageRank
        this.rankSource = 1.0/numOfWebsites;
    }

    /**
     * Calculate the corresponding PageRank of each website in 'websiteList'
     */
    public void calculatePageRank() {

        // Initialize all websites with the starting PageRank
        for (Website site : websiteList) {
            site.setPageRank(rankSource);
        }

        // Add a list to each website containing all sites that reference said website
        for (Website site : websiteList) {
            addReferencingSites(site);
        }

        // Iteratively calculate the PageRank for all websites until the total difference between old and new PageRank values is below 0.0001
        double deltaPR;
        do {
            Map<String, Double> newPageRankValues = new LinkedHashMap<>();

            deltaPR = 0;
            // Calculate new PageRank values for each website
            for (Website site : websiteList) {
                double newPageRank = 0;
                for (Website referencingSite : site.getSitesWithLinkToWebsite()) {
                    newPageRank = newPageRank + (referencingSite.getPageRank()/referencingSite.getNumOfExternalLinks());
                }
                newPageRank = newPageRank * DAMPING_FACTOR + (1 - DAMPING_FACTOR) * rankSource;

                // Accumulate the total difference between old and new PageRanks
                deltaPR = deltaPR + Math.abs(site.getPageRank() - newPageRank);

                newPageRankValues.put(site.url, newPageRank);
            }

            // Store all new calculated PageRanks within the current iteration in 'websiteList'
            for (Website site : websiteList) {
                site.setPageRank(newPageRankValues.get(site.url));
            }
        } while (deltaPR >= 0.0001);
    }
   
    private void addReferencingSites(Website site) {
        // Add all websites that reference the given site to its backlinks list.
        for (Website referencingSite : websiteList) {
            if (referencingSite.getExternalLinks().contains(site.url)) {
                site.addSiteWithLinkToWebsite(referencingSite);
            }
        }
    }

    /**
     * Return an unmodifiable view on the list of websites with their calculated PageRank values.
     * 
     * @return List<Website>    An unmodifiable view on the list of websites provided to the 'PageRankCalculator'
     *                          during its initialization, now updated with their calculated PageRank values.                   
     */
    public List<Website> getWebsitesWithPageRank() {
        return Collections.unmodifiableList(websiteList);
    }
}

