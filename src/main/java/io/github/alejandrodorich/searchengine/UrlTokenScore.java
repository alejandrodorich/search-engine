package io.github.alejandrodorich.searchengine;

/**
 * Store TF (term frequency) and TF-IDF (term frequency - inverted document frequency)
 * values of a specific URL and token pair, used in ReverseIndex.
 * 
 * @see UrlData         The superclass providing the base URL field.
 * @see ReverseIndex    An index mapping tokens to URLs and their TF-IDF values. 
 */
public class UrlTokenScore extends UrlData {
    private double termFrequency;
    private Double tFIDF;
    private final String token;

    /**
     * Construct an UrlTokenScore object with the given URL and TF value.
     * 
     * @param url               The URL to be referenced.
     * @param token             The token to be referenced.
     * @param termFrequency     The TF value for the referencing URL and token in a ReverseIndex.
     */
    public UrlTokenScore(String url, String token, double termFrequency) {
        super(url);
        this.token = token;
        this.termFrequency = termFrequency;
    }

    /**
     * Return the TF-IDF value for the referencing URL and token in a ReverseIndex.
     * 
     * @return  The TF-IDF value.
     * @throws IllegalStateException If the TF-IDF value has not been calculated.
     */
    public double getTFIDF() {
        if (tFIDF == null) throw new IllegalStateException("TF-IDF value has not been calculated.");
        return tFIDF;
    }

    public String getToken() {
        return token;
    }

    /**
     * Calculate the TF-IDF value based on the TF and the given IDF value.
     * 
     * @param invertedDocumentFrequency The IDF value for this token.
     */
    public void calculateTFIDF(double invertedDocumentFrequency) {
        tFIDF = termFrequency * invertedDocumentFrequency;
    }

    /**
     * Set the TF-IDF value for this URL-token pair, used to accumulate the TF-IDF values
     * across multiple query tokens during each search.
     * 
     * @param tFIDF The TF-IDF value to set.
     */
    public void setTFIDF(double tFIDF) {
        this.tFIDF = tFIDF;
    }
}
