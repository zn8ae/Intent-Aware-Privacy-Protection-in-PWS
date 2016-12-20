package edu.virginia.cs.engine;

import edu.virginia.cs.utility.SpecialAnalyzer;
import edu.virginia.cs.user.Profile;
import edu.virginia.cs.utility.SortMap;
import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

public class Searcher {

    private IndexSearcher indexSearcher;
    private final SpecialAnalyzer analyzer;
    private static SimpleHTMLFormatter formatter;

    /* User profile which is constructed and maintained in the server side */
    private Profile userProfile;
    /* Flag to turn on or off personalization */
    private boolean activatePersonalization = false;

    /**
     * Sets up the Lucene index Searcher with the specified index.
     *
     * @param indexPath The path to the desired Lucene index.
     */
    public Searcher(String indexPath) {
        analyzer = new SpecialAnalyzer(true, true);
        try {
            IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
            indexSearcher = new IndexSearcher(reader);
            formatter = new SimpleHTMLFormatter("****", "****");
        } catch (IOException exception) {
            Logger.getLogger(Searcher.class.getName()).log(Level.SEVERE, null, exception);
        }
    }

    /**
     * Activate or deactivate personalization.
     *
     * @param flag
     */
    public void activatePersonalization(boolean flag) {
        activatePersonalization = flag;
    }

    /**
     * Initialize the user profile maintained by the server side.
     *
     */
    public void initializeUserProfile() {
        userProfile = new Profile(1);
    }

    /**
     * Update user profile based on the clicked document content.
     *
     * @param content
     * @throws java.io.IOException
     */
    public void updateUProfileUsingClickedDocument(String content) throws IOException {
        userProfile.updateUserProfileUsingClickedDocument(content);
    }

    /**
     * Return user profile maintained by the server side.
     *
     * @return user profile
     */
    public Profile getUserProfile() {
        return userProfile;
    }

    /**
     * Sets ranking function for index searching.
     *
     * @param sim
     */
    public void setSimilarity(Similarity sim) {
        indexSearcher.setSimilarity(sim);
    }

    /**
     * The main search function.
     *
     * @param searchQuery Set this object's attributes as needed.
     * @return
     */
    public SearchResult search(SearchQuery searchQuery) {
        BooleanQuery combinedQuery = new BooleanQuery();
        for (String field : searchQuery.fields()) {
            QueryParser parser = new QueryParser(Version.LUCENE_46, field, analyzer);
            try {
                Query textQuery = parser.parse(QueryParser.escape(searchQuery.queryText()));
                combinedQuery.add(textQuery, BooleanClause.Occur.MUST);
            } catch (ParseException exception) {
                Logger.getLogger(Searcher.class.getName()).log(Level.SEVERE, null, exception);
            }
        }
        return runSearch(combinedQuery, searchQuery);
    }

    /**
     * The simplest search function. Searches the abstract field and returns a
     * the default number of results.
     *
     * @param queryText The text to search
     * @return the SearchResult
     */
    public SearchResult search(String queryText) {
        return search(new SearchQuery(queryText, defaultField));
    }

    /**
     * Searches for a document content in the index.
     *
     * @param queryText the document title, a URL
     * @param field
     * @return clicked document content
     */
    public String search(String queryText, String field) {
        return runSearch(new SearchQuery(queryText, field), "content");
    }

    /**
     * Performs the actual Lucene search.
     *
     * @param luceneQuery
     * @param numResults
     * @return the SearchResult
     */
    private SearchResult runSearch(Query luceneQuery, SearchQuery searchQuery) {
        try {
            TopDocs docs = indexSearcher.search(luceneQuery, searchQuery.fromDoc() + searchQuery.numResults());
            ScoreDoc[] hits;
            String field = searchQuery.fields().get(0);
            if (activatePersonalization) {
                ScoreDoc[] relDocs = docs.scoreDocs;

                /* Store return document with their personalized score */
                HashMap<String, Float> mapDocToScore = new HashMap<>();
                /* Unique terms found in the returned document */
                HashMap<String, Integer> uniqueDocTerms;
                /* Fetching server side user profile for personalization */
                HashMap<String, Integer> uProf = userProfile.getUserProfile();
                StringTokenizer st = new StringTokenizer(true, true);

//                System.out.println("Personalizing....");
                for (int i = 0; i < relDocs.length; i++) {
                    Document doc = indexSearcher.doc(relDocs[i].doc);
                    /**
                     * Extract the unique tokens from a relevant document
                     * returned by the lucene index searcher.
                     */
                    uniqueDocTerms = new HashMap<>();
                    List<String> tokens = st.TokenizeString(doc.getField(field).stringValue());
                    // computing term frequency of all the unique terms found in the document
                    for (String tok : tokens) {
                        if (uniqueDocTerms.containsKey(tok)) {
                            uniqueDocTerms.put(tok, uniqueDocTerms.get(tok) + 1);
                        } else {
                            uniqueDocTerms.put(tok, 1);
                        }
                    }

                    /* Score after personalizing result */
                    float score = 0;
                    /* Smoothing paramter for linear interpolation */
                    float lambda = 0.1f;

                    /**
                     * Computing score for a returned document based on user
                     * profile maintained by the server side.
                     */
                    int count_for_normalization = 0;
                    for (String term : userProfile.getUserProfile().keySet()) {
                        if (!uniqueDocTerms.containsKey(term)) {
                            continue;
                        }
                        int value = userProfile.getUserProfile().get(term);
                        count_for_normalization += value;
                        Float tokenProb = (uniqueDocTerms.get(term) * 1.0f) / tokens.size();
                        Float refProb = userProfile.getReferenceModel().get(term);
                        if (refProb == null) {
                            refProb = 0.0f;
                        }
                        /* Smoothing using linear interpolation */
                        Float smoothedTokenProb = (1 - lambda) * tokenProb + lambda * refProb;
                        smoothedTokenProb = smoothedTokenProb / (lambda * refProb);
                        score = score + (value * (float) Math.log(smoothedTokenProb));
                    }
                    if (count_for_normalization != 0) {
                        score = score / count_for_normalization;
                    }
//                    Document d = indexSearcher.doc(relDocs[i].doc);
//                    System.out.println(d.getField("clicked_url").stringValue() + " -> " + relDocs[i].score + " + " + score);
                    mapDocToScore.put(String.valueOf(i), score);
                }

                /**
                 * Re-ranking for personalization using server side user
                 * profile.
                 */
                Map<String, Float> tempMap = sortByComparator(mapDocToScore, false);

                /**
                 * Computing score for documents through a ranking aggregation
                 * method called Borda's method.
                 */
                int i = 0;
                for (Map.Entry<String, Float> entry : tempMap.entrySet()) {
                    float score = 0;
                    // Giving 50% weight to personalization and 50% to OkapiBM25.
                    score = 0.5f * (1.0f / (i + 1)) + 0.5f * (1.0f / (Integer.parseInt(entry.getKey() + 1)));
                    mapDocToScore.put(entry.getKey(), score);
                    /**
                     * Storing the final score of documents computed through
                     * Borda's method.
                     */
                    relDocs[Integer.parseInt(entry.getKey())].score = score;
                    i++;
                }

                /**
                 * Final re-ranking through ranking aggregation.
                 */
                Map<String, Float> resultedMap = SortMap.sortMapByValue(mapDocToScore, false);
                i = 0;
                hits = new ScoreDoc[relDocs.length];
                for (Map.Entry<String, Float> entry : resultedMap.entrySet()) {
                    hits[i] = relDocs[Integer.parseInt(entry.getKey())];
                    i++;
                }
                /* Updating the server side user profile with the query text */
                userProfile.updateUserProfile(searchQuery.queryText());
            } else {
                hits = docs.scoreDocs;
            }

            SearchResult searchResult = new SearchResult(searchQuery, docs.totalHits);
            for (ScoreDoc hit : hits) {
                Document doc = indexSearcher.doc(hit.doc);
                ResultDoc rdoc = new ResultDoc(hit.doc);
                String highlighted;
                try {
                    Highlighter highlighter = new Highlighter(formatter, new QueryScorer(luceneQuery));
                    rdoc.title("" + (hit.doc + 1));
                    String contents = doc.getField(field).stringValue();
                    String contentsJudge = doc.getField("clicked_url").stringValue();
                    rdoc.content(contents);
                    rdoc.url(contentsJudge);
                    String[] snippets = highlighter.getBestFragments(analyzer, field, contents, numFragments);
                    highlighted = createOneSnippet(snippets);
                } catch (InvalidTokenOffsetsException exception) {
                    exception.printStackTrace();
                    highlighted = "(no snippets yet)";
                }
                searchResult.addResult(rdoc);
                searchResult.setSnippet(rdoc, highlighted);
            }
            searchResult.trimResults(searchQuery.fromDoc());
            return searchResult;
        } catch (IOException exception) {
            Logger.getLogger(Searcher.class.getName()).log(Level.SEVERE, null, exception);
        }
        return new SearchResult(searchQuery);
    }

    /**
     * Searches for document content in the lucene index.
     *
     * @param searchQuery a clicked URL
     * @param indexableField content of this field needs to be returned
     * @return clicked document content
     */
    private String runSearch(SearchQuery searchQuery, String indexableField) {
        Query luceneQuery = new TermQuery(new Term(searchQuery.fields().get(0), searchQuery.queryText()));
        String returnedResult = null;
        try {
            TopDocs docs = indexSearcher.search(luceneQuery, 1);
            ScoreDoc[] hits = docs.scoreDocs;
            Document doc = indexSearcher.doc(hits[0].doc);
            returnedResult = doc.getField(indexableField).stringValue();
        } catch (IOException exception) {
            Logger.getLogger(Searcher.class.getName()).log(Level.SEVERE, null, exception);
        }

        return returnedResult;
    }

    /**
     * Create one string of all the extracted snippets from the highlighter
     *
     * @param snippets
     * @return
     */
    private String createOneSnippet(String[] snippets) {
        String result = " ... ";
        for (String s : snippets) {
            result += s + " ... ";
        }
        return result;
    }

}
