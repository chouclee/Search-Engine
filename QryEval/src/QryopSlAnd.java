/**
 *  This class implements the AND operator for all retrieval models.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlAnd extends QryopSl {

  /**
   * It is convenient for the constructor to accept a variable number of arguments. Thus new
   * qryopAnd (arg1, arg2, arg3, ...).
   * 
   * @param q
   *          A query argument (a query operator).
   */
  public QryopSlAnd(Qryop... q) {
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
    else if (r instanceof RetrievalModelIndri)
      return evaluateIndri(r);

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

    // Sort the arguments so that the shortest lists are first. This
    // improves the efficiency of exact-match AND without changing
    // the result.
    Collections.sort(this.daatPtrs);

    // Exact-match AND requires that ALL scoreLists contain a
    // document id. Use the first (shortest) list to control the
    // search for matches.

    // Named loops are a little ugly. However, they make it easy
    // to terminate an outer loop from within an inner loop.
    // Otherwise it is necessary to use flags, which is also ugly.

    DaaTPtr ptr0 = this.daatPtrs.get(0);

    EVALUATEDOCUMENTS: for (; ptr0.nextDoc < ptr0.scoreList.scores.size(); ptr0.nextDoc++) {

      int ptr0Docid = ptr0.scoreList.getDocid(ptr0.nextDoc);
      double docScore = 1.0;

      if (r instanceof RetrievalModelRankedBoolean)
        docScore = (double) ptr0.scoreList.getDocidScore(ptr0.nextDoc);

      // Do the other query arguments have the ptr0Docid?

      for (int j = 1; j < this.daatPtrs.size(); j++) {

        DaaTPtr ptrj = this.daatPtrs.get(j);

        while (true) {
          if (ptrj.nextDoc >= ptrj.scoreList.scores.size())
            break EVALUATEDOCUMENTS; // No more docs can match
          else if (ptrj.scoreList.getDocid(ptrj.nextDoc) > ptr0Docid)
            continue EVALUATEDOCUMENTS; // The ptr0docid can't match.
          else if (ptrj.scoreList.getDocid(ptrj.nextDoc) < ptr0Docid)
            ptrj.nextDoc++; // Not yet at the right doc.
          else {// now at the right doc, update score
            if (r instanceof RetrievalModelRankedBoolean) {
              docScore = Math.min(docScore, (double) ptrj.scoreList.getDocidScore(ptrj.nextDoc));
            }
            break; // ptrj matches ptr0Docid
          }
        }
      }
      // The ptr0Docid matched all query arguments, so save it.
      result.docScores.add(ptr0Docid, docScore);
    }

    freeDaaTPtrs();

    return result;
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
    allocDaaTPtrs(r);
    QryResult result = new QryResult();

    int q = this.args.size();
    ArrayList<Integer> uniqueDocid = getUniqueDocid(); // get list of all doc id
    int docidSize = uniqueDocid.size();
    ArrayList<Double> scores = new ArrayList<Double>();// initialize socres
    for (int i = 0; i < docidSize; i++)
      scores.add(1.0);
    
    int ptriDocid;
    // iterate over all terms
    for (int i = 0; i < this.daatPtrs.size(); i++) { 
      DaaTPtr ptri = this.daatPtrs.get(i);
      int m = 0;
      for (int n = 0; n < ptri.size; n++) { // iterate over all doc id in this term
        ptriDocid = ptri.scoreList.getDocid(n);
        while (uniqueDocid.get(m) != ptriDocid) { 
          // if doc id in uniqueDocid is less than term's current doc id
          scores.set(m, scores.get(m) * ((QryopSl)this.args.get(i)).getDefaultScore(r, 
                  uniqueDocid.get(m)));     // calculate default score
          m++;
        }
        // now they have the same doc id, simply multiply both socores
        scores.set(m, scores.get(m) * ptri.scoreList.getDocidScore(n));
        m++;
      }
      while (m < docidSize) {  // deal with the doc id that are not in this term
        scores.set(m, scores.get(m) * ((QryopSl)this.args.get(i)).getDefaultScore(r, 
                uniqueDocid.get(m)));
        m++;
      }
    }
    for (int i = 0; i < uniqueDocid.size(); i++) {
      result.docScores.add(uniqueDocid.get(i), Math.pow(scores.get(i), 1.0 / q));
    }
    freeDaaTPtrs();

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
    if (r instanceof RetrievalModelIndri) {
      double defaultScore = 1.0;
      int q = this.args.size();
      for (Qryop operation: this.args) {
        defaultScore *= ((QryopSl)operation).getDefaultScore(r, docid);
      }
      return Math.pow(defaultScore, 1.0/q);
    }

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

    return ("#AND( " + result + ")");
  }
}
