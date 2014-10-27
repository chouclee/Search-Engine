import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class QryExpansion {
  private ArrayList<Integer> docIds;
  private ArrayList<Double> scores;
  private double mu;

  public QryExpansion(ArrayList<Integer> docIds, ArrayList<Double> scores,
          double mu) {
    this.docIds = docIds;
    this.scores = scores;
    this.mu = mu;
  }
  
  public QryResult evaluate(RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelIndri)
      return evaluateIndri(r);
    return null;
  }

  /**
   * Evaluates the query operator for Indri retrieval model, including any child operators and
   * returns the result.
   * 
   * @param r
   *          A retrieval model that controls how the operator behaves.
   * @return The result of evaluating the query.
   * @throws IOException
   */
  public QryResult evaluateIndri(RetrievalModel r) throws IOException {
    HashMap<String, Double> termScoreMap = new HashMap<String, Double>();
    double docScore, termScore;
    int tf, collectionTermFreq;
    long docLen, collectionLength;
    for (int i = 0; i < docIds.size(); i++) { // starts from 1!!!! 0 stands for stopwords
      // assume field comes from field
      TermVector termVec = new TermVector(docIds.get(i), "body");
      docScore = scores.get(i);
      collectionLength = QryEval.READER.getSumTotalTermFreq("body");
      docLen = QryEval.docLenStore.getDocLength("body", docIds.get(i));
      collectionTermFreq = (int) termVec.totalStemFreq(i);
      float maxLikeliEstim = (float) collectionTermFreq / collectionLength;
      for (int j = 0; j < termVec.stemsLength(); j++) {
        tf = termVec.stemFreq(j);
      
    }
    return null;
 
  }

}
