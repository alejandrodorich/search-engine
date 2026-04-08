package io.github.alejandrodorich.searchengine;

/**
 * Store crucial information of URLs for a VectorizedForwardIndex including the PageRank,
 * the cosine similarity and the relevance factor for a specific URL.
 * 
 * @see UrlData                     The superclass providing the base URL field.
 * @see VectorizedForwardIndex      An index mapping URLs to vectors based on TF-IDF values.
 */
public class UrlCosineScore extends UrlData {

    // Product of normalized PageRank and cosine similarity (used for relevance sorting)
    private Double relevanceFactor;

    private Double cosineSimilarity;
    private Double pageRank;
    private Double normalizedCosineSimilarity;
    private Double normalizedPageRank;

    /**
     * Construct an UrlCosineScore object with the given URL.
     * 
     * @param url   The referencing URL.
     */
    public UrlCosineScore(String url) {
        super(url);
    }

    /**
     * Set the cosine similarity value for this URL.
     * 
     * @param cosineSimilarity  The cosine similarity value corresponding to this URL in a VectorizedForwardIndex.
     */
    public void setCosineSimilarity(Double cosineSimilarity) {
        this.cosineSimilarity = cosineSimilarity;
    }

    /**
     * Set the PageRank value for this URL.
     * 
     * @param pageRank  The PageRank value calculated in the crawling process by the PageRankCalculator.
     */
    public void setPageRank(Double pageRank) {
        this.pageRank = pageRank;
    }

    /**
     * Set the normalized cosine similarity in order to calculate the 'relevanceFactor'.
     * 
     * @param normalizedCosineSimilarity    The normalized cosine similarity value.
     */
    public void setNormalizedCosineSimilarity(Double normalizedCosineSimilarity) {
        this.normalizedCosineSimilarity = normalizedCosineSimilarity;
    }

    /**
     * Set the normalized PageRank value in order to calculate the 'relevanceFactor'.
     * 
     * @param normalizedPageRank    The normalized PageRank value.
     */
    public void setNormalizedPageRank(Double normalizedPageRank) {
        this.normalizedPageRank = normalizedPageRank;
    }

    /**
     * Calculate the relevance factor for this website according to the provided weight for the normalized
     * PageRank and cosine similarity values. 
     * 
     * @param weightPageRank            The given weight for the PageRank.
     * @param weightCosinesimilarity    The given weight for the cosine similarity.
     * @throws IllegalStateException     If the PageRank or cosine similarity or their normalized values have still not been set.
     */
    public void calculateRelevanceFactor(double weightPageRank, double weightCosineSimilarity) {
        if (this.pageRank == null || this.cosineSimilarity == null) {
            throw new IllegalStateException("PageRank or cosine similarity has not been calculated.");
        } else if (this.normalizedPageRank == null || this.normalizedCosineSimilarity == null) {
            throw new IllegalStateException("PageRank or cosine similarity has not been normalized.");
        }
        relevanceFactor = (weightPageRank * normalizedPageRank) + (weightCosineSimilarity * normalizedCosineSimilarity);
    }

    /**
     * Return the cosine similarity value corresponding to this object in a VectorizedForwardIndex.
     * 
     * @return  The cosine similarity value.
     * @throws  IllegalStateException If the cosine similarity has not been calculated.
     */
    public double getCosineSimilarity() {
        if (cosineSimilarity == null) throw new IllegalStateException("The cosine similarity has not been calculated.");
        return cosineSimilarity;
    }

    /**
     * Return the normalized PageRank value for this object required in order to calculate the 'relevanceFactor'.
     * 
     * @return  The normalized PageRank.
     * @throws  IllegalStateException If the normalized PageRank has not been calculated.
     */
    public double getNormalizedPageRank() {
        if (normalizedPageRank == null) throw new IllegalStateException("The normalized PageRank value has not been calculated.");
        return normalizedPageRank;
    }

    /**
     * Return the cosine similarity required in order to calculate the 'relevanceFactor'.
     * 
     * @return  The normalized cosine similarity value.
     * @throws  IllegalStateException If the normalized cosine similarity has not been calculated.
     */
    public double getNormalizedCosineSimilarity() {
        if (normalizedCosineSimilarity == null) throw new IllegalStateException("The normalized cosine similarity has not been calculated.");
        return normalizedCosineSimilarity;
    }

    /**
     * Return the PageRank value calculated during the crawling process for this object.
     * 
     * @return  The PageRank value.
     * @throws  IllegalStateException If the PageRank has not been calculated.
     * 
     */
    public double getPageRank() {
        if (pageRank == null) throw new IllegalStateException("The PageRank value has not been calculated.");
        return pageRank;
    }
    
    /**
     * Return the relevance factor for this URL used in order to sort the search results
     * in a VectorizedForwardIndex.
     * 
     * @return  The relevance factor required in order to sort a list of UrlCosineScore objects.
     * @throws IllegalStateException If the 'relevanceFactor' has not been calculated.
     */
    public double getRelevanceFactor() {
        if (relevanceFactor == null) throw new IllegalStateException("The relevance factor has not been calculated.");
        return relevanceFactor;
    }
}
