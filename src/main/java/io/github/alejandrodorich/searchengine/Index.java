package io.github.alejandrodorich.searchengine;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

/**
 * Abstract base class for searching through data obtained by the crawler.
 * Provide methods for lemmatization and tokenization of user queries.
 * 
 * @see ReverseIndex            A subclass implementing a reverse index for data storage, management, and search.
 * @see VectorizedForwardIndex  A subclass implementing a vectorized forward index for data storage, management, and search.
 */
public abstract class Index<T extends UrlData>{

    private static StanfordCoreNLP classPipeline;

    /**
     * Build and return a StanfordCoreNLP pipeline setup for tokenization
     * and lemmatization. Ensure that the pipeline is built only once.
     * 
     * @return The pipeline for text processing.
     */
    protected static StanfordCoreNLP getPipeline() {

        if (classPipeline == null) {
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
            classPipeline = new StanfordCoreNLP(props);
        }
        return classPipeline;
    }

    /**
     * Lemmatize and tokenize the given request and return the tokens in a String list.
     * Stop-words are removed from the list.
     * 
     * @param request   The text that will be lemmatized and tokenized
     * @return          The given request as a tokenized and lemmatized list, without any
     *                  stop-words.
     */
    protected List<String> getLemmatizedTokens(String request) {
        
        List<String> tokenList = new ArrayList<>();

        StanfordCoreNLP pipeline = Index.getPipeline();

        CoreDocument document = pipeline.processToCoreDocument(request);
        for (CoreLabel tok : document.tokens()) {
            tokenList.add(tok.lemma().toLowerCase());
        }

        StopWords.deleteStopWords(tokenList);
        return tokenList;
    }

    /**
     * Check whether a given list of String elements is made up of single tokens.
     * 
     * @param list      A list that will be analyzed to check if it is tokenized.
     * @return          True, if each entry in the list only contains a single token. False, otherwise.
     */
    protected boolean isTokenized(String[] list) {

        String regex = "[,\\.\\s]";        
        for (String element : list) {
            String[] myArray = element.split(regex);
            if (myArray.length != 1) return false;
        }
        return true;
    }

    /**
     * Lemmatize and tokenize the given search request, then searche for
     * these tokens in the index. Return a list containing all relevant URLs,
     * sorted according to their relevance.
     * 
     * @param searchRequest     The query to be processed.
     * @return                  A list of objects of a subclass of UrlData containing all
     *                          URLs corresponding to the query, sorted according to their relevance.
     */
    public List<T> searchQuery(String searchRequest) {

       
        List<String> searchRequestTokens = getLemmatizedTokens(searchRequest);

        String[] query = searchRequestTokens.toArray(new String[searchRequestTokens.size()]);
        return searchLemmatizedArray(query);
    }

    /**
     * Search through all tokens stored in the index for lemmatized tokens in the 
     * query and return all sorted URLs according to their relevance in a list.
     * 
     * @param tokLemmaQuery     The lemmatized and tokenized search request.
     * @return          A list of objects of a subclass of UrlData containing all URLs
     *                  corresponding to the query, sorted according to their relevance.
     */
    protected abstract List<T> searchLemmatizedArray(String[] tokLemmaQuery);
}
