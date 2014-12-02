import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;

public class FeatureVector {
  private boolean[] featureDisable;

  private final int featureSize;

  private Map<String, Double> pageRank;

  private ArrayList<ArrayList<Double>> features;

  // private ArrayList<Integer> docidList;
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

  public FeatureVector(RetrievalModel r, int queryID, String query, Map<String, Double> pageRank)
          throws IOException {
    this(r, queryID, query, pageRank, "");
  }

  public FeatureVector(RetrievalModel r, int queryID, String query, Map<String, Double> pageRank,
          String featureDisable) throws IOException {
    this.model = (RetrievalModelLearnToRank) r;

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
    if (featureDisable != null && !featureDisable.equals("")) {
      String[] disabled = featureDisable.split(",");
      for (String str : disabled)
        this.featureDisable[Integer.parseInt(str.trim()) - 1] = true;
    }

    // docidList = new ArrayList<Integer>();

    this.pageRank = pageRank;

    relList = new ArrayList<Integer>();
    externalIDList = new ArrayList<String>();

  }

  public void addDocID(RetrievalModel r, String externalID, int rel) throws Exception {
    int docid = QryEval.getInternalDocid(externalID);
    Document d = QryEval.READER.document(docid);

    /*if (externalID.equals("clueweb09-en0009-28-27170")) {
      // if (externalID.equals("clueweb09-en0009-15-36339")) {
      System.out.println("found");
    }*/

    relList.add(rel);
    externalIDList.add(externalID);

    // f1: Spam score for d (read from index).
    if (!featureDisable[0]) {
      double spamScore = Integer.parseInt(d.get("score"));
      features.get(0).add(spamScore);
    }

    // f2: Url depth for d(number of '/' in the rawUrl field).
    String rawUrl = d.get("rawUrl");
    if (!featureDisable[1]) {
      double urlDepth = getUrlDepth(rawUrl);
      features.get(1).add(urlDepth);
    }

    // f3: FromWikipedia score for d (1 if the rawUrl contains "wikipedia.org", otherwise 0).
    if (!featureDisable[2]) {
      //System.out.println(rawUrl);
      double wikiScore = getWikiScore(rawUrl);
      features.get(2).add(wikiScore);
    }

    // f4: PageRank score for d (read from file).
    if (!featureDisable[3]) {
      try {
        double pageRankScore = pageRank.get(externalID);
        features.get(3).add(pageRankScore);
      } catch (Exception e) {
        features.get(3).add(Double.NaN);
        // System.err.println("ExternalID: " + externalID);
      }
    }

    // ---------------Body-----------------//
    TermVector termVec = null;
    if (!featureDisable[4] || !featureDisable[5] || !featureDisable[6]) {
      try {
        termVec = new TermVector(docid, "body");
      } catch (Exception e) {
        termVec = null;
      }
    }

    // f5: BM25 score for <q, dbody>.
    if (!featureDisable[4]) {
      if (termVec != null) {
        double bm25Body = BM25Evaluation(termVec, "body", docid);
        features.get(4).add(bm25Body);
      } else
        features.get(4).add(Double.NaN);
    }
    // f6: Indri score for <q, dbody>.
    if (!featureDisable[5]) {
      if (termVec != null) {
        double indriBody = IndriEvaluation(termVec, "body", docid);
        features.get(5).add(indriBody);
      } else
        features.get(5).add(Double.NaN);
    }
    
    // f17: A custom feature - use your imagination.
    /*if (!featureDisable[16]) {
      if (termVec != null) {
        double score = tfMulIdf(termVec, query);
        //double score = avgWordLengthSpam(termVec);
        features.get(16).add(score);
      } else
        features.get(16).add(Double.NaN);
    }*/
    
    if (!featureDisable[17]) {
      if (termVec != null) {
        features.get(17).add((double)containsQueryTerm(termVec));
      } else
        features.get(17).add(Double.NaN);
    }

    // f7: Term overlap score for <q, dbody>.
    if (!featureDisable[6]) {
      if (termVec != null) {
        double overlapBody = overlap(termVec, query);
        features.get(6).add(overlapBody);
      } else
        features.get(6).add(Double.NaN);
    }

    // ---------------Title------------------//
    if (!featureDisable[7] || !featureDisable[8] || !featureDisable[9]) {
      try {
        termVec = new TermVector(docid, "title");
      } catch (Exception e) {
        termVec = null;
      }
    }
    // f8: BM25 score for <q, dtitle>.
    if (!featureDisable[7]) {
      if (termVec != null) {
        double bm25Title = BM25Evaluation(termVec, "title", docid);
        features.get(7).add(bm25Title);
      } else
        features.get(7).add(Double.NaN);
    }
    // f9: Indri score for <q, dtitle>.
    if (!featureDisable[8]) {
      if (termVec != null) {
        double indriTitle = IndriEvaluation(termVec, "title", docid);
        features.get(8).add(indriTitle);
      } else
        features.get(8).add(Double.NaN);
    }
    // f10: Term overlap score for <q, dtitle>.
    if (!featureDisable[9]) {
      if (termVec != null) {
        double overlapTitle = overlap(termVec, query);
        features.get(9).add(overlapTitle);
      } else
        features.get(9).add(Double.NaN);
    }
    
    // f17: A custom feature - use your imagination.
   /* if (!featureDisable[16]) {
      if (termVec != null) {
        double score = tfMulIdf(termVec, query);
        //double score = avgWordLengthSpam(termVec);
        ArrayList<Double> originalScore = features.get(16);
        int size = originalScore.size();
        originalScore.set(size- 1, originalScore.get(size - 1) + score);
      }
    }*/
    
    // f17: A custom feature - use your imagination.
    if (!featureDisable[17]) {
      if (termVec != null) {
        ArrayList<Double> originalScore = features.get(17);
        int size = originalScore.size();
        originalScore.set(size- 1, originalScore.get(size - 1) + 
                containsQueryTerm(termVec));
      }
    }

    // ---------------URL------------------//
    if (!featureDisable[10] || !featureDisable[11] || !featureDisable[12]) {
      try {
        termVec = new TermVector(docid, "url");
      } catch (Exception e) {
        termVec = null;
      }
    }
    // f11: BM25 score for <q, durl>.
    if (!featureDisable[10]) {
      if (termVec != null) {
        double bm25Url = BM25Evaluation(termVec, "url", docid);
        features.get(10).add(bm25Url);
      } else
        features.get(10).add(Double.NaN);
    }
    // f12: Indri score for <q, durl>.
    if (!featureDisable[11]) {
      if (termVec != null) {
        double indriUrl = IndriEvaluation(termVec, "url", docid);
        features.get(11).add(indriUrl);
      } else
        features.get(11).add(Double.NaN);
    }
    // f13: Term overlap score for <q, durl>.
    if (!featureDisable[12]) {
      if (termVec != null) {
        double overlapUrl = overlap(termVec, query);
        features.get(12).add(overlapUrl);
      } else
        features.get(12).add(Double.NaN);
    }
    // f17: A custom feature - use your imagination.
    /*if (!featureDisable[16]) {
      if (termVec != null) {
        double score = tfMulIdf(termVec, query);
        //double score = avgWordLengthSpam(termVec);
        ArrayList<Double> originalScore = features.get(16);
        int size = originalScore.size();
        originalScore.set(size- 1, originalScore.get(size - 1) + score);
      }
    }*/
    
    // f18: A custom feature - use your imagination.
    if (!featureDisable[17]) {
      if (termVec != null) {
        ArrayList<Double> originalScore = features.get(17);
        int size = originalScore.size();
        originalScore.set(size- 1, originalScore.get(size - 1) + 
                containsQueryTerm(termVec));
      }
    }

    // -------------Inlink------------------//
    if (!featureDisable[13] || !featureDisable[14] || !featureDisable[15]) {
      try {
        termVec = new TermVector(docid, "inlink");
      } catch (Exception e) {
        termVec = null;
      }
    }
    // f14: BM25 score for <q, dinlink>.
    if (!featureDisable[13]) {
      if (termVec != null) {
        double bm25Inlink = BM25Evaluation(termVec, "inlink", docid);
        features.get(13).add(bm25Inlink);
      } else
        features.get(13).add(Double.NaN);
    }
    // f15: Indri score for <q, dinlink>.
    if (!featureDisable[14]) {
      if (termVec != null) {
        double indriInlink = IndriEvaluation(termVec, "inlink", docid);
        features.get(14).add(indriInlink);
      } else
        features.get(14).add(Double.NaN);
    }
    // f16: Term overlap score for <q, dinlink>.
    if (!featureDisable[15]) {
      if (termVec != null) {
        double overlapInlink = overlap(termVec, query);
        features.get(15).add(overlapInlink);
      } else
        features.get(15).add(Double.NaN);
    }
    // f17: A custom feature - use your imagination.
    /*if (!featureDisable[16]) {
      if (termVec != null) {
        double score = tfMulIdf(termVec, query);
        //double score = avgWordLengthSpam(termVec);
        ArrayList<Double> originalScore = features.get(16);
        int size = originalScore.size();
        originalScore.set(size- 1, originalScore.get(size - 1) + score);
      }
    }*/
    
    // f18: A custom feature - use your imagination.
    if (!featureDisable[17]) {
      if (termVec != null) {
        ArrayList<Double> originalScore = features.get(17);
        int size = originalScore.size();
        originalScore.set(size- 1, originalScore.get(size - 1) + 
                containsQueryTerm(termVec));
      }
    }

  
    // f18: A custom feature - use your imagination.
    if (!featureDisable[16]) {
      features.get(16).add(topLevelDomain(rawUrl));
    }

  }
  
  private double avgWordLengthSpam(TermVector termVec) {
    String stemString;
    long totalLength = 0;
    int num = 0, stemFreq = 0;
    for (int i = 1; i < termVec.stemsLength(); i++) {
      stemString = termVec.stemString(i);
      stemFreq = termVec.stemFreq(i);
      num += stemFreq;
      totalLength += stemString.length() * stemFreq;
    }
    double avgWordLength =  (double)totalLength / num;
    //double avgTermFreq = (double)num / termVec.stemsLength();
    //System.out.println(avgTermFreq);
    //System.out.println(avgWordLength);
    return avgWordLength;
  }
  
  private double topLevelDomain(String url) {
    if (url.contains(".edu"))
      return 1.0;
    if (url.contains(".mil"))
      return 1.0;
    if (url.contains(".org"))
      return 0.95;
    /*if (url.contains(".net"))
      return 0.85;
    /*if (url.contains(".de"))
      return Math.exp(0.82);
    if (url.contains("com"))
      return Math.exp(0.8);
    /*if (url.contains("us"))
      return 0.62;*/
    return 0.0;
  }

  private int containsQueryTerm(TermVector termVec) {
    String stemString;
    for (int i = 1; i < termVec.stemsLength(); i++) {
      stemString = termVec.stemString(i);
      if (termTable.containsKey(stemString)) {
        return 1;
      }
    }
    return 0;
  }
  
  /*private int containsQueryTerm(TermVector termVec) {
    String stemString;
    HashSet<String> set = new HashSet<String>();
    for (int i = 1; i < termVec.stemsLength(); i++) {
      stemString = termVec.stemString(i);
      if (termTable.containsKey(stemString) && !set.contains(stemString)) {
        set.add(stemString);
      }
    }
    return set.size() == termTable.size() ? 1 : 0;
  }*/
  
  private double tfMulIdf(TermVector termVec, String Query) throws Exception {
    double score = 1.0;
    //double score = 0.0;
    String stemString;
    Integer idx;
    HashMap<String, Integer> termFindMap = new HashMap<String, Integer>();
    int maxStemFreq = Integer.MIN_VALUE;
    for (int i = 1; i < termVec.stemsLength(); i++) {
      stemString = termVec.stemString(i);
      if (maxStemFreq < termVec.stemFreq(i))
        maxStemFreq = termVec.stemFreq(i);
      if (termTable.containsKey(stemString))
        termFindMap.put(stemString, i);
    }
    boolean hasOverlap = false;
    for (String stem : termTable.keySet()) {
      idx = termFindMap.get(stem);
      if (idx != null) {
        hasOverlap = true;
      }
      if (hasOverlap)
        break;
    }
    if (hasOverlap) {
      for (int i = 1; i < termVec.stemsLength(); i++) {
        stemString = termVec.stemString(i);
        // if (stemString == null || stemString == "") // null or empty string, continue to next
        // term
        // continue;
        int docFreq, tf;
        if (termTable.containsKey(stemString)) {
          docFreq = termVec.stemDf(i);

          tf = termVec.stemFreq(i);

          //score *= Math.pow((double) tf / docFreq, 1.0 / queryLength);//0.2715
          score += (double)tf/docFreq; // 0.2717
          //score += (Math.log(tf) + 1)*Math.log((double) model.N / docFreq);//0.2699
          //score += ((double)Math.log(tf)/maxStemFreq + 1) * Math.log((double)model.N / docFreq);//0.2694
          //score *=Math.pow((Math.log(tf) + 1)*Math.log((double) model.N / docFreq), 1.0/queryLength);//0.2713
        }
      }
    }
    else score = 0.0;
    return score;
  }

  private double overlap(TermVector termVec, String Query) {
    int count = 0;
    String stemString;
    for (int i = 1; i < termVec.stemsLength(); i++) {
      stemString = termVec.stemString(i);
      // if (stemString == null || stemString == "") // null or empty string, continue to next term
      // continue;
      if (termTable.containsKey(stemString))
        count += termTable.get(stemString);
    }
    return (double) count / queryLength;
  }

  private int getUrlDepth(String rawUrl) {
    if (rawUrl == null || rawUrl.length() == 0)
      return 0;
    String temp = rawUrl.replace("/", "");
    return rawUrl.length() - temp.length();
  }

  // FromWikipedia score for d (1 if the rawUrl contains "wikipedia.org", otherwise 0).
  private static int getWikiScore(String rawUrl) {
    if (rawUrl == null || rawUrl.length() == 0)
      return 0;
    return rawUrl.toLowerCase().contains("wikipedia.org") ? 1 : 0;
    // return rawUrl.matches("(?i).*wikipedia\\.org.*") ? 1 : 0;
  }

  private double BM25Evaluation(TermVector termVec, String field, int docid) throws Exception {
    double totalBM25Score = 0.0;
    float avgDocLen = this.model.avgDocLenMap.get(field);

    // RetrievalModelLearnToRank model = (RetrievalModelLearnToRank)r;
    long docLen = QryEval.docLenStore.getDocLength(field, docid);
    String stemString = null;
    int docFreq, tf;
    double RSJWeight, tfWeight, userWeight;
    for (int i = 1; i < termVec.stemsLength(); i++) {
      stemString = termVec.stemString(i);
      // if (stemString == null || stemString == "") // null or empty string, continue to next term
      // continue;
      if (termTable.containsKey(stemString)) {
        docFreq = termVec.stemDf(i);

        // RSJ weight
        RSJWeight = Math.log((this.model.N - docFreq + 0.5) / (docFreq + 0.5));

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

  private double IndriEvaluation(TermVector termVec, String field, int docid) throws Exception {
    double totalIndriScore = 1.0;
    // RetrievalModelLearnToRank model = (RetrievalModelLearnToRank)r;

    long collectionLength = this.model.collectionLengthMap.get(field);
    // long collectionLength = QryEval.READER.getSumTotalTermFreq(field);
    long docLen = QryEval.docLenStore.getDocLength(field, docid);

    long collectionTermFreq;
    String stemString;
    double maxLikeliEstim;
    int tf;
    Integer idx;
    HashMap<String, Integer> termFindMap = new HashMap<String, Integer>();
    for (int i = 1; i < termVec.stemsLength(); i++) {
      stemString = termVec.stemString(i);
      if (termTable.containsKey(stemString))
        termFindMap.put(stemString, i);
    }
    boolean hasOverlap = false;
    for (String stem : termTable.keySet()) {
      idx = termFindMap.get(stem);
      if (idx != null) {
        hasOverlap = true;
      }
      if (hasOverlap)
        break;
    }

    if (hasOverlap) {
      for (String stem : termTable.keySet()) {
        idx = termFindMap.get(stem);
        if (idx != null) {
          // collectionTermFreq = QryEval.READER.totalTermFreq (new Term (field, new
          // BytesRef(stem)));
          collectionTermFreq = termVec.totalStemFreq(idx); // ctf
          tf = termVec.stemFreq(idx);
          maxLikeliEstim = (double) collectionTermFreq / collectionLength; // P_MLE
          totalIndriScore *= (Math.pow(lambda * (tf + mu * maxLikeliEstim) / (docLen + mu)
                  + (1 - lambda) * maxLikeliEstim, (double) termTable.get(stem) / queryLength));
          // totalIndriScore *= lambda * (tf + mu * maxLikeliEstim)/ (docLen + mu)
          // + (1 - lambda) * maxLikeliEstim;
        } else {
          collectionTermFreq = QryEval.READER.totalTermFreq(new Term(field, new BytesRef(stem)));
          maxLikeliEstim = (double) collectionTermFreq / collectionLength; // P_MLE
          totalIndriScore *= (Math.pow(lambda * mu * maxLikeliEstim / (docLen + mu) + (1 - lambda)
                  * maxLikeliEstim, (double) termTable.get(stem) / queryLength));
          // totalIndriScore *= lambda * mu * maxLikeliEstim / (docLen + mu)
          // + (1 - lambda) * maxLikeliEstim;
        }
      }
    } else
      totalIndriScore = 0.0;
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
    double f;
    for (int i = 0; i < feature.size(); i++) {
      f = feature.get(i);
      if (!Double.isNaN(f)) {
        if (f > max)
          max = f;
        else if (f < min)
          min = f;
      }
    }
    if ((max == 0 && min == 0) || (max == 1 && min == 0)) {
      for (int i = 0; i < feature.size(); i++) {
        if (Double.isNaN(feature.get(i)))
          feature.set(i, 0.0);
      }
      return;
    }
    if (max == min) {
      for (int i = 0; i < feature.size(); i++) {
        feature.set(i, 0.0);
      }
      return;
    }
    for (int i = 0; i < feature.size(); i++) {
      f = feature.get(i);
      if (Double.isNaN(f))
        feature.set(i, 0.0);
      else
        feature.set(i, (f - min) / (max - min));
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < externalIDList.size(); i++) {
      sb.append(relList.get(i) + " ");
      sb.append("qid:" + queryID + " ");
      for (int j = 0; j < featureSize; j++) {
        if (!featureDisable[j]) {
          sb.append((j + 1) + ":" + features.get(j).get(i) + " ");
        }
      }
      sb.append("# " + externalIDList.get(i) + "\n");
    }
    return sb.toString();
  }
}
