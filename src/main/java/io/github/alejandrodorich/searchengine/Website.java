package io.github.alejandrodorich.searchengine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Store essential website data (URL, title, header, content, and external links) gathered during crawling.
 * This data is foundational for creating any type of index and performing relevance-based searches.
 * 
 * @see UrlData         The superclass providing the base URL field.
 * @see Crawler         Retrieves and stores website data during crawling.
 * @see ReverseIndex    Creates forward/reverse indices based on 'Website' objects.
 */
public class Website extends UrlData {

    // A list including all reference URLs found in this website
    private final List<String> urlList;

    // Websites that reference this website (backlinks)
    private List<Website> sitesWithLinkToWebsite = new ArrayList<>();

    public final String title;
    public final String header;
    public final String content;
    private Double pageRank;

    /**
     * Construct a Website object with the following attributes:
     * 
     * @param url           The URL of this website.
     * @param title         The title of this website.
     * @param header        The header of this website.
     * @param content       The entire content of this website.
     * @param urlList       All external links found on the website.
     */
    public Website (String url, String title, String header, String content, List<String> urlList) {
        super(url);
        this.title = title;
        this.header = header;
        this.content = content;
        this.urlList = urlList;
    }
    
    public void setPageRank(double pageRank) {
        this.pageRank = pageRank;
    }

    /**
     * Add a site that has a reference to this website to 'sitesWithLinkToWebsite'.
     * 
     * @param site  A site that references this website.
     * @throws IllegalArgumentException     If the given site does not contain a link to this website.
     */
    public void addSiteWithLinkToWebsite(Website site) {

        if (!site.urlList.contains(this.url)) throw new IllegalArgumentException("Given site does not contain a link to this website.");

        sitesWithLinkToWebsite.add(site);
    }

    /**
     * Return the PageRank value calculated during the crawling process for this website.
     * 
     * @return  The PageRank value of this website.
     * @throws IllegalStateException If the PageRank value of this website has not been set.
     */
    public double getPageRank() {
        if (pageRank == null) throw new IllegalStateException("The PageRank value has not been set.");
        return pageRank;
    }

    public int getNumOfExternalLinks() {
        return urlList.size();
    }

     /**
     * @return  An unmodifiable list of external links found on this website.
     */
    public List<String> getExternalLinks() {
        return Collections.unmodifiableList(urlList);
    }

    /**
     * @return  An unmodifiable list of websites that reference this website (backlinks).
     */
    public List<Website> getSitesWithLinkToWebsite() {
        return Collections.unmodifiableList(sitesWithLinkToWebsite);
    }
}
