/**
 *  This class implements the SCORE operator for all retrieval models.
 *  The single argument to a score operator is a query operator that
 *  produces an inverted list.  The SCORE operator uses this
 *  information to produce a score list that contains document ids and
 *  scores.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlScore extends QryopSl {

  /**
   * Construct a new SCORE operator. The SCORE operator accepts just one argument.
   * 
   * @param q
   *          The query operator argument.
   * @return @link{QryopSlScore}
   */
  public QryopSlScore(Qryop q) {
    this.args.add(q);
  }

  /**
   * Construct a new SCORE operator. Allow a SCORE operator to be created with no arguments. This
   * simplifies the design of some query parsing architectures.
   * 
   * @return @link{QryopSlScore}
   */
  public QryopSlScore() {
  }

  /**
   * Appends an argument to the list of query operator arguments. This simplifies the design of some
   * query parsing architectures.
   * 
   * @param q
   *          The query argument to append.
   */
  public void add(Qryop a) {
    this.args.add(a);
  }

  /**
   * Evaluate the query operator.
   * 
   * @param r
   *          A retrieval model that controls how the operator behaves.
   * @return The result of evaluating the query.
   * @throws IOException
   */
  public QryResult evaluate(RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean)
      return (evaluateBoolean(r));
    if (r instanceof RetrievalModelRankedBoolean)
      return (evaluateBoolean(r));
    if (r instanceof RetrievalModelBMxx)
      return (evaluateBMxx(r));
    if (r instanceof RetrievalModelIndri)
      return evaluateIndri(r);
    return null;
  }

  /**
   * Evaluate the query operator for boolean retrieval models.
   * 
   * @param r
   *          A retrieval model that controls how the operator behaves.
   * @return The result of evaluating the query.
   * @throws IOException
   */
  public QryResult evaluateBoolean(RetrievalModel r) throws IOException {

    // Evaluate the query argument.

    QryResult result = args.get(0).evaluate(r);

    // Each pass of the loop computes a score for one document. Note:
    // If the evaluate operation above returned a score list (which is
    // very possible), this loop gets skipped.
    if (result == null)
      return null;

    // DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
    // Unranked Boolean. All matching documents get a score of 1.0.
    if (r instanceof RetrievalModelUnrankedBoolean) {
      for (int i = 0; i < result.invertedList.df; i++)
        result.docScores.add(result.invertedList.postings.get(i).docid, (float) 1.0);
    } else if (r instanceof RetrievalModelRankedBoolean) {
      for (int i = 0; i < result.invertedList.df; i++)
        // for RankedBoolean, use term frequency as score
        result.docScores.add(result.invertedList.postings.get(i).docid,
                result.invertedList.getTf(i));
    }
    // The SCORE operator should not return a populated inverted list.
    // If there is one, replace it with an empty inverted list.

    if (result.invertedList.df > 0)
      result.invertedList = new InvList();

    return result;
  }

  /**
   * Evaluate the query operator for BMxx retrieval model.
   * 
   * @param r
   *          A retrieval model that controls how the operator behaves.
   * @return The result of evaluating the query.
   * @throws IOException
   */
  public QryResult evaluateBMxx(RetrievalModel r) throws IOException {

    // Evaluate the query argument.

    QryResult result = args.get(0).evaluate(r);

    // Each pass of the loop computes a score for one document. Note:
    // If the evaluate operation above returned a score list (which is
    // very possible), this loop gets skipped.
    if (result == null)
      return null;

    if (r instanceof RetrievalModelBMxx) {
      // load parameters
      float k_1 = ((RetrievalModelBMxx) r).getParameter("k_1");
      float b = ((RetrievalModelBMxx) r).getParameter("b");

      // some constant parameters
      int docFreq = result.invertedList.df; // document frequency containing this term
      String field = result.invertedList.field; // term field
      int N = QryEval.READER.numDocs(); // total number of documents
      float avgDocLen = (float) QryEval.READER.getSumTotalTermFreq(field)
              / QryEval.READER.getDocCount(field); // average doc length

      // RSJ weight
      float RSJWeight = (float) Math.log((N - docFreq + 0.5) / (docFreq + 0.5));

      int tf, docid;
      long docLen;
      float tfWeight;
      for (int i = 0; i < docFreq; i++) {
        tf = result.invertedList.getTf(i);
        docid = result.invertedList.getDocid(i);
        docLen = QryEval.docLenStore.getDocLength(field, docid);
        // tf Weight
        tfWeight = tf / (tf + k_1 * (1 - b + b * docLen / avgDocLen));
        result.docScores.add(docid, RSJWeight * tfWeight);
      }
    }

    // The SCORE operator should not return a populated inverted list.
    // If there is one, replace it with an empty inverted list.

    if (result.invertedList.df > 0)
      result.invertedList = new InvList();

    return result;
  }

  /**
   * Evaluate the query operator for Indri retrieval model.
   * 
   * @param r
   *          A retrieval model that controls how the operator behaves.
   * @return The result of evaluating the query.
   * @throws IOException
   */
  public QryResult evaluateIndri(RetrievalModel r) throws IOException {

    // Evaluate the query argument.

    QryResult result = args.get(0).evaluate(r);

    // Each pass of the loop computes a score for one document. Note:
    // If the evaluate operation above returned a score list (which is
    // very possible), this loop gets skipped.
    if (result == null)
      return null;

    if (r instanceof RetrievalModelIndri) {
      // load parameters
      float mu = ((RetrievalModelIndri) r).getParameter("mu");
      float lambda = ((RetrievalModelIndri) r).getParameter("lambda");

      // some constant parameters
      int docFreq = result.invertedList.df; // document frequency containing this term
      String field = result.invertedList.field; // term field
      long docLenCollection = QryEval.READER.getSumTotalTermFreq(field);
      
      result.docScores.ctf.add(result.invertedList.ctf);
      result.docScores.field.add(field);
     // result.docScores.docLenCollection = docLenCollection;
      

      float maxLikelyEstim = (float) result.invertedList.ctf / docLenCollection;
      result.docScores.maxLikelyEstim.add(maxLikelyEstim);
      int tf, docid;
      long docLen;

      for (int i = 0; i < docFreq; i++) {
        tf = result.invertedList.getTf(i);
        docid = result.invertedList.getDocid(i);
        docLen = QryEval.docLenStore.getDocLength(field, docid);
        // tf Weight
        result.docScores.add(docid, lambda * (tf + mu * maxLikelyEstim) / (docLen + mu)
                + (1 - lambda) * maxLikelyEstim);
      }
    }

    // The SCORE operator should not return a populated inverted list.
    // If there is one, replace it with an empty inverted list.

    if (result.invertedList.df > 0)
      result.invertedList = new InvList();

    return result;
  }

  /*
   * Calculate the default score for a document that does not match the query argument. This score
   * is 0 for many retrieval models, but not all retrieval models.
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
      return (0.0);

    return 0.0;
  }

  /**
   * Return a string version of this query operator.
   * 
   * @return The string version of this query operator.
   */
  public String toString() {

    String result = new String();

    for (Iterator<Qryop> i = this.args.iterator(); i.hasNext();)
      result += (i.next().toString() + " ");

    return ("#SCORE( " + result + ")");
  }
}
