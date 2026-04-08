package io.github.alejandrodorich.searchengine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Collections;

/**
 * Implement a reverse index to store, manage and search through data retrieved by the crawler.
 * 
 * Provide methods to lemmatize and tokenize the data obtained by the crawler and use this data
 * in order to create a basic forward index. This forward index serves as a blueprint for building
 * the reverse index. The reverse index is represented by a map, where each key is a token and the value
 * is a list of UrlTokenScore objects. These objects store the URLs that mention the token in their data,
 * as retrieved by the crawler, along with their associated TF-IDF (term frequency - inverted document
 * frequency) values.
 * 
 * @see VectorizedForwardIndex  Builds on the reverse index and basic forward index in order to create a more
 *                              detailed forward index, enabling query searches based on the PageRank
 *                              and cosine similarity values calculated for Website objects.
 */
public class ReverseIndex extends Index<UrlTokenScore> {

    // Represent a reverse index that maps each token to a list of UrlTokenScore objects
    // The UrlTokenScore objects mention the token in their title, paragraph or content
    private Map<String, List<UrlTokenScore>> reverseIndexMap = new LinkedHashMap<>();

    // Represent a basic forward index that maps each URL to a list of tokens
    // The tokens correspond to the title, header and content of the URL
    private Map<String, List<String>> basicForwardIndexMap = new HashMap<>();

    private List<Website> intranetSiteList;

    /**
     * Exception indicating that the list containing all website content retrieved by the crawler has
     * not been set in the reverse index.
     */
    public class WebsiteContentListNotSet extends Exception {
        /**
         * Construct a new WebsiteContentListNotSet exception with the given message.
         * @param message   The message explaining why the exception was triggered.
         */
        WebsiteContentListNotSet(String message) {
            super(message);
        }
    }

    /**
     * Create a reverse index based on the content retrieved by the crawler.
     * 
     * This method lemmatizes and tokenizes the entire content stored in 'intranetSiteList' in the superclass. First, it
     * creates a corresponding basic forward index that maps each URL to a list of tokens that appear in the title, header
     * or content of the site associated with the URL. Using the forward index as a base, it builds a reverse index which
     * essentially inverts the forward index by mapping tokens to their corresponding URLs derived from the forward index.
     * Additionally, it calculates the TF-IDF values for each URL and stores these values along with the URL as UrlTokenScore
     * objects in the reverse index.
     * 
     * @throws WebsiteContentListNotSet If 'intranetSiteList' has not been initialized in the superclass.
     */
    public void createIndex() throws WebsiteContentListNotSet {

        createBasicForwardIndex();
        basicForwardIndexMap = getBasicForwardIndexMap();

        // use the forward index as a blueprint to create the reverse index
        for (var entry : basicForwardIndexMap.entrySet()) {
            String url = entry.getKey();
            List <String> tokenList = entry.getValue();

            for (String token : tokenList) {

                reverseIndexMap.putIfAbsent(token, new ArrayList<>());

                double termFrequency = calculateTF(token, url);
                UrlTokenScore urlTokenScore = new UrlTokenScore(url, token, termFrequency);

                List<UrlTokenScore> documentList = reverseIndexMap.get(token);
                if (!included(documentList, url)) {
                    reverseIndexMap.get(token).add(urlTokenScore);
                }
            }
        }

        // Calculate the TF-IDF and add the value to each document in the reverse index
        for (var entry : reverseIndexMap.entrySet()) {
            String token = entry.getKey();
            List<UrlTokenScore> documentList = entry.getValue();

            for (UrlTokenScore document : documentList) {
                double invertedDocumentFrequency = calculateIDF(token, reverseIndexMap);
                document.calculateTFIDF(invertedDocumentFrequency);
            }
        }
    }
    
    /**
     * Lemmatize and tokenize the entire content stored in 'intranetSiteList'.
     * Create a corresponding basic forward index that maps each URL to a list of tokens
     * that appear in the title, header or content of the site associated with the URL.
     */
    private void createBasicForwardIndex() throws WebsiteContentListNotSet {
        
        if (intranetSiteList == null) throw new WebsiteContentListNotSet("First use method setWebsiteContentList(List<Website> intranetSiteList)");

        // Add all relevant content of intranetSiteList as lemmatized tokens into the basic forward index
        for (Website site : intranetSiteList) {
            
            String websiteData = site.title + " " + site.header + " " + site.content;
            List<String> tokenList = getLemmatizedTokens(websiteData);
            StopWords.deleteStopWords(tokenList);

            // Add all lemmatize tokens of the current site to the basic forward index, sets URL as key
            basicForwardIndexMap.put(site.url, tokenList);
        }
    }

    /**
     * Calculate the TF (term frequency) of the given token corresponding to the given URL in
     * the basic forward index
     * 
     * @param token     The token whose TF value is to be calculated.
     * @param url       The URL of the document in which the token appears.
     * @return          The term TF of the given token in the given URL.
     */
    private double calculateTF(String token, String url) {
        
        // Store all tokens mapped to the URL in the basic forward index under 'listOftokensInDocument'
        List<String> listOftokensInDocument = basicForwardIndexMap.get(url);

        // Count the number of tokens in 'listOftokensInDocument' that are equal to the given token
        int countOfTokenInDoc = 0;
        for (String element : listOftokensInDocument) {
            if (element.equals(token)) countOfTokenInDoc++;
        }
        
        // Calculate and return the TF
        return (double)  countOfTokenInDoc / listOftokensInDocument.size();
    }

    // Check if the given URL is already included in the given list of UrlTokenScore objects
    private boolean included(List<UrlTokenScore> documentList, String url) {
        boolean contains = false;
        for (UrlTokenScore document : documentList) {
            if (document.getUrl().equals(url)) contains = true;
        }
        return contains;
    }

    // Return the UrlTokenScore object in the given document list that corresponds to the given URL
    private UrlTokenScore searchUrlInList(List<UrlTokenScore> documentList, String url) {
        UrlTokenScore foundDocument = null;
        for (UrlTokenScore document : documentList) {
            if (document.url.equals(url)) {
                foundDocument = document;
                return foundDocument;
            }
        }
        return foundDocument;
    }

    // Calculate the IDF (inverted document frequency) of a given token in a given reverse index
    private double calculateIDF(String token, Map<String, List<UrlTokenScore>> reverseIndexMap) {
        // Get the amount of URLs in the basic forward index
        double numbOfDocs = basicForwardIndexMap.size();

        // Get the amount of URLs that contain the given token in their data
        List<UrlTokenScore> listOfDocumentsContainingToken = reverseIndexMap.get(token);
        double numDocsContainToken = listOfDocumentsContainingToken.size();

        // Calculate and return the IDF of the given token
        return Math.log(numbOfDocs / numDocsContainToken);
    }

    /**
     * {@inheritDoc}
     * 
     * This method performs a search in the reverse index for all given lemmatized tokens in 'tokLemmaQuery'.
     * The relevant UrlTokenScore objects found, corresponding to these tokens in the index, are retrieved
     * and sorted according to their TF-IDF values. Finally, the resulting UrlTokenScore objects are returned
     * in a list.
     * 
     * @param tokLemmaQuery         The lemmatized and tokenized search request.
     * @return                      A list of UrlTokenScore objects that mention the given tokens in
     *                              their title, paragraph or content, sorted by their TF-IDF values.
     * @throws IllegalArgumentException If the given list does not solely contain single tokens.
     */
    @Override
    protected List<UrlTokenScore> searchLemmatizedArray(String[] tokLemmaQuery) {

        // Throw an exception if the given list does not solely contain single tokens
        if (!isTokenized(tokLemmaQuery)) throw new IllegalArgumentException("The given list has to contain single lemmatized tokens.");

        // Store all the search results
        List<UrlTokenScore> searchResults = new ArrayList<>();

        // Iterate through all lemmatized tokens and add all UrlTokenScore objects that contain these tokens to 'searchResults'.
        for (String token : tokLemmaQuery) {
            if (reverseIndexMap.containsKey(token)) {
                List<UrlTokenScore> foundDocuments = reverseIndexMap.get(token);

                // If a UrlTokenScore object that contains the current token is already included in searchResults,
                // increment the TF-IDF value of the URL in 'foundDocuments' by the TF-IDF value of the document.
                // Else, add the UrlTokenScore object to 'searchResults'.
                for (UrlTokenScore document : foundDocuments) {
                    // Check if 'document' is already included in 'searchResults'
                    if (included(searchResults, document.url)) {
                        // Increment the TF-IDF value of the UrlTokenScore object in 'searchResults'
                        UrlTokenScore incudedDocument = searchUrlInList(searchResults, document.url);
                        incudedDocument.setTFIDF(incudedDocument.getTFIDF() + document.getTFIDF());
                    } else {
                        // Add the previously not included UrlTokenScore object to 'searchResults'
                        searchResults.add(document);
                    }
                }
            }
        }
    
        // Sort 'searchResults' according to the correspondent TF-IDF value of each UrlTokenScore object
        Collections.sort(searchResults, new Comparator<UrlTokenScore>() {
            @Override
            public int compare(UrlTokenScore first, UrlTokenScore second) {
                Double firstDouble = (double) first.getTFIDF();
                Double secondDouble = (double)second.getTFIDF();
                return secondDouble.compareTo(firstDouble);
            }
        });
        return searchResults;
    }

    /**
     * Retrieve an unmodifiable view on the basic forward index required in order to create more detailed indexes.
     * 
     * @return An an unmodifiable view on the basic forward index as a map.
     */
    public Map<String, List<String>> getBasicForwardIndexMap() {
        return Collections.unmodifiableMap(basicForwardIndexMap);
    }
    
    /**
     * Return an unmodifiable view on the reverse index as a map.
     * 
     * @return      An unmodifiable view on a map setup out of tokens as keys and a
     *              list of URLTokenScore objects as values. Each list of URLTokenScore
     *              objects represents the URLs that mention the key token in their content.
     */
    public Map<String, List<UrlTokenScore>> getIndexMap() {
        return Collections.unmodifiableMap(reverseIndexMap);
    }

    /**
     * Store the given list of Website elements, containing all relevant information 
     * required in order to create the index under 'intranetSiteList'.
     * 
     * @param intranetSiteList  A list of Website elements containing all the necessary
     *                          data in order to create the index.
     */
    public void setWebsiteContentList(List<Website> intranetSiteList) {
        this.intranetSiteList = intranetSiteList;
    }

    /**
     * Retrieve an unmodifiable view on the list of Website objects containing all data
     * gathered by the crawler and used in order to build the basic forward index.
     * 
     * @return An unmodifiable view on the list with all data gathered by the crawler.
     */
    public List<Website> getWebsiteContentList() {
        return Collections.unmodifiableList(intranetSiteList);
    }
}
