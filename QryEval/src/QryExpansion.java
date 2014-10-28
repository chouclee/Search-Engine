import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;


public class QryExpansion {
  private ArrayList<Integer> docIds;
  private ArrayList<Double> scores;
  private double mu;
  private int topNTerm;

  public QryExpansion(ArrayList<Integer> docIds, ArrayList<Double> scores,
          double mu, int topNTerm) {
    this.docIds = docIds;
    this.scores = scores;
    this.mu = mu;
    this.topNTerm = topNTerm;
  }
  
  public String evaluate(RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelIndri)
      return evaluateIndri(r);
    return null;
  }
  
 /*
  * TermScore is an private class that is only used in Query Expansion
  */
  private class TermScore implements Comparable<TermScore> {
    public String term;
    public double score;
    
    public TermScore(String term, double score) {
      this.term = term;
      this.score = score;
    }

    @Override
    public int compareTo(TermScore o) {
      // TODO Auto-generated method stub
      if (this.score < o.score)
        return -1;
      if (this.score > o.score)
        return +1;
      return 0;
    }    
  }
  
  /**
   * Comparator for TermScore, using score as key
   * @author happyuser
   *
   */
  public static class ScoreOrder implements Comparator<TermScore> {
    public int compare(TermScore a, TermScore b) {
      return a.compareTo(b);
    }
  }
  
  /**
   * Generate a new query using Indri retrieval model
   * 
   * @param r
   *          A retrieval model that controls how the operator behaves.
   * @return The new query of evaluating the query.
   * @throws IOException
   */
  public String evaluateIndri(RetrievalModel r) throws IOException {
    HashMap<String, Double> termScoreMap = new HashMap<String, Double>();
    double docScore, termScore;
    int tf, collectionTermFreq;
    long docLen, collectionLength;
    float maxLikeliEstim;
    String stemString;
    for (int i = 0; i < docIds.size(); i++) { // starts from 1!!!! 0 stands for stopwords
      // assume field comes from field
      TermVector termVec = new TermVector(docIds.get(i), "body");
      docScore = scores.get(i);
      collectionLength = QryEval.READER.getSumTotalTermFreq("body");// collection length
      docLen = QryEval.docLenStore.getDocLength("body", docIds.get(i)); // document length

      
      // travese a single document
      for (int j = 0; j < termVec.stemsLength(); j++) {
        stemString = termVec.stemString(j);
        if (stemString == null || stemString == "") // null or empty string, continue to next term
          continue;
        if (stemString.matches("(?i).+(\\.|,).*")) {// has "," or "."
            String[] splited = stemString.split("(\\.|,)"); // split the word
            // if the right part of "." or "," is not a field, skip this term
            if (splited.length >=2 && !splited[1].matches("(body|url|keywords|title|inlink)"))
              continue;
        }
        collectionTermFreq = (int) termVec.totalStemFreq(j); //ctf
        maxLikeliEstim = (float) collectionTermFreq / collectionLength; //P_MLE
        tf = termVec.stemFreq(j);
        // given a document, calculate term score for this term
        termScore = (tf + mu*maxLikeliEstim)/(docLen + mu) * docScore * Math.log(1.0/maxLikeliEstim);
        
        if (!termScoreMap.containsKey(stemString)) {
          termScoreMap.put(stemString, termScore);
        } else {
          termScoreMap.put(stemString, termScoreMap.get(stemString) + termScore);
        }
      }
    }
    
    TermScore[] topRank = getTopNTerm(termScoreMap, topNTerm);
    StringBuilder newQueryBuilder = new StringBuilder();
    for (TermScore ts : topRank) {
      newQueryBuilder.append(ts.score + " " + ts.term + " ");
    }
    return "#WAND(" + newQueryBuilder.toString() + ")"; 
  }
  
  TermScore[] getTopNTerm(HashMap<String, Double> termScoreMap, int numDocs) {
    Comparator<TermScore> SCORE_ORDER = new ScoreOrder();
    PriorityQueue<TermScore> pq = new PriorityQueue<TermScore>(
            numDocs, SCORE_ORDER);
    int cnt = 0;
    ArrayList<String> keySet = new ArrayList<String>(termScoreMap.keySet());
    for (; cnt < numDocs; cnt++) {
      pq.add(new TermScore(keySet.get(cnt), termScoreMap.get(keySet.get(cnt))));
    }
    for (; cnt < keySet.size(); cnt++) {
      if ((termScoreMap.get(keySet.get(cnt))).compareTo(pq.peek().score) > 0) {
        pq.poll();
        pq.add(new TermScore(keySet.get(cnt), termScoreMap.get(keySet.get(cnt))));
      }
    }
    TermScore[] topRank = new TermScore[numDocs];
    for (int i = numDocs; i > 0; i--) {
      topRank[i - 1] = pq.poll();
    }
    return topRank;
  }
}
