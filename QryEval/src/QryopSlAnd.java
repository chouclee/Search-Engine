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

  private int getArgsNum() {
    return this.args.size();
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

    double docScoreOld;
    int ptriDocid, ptr0Docid;
    double q = (double) getArgsNum();
    DaaTPtr ptr0 = this.daatPtrs.get(0);
    Comparator<ScoreList.ScoreListEntry> DOCID_ORDER = new ScoreList.DocidOrder();
    for (int i = 1; i < this.daatPtrs.size(); i++) {
      DaaTPtr ptri = this.daatPtrs.get(i);
      int docNumPtr0 = ptr0.scoreList.scores.size(); // get number of docs of first argument
      int docNumPtri = ptri.scoreList.scores.size(); // get number of docs of i_th argument
      int m = 0, n = 0;
      while (m < docNumPtr0 && n < docNumPtri) {
        ptr0Docid = ptr0.scoreList.getDocid(m);
        ptriDocid = ptri.scoreList.getDocid(n);
        if (ptr0Docid < ptriDocid) {
          docScoreOld = ptr0.scoreList.getDocidScore(m);
          ptr0.scoreList.scores.get(m).setScore(docScoreOld * getDefaultScore(r, ptr0Docid));
          m++;
        } else if (ptr0Docid == ptriDocid) {
          docScoreOld = ptr0.scoreList.getDocidScore(m);
          ptr0.scoreList.scores.get(m).setScore(docScoreOld * ptri.scoreList.getDocidScore(n));
          m++;
          n++;
        } else {
          docScoreOld = ptr0.scoreList.getDocidScore(n);
          ptr0.scoreList.add(ptriDocid, docScoreOld * getDefaultScore(r, ptriDocid));
          n++;
        }
      }
      while (m < docNumPtr0) {
        ptr0Docid = ptr0.scoreList.getDocid(m);
        docScoreOld = ptr0.scoreList.getDocidScore(m);
        ptr0.scoreList.scores.get(m).setScore(docScoreOld * getDefaultScore(r, ptr0Docid));
        m++;
      }
      while (n < docNumPtri) {
        ptriDocid = ptri.scoreList.getDocid(n);
        docScoreOld = ptri.scoreList.getDocidScore(n);
        ptr0.scoreList.add(ptriDocid, docScoreOld * getDefaultScore(r, ptriDocid));
        n++;
      }
      Collections.sort(ptr0.scoreList.scores, DOCID_ORDER);
      
      
    }
    for (int i = 0; i < ptr0.scoreList.scores.size(); i++) {
      result.docScores.add(ptr0.scoreList.getDocid(i), 
              Math.pow(ptr0.scoreList.scores.get(i).getScore(), 1/q));
    }
    
    for (int i = 0; i < this.daatPtrs.size(); i++) {
      result.docScores.maxLikelyEstim.addAll(this.daatPtrs.get(i).scoreList.maxLikelyEstim);
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
      return 1.0;
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
