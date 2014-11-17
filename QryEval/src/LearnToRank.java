import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class LearnToRank {
  private ArrayList<Integer> queriesID;
  private HashMap<Integer, String> queries;
  private HashMap<Integer, ArrayList<String[]>> relevance;
  private Scanner scan;
  private HashMap<String, Double> pageRank;
  
  public LearnToRank(Map<String, String> params) throws Exception {
    if (!params.containsKey("letor:trainingQueryFile")) {
      System.err.println("Error: training query file was missing.");
      System.exit(1);
    }
    queriesID = new ArrayList<Integer>();
    // use a hashmap to relate query IDs and queries
    queries = new HashMap<Integer, String>();
    scan = new Scanner(new File(params.get("letor:trainingQueryFile")));
    String line = null;
    do {
      line = scan.nextLine();
      String[] pair = line.split(":");
      queriesID.add(Integer.parseInt(pair[0].trim()));
      queries.put(Integer.parseInt(pair[0].trim()), pair[1].trim());
    } while (scan.hasNext());
    scan.close();
    
    // use a hashmap to relate query IDs and relevance judgment
    if (!params.containsKey("letor:trainingQrelsFile")) {
      System.err.println("Error: Relevance judgement file was missing.");
      System.exit(1);
    }
    relevance = new HashMap<Integer, ArrayList<String[]>>();
    scan = new Scanner(new File(params.get("letor:trainingQrelsFile")));
    ArrayList<String[]> list = null;
    int id;
    do {
      // read each line of relevance judgment, put the result in a list
      // update the hashmap for this query ID
      line = scan.nextLine();
      String[] pair = line.split("\\s+");
      String[] val = new String[2];
      id = Integer.parseInt(pair[0].trim());
      val[0] = pair[2].trim(); // put external doc id
      val[1] = pair[3].trim(); // put relevance value
      if (!relevance.containsKey(id)) {
        list = new ArrayList<String[]>();
        list.add(val);
      }
      else {
        list = relevance.get(id);
        list.add(val);
      }
      relevance.put(id, list);
    } while (scan.hasNext());
    scan.close();
    
    /*****************Retrieval Model***********************/
    RetrievalModel r = new RetrievalModelLearnToRank();
    if (!r.setParameter("k_1", params.get("BM25:k_1")) ||
            !r.setParameter("b", params.get("BM25:b")) ||
            !r.setParameter("k_3", params.get("BM25:k_3")) ||
            !r.setParameter("mu", params.get("Indri:mu")) ||
            !r.setParameter("lambda", params.get("Indri:lambda"))) {
      System.err.println("paramter is missing");
      System.exit(1);
    }

    
    /*****************PageRank HashMap**********************/
    pageRank = new HashMap<String, Double>();
    scan = new Scanner(new File(params.get("letor:pageRankFile")));
    String[] pair = null;
    do {
      line = scan.nextLine();
      pair = line.split("\\s+");
      pageRank.put(pair[0].trim(), Double.parseDouble(pair[1].trim()));
    } while (scan.hasNext());
    scan.close();
    
    generateTrainingData(r);
  }
  
  /*
   *  while a training query q is available {
   *    use QryEval.tokenizeQuery to stop & stem the query
   *    foreach document d in the relevance judgements for training query q {
   *      create an empty feature vector
   *      read the PageRank feature from a file
   *      fetch the term vector for d
   *      calculate other features for <q, d>
   *    }
   *
   *    normalize the feature values for query q to [0..1] 
   *    write the feature vectors to file
   * }
   */
  public void generateTrainingData(RetrievalModel r) throws Exception {
    String externalID = null;
    String query = "";
    int docID;
    for (Integer queryID : queriesID) {
      // use QryEval.tokenizeQuery to stop & stem the query
      String[] terms = QryEval.tokenizeQuery(queries.get(queryID));
      for (String term : terms)
        query = query + term + " ";
      query = query.trim();
      FeatureVector featureVec = new FeatureVector(r, query, pageRank);
      
      for (String[] rel : relevance.get(queryID)) {
        externalID = rel[0];
        featureVec.addDocID(r, externalID, Integer.parseInt(rel[1]));
      }
    }
    int a = 0;
  }
}
