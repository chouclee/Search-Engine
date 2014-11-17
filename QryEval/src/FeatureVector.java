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
  private int queryID;
  private int queryLength;
  private Hashtable<String, Integer> termTable;
  private RetrievalModelLearnToRank model;
  private float k_1;
  private float b;
  private float k_3;
  private float mu;
  private float lambda;
  private ArrayList<Integer> relList;
  private ArrayList<String> externalIDList;

  public FeatureVector(RetrievalModel r, int queryID, String query, 
          Map<String, Double> pageRank) throws IOException {
    this(r, queryID, query, pageRank, "");
  }
  
  public FeatureVector(RetrievalModel r, int queryID, String query, 
          Map<String, Double> pageRank,
          String featureDisable) throws IOException {
    this.model = (RetrievalModelLearnToRank)r;
    
    // some constant parameters
    k_1 = model.getParameter("k_1");
    b = model.getParameter("b");
    k_3 = model.getParameter("k_3");
    mu = model.getParameter("mu");
    lambda = model.getParameter("lambda");
    
    
    
    this.query = query;
    this.queryID = queryID;
    termTable = new Hashtable<String, Integer>();
    String[] terms = query.split("\\s+");
    queryLength = terms.length;
    for (String term : terms) {
      if (!termTable.containsKey(term))
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
    
    relList = new ArrayList<Integer>();
    externalIDList = new ArrayList<String>();
   
  }
  
  public void addDocID(RetrievalModel r, String externalID, int rel) throws Exception {
    int docid = QryEval.getInternalDocid(externalID);
    Document d = QryEval.READER.document(docid);
    
    if (externalID.equals("clueweb09-en0000-01-21462")) {
      System.out.println("found");
    }
    
    relList.add(rel);
    externalIDList.add(externalID);
    
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
      //if (wikiScore == 0)
      //  System.out.println(rawUrl);
      features.get(2).add(wikiScore);
    }
      
    //f4: PageRank score for d (read from file).
    if (!featureDisable[3]) {
      try {
        double pageRankScore = pageRank.get(externalID);
        features.get(3).add(pageRankScore);
      }
      catch (Exception e) {
        features.get(3).add(0.0);
        System.err.println("ExternalID: " + externalID);
      }
    }
    
    //---------------Body-----------------//
    TermVector termVec = null;
    if (!featureDisable[4] || !featureDisable[5] || !featureDisable[6]) {
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
    if (!featureDisable[7] || !featureDisable[8] || !featureDisable[9]) {
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
    if (!featureDisable[10] || !featureDisable[11] || !featureDisable[12]) {
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
    if (!featureDisable[13] || !featureDisable[14] || !featureDisable[15]) {
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
    if (!featureDisable[16]) {
      features.get(16).add(0.0);
    }
    //f18: A custom feature - use your imagination.
    if (!featureDisable[17]) {
      features.get(17).add(0.0);
    }

  }
  
  private double overlap(TermVector termVec, String Query) {
    int count = 0;
    String stemString;
    for (int i = 1; i < termVec.stemsLength(); i++) {
      stemString = termVec.stemString(i);
      //if (stemString == null || stemString == "") // null or empty string, continue to next term
      //  continue;
      if (termTable.containsKey(stemString))
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
  private static int getWikiScore(String rawUrl) {
    if (rawUrl == null || rawUrl.length() == 0)
      return 0;
    return rawUrl.toLowerCase().contains("wikipedia.org") ? 1: 0;
    //return rawUrl.matches("(?i).*wikipedia\\.org.*") ? 1 : 0;
  }
  
  private double BM25Evaluation(TermVector termVec, 
          String field, int docid) throws Exception {
    double totalBM25Score = 0.0;
    if (docid == 263887) {
      int a = 1;
    }
    float avgDocLen = this.model.avgDocLenMap.get(field);
    
    //RetrievalModelLearnToRank model = (RetrievalModelLearnToRank)r;
    long docLen = QryEval.docLenStore.getDocLength(field, docid);
    String stemString = null;
    int docFreq, tf;
    float RSJWeight, tfWeight, userWeight;
    for (int i = 1; i < termVec.stemsLength(); i++) {
      stemString = termVec.stemString(i);
      //if (stemString == null || stemString == "") // null or empty string, continue to next term
      //  continue;
      if (termTable.containsKey(stemString)) {
        docFreq = termVec.stemDf(i);
        
        // RSJ weight
        RSJWeight = (float) Math.log((this.model.N - docFreq + 0.5) / (docFreq + 0.5));
        
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

    long collectionLength = this.model.collectionLengthMap.get(field);
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
      if (!model.ctfMap.containsKey(stemString)) {
        collectionTermFreq = termVec.totalStemFreq(i); // ctf
        model.ctfMap.put(stemString, collectionTermFreq);
      }
      else {
        collectionTermFreq = model.ctfMap.get(stemString);
      }
      maxLikeliEstim = (float) collectionTermFreq / collectionLength; // P_MLE
      if (termTable.containsKey(stemString)) {
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
  
  public void normalize() {
    for (int i = 0; i < featureSize; i++) {
      if (!featureDisable[i]) 
        normalize(i);
    }
  }
  
  private void normalize(int featureIdx) {
    ArrayList<Double> feature = features.get(featureIdx);
    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;
    for (int i = 0; i < feature.size(); i++) {
      if (feature.get(i) > max)
        max = feature.get(i);
      else if (feature.get(i) < min)
        min = feature.get(i);
    }
    if (max == 0 && min == 0) {
      return;
    }
    if (max == 1 && min == 0)
      return;
    if (max == min) {
      for (int i = 0; i < feature.size(); i++) {
        feature.set(i, 0.0);
      }
      return;
    }
    for (int i = 0; i < feature.size(); i++) {
      feature.set(i, (feature.get(i) - min) / (max - min));
    } 
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < externalIDList.size(); i++) {
      sb.append(relList.get(i) + " ");
      sb.append("qid:"+ queryID + " ");
      for (int j = 0; j < featureSize; j++) {
        if (!featureDisable[j]) {
          sb.append((j+1)+":"+features.get(j).get(i)+" ");
        }
      }
      sb.append("# " + externalIDList.get(i) + "\n");
    }
    return sb.toString();
  }
}
