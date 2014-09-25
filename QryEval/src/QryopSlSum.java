import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class QryopSlSum extends QryopSl {

  /**
   * It is convenient for the constructor to accept a variable number of arguments. Thus new qryopSum
   * (arg1, arg2, arg3, ...).
   * 
   * @param q
   *          A query argument (a query operator).
   */
  public QryopSlSum(Qryop... q) {
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

    if (r instanceof RetrievalModelBMxx)
      return evaluateBMxx(r);
    return null;
  }

  /**
   * Evaluates the query operator for BMxx, including any child operators and
   * returns the result.
   * 
   * @param r
   *          A retrieval model that controls how the operator behaves.
   * @return The result of evaluating the query.
   * @throws IOException
   */
  public QryResult evaluateBMxx(RetrievalModel r) throws IOException {

    // Initialization
    allocDaaTPtrs(r);
    QryResult result = new QryResult();
    
    // use hashMap to check whether a DocID has appeared or not.
    HashMap<Integer, Double> docidScoreMap = new HashMap<Integer, Double>();

    double docScore = 1.0, docScoreOld;
    DaaTPtr ptri;  // ptr to current term
    int ptriDocid; // current doc id
    
    float k_3 = (float) ((RetrievalModelBMxx) r).getParameter("k_3");
    int qtf = 1; // query term frequency

    // out most level: iterate all terms
    for (int i = 0; i < this.daatPtrs.size(); i++) {
      ptri = this.daatPtrs.get(i);
      int docFreq = ptri.scoreList.scores.size();
      double usrWeight = (k_3 + 1)*qtf / (k_3 + qtf);
      for (int j = 0; j < docFreq; j++) {
        
        ptriDocid = ptri.scoreList.getDocid(j);
        docScore = ptri.scoreList.getDocidScore(j) * usrWeight;
        //System.out.print("j's docID: "+ptriDocid + ":");
        //System.out.println(ptri.scoreList.getDocidScore(j));
        
        if (!docidScoreMap.containsKey(ptriDocid))
          // if it's not in the hashMap, put it in the hashMap
          docidScoreMap.put(ptriDocid, docScore);

        else {
          docScoreOld = docidScoreMap.get(ptriDocid);
          docidScoreMap.put(ptriDocid, docScoreOld + docScore);
        }
      }
    }
    freeDaaTPtrs();
 /*   ArrayList<Integer> sortedKeys = new ArrayList<Integer>(docidScoreMap.keySet());
    Collections.sort(sortedKeys);
    for (int i = 0; i < sortedKeys.size(); i++)
      result.docScores.add(sortedKeys.get(i), docidScoreMap.get(sortedKeys.get(i)));*/
    for (Entry <Integer, Double> entry : docidScoreMap.entrySet())
      result.docScores.add(entry.getKey(), entry.getValue());
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

    return ("#SUM( " + result + ")");
  }
}
