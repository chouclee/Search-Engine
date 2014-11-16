import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import org.apache.lucene.document.Document;


public class FeatureVector {
  private boolean[] featureDisable;    
  private final int featureSize;
  private Map<Integer, Double> pageRank;
  private ArrayList<ArrayList<Double>> features;
  private ArrayList<Integer> docidList;
  private String query;
  private int queryLength;
  private Hashtable<String, Integer> termTable;
  private RetrievalModelLearnToRank model;
  
  public FeatureVector(RetrievalModel r, String query, Map<Integer, Double> pageRank) {
    this(r, query, pageRank, "");
  }
  
  public FeatureVector(RetrievalModel r, String query, Map<Integer, Double> pageRank,
          String featureDisable) {
    this.model = (RetrievalModelLearnToRank)r;
    
    this.query = query;
    Hashtable<String, Integer> termTable = new Hashtable<String, Integer>();
    String[] terms = query.split("\\s+");
    queryLength = terms.length;
    for (String term : terms) {
      if (!termTable.contains(term))
        termTable.put(term, 1);
      else
        termTable.put(term, termTable.get(term) + 1); // in case there are duplicate terms
    }
    
    featureSize = 18;
    this.featureDisable = new boolean[featureSize];
    
    features = new ArrayList<ArrayList<Double>>(featureSize);
    for (int i = 0; i < featureSize; i++) {
      features.add(new ArrayList<Double>());
    }
    if (featureDisable !=null && !featureDisable.equals("")) {
      String[] disabled = featureDisable.split("\\s+");
      for (String str : disabled)
        this.featureDisable[Integer.parseInt(str.trim()) - 1] = true;
    }
    
    docidList = new ArrayList<Integer>();
    
    this.pageRank = pageRank;
   
  }
  
  public void addDocID(RetrievalModel r, int docid) throws Exception {
    Document d = QryEval.READER.document(docid);
    
    //f1: Spam score for d (read from index).
    if (!featureDisable[0]) {
      double spamScore = Integer.parseInt(d.get("score"));
      features.get(0).add(spamScore);
    }
    
    //f2: Url depth for d(number of '/' in the rawUrl field).
    String rawUrl = d.get("rawUrl");
    if (!featureDisable[1]) {
      double urlDepth = getUrlDepth(rawUrl);
      features.get(1).add(urlDepth);
    }
      
    //f3: FromWikipedia score for d (1 if the rawUrl contains "wikipedia.org", otherwise 0).
    if (!featureDisable[2]) {
      double wikiScore = getWikiScore(rawUrl);
      features.get(2).add(wikiScore);
    }
      
    //f4: PageRank score for d (read from file).
    if (!featureDisable[3]) {
      double pageRankScore = pageRank.get(docid);
      features.get(3).add(pageRankScore);
    }
    
    //---------------Body-----------------//
    TermVector termVec = null;
    if (!!featureDisable[4] || !featureDisable[5] || !featureDisable[5])
      termVec = new TermVector(docid, "body");
    /*if (termVec == null) {
      // field doesn't exist!
      System.err.println("Doc missing field: " + docid + " " + "body");
      System.exit(1);
    }*/
    //f5: BM25 score for <q, dbody>.
    if (!featureDisable[4]) {
      if (termVec != null) {
        double bm25Body = BM25Evaluation(termVec, "body", docid);
        features.get(4).add(bm25Body);
      }
      else features.get(4).add(0.0);
    }
    //f6: Indri score for <q, dbody>.
    if (!featureDisable[5]) {
      if (termVec != null) {
        double indriBody = IndriEvaluation(termVec, "body", docid);
        features.get(5).add(indriBody);
      }
      else features.get(5).add(0.0);
    }
    
    //f7: Term overlap score for <q, dbody>.
    if (!featureDisable[6]) {
      if (termVec != null) {      
        double overlapBody = overlap(termVec, query);
        features.get(6).add(overlapBody);
      }
      else features.get(6).add(0.0);
    }
    
    //---------------Title------------------//
    if (!!featureDisable[7] || !featureDisable[8] || !featureDisable[9])
      termVec = new TermVector(docid, "title");
    //f8: BM25 score for <q, dtitle>.
    if (!featureDisable[7]) {
      if (termVec != null) {
        double bm25Title = BM25Evaluation(termVec, "title", docid);
        features.get(7).add(bm25Title);
      }
      else features.get(7).add(0.0);
    }
    //f9: Indri score for <q, dtitle>.
    if (!featureDisable[8]) {
      if (termVec != null) {
        double indriTitle = IndriEvaluation(termVec, "title", docid);
        features.get(8).add(indriTitle);
      }
      else features.get(8).add(0.0);
    }
    //f10: Term overlap score for <q, dtitle>.
    if (!featureDisable[9]) {
      if (termVec != null) {
        double overlapTitle = overlap(termVec, query);
        features.get(9).add(overlapTitle);
      }
      else features.get(9).add(0.0);
    }
    
    //---------------URL------------------//
    if (!!featureDisable[10] || !featureDisable[11] || !featureDisable[12])
      termVec = new TermVector(docid, "url");
    //f11: BM25 score for <q, durl>.
    if (!featureDisable[10]) {
      if (termVec != null) {
        double bm25Url = BM25Evaluation(termVec, "url", docid);
        features.get(10).add(bm25Url);
      }
      else features.get(10).add(0.0);
    }
    //f12: Indri score for <q, durl>.
    if (!featureDisable[11]) {
      if (termVec != null) {
        double indriUrl = IndriEvaluation(termVec, "url", docid);
        features.get(11).add(indriUrl);
      }
      else features.get(11).add(0.0);
    }
    //f13: Term overlap score for <q, durl>.
    if (!featureDisable[12]) {
      if (termVec != null) {
        double overlapUrl = overlap(termVec, query);
        features.get(12).add(overlapUrl);
      }
      else features.get(12).add(0.0);
    }
    
    //-------------Inlink------------------//
    if (!!featureDisable[13] || !featureDisable[14] || !featureDisable[15])
      termVec = new TermVector(docid, "inlink");
    //f14: BM25 score for <q, dinlink>.
    if (!featureDisable[13]) {
      if (termVec != null) {
        double bm25Inlink = BM25Evaluation(termVec, "inlink", docid);;
        features.get(13).add(bm25Inlink);
      }
      else features.get(13).add(0.0);
    }
    //f15: Indri score for <q, dinlink>.
    if (!featureDisable[14]) {
      if (termVec != null) {
        double indriInlink = IndriEvaluation(termVec, "inlink", docid);
        features.get(14).add(indriInlink);
      }
      else features.get(14).add(0.0);
    }
    //f16: Term overlap score for <q, dinlink>.
    if (!featureDisable[15]) {
      if (termVec != null) {
        double overlapInlink = overlap(termVec, query);
        features.get(15).add(overlapInlink);
      }
      else features.get(15).add(0.0);
    }
    
    //f17: A custom feature - use your imagination.
    //f18: A custom feature - use your imagination.

  }
  
  private double overlap(TermVector termVec, String Query) {
    int count = 0;
    for (int i = 0; i < termVec.stems.length; i++) {
      if (termTable.contains(termVec.stems[i]))
        count += termTable.get(termVec.stems[i]);
    }
    return (double)count / queryLength;
  }
  
    
  private int getUrlDepth(String rawUrl) {
    if (rawUrl == null || rawUrl.length() == 0)
      return 0;
    return rawUrl.split("/").length - 1;
  }
  
  //FromWikipedia score for d (1 if the rawUrl contains "wikipedia.org", otherwise 0).
  private int getWikiScore(String rawUrl) {
    if (rawUrl == null || rawUrl.length() == 0)
      return 0;
    return rawUrl.contains("wikipedia.org") ? 1 : 0;
  }
  
  private double BM25Evaluation(TermVector termVec, 
          String field, int docid) throws Exception {
    double totalBM25Score = 0.0;
    //RetrievalModelLearnToRank model = (RetrievalModelLearnToRank)r;
    float k_1 = model.getParameter("k_1");
    float b = model.getParameter("b");
    float k_3 = model.getParameter("k_3");

    // some constant parameters
    int N = QryEval.READER.numDocs(); // total number of documents
    float avgDocLen = (float) QryEval.READER.getSumTotalTermFreq(field)
            / QryEval.READER.getDocCount(field); // average doc length
    long docLen = QryEval.docLenStore.getDocLength(field, docid);
    
    String stemString = null;
    int docFreq, tf;
    float RSJWeight, tfWeight, userWeight;
    for (int i = 0; i < termVec.stemsLength(); i++) {
      stemString = termVec.stemString(i);
      if (stemString == null || stemString == "") // null or empty string, continue to next term
        continue;
      if (termTable.contains(stemString)) {
        docFreq = termVec.stemDf(i);
        
        // RSJ weight
        RSJWeight = (float) Math.log((N - docFreq + 0.5) / (docFreq + 0.5));
        
        tf = termVec.stemFreq(i);
        // tf Weight
        tfWeight = tf / (tf + k_1 * (1 - b + b * docLen / avgDocLen));
        
        // user Weight
        userWeight = (k_3 + 1) * termTable.get(stemString) / (k_3 + termTable.get(stemString));
        
        totalBM25Score += RSJWeight * tfWeight * userWeight;
      }
    }
    return totalBM25Score;
  }
  
  private double IndriEvaluation(TermVector termVec, 
          String field, int docid) throws Exception {
    double totalIndriScore = 1.0;
    //RetrievalModelLearnToRank model = (RetrievalModelLearnToRank)r;
    float mu = model.getParameter("mu");
    float lambda = model.getParameter("lambda");

    // some constant parameters
    long collectionLength = QryEval.READER.getSumTotalTermFreq(field);
    long docLen = QryEval.docLenStore.getDocLength(field, docid);
    
    long collectionTermFreq;
    String stemString;
    float maxLikeliEstim;
    int tf;
    for (int i = 0; i < termVec.stemsLength(); i++) {
      stemString = termVec.stemString(i);
      if (stemString == null || stemString == "") // null or empty string, continue to next term
        continue;
      collectionTermFreq = termVec.totalStemFreq(i); // ctf
      maxLikeliEstim = (float) collectionTermFreq / collectionLength; // P_MLE
      if (termTable.contains(stemString)) {
        tf = termVec.stemFreq(i);
        totalIndriScore *= Math.pow(lambda * (tf + mu * maxLikeliEstim) / (docLen + mu)
                + (1 - lambda) * maxLikeliEstim, (double)termTable.get(stemString)/queryLength);
      }
      else {
        totalIndriScore *= Math.pow(lambda * mu * maxLikeliEstim / (docLen + mu)
                + (1 - lambda) * maxLikeliEstim, 1.0 / queryLength);
      }
    }
    
    return totalIndriScore;
  }
}
