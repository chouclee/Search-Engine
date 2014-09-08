/**
 *  This class implements the Near operator for all retrieval models.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;


public class QryopIlNear extends QryopIl {
  private int distance;
  /**
   * It is convenient for the constructor to accept a variable number of arguments. Thus new
   * QryopIlNear (arg1, arg2, arg3, ...).
   */
  public QryopIlNear(int distance, Qryop... q) {
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

    // Initialization

    allocDaaTPtrs(r);
    syntaxCheckArgResults(this.daatPtrs);

    QryResult result = new QryResult();
    
    // Sort the arguments so that the shortest lists are first. This
    // improves the efficiency
    Collections.sort(this.daatPtrs);
    
    // not sure about this line. Should all terms come from same field?
    result.invertedList.field = new String(this.daatPtrs.get(0).invList.field);
    
    // very similar to QryopSlAnd
    DaaTPtr ptr0 = this.daatPtrs.get(0);

    EVALUATEDOCUMENTS: for (; ptr0.nextDoc < ptr0.invList.df; ptr0.nextDoc++) {

      int ptr0Docid = ptr0.invList.getDocid(ptr0.nextDoc);
      
      // pointer to the current doc's invlist's posting
      InvList.DocPosting pst0 = ptr0.invList.postings.get(ptr0.nextDoc);
      
      // Do the other query arguments have the ptr0Docid?    
      // use arraylist to store all match positions in previous comparison
      // e.g. if term "cheap" and term "internet" has in doc ptr0Docid has 
      // matching positions(i.e. "cheap" pos{1,4,6,9}, "internet" pos{2,7,11}. distance:1,
      // then we store {2,7} in this arraylist, for next term)
      ArrayList<Integer> prevMatchPostions = new ArrayList<Integer>(pst0.positions);
      int prevPos, nextPos;
      EVALUATETERM: for (int j = 1; j < this.daatPtrs.size(); j++) {

        DaaTPtr ptrj = this.daatPtrs.get(j);
        
        InvList.DocPosting pstj;
        while (true) {
          if (ptrj.nextDoc >= ptrj.invList.df)
            break EVALUATEDOCUMENTS; // No more docs can match, this term doesn't have common doc id
          else if (ptrj.invList.getDocid(ptrj.nextDoc) > ptr0Docid)
            continue EVALUATEDOCUMENTS; // The ptr0docid can't match, evaluate next ptr0docid
          else if (ptrj.invList.getDocid(ptrj.nextDoc) < ptr0Docid)
            ptrj.nextDoc++; // Not yet at the right doc.
          else {// now at the same doc, test the distance
            pstj = ptrj.invList.postings.get(ptrj.nextDoc);
            int m = 0, n = 0; 
            // m : index in prevMatchPositions   
            // n : index in pstj.postions 
            ArrayList<Integer> tempPos = new ArrayList<Integer>();
            while (m < prevMatchPostions.size() && n < pstj.positions.size()) {
              
              prevPos = prevMatchPostions.get(m);
              nextPos = pstj.positions.get(n);
              if ( nextPos < prevPos)
                n++;// it is impossible that nextPos and prevPos are equal since they are in the same doc
              else if ((nextPos - prevPos) <= this.distance) {
                // match, store new match position in prevMatchPositions
                tempPos.add(nextPos);
                n++;
                m++;
              }
              else if ((nextPos - prevPos) > this.distance) 
                m++;// m is too small, increase m and flag this position as impossible
            }
            if (tempPos.size() == 0) // there is no need to check rest terms with docid ptr0docid
              continue EVALUATEDOCUMENTS;
            prevMatchPostions = tempPos;
            continue EVALUATETERM; // ready for next term evaluation
          }
        }
        
        // **********************Attention********************************
        // update result, use last term's position as seach result's position 
        // might need modify this in future
        
      }
      result.invertedList.appendPosting(ptr0Docid, prevMatchPostions);
    }

    freeDaaTPtrs();
    return result;
  }


  /**
   * syntaxCheckArgResults does syntax checking that can only be done after query arguments are
   * evaluated.
   * 
   * @param ptrs
   *          A list of DaaTPtrs for this query operator.
   * @return True if the syntax is valid, false otherwise.
   */
  public Boolean syntaxCheckArgResults(List<DaaTPtr> ptrs) {

    for (int i = 0; i < this.args.size(); i++) {

      if (!(this.args.get(i) instanceof QryopIl))
        QryEval.fatalError("Error:  Invalid argument in " + this.toString());
      else if ((i > 0) && (!ptrs.get(i).invList.field.equals(ptrs.get(0).invList.field)))
        QryEval.fatalError("Error:  Arguments must be in the same field:  " + this.toString());
    }

    return true;
  }

  /*
   * Return a string version of this query operator.
   * 
   * @return The string version of this query operator.
   */
  public String toString() {

    String result = new String();

    for (Iterator<Qryop> i = this.args.iterator(); i.hasNext();)
      result += (i.next().toString() + " ");

    return ("#NEAR/" + this.distance + "( " + result + ")");
  }
}
