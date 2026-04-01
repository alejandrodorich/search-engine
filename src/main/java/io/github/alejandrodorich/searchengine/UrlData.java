package io.github.alejandrodorich.searchengine;

/**
 * This abstract class serves as a base class for URLs and related data required in order to create an index.
 * 
 * @see UrlCosineScore  A subclass of UrlData that stores values such as the cosine similarity and PageRank,
 *                      required to create a VectorizedForwardIndex.
 * @see UrlTokenScore   A subclass of UrlData that stores values such as the TF-IDF,
 *                      required to create a ReverseIndex.
 * @see Website         A subclass of UrlData that stores values such as title, header, content and all reference
 *                      links, required to create any type of index.
 */
abstract class UrlData {
    
    protected final String url;

    /**
     * Construct an UrlData object with the given URL.
     * 
     * @param url   The URL to be referenced by this object.
     */
    protected UrlData(String url) {
        this.url = url;
    }

    /**
     * Return the URL referenced by this object.
     * 
     * @return  The referenced URL.
     */
    public String getUrl() {
        return url;
    }
}
