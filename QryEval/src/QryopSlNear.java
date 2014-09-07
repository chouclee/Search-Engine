/**
 *  This class is used as a QryopSl wrapper for QryopIlNear 
 *  
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;

public class QryopSlNear extends QryopSl {
  private int distance;

  /**
   * It is convenient for the constructor to accept a variable number of arguments. Thus new qryopOr
   * (arg1, arg2, arg3, ...).
   * 
   * @param q
   *          A query argument (a query operator).
   */
  public QryopSlNear(int distance, Qryop... q) {
    this.distance = distance;
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

    DaaTPtr ptr0 = this.daatPtrs.get(0);
    int ptr0Docid;
    for (int i = 0; i < ptr0.scoreList.scores.size(); i++) {
      ptr0Docid = ptr0.scoreList.getDocid(i);
      result.docScores.add(ptr0Docid, ptr0.scoreList.scores.get(i).getScore());
    }

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
      return 0.0;

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

    return ("#NEAR/" + this.distance + "( " + result + ")");
  }
}
