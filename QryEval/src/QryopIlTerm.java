/**
 *  This class implements the TERM operator for all retrieval models.
 *  The TERM operator stores a query term, for example "apple" in the
 *  query "#AND (apple pie).  Although it may seem odd to use a query
 *  operator to store a term, doing so makes it easy to build
 *  structured queries with nested query operators.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

public class QryopIlTerm extends QryopIl {

  private String term;
  private String field;

  /**
   *  Constructor.  The term is assumed to match the body field.
   *  @param t A term string.
   *  @return @link{QryopIlTerm} A TERM query operator.
   */
  public QryopIlTerm(String t) {
    this.term = t;
    this.field = "body";	// Default field if none is specified.
  }

  /**
   *  Constructor.  The term matches in the specified field.
   *  @param t A term string.
   *  @param f A field name.
   *  @return @link{QryopIlTerm} A TERM query operator.
   */
  public QryopIlTerm(String t, String f) {
    this.term = t;
    this.field = f;
  }

  /*
   *  Every Qryop is required to have an add method that appends
   *  query arguments, but that doesn't make sense for the Term
   *  query operator.  So, it's here, but it does nothing.  Ugly.
   *  @param {q} q The query argument (query operator) to append.
   *  @return void
   */
  public void add (Qryop q) {
  }

  /**
   *  Evaluates the query operator and returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluate(RetrievalModel r) throws IOException {
    QryResult result = new QryResult();
    result.invertedList = new InvList(this.term, this.field);
    return result;
  }

  /*
   *  Return a string version of this query operator.  
   *  @return The string version of this query operator.
   */
  public String toString(){
    return (this.term + "." + this.field);
  }
}
