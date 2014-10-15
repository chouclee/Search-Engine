/**
 *  This class implements the Near operator for all retrieval models.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;


public class QryopIlWindow extends QryopIl {
  private int distance;
  /**
   * It is convenient for the constructor to accept a variable number of arguments. Thus new
   * QryopIlWindow (arg1, arg2, arg3, ...).
   */
  public QryopIlWindow(int distance, Qryop... q) {
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
    
    Collections.sort(this.daatPtrs);
    
    // set field of result
    result.invertedList.field = new String(this.daatPtrs.get(0).invList.field);
    
    // very similar to QryopSlAnd
    DaaTPtr ptr0 = this.daatPtrs.get(0);
    
    int numArgs = this.daatPtrs.size();
    
    if (numArgs == 1) {
      result.invertedList = ptr0.invList;
      freeDaaTPtrs();
      return result;
    }
    
    EVALUATEDOCUMENTS: for (; ptr0.nextDoc < ptr0.invList.df; ptr0.nextDoc++) {

      int ptr0Docid = ptr0.invList.getDocid(ptr0.nextDoc);
      
      // pointer to the current doc's invlist's posting
      //InvList.DocPosting pst0 = ptr0.invList.postings.get(ptr0.nextDoc);
      
      

      // Do the other query arguments have the ptr0Docid?    
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
          else if (j != numArgs - 1)
            continue EVALUATETERM; // ready for next term evaluation
          else {// now at the same doc and reach the last term, test the distance
            int[] termPos = new int[numArgs]; // to save term positions
            int[] posPtr = new int[numArgs]; // to store pointers to postings
            int[] termLength = new int[numArgs];
            boolean hasMorePos = true;
            int[] checkValid = new int[3];
            int minIdx;
            ArrayList<Integer> tempPos = new ArrayList<Integer>();
            DaaTPtr ptr;
            
            for (int i = 0; i < numArgs; i++) {
              ptr = this.daatPtrs.get(i); // get i th term
              pstj = ptr.invList.postings.get(ptr.nextDoc); // get postings
              termPos[i] = pstj.positions.get(posPtr[i]); // 
              termLength[i] = pstj.positions.size();
              //posPtr[i]++;
            }
            while (hasMorePos) {
              checkValid = validation(termPos);
              if (checkValid[0] == 1) { //found pair
                tempPos.add(termPos[checkValid[2]]); // add last position to temporary position list
                for (int i = 0; i < numArgs; i++) {
                  posPtr[i]++; // update i th pointer
                  if (posPtr[i] >= termLength[i]) {// check i th pointer
                    hasMorePos = false;
                    break;
                  }
                  ptr = this.daatPtrs.get(i); // get i th term
                  pstj = ptr.invList.postings.get(ptr.nextDoc); // get postings
                  termPos[i] = pstj.positions.get(posPtr[i]); // update term's position
                }
              }
              else { // it's not a valid pair, update the term position of minimum position
                minIdx = checkValid[1];
                posPtr[minIdx]++;
                if (posPtr[minIdx] >= termLength[minIdx]) // check pointer
                  break;
                ptr = this.daatPtrs.get(minIdx); // get i th term
                pstj = ptr.invList.postings.get(ptr.nextDoc); // get postings
                termPos[minIdx] = pstj.positions.get(posPtr[minIdx]); // update term's position
              }
            }
            if (tempPos.size() != 0)
              result.invertedList.appendPosting(ptr0Docid, tempPos);
            continue EVALUATEDOCUMENTS;
          }
        }        
      }    
    }

    freeDaaTPtrs();
    return result;
  }
  
  private int[] validation(int[] termPos) {
    int minPos = termPos[0];
    int maxPos = termPos[0];
    int result[] = new int[3];
    // isValid result[0]
    // minIdx - result[1]
    // maxIdx - result[2]
    for (int i = 1; i < termPos.length; i++) {
      if (termPos[i] < minPos) {
        minPos = termPos[i];
        result[1] = i;
      }
      else if (termPos[i] > maxPos){
        maxPos = termPos[i];
        result[2] = i;
      }
    }
    if ((maxPos - minPos) <= (this.distance - 1))
      result[0] = 1;
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

    return ("#Window/" + this.distance + "( " + result + ")");
  }
}

