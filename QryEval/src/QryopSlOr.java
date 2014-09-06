import java.io.IOException;
import java.util.*;

public class QryopSlOr extends QryopSl {

  /**
   * It is convenient for the constructor to accept a variable number of arguments. Thus new qryopOr
   * (arg1, arg2, arg3, ...).
   * 
   * @param q
   *          A query argument (a query operator).
   */
  public QryopSlOr(Qryop... q) {
    for (int i = 0; i < q.length; i++)
      this.args.add(q[i]);
  }

  /**
   * Appends an argument to the list of query operator arguments. This simplifies the design of some
   * query parsing architectures.
   * 
   * @param {q} q The query argument (query operator) to append.
   * @return void
   * @throws IOException
   */
  public void add(Qryop a) {
    this.args.add(a);
  }

  /**
   * Evaluates the query operator, including any child operators and returns the result.
   * 
   * @param r
   *          A retrieval model that controls how the operator behaves.
   * @return The result of evaluating the query.
   * @throws IOException
   */
  public QryResult evaluate(RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean)
      return (evaluateBoolean(r));
    else if (r instanceof RetrievalModelRankedBoolean)
      return (evaluateBoolean(r));
    return null;
  }

  /**
   * Evaluates the query operator for boolean retrieval models, including any child operators and
   * returns the result.
   * 
   * @param r
   *          A retrieval model that controls how the operator behaves.
   * @return The result of evaluating the query.
   * @throws IOException
   */
  public QryResult evaluateBoolean(RetrievalModel r) throws IOException {

    // Initialization

    allocDaaTPtrs(r);
    QryResult result = new QryResult();

    // use hashMap to check whether a DocID has appeared or not.

    HashMap<Integer, Double> docidScoreMap = new HashMap<Integer, Double>();

    double docScore = 1.0, docScoreOld;
    DaaTPtr ptri;
    int ptriDocid;

    for (int i = 0; i < this.daatPtrs.size(); i++) {
      ptri = this.daatPtrs.get(i);
      for (int j = 0; j < ptri.scoreList.scores.size(); j++) {
        docScore = 1.0;
        ptriDocid = ptri.scoreList.getDocid(j);
        if (r instanceof RetrievalModelRankedBoolean)
          docScore = (double) ptri.scoreList.getDocidScore(j);

        if (!docidScoreMap.containsKey(ptriDocid))
          // if it's not in the hashMap, put it in the hashMap
          docidScoreMap.put(ptriDocid, docScore);

        else if (r instanceof RetrievalModelRankedBoolean) {
          docScoreOld = docidScoreMap.get(ptriDocid);
          docidScoreMap.put(ptriDocid, Math.max(docScore, docScoreOld));
        }
      }
    }
    freeDaaTPtrs();
    ArrayList<Integer> sortedKeys = new ArrayList<Integer>(docidScoreMap.keySet());
    Collections.sort(sortedKeys);
    for (int i = 0; i < sortedKeys.size(); i++)
      result.docScores.add(sortedKeys.get(i), docidScoreMap.get(sortedKeys.get(i)));
    return result;
  }

  /*
   * Calculate the default score for the specified document if it does not match the query operator.
   * This score is 0 for many retrieval models, but not all retrieval models.
   * 
   * @param r A retrieval model that controls how the operator behaves.
   * 
   * @param docid The internal id of the document that needs a default score.
   * 
   * @return The default score.
   */
  public double getDefaultScore(RetrievalModel r, long docid) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean)
      return (0.0);
    if (r instanceof RetrievalModelRankedBoolean)
      return 1.0;

    return 0.0;
  }

  /*
   * Return a string version of this query operator.
   * 
   * @return The string version of this query operator.
   */
  public String toString() {

    String result = new String();

    for (int i = 0; i < this.args.size(); i++)
      result += this.args.get(i).toString() + " ";

    return ("#OR( " + result + ")");
  }
}
