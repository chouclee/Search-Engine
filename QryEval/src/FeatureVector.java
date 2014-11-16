import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import org.apache.lucene.document.Document;


public class FeatureVector {
  private boolean[] featureDisable;    
  private final int featureSize;
  private Map<String, Double> pageRank;
  private ArrayList<ArrayList<Double>> features;
  private ArrayList<Integer> docidList;
  private String query;
  private int queryLength;
  private Hashtable<String, Integer> termTable;
  private RetrievalModelLearnToRank model;
  private float k_1;
  private float b;
  private float k_3;
  private int N;
  private float[] avgDocLen;
  private float mu;
  private float lambda;
  private long[] collectionLength;



  
  public FeatureVector(RetrievalModel r, String query, 
          Map<String, Double> pageRank) throws IOException {
    this(r, query, pageRank, "");
  }
  
  public FeatureVector(RetrievalModel r, String query, 
          Map<String, Double> pageRank,
          String featureDisable) throws IOException {
    this.model = (RetrievalModelLearnToRank)r;
    
    // some constant parameters
    k_1 = model.getParameter("k_1");
    b = model.getParameter("b");
    k_3 = model.getParameter("k_3");
    N = QryEval.READER.numDocs(); // total number of documents
    
    avgDocLen = new float[4];
    
    avgDocLen[0] = (float) QryEval.READER.getSumTotalTermFreq("body")
            / QryEval.READER.getDocCount("body"); // average doc length
    avgDocLen[1] = (float) QryEval.READER.getSumTotalTermFreq("title")
            / QryEval.READER.getDocCount("title"); // average doc length
    avgDocLen[2] = (float) QryEval.READER.getSumTotalTermFreq("url")
            / QryEval.READER.getDocCount("url"); // average doc length
    avgDocLen[3] = (float) QryEval.READER.getSumTotalTermFreq("inlink")
            / QryEval.READER.getDocCount("inlink"); // average doc length
    
    mu = model.getParameter("mu");
    lambda = model.getParameter("lambda");
    collectionLength = new long[4];
    collectionLength[0] = QryEval.READER.getSumTotalTermFreq("body");
    collectionLength[1] = QryEval.READER.getSumTotalTermFreq("title");
    collectionLength[2] = QryEval.READER.getSumTotalTermFreq("url");
    collectionLength[3] = QryEval.READER.getSumTotalTermFreq("inlink");
    
    
    
    
    this.query = query;
    termTable = new Hashtable<String, Integer>();
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
  
  public void addDocID(RetrievalModel r, String externalID) throws Exception {
    int docid = QryEval.getInternalDocid(externalID);
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
      try {
        double pageRankScore = pageRank.get(externalID);
        features.get(3).add(pageRankScore);
      }
      catch (Exception e) {
        System.err.println("ExternalID: " + externalID);
      }
    }
    
    //---------------Body-----------------//
    TermVector termVec = null;
    if (!!featureDisable[4] || !featureDisable[5] || !featureDisable[5]) {
      try {
        termVec = new TermVector(docid, "body");
      }
      catch (Exception e) {
        termVec = null;
      }
    }
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
    if (!!featureDisable[7] || !featureDisable[8] || !featureDisable[9]) {
      try {
        termVec = new TermVector(docid, "title");
      }
      catch (Exception e) {
        termVec = null;
      }
    }
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
    if (!!featureDisable[10] || !featureDisable[11] || !featureDisable[12]) {
      try {
        termVec = new TermVector(docid, "url");
      }
      catch (Exception e) {
        termVec = null;
      }
    }
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
    if (!!featureDisable[13] || !featureDisable[14] || !featureDisable[15]) {
      try {
        termVec = new TermVector(docid, "inlink");
      }
      catch (Exception e) {
        termVec = null;
      }
    }
    //f14: BM25 score for <q, dinlink>.
    if (!featureDisable[13]) {
      if (termVec != null) {
        double bm25Inlink = BM25Evaluation(termVec, "inlink", docid);
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
    String stemString;
    for (int i = 1; i < termVec.stems.length; i++) {
      stemString = termVec.stemString(i);
      //if (stemString == null || stemString == "") // null or empty string, continue to next term
      //  continue;
      if (termTable.contains(stemString))
        count += termTable.get(stemString);
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
    
    float avgDocLen = 0;
    if (field.equals("body"))
      avgDocLen = this.avgDocLen[0];
    else if (field.equals("title"))
      avgDocLen = this.avgDocLen[1];
    else if (field.equals("url"))
      avgDocLen = this.avgDocLen[2];
    else
      avgDocLen = this.avgDocLen[3];
    
    //RetrievalModelLearnToRank model = (RetrievalModelLearnToRank)r;
    long docLen = QryEval.docLenStore.getDocLength(field, docid);
    String stemString = null;
    int docFreq, tf;
    float RSJWeight, tfWeight, userWeight;
    for (int i = 1; i < termVec.stemsLength(); i++) {
      stemString = termVec.stemString(i);
      //if (stemString == null || stemString == "") // null or empty string, continue to next term
      //  continue;
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

    long collectionLength;
    // some constant parameters
    if (field.equals("body"))
      collectionLength = this.collectionLength[0];
    else if (field.equals("title"))
      collectionLength = this.collectionLength[1];
    else if (field.equals("url"))
      collectionLength = this.collectionLength[2];
    else
      collectionLength = this.collectionLength[3];
    //long collectionLength = QryEval.READER.getSumTotalTermFreq(field);
    long docLen = QryEval.docLenStore.getDocLength(field, docid);
    
    long collectionTermFreq;
    String stemString;
    float maxLikeliEstim;
    int tf;
    for (int i = 1; i < termVec.stemsLength(); i++) {
      stemString = termVec.stemString(i);
      //if (stemString == null || stemString == "") // null or empty string, continue to next term
      //  continue;
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
  
  private void normalize() {
    
  }
  
  private double[] findMinMax(int featureIdx) {
   return null;  
  }
}
