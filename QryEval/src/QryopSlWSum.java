import java.io.IOException;
import java.util.ArrayList;


public class QryopSlWSum extends QryopSl {
  private ArrayList<Float> weights;
  /**
   * It is convenient for the constructor to accept a variable number of arguments. Thus new qryopSum
   * (arg1, arg2, arg3, ...).
   * 
   * @param q
   *          A query argument (a query operator).
   */
  public QryopSlWSum(Qryop... q) {
    for (int i = 0; i < q.length; i++)
      this.args.add(q[i]);
    this.weights = new ArrayList<Float>();
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
  
  public void addWeight(float weight) {
    this.weights.add(weight);
  }
  
  private void normalizeWeight() {
    float totalWeight = 0.0f;
    
    // get totoal weight
    for (int i = 0; i < this.weights.size(); i++)
      totalWeight += this.weights.get(i);
    
    for (int i = 0; i < this.weights.size(); i++)
      this.weights.set(i, this.weights.get(i) / totalWeight);
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
    allocDaaTPtrs(r);
    QryResult result = new QryResult();

    ArrayList<Integer> uniqueDocid = getUniqueDocid(); // get list of all doc id
    int docidSize = uniqueDocid.size();
    ArrayList<Double> scores = new ArrayList<Double>();// initialize socres
    for (int i = 0; i < docidSize; i++)
      scores.add(0.0);
    
    normalizeWeight(); // normalize all weight
    int ptriDocid;
    // iterate over all terms
    for (int i = 0; i < this.daatPtrs.size(); i++) { 
      float weight = this.weights.get(i);
      DaaTPtr ptri = this.daatPtrs.get(i);
      int m = 0;
      for (int n = 0; n < ptri.size; n++) { // iterate over all doc id in this term
        ptriDocid = ptri.scoreList.getDocid(n);
        while (uniqueDocid.get(m) != ptriDocid) { 
          // if doc id in uniqueDocid is less than term's current doc id
          scores.set(m, scores.get(m) + ((QryopSl)this.args.get(i)).getDefaultScore(r, 
                  uniqueDocid.get(m)) * weight);     // calculate default score
          m++;
        }
        // now they have the same doc id, simply multiply both socores
        scores.set(m, scores.get(m) + ptri.scoreList.getDocidScore(n) * weight);
        m++;
      }
      while (m < docidSize) {  // deal with the doc id that are not in this term
        scores.set(m, scores.get(m) + ((QryopSl)this.args.get(i)).getDefaultScore(r, 
                uniqueDocid.get(m)) * weight);
        m++;
      }
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
      double defaultScore = 0.0;
      int q = this.args.size();
      for (int i = 0; i < q; i++) {
        Qryop operation = this.args.get(i);
        defaultScore += ((QryopSl)operation).getDefaultScore(r, docid) *
                this.weights.get(i);
      }
      return defaultScore;
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
      result += this.weights.get(i) + " " + this.args.get(i).toString() + " ";

    return ("#WSUM( " + result + ")");
  }
}


