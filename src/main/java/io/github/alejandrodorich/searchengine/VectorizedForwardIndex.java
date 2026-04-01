package io.github.alejandrodorich.searchengine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

/**
 * This class implements a vectorized forward index to store and search through data retrieved by the crawler.
 * 
 * The vectorized forward index expands upon the basic forward index and reverse index, by storing the TF-IDF values for
 * all tokens found in the index for each URL. This enables the calculation of the cosine similarity, that is used
 * alongside the PageRank calculated in the crawling process, in order to return more precise search results.
 * 
 * The vectorized forward index is represented by a map, where each key is a UrlCosineScore object and the corresponding
 * value is a vector of TF-IDF values corresponding to all lemmatized tokens of the data retrieved by the crawler.
 * 
 * @see ReverseIndex    A related type of index used by this class in order to efficiently create a vectorized forward index.
 */
public class VectorizedForwardIndex extends Index<UrlCosineScore> {

    // Represent all tokens found in the tokenized and lemmatized data retrieved during the crawling process
    // Implicitly assign a unique index for every token, corresponding to its position in the list
    private List<String> indexValuesForTokens = new ArrayList<>();

    private List<Website> intranetSiteList;

    // State if all TF-IDF values in the vectorized forward index have been normalized according
    // to their euclidean norm.
    private boolean normalizedVectors;
    
    // Represent the vectorized forward index
    // Maps each UrlCosineScore object to a vector of TF-IDF values corresponding to all tokens in 'indexValuesForTokens'
    private Map<UrlCosineScore, double[]> vectorizedFIndexMap = new LinkedHashMap<>();

    /**
     * Exception indicating that the reverse index has not been created.
     */
    public class ReverseIndexNotCreated extends Exception {
         /**
         * Construct a new ReverseIndexNotCreated exception with the given message.
         * @param message   The message explaining why the exception was triggered.
         */
        ReverseIndexNotCreated(String message) {
            super(message);
        }
    }

    /**
     * Create a vectorized forward index based on the given reverse index.
     * 
     * This method retrieves the corresponding basic forward index that maps each URL to a list of tokens that appear in the title, 
     * header or content of the site associated with the URL. Using the basic forward index and the given reverse index as a base,
     * it builds a vectorized forward index, which expands the basic forward index, by mapping each URL to a vector setup of TF-IDF
     * values corresponding to all tokens found in the given reverse index. The map is stored under 'vectorizedFIndexMap', and the
     * keys are set as UrlCosineScore objects, corresponding to a specific URL but also including the PageRank value as calculated
     * during the crawling process.
     * 
     * To map each TF-IDF value in the vectorized forward index to its corresponding token, all tokens are stored in the list
     * 'indexValuesForTokens', where each token is implicitly assigned a unique index corresponding to its position in the list.
     * Each vector in the vectorized forward index is structured as such, that the value at the position i represents the TF-IDF
     * value for the i-th token in 'indexValuesForTokens'.
     * 
     * @param reverseIndex              The reverse index used as a basis in order to create the vectorized forward index.                           
     * @throws ReverseIndexNotCreated   If the given reverse index is empty.
     */
    public void createIndex(ReverseIndex reverseIndex) throws ReverseIndexNotCreated  {

        Map<String, List<UrlTokenScore>> reverseIndexMap = reverseIndex.getIndexMap();

        // Transfer all URLs, website titles, headers, content and external links obtained in the crawling process to 'intranetSiteList' 
        intranetSiteList = reverseIndex.getWebsiteContentList();

        // Set an index as an identifier for each token in the reverseIndex by storing all tokens in 'indexValuesForTokens'
        if (reverseIndexMap.isEmpty()) throw new ReverseIndexNotCreated("The given ReverseIndex is empty. Use 'createIndex' method in ReverseIndex.");
        for (String token : reverseIndexMap.keySet()) {
            indexValuesForTokens.add(token);
        }

        // Set 'vectorizedFIndexMap' as an forward index where each document is represented by a vector
        // Every entry in each vector corresponds to a token in 'indexValuesForTokens'
        createEmptyVectorizedForwardIndexMap(reverseIndex);

        // Set the TF-IDF values for all entries in 'vectorizedFIndexMap'
        for (var entryRI : reverseIndexMap.entrySet()) {
            String token = entryRI.getKey();
            List<UrlTokenScore> urlTokenScores = entryRI.getValue();
            // Get the index of the current token
            int indexOfToken = indexValuesForTokens.indexOf(token);

            // Store the TF-IDF values of the current token in 'vectorizedFIndexMap'
            for (UrlTokenScore document : urlTokenScores) {
                String url = document.url;
                double tFIDF = document.getTFIDF();

                for (var entryVFI : vectorizedFIndexMap.entrySet()) {
                    double[] urlVector = entryVFI.getValue();
                    if (entryVFI.getKey().url.equals(url)) {
                        urlVector[indexOfToken] = tFIDF;
                    }
                }
            }
        }
        storePageRank();
    }

    // Create an empty vectorized forward index as a map where each URL is set as a key and an array filled with zeros 
    // is set as the corresponding value. Each entry in the array corresponds to a token in 'indexValuesForTokens' and
    // all arrays have the same structure.
    private void createEmptyVectorizedForwardIndexMap(ReverseIndex reverseIndex) {

        // Set 'basicForwardIndexMap' as the basic forward index used as a blueprint in order to create the
        // vectorized forward index.
        Map<String, List<String>> basicForwardIndexMap = reverseIndex.getBasicForwardIndexMap();

        // Iterate through all documents in 'basicForwardIndexMap' and create a vectorized forward index
        // under 'vectorizedFIndexMap'
        for (var entry : basicForwardIndexMap.entrySet()) {

            // Create an empty vector named 'termValues' for the current URL in 'basicForwardIndexMap' where each entry 
            // corresponds to a token as set in 'indexValuesForTokens'.
            String url = entry.getKey();
            double[] termValues = new double[indexValuesForTokens.size()];
            vectorizedFIndexMap.put(new UrlCosineScore(url), termValues);
        }
    }
    
    // Add the PageRank value of each URL to 'vectorizedFIndexMap'
    private void storePageRank() {
        for (UrlCosineScore document : vectorizedFIndexMap.keySet()) {
            for (Website site : intranetSiteList) {
                if (site.url.equals(document.getUrl())) document.setPageRank(site.getPageRank());
            }
        }
    }
    
    /**
     * Calculate the cosine similarity for the two given vectors.
     * 
     * @param vectorA               The first vector, representing a document or query in numerical values, e.g. TF-IDF or token frequency in query.
     * @param vectorB               The second vector, representing a document or query in numerical values, e.g. TF-IDF or token frequency in query.
     * @param normalizedVectors     True, if the all vectors in 'vectorizedFIndexMap' have been normalized according to their euclidean norm.
     * @return                      The cosine similarity value.
     * @throws IllegalArgumentException If the length of the given vectors is not the same.
     */

    public double calculateCosineSimilarity(double[] vectorA, double[] vectorB, boolean normalizedVectors) {

        if (vectorA.length != vectorB.length) throw new IllegalArgumentException("The length of the given vectors is not the same.");

        // Multiply each entry in 'vectorA' with their corresponding entry in 'vectorB'
        // Store the sum of all multiplications in 'sumOfMultiplication'.
        double sumOfMultiplication = 0;
        for (int i = 0; i < vectorA.length; i++) {
            sumOfMultiplication = sumOfMultiplication + (vectorA[i] * vectorB[i]); 
        }
        
        // Check if the given vectors have been normalized according to their euclidean norm
        // and calculate and returns the cosine similarity accordingly
        if (normalizedVectors) {
             // Return the cosine similarity of 'vectorA' and 'vectorB'
            return sumOfMultiplication;
        } else {
            // Calculate the euclidean norm for vectors A and B and multiply their norms
            double aEuclideanNorm = calculateEuclideanNorm(vectorA);
            double bEuclideanNorm = calculateEuclideanNorm(vectorB);
            double euclideanNormProduct = aEuclideanNorm * bEuclideanNorm;

            // Return the cosine similarity of 'vectorA' and 'vectorB'
            return sumOfMultiplication / euclideanNormProduct;
        }
    }

    // Calculate the euclidean norm of the given vector
    private double calculateEuclideanNorm(double[] vector) {
        double sumOfSquares = 0;
        for (double entry : vector) {
            sumOfSquares = sumOfSquares + (entry * entry);
        }
        return Math.sqrt(sumOfSquares);
    }

    /**
     * {@inheritDoc}
     * 
     * Perform a search in the vectorized forward index. The relevant URLs are retrieved
     * from 'vectorizedFIndexMap', where they are set as keys of vectors representing their TF-IDF
     * values for all tokens in the index.
     * The resulting list is sorted according to the factorized product of the normalized
     * PageRank and cosine similarity values of each UrlCosineScore object.
     * 
     * @param tokLemmaQuery             The lemmatized and tokenized search request.
     * @return List<UrlCosineScore>     A list containing all UrlCosineScore objects that contain the tokens
     *                                  out of 'tokLemmaQuery' in their title, paragraph or content, sorted
     *                                  by their relevance.
     * @throws IllegalArgumentException If the given list does not solely contain single tokens
     */
    @Override
    protected List<UrlCosineScore> searchLemmatizedArray(String[] tokLemmaQuery) {

        // Throw an exception if the given list does not solely contain single tokens
        if (!isTokenized(tokLemmaQuery)) throw new IllegalArgumentException("The given list has to contain single lemmatized tokens.");
        
        // Create a vector representation of query, where each index corresponds to a token in
        // 'indexValuesForTokens'. The number stored in a single index represent how often the token
        // appears in the given query.
        double[] vectorQuery = new double[indexValuesForTokens.size()];
        boolean noTokenFoundInIndex = true;
        for (String token : tokLemmaQuery) {
            if (indexValuesForTokens.contains(token)) {
                noTokenFoundInIndex = false;
                int index = indexValuesForTokens.indexOf(token);
                vectorQuery[index]++;
            }
        }

        if (noTokenFoundInIndex) return new ArrayList<>();
        
        List<UrlCosineScore> searchResultList = getResultingWebsiteList(vectorQuery, false);
        
        return sortResultingWebsiteList(searchResultList);
    }

    // Calculate the cosine similarity of each vector in 'vectorizedFIndexMap' and the given vector representation
    // of the query. Returns an unsorted search result list.
    private List<UrlCosineScore> getResultingWebsiteList(double[] vectorQuery, boolean normalizedVectors) {

        // Check if 'vectorQuery' represents all tokens in 'indexValuesFortokens'
        if (indexValuesForTokens.size() != vectorQuery.length) {
            throw new java.lang.IllegalArgumentException("The size of the given vectorQuery does not correspond to the amount of tokens in the vectorized forward index.");
        }

        List<UrlCosineScore> searchResultList = new ArrayList<>();

        for (var entry : vectorizedFIndexMap.entrySet()) {

            // Calculate the cosine similarity of each vector and the given 'vectorQuery'
            UrlCosineScore document = entry.getKey();
            double[] vector = entry.getValue();
            double cosineSimValue = calculateCosineSimilarity(vector, vectorQuery, normalizedVectors);
            document.setCosineSimilarity(cosineSimValue);


            // Add all UrlCosineScore objects with a cosine similarity unequal to 0 to 'searchResultList'
            if (cosineSimValue != 0) {
                searchResultList.add(document);
            }
        }

        // Return an unsorted list containing all search results
        return searchResultList;
    }

    // Calculate the relevance of each object in the given 'websiteList' according to their PageRank and
    // cosine similarity values and sorts the list accordingly.
    private List<UrlCosineScore> sortResultingWebsiteList(List<UrlCosineScore> websiteList) {

        /*
         * Normalize the values of cosine similarity and PageRank of all objects in 'websiteList'.
         * Calculate the relevance factor for each site in 'websiteList' as the weighted product
         * of their normalized cosine similarity and PageRank values.
         */
        double maxCosineSim = 0;
        double maxPageRank = 0;
        for (UrlCosineScore site : websiteList) {
            if (site.getPageRank() > maxPageRank) maxPageRank = site.getPageRank();
            if (site.getCosineSimilarity() > maxCosineSim) maxCosineSim = site.getCosineSimilarity();
        }

        for (UrlCosineScore site : websiteList) {
            if (maxPageRank != 0 && maxCosineSim != 0) {
                site.setNormalizedPageRank(site.getPageRank() / maxPageRank);
                site.setNormalizedCosineSimilarity(site.getCosineSimilarity() / maxCosineSim);
                site.calculateRelevanceFactor(0.5, 0.5);
            }
        }

        // Sort 'websiteList' according to the correspondent relevance factor of each website
        Collections.sort(websiteList, new Comparator<UrlCosineScore>() {
            @Override
            public int compare(UrlCosineScore first, UrlCosineScore second) {
                Double firstDouble = first.getRelevanceFactor();
                Double secondDouble = second.getRelevanceFactor();
                return secondDouble.compareTo(firstDouble);
            }
        });

        // Return the sorted 'websiteList'
        return websiteList;
    }

    /**
     * Return an unmodifiable view on the vectorized forward index as a map.
     * 
     * The map is setup out of UrlCosineScore objects as keys, which represent all URLs found in the
     * crawling process, along with their PageRank and cosine similarity values. The values are vectors,
     * where each entry corresponds to the TF-IDF value of a token.
     * 
     * @return  An an unmodifiable view on a mapping of each UrlCosineScore object to
     *          its corresponding TF-IDF vector.
     */
    public Map<UrlCosineScore, double[]> getIndexMap() {
        return Collections.unmodifiableMap(vectorizedFIndexMap);
    }


    /**
     * Return an unmodifiable view on a list containing all tokens stored in the vectorized forward index.
     * 
     * The index of each token in the list serves as a key to find all corresponding TF-IDF values
     * to the token in the vectorized forward index.
     * 
     * @return List<String> List of all tokens in the index, serves as key.
     */
    public List<String> getIndexValuesForTokens() {
        return Collections.unmodifiableList(indexValuesForTokens);
    }

    /**
     * Search for tokens in the vectorized forward index with additional weightning for each token.
     * 
     * This method provides a more detailed search compared to 'searchQuery' by allowing the prioritization
     * of tokens based on their given weights in 'weightedQuery'. Tokens with higher weights are prioritized
     * when sorting the search results.
     * 
     * The method lemmatizes all tokens in the given 'weightedQuery' map and searches for them in the vectorized
     * forward index. Additionally, all TF-IDF values in the vectorized forward index are normalized in order to
     * enhance the cosine similarity calculation and enhance its efficiency.
     * 
     * The found UrlCosineScore objects are sorted according to the product of their cosine similarity
     * and PageRank values. Since the cosine similarity is calculated taking the weight of each token 
     * into account, the given weights have a direct influence over the order of the search results.
     * 
     * @param weightedQuery         The tokenized search request, where each token is assigned a weight
     *                              representing its importance.
     * @return                      The sorted search results depicting all UrlCosineScore elements.
     */
    public List<UrlCosineScore> searchWeightedQuery(Map<String, Double> weightedQuery) {

        // Check that all string elements in 'weightedQuery' are single tokens and that their
        // given weight is a positive number
        String regex = "[,\\.\\s]";
        for (var entry : weightedQuery.entrySet()) {
            String[] searchRequestTokens = entry.getKey().split(regex);
            if (searchRequestTokens.length != 1) throw new IllegalArgumentException("Only one token per index of weightedQuery allowed.");

            double weight = entry.getValue();
            if (weight < 0) throw new IllegalArgumentException("The weight of each token has to be a positive number.");
        }

        // Set 'pipeline' as a text processing pipeline setup for tokenization and lemmatization
        StanfordCoreNLP pipeline = Index.getPipeline();

        // Create a vector representation of the given 'weightedQuery', where each index corresponds to a token 
        // in 'indexValuesForTokens'. The number stored in a single index represents the token amount
        // multiplied by its given weight.
        double[] vectorQuery = new double[indexValuesForTokens.size()];
        boolean noTokenFoundInIndex   = true;
        for (var entry : weightedQuery.entrySet()) {
            String token = entry.getKey();
            double weight = entry.getValue();

            CoreDocument document = pipeline.processToCoreDocument(token);
            String lemma = document.tokens().get(0).lemma();

            if (indexValuesForTokens.contains(lemma) && !StopWords.stopWordsSet.contains(lemma)) {
                noTokenFoundInIndex = false;
                int index = indexValuesForTokens.indexOf(lemma);
                vectorQuery[index] += weight;
            }
        }
        
        // Return an empty list if no search results are found
        if (noTokenFoundInIndex) return new ArrayList<>();

        // Normalize the 'vectorQuery'
        double euclideanNormOfVector = calculateEuclideanNorm(vectorQuery);
        for (int i = 0; i < vectorQuery.length; i++) {
            vectorQuery[i] = vectorQuery[i] / euclideanNormOfVector;
        }

        // Normalize the value of all vectors in 'vectorizedFIndexMap' according to their euclidean norm
        if (!normalizedVectors) normalizeVectors();
        
        List<UrlCosineScore> searchResultList = getResultingWebsiteList(vectorQuery, true);
        return sortResultingWebsiteList(searchResultList);
    }

    // Normalize the value of all vectors in 'vectorizedFIndexMap' according to their euclidean norm
    private void normalizeVectors() {   

        // Iterate through all vectors in 'vectorizedFIndexMap' and normalize their values
        for (var entry : vectorizedFIndexMap.entrySet()) {
            double[] vector = entry.getValue();
            double euclideanNorm = calculateEuclideanNorm(vector);
        
            // Normalize all TF-IDF values of the current vector
            for (int i = 0; i < vector.length; i++) {
                vector[i] = vector[i] / euclideanNorm;
            }
        }
        normalizedVectors = true;
    }
}
