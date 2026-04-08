package io.github.alejandrodorich.searchengine;

import io.github.alejandrodorich.searchengine.Crawler.PageRankNotCalculated;
import io.github.alejandrodorich.searchengine.ReverseIndex.WebsiteContentListNotSet;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxParallelEdgeLayout;
import com.mxgraph.view.mxGraph;
import com.mxgraph.util.mxConstants;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Font;
import java.io.File;


/**
 * This class implements methods that crawl through a given maximum amount of websites in the lima-labs network.
 * Additionally, these methods allow the creation of a graph illustrating the references among the crawled sites
 * by displaying directed edges among the referencing sites. Displaying values such as PageRank and TF-IDF, 
 * scaling the nodes according to PageRank values and highlighting of the most important URLs concerning
 * the search results is also made possible by this class. Finally, the class implements methods that allow
 * the graph to be stored as a PNG file.
 * 
 * @see Crawler                 For crawling and retrieving website data.
 * @see ReverseIndex            For indexing website content, calculating TF-IDF values and performing searches
 *                              based on these values.
 * @see VectorizedForwardIndex  For indexing website content, calculating PageRank and cosine similarity values
 *                              and performing advanced searches based on these values.
 */
public class DirectedGraph {

    /**
     * Crawl the given amount of sites in the lima-labs network and return a list with all the relevant
     * data obtained by the crawler.
     * 
     * @param numberOfSites     The maximal number of sites that should be crawled.
     * @return                  The data gathered by the crawler during the crawling process as a list of 
     *                          Website objects.
     * @throws IOException      If an error occurs while reading the JSON file.
     */
    static List<Website> crawlThroughNetwork(int numberOfSites) {
        
        List<Website> documents = new ArrayList<>();

        try {
            // Load the metadata from the JSON file
            JsonObject jsonFile =  Utils.parseJSONFile("intranet/lima-labs-customers.json");
            String[] seedUrls = new Gson().fromJson(jsonFile.get("Seed-URLs"), String[].class);
            String jsonFileName = new Gson().fromJson(jsonFile.get("Net-Name"), String.class);
    
            Crawler crawler = new Crawler();
            crawler.setMaximalCrawledPages(numberOfSites);
            crawler.crawl(seedUrls, jsonFileName);
            crawler.calculatePageRank();
    
            documents = crawler.getCrawledSitesData();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (PageRankNotCalculated p) {
            p.printStackTrace();
        }
        return documents;
    }

    /**
     * Create a reverse index out of the given website list. 
     * 
     * @param crawledWebsites   A list of Website objects containing all information extracted during the
     *                          crawling process.
     * @return                  The reverse index created out of the given website list.
     */
    static ReverseIndex createReverseIndex(List<Website> crawledWebsites) {

        ReverseIndex reverseIndex = new ReverseIndex();
        reverseIndex.setWebsiteContentList(crawledWebsites);
        try {
            reverseIndex.createIndex();
        } catch (WebsiteContentListNotSet w) {
            w.printStackTrace();
        }
        return reverseIndex;
    }

    /**
     * Create a vectorized forward index out of the data in the given reverse index and searche
     * for the given search word in the index. Return the sorted search results.
     * 
     * @param searchWord            The given query that has to be searched for in the vectorized 
     *                              forward index.
     * @param rIndex                The given reverse index used as a base in order to create the vectorized
     *                              forward index.
     * @return                      A list containing all UrlCosineScore objects out of the vectorized forward
     *                              index, whose referencing URLs mention the lemmatized tokens out of 'searchWord'
     *                              in their title, paragraph or content, sorted their relevance factor.
     */
    static List<UrlCosineScore> searchVectorizedForwardIndex(String searchWord, ReverseIndex reverseIndex) {
 
        // Create a vectorized forward index that stores all the relevant data out of the given reverse index
        VectorizedForwardIndex vectorizedForwardIndex = new VectorizedForwardIndex();
        try {
            vectorizedForwardIndex.createIndex(reverseIndex);
        } catch (VectorizedForwardIndex.ReverseIndexNotCreated r) {
            r.printStackTrace();
        }

        return vectorizedForwardIndex.searchQuery(searchWord);
     }

    /**
     * Create a graph that sets a node for each Website object in the given 'documents' list. Names each node
     * as its corresponding URL. Add directed edges to illustrate the references among the site and return
     * the graph.
     * 
     * @param documents The data gathered by the crawler during the crawling process.
     * @return          The graph illustrating all websites and the references among these sites.
     */
    static mxGraph createGraph(List<Website> documents) {

        mxGraph graph = new mxGraph();
        Object parent = graph.getDefaultParent();

        mxGraphModel model = (mxGraphModel) graph.getModel();
        model.beginUpdate();

        try {
            // Add nodes representing all URLs in documents to the graph
            for (Website doc : documents) {
                graph.insertVertex(parent, doc.url, doc.url, 0, 0, 200, 100);
            }

            // Add all the edges representing references between sites to the graph
            for (Website doc : documents) {
                String originUrl = doc.url;
                var originNode = model.getCell(originUrl);

                for (String link : doc.getExternalLinks()) {
                    var targetNode = model.getCell(link);
                    if (targetNode != null) {
                        graph.insertEdge(parent, null, "", originNode, targetNode);
                    }
                }
            }
            mxCircleLayout nodeLayout = new mxCircleLayout(graph);
            nodeLayout.execute(parent);
        }
        finally
        {
            model.endUpdate();
        }
        return graph;
    }

    /**
     * Add the provided TF-IDF values to the nodes of the given graph according to 'reverseIndexSearchResults'.
     * 
     * @param graph                         The graph created by the 'createGraph' method to which the TF-IDF
     *                                      values will be added.
     * @param reverseIndexSearchResults     The resulting UrlTokenScore list obtained from searching for a 
     *                                      query in a reverse index.
     */
    static void addTFIDFValuesToGraph(mxGraph graph, List<UrlTokenScore> reverseIndexSearchResults) {

        mxGraphModel model = (mxGraphModel) graph.getModel();
        model.beginUpdate();
        try {
            for (UrlTokenScore site : reverseIndexSearchResults) {

                var node =  model.getCell(site.url);

                if (node == null) throw new IllegalArgumentException("The provided graph does not match the given search results.");

                // Set the URL and TF-IDF value of site as the name of its corresponding node in the graph 
                NumberFormat nf = new DecimalFormat("##.#####");
                model.setValue(node, site.url + "\n" + "TF-IDF: " + nf.format(site.getTFIDF()));
            }
        } finally {
            model.endUpdate();
        }
    }
    
    /**
     * Add the provided PageRank values out of 'crawledWebsites' to the edges of the given graph.
     * Additionally, scale all nodes in the graph according to the PageRank of their corresponding
     * Website object.
     * 
     * @param graph                 The graph to which the PageRank values will be added.
     * @param crawledWebsites       All content obtained by the crawler during the crawling process. 
     */
    static void addPageRankValuesToGraph(mxGraph graph, List<Website> crawledWebsites) {
    
        mxGraphModel model = (mxGraphModel) graph.getModel();
        model.beginUpdate();
        try {
            for (Website site : crawledWebsites) {

                // Get the PageRank value that is passed to all referenced websites of 'site'
                double passedPageRank = site.getPageRank() / site.getNumOfExternalLinks();

                var originNode = model.getCell(site.url);

                for (String referencedSite : site.getExternalLinks()) {

                    var targetNode = model.getCell(referencedSite);

                    // Get the edge from 'originNode' to 'targetNode'
                    Object[] edges = mxGraphModel.getEdgesBetween(model, originNode, targetNode);
                    Object edge = null;
                    for (Object directedEdge : edges) {
                        var sourceNodeOfEdge = model.getTerminal(directedEdge, true); 
                        var targetNodeOfEdge = model.getTerminal(directedEdge, false);
                        if (originNode == sourceNodeOfEdge && targetNode == targetNodeOfEdge) {
                            edge = directedEdge;
                        }
                    }

                    if (edge == null) throw new IllegalArgumentException("The provided graph does not match the crawledWebsites list.");

                    // Store the PageRank value that is passed form site to 'referencedSite' in the corresponding edge in the graph
                    NumberFormat nf = new DecimalFormat("##.####");
                    model.setValue(edge, nf.format(passedPageRank));
                }
            }
            // Display all bidirectional edges as parallel edges
            mxParallelEdgeLayout edgeLayout = new mxParallelEdgeLayout(graph);
            edgeLayout.execute(graph.getDefaultParent());
        } finally {
            model.endUpdate();
        }
        scaleNodesAccordingToPageRank(graph, crawledWebsites);
    }

    // Edit the given graph by scaling all nodes according to their PageRank values
    private static void scaleNodesAccordingToPageRank(mxGraph graph, List<Website> crawledWebsites) {
        
        double maxPageRank = 0;
        for (Website site : crawledWebsites) {
            if (site.getPageRank() > maxPageRank) maxPageRank = site.getPageRank();
        }

        mxGraphModel model = (mxGraphModel) graph.getModel();
        model.beginUpdate();
        try {
            for (Website site : crawledWebsites) {
                double normalizedPageRank = site.getPageRank() / maxPageRank;
                var node =  model.getCell(site.url);

                if (node == null) throw new IllegalArgumentException("The provided graph does not match the crawledWebsites list.");
                
                // Scale nodes
                mxGeometry geo = model.getGeometry(node);
                geo.setHeight(normalizedPageRank * 200);
                geo.setWidth(normalizedPageRank * 300);
            }

        } finally {
            model.endUpdate();
        }
    }

    /**
     * Add the provided 'relevanceFactor' values out of 'searchResultsVectorizedFIndex' to the nodes
     * of the given graph.
     * 
     * @param graph                                 The graph to which the 'relevanceFactor' values will be added.
     * @param searchResultsVectorizedFIndex         The resulting UrlCosineScore list obtained from searching for a 
     *                                              query in a vectorized forward index.
     */
    static void addRelevanceFactorToGraph(mxGraph graph, List<UrlCosineScore> searchResultsVectorizedFIndex) {

        mxGraphModel model = (mxGraphModel) graph.getModel();
        model.beginUpdate();
        try {
            for (UrlCosineScore resultUrlScore : searchResultsVectorizedFIndex) {

                String url = resultUrlScore.getUrl();
                var node = model.getCell(url);
                if (node == null) throw new IllegalArgumentException("The provided graph does not match the searchResults list.");

                double relevanceFactor = resultUrlScore.getRelevanceFactor();

                // Add the 'relevanceFactor' value to the name of the node
                String nodeName = model.getValue(node).toString();
                NumberFormat nf = new DecimalFormat("##.#####");
                model.setValue(node, nodeName + "\n" + "Relevance Factor: " + nf.format(relevanceFactor));
            }
        } finally {
            model.endUpdate();
        }

    }

    /**
     * Highlight the three most relevant nodes of the given graph based on the given search results as listed
     * in 'searchResultsVectorizedFIndex'. The highlighting is achieved by changing the color of the nodes to light green.
     * 
     * @param graph                             The graph in which the nodes will be changed.
     * @param searchResultsVectorizedFIndex     The resulting UrlCosineScore list obtained from searching for a 
     *                                          query in a vectorized forward index.
     * @throws IllegalArgumentException         If the provided graph does not contain the URLs given in the search
     *                                          results.
     * 
     */
    static void markThreeMostImportantSites(mxGraph graph, List<UrlCosineScore> searchResultsVectorizedFIndex) {

        mxGraphModel model = (mxGraphModel) graph.getModel();
        model.beginUpdate();
        try {
            for (int i = 0; i < 3; i++) {
                String url = searchResultsVectorizedFIndex.get(i).getUrl();
                var node = model.getCell(url);
                if (node == null) throw new IllegalArgumentException("The provided graph does not match the searchResults list.");
 
                // Change the color of the node to light green
                model.setStyle(node, mxConstants.STYLE_FILLCOLOR + "=#90EE90");
            }
        } finally {
            model.endUpdate();
        }
    }

    // Delete all URLs referenced by websites in 'websiteList' that do not belong to any website of the given list
    private static List<Website> deleteAllIrrelevantLinks(List<Website> websiteList) {

        List<Website> websiteListNoIrrelevantLinks = new ArrayList<>();

        for (Website site : websiteList) {
            
            List<String> linksInSite = new ArrayList<>(site.getExternalLinks());
            List<String> new_linksList = new ArrayList<>();

            // Only keep links that correspond to a website in 'websiteList'
            for (String url : linksInSite) {
                for (Website document : websiteList) {
                     if (document.url.equals(url)) {
                        new_linksList.add(url);
                        break;
                    }
                }
            }
            Website siteWithNoIrrelevantLinks = new Website(site.url, site.title, site.header, site.content, new_linksList);
            siteWithNoIrrelevantLinks.setPageRank(site.getPageRank());
            websiteListNoIrrelevantLinks.add(siteWithNoIrrelevantLinks);
        }
        return websiteListNoIrrelevantLinks;
    }

    /**
     * Store the given graph in the 'figures' folder of the project directory as a PNG file
     * under the given file name. 
     * 
     * @param graph         The graph to be stored as a PNG file.
     * @param fileName      The name under which the file will be stored.
     */
    static void storeImage(mxGraph graph, String fileName) {
        try {
            BufferedImage image = mxCellRenderer.createBufferedImage(graph, null, 1, Color.WHITE, true, null);
            File outputFile = new File("figures/"+fileName+".png");
            ImageIO.write(image, "png", outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    } 

    /**
     * Store the given graph in the 'figures' folder of the project directory as a PNG file
     * under the given file name and display the query in the legend of the created file.
     * 
     * @param graph         The graph to be stored as a PNG file.
     * @param fileName      The name under which the file will be stored.
     * @param searchWord    The search query to be displayed in the legend of the file.
     */
    static void storeImageIncludingLegend(mxGraph graph, String fileName, String searchWord) {
        try {
            BufferedImage image = mxCellRenderer.createBufferedImage(graph, null, 1, Color.WHITE, true, null);

            // Display the given search word in the legend of the image
            Graphics2D g = image.createGraphics();
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", 0, 30));
            g.drawString("Search word: " + searchWord, 50, 40);
    
            // Store image
            File outputFile = new File("figures/"+fileName+".png");
            ImageIO.write(image, "png", outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }   
    
    /**
     * Main workflow:
     * 1. Crawl the first 8 sites in the lima-labs-customer network
     * 2. Create a graph based on the crawling results displaying each crawled site as a node
     *    and all references between these sites as edges.
     * 3. Store different implementations of this graph in the 'figures' folder:
     *      - Net graph -> store as net-graph.png
     *      - Based on query = "support":
     *          - Including TF-IDF values               → store as support.png.
     *          - Adding PageRank values                → store as support_page-rank.png.
     *          - Adding highlighting of top 3 sites    → store as support_top3.png.
     * @param args Command line arguments
     */
    public static void main(String[] args) {

        // Crawl the first 8 sites in the lima-labs-customer network and store all relevant data under 'websiteList'
        List<Website> crawledSites = crawlThroughNetwork(8);

        // Delete all reference links in websites out of 'websiteList' that are irrelevant
        List<Website> websiteList = deleteAllIrrelevantLinks(crawledSites);

        // Create a graph displaying all sites in 'websiteList' where the edges represent references between them
        mxGraph graph = createGraph(websiteList);

        // Store the graph as net-graph.png in 'figures' folder
        storeImage(graph, "net-graph");

        String query = "support";
        ReverseIndex rIndex = createReverseIndex(websiteList);
        List<UrlTokenScore> searchResultsReverseIndex = rIndex.searchQuery(query);

        // Add TF-IDF values to the graph and store the results as support.png in 'figures' folder
        addTFIDFValuesToGraph(graph, searchResultsReverseIndex);
        storeImageIncludingLegend(graph, query, query);

        // Add PageRank values to the graph and store results as support_page-rank.png in 'figures' folder
        addPageRankValuesToGraph(graph, websiteList);
        storeImageIncludingLegend(graph, query + "_page-rank", query);

        // Add a relevance factor to all nodes in the graph and change the color of the 3 most relevant nodes 
        // according to the query to light green, store results as support_top3.png in 'figures' folder
        List<UrlCosineScore> searchResultsVectorizedFIndex = searchVectorizedForwardIndex(query, rIndex);
        addRelevanceFactorToGraph(graph, searchResultsVectorizedFIndex);
        markThreeMostImportantSites(graph, searchResultsVectorizedFIndex);
        storeImageIncludingLegend(graph, query + "_top3", query);
    }
}
