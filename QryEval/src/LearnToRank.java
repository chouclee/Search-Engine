import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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
  private String disableFeature;
  
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
    
    /*****************letor:featureDisable*****************/
    if (params.containsKey("letor:featureDisable"))
      disableFeature = params.get("letor:featureDisable").trim();
    
    String filePath = params.get("letor:trainingFeatureVectorsFile").trim();
    
    generateTrainingData(r, filePath);
    
    callSVMTrain(params.get("letor:svmRankLearnPath").trim(),          
            params.get("letor:svmRankParamC").trim(),
            filePath,
            params.get("letor:svmRankModelFile").trim());
    
    callSVMClassify(params.get("letor:svmRankClassifyPath").trim(),
            params.get("letor:testingFeatureVectorsFile").trim(),
            params.get("letor:svmRankModelFile").trim(),
            params.get("letor:testingDocumentScores").trim());
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
  public void generateTrainingData(RetrievalModel r, 
          String filePath) throws Exception {
    String externalID = null;
    String query = "";
    for (Integer queryID : queriesID) {
      // use QryEval.tokenizeQuery to stop & stem the query
      String[] terms = QryEval.tokenizeQuery(queries.get(queryID));
      for (String term : terms)
        query = query + term + " ";
      query = query.trim();
      FeatureVector featureVec = new FeatureVector(r, queryID, query, 
              pageRank, this.disableFeature);
      
      for (String[] rel : relevance.get(queryID)) {
        externalID = rel[0];
        featureVec.addDocID(r, externalID, Integer.parseInt(rel[1]));
      }
      featureVec.normalize();
      BufferedWriter writer = null;
      File file = new File(filePath);
      writer = new BufferedWriter(new FileWriter(file, true));
      writer.write(featureVec.toString());
      writer.close();
    }
  }
  
  public void callSVMTrain(String execPath, String FEAT_GEN_c, 
          String qrelsFeatureOutputFile,
          String modelOutputFile ) throws Exception {
    // runs svm_rank_learn from within Java to train the model
    // execPath is the location of the svm_rank_learn utility, 
    // which is specified by letor:svmRankLearnPath in the parameter file.
    // FEAT_GEN.c is the value of the letor:c parameter.
    callCmd(new String[] { execPath, "-c", FEAT_GEN_c, 
            qrelsFeatureOutputFile,
                modelOutputFile });
  }
  
  public void callSVMClassify(String execPath, String testData, 
          String modelFile, String predictions) throws Exception {
    callCmd(new String[] {execPath, testData, modelFile, predictions});
  }
  
  
  public void evaluate(QryResult initialRanking, int topN) {
    ScoreList.ScoreListEntry[] topRank = QryEval.getTopNDocuments(initialRanking, topN);
    
  }
  
  private void callCmd(String[] args) throws Exception {
    Process cmdProc = Runtime.getRuntime().exec(args);

        // The stdout/stderr consuming code MUST be included.
        // It prevents the OS from running out of output buffer space and stalling.

        // consume stdout and print it out for debugging purposes
        BufferedReader stdoutReader = new BufferedReader(
            new InputStreamReader(cmdProc.getInputStream()));
        String line;
        while ((line = stdoutReader.readLine()) != null) {
          System.out.println(line);
        }
        // consume stderr and print it for debugging purposes
        BufferedReader stderrReader = new BufferedReader(
            new InputStreamReader(cmdProc.getErrorStream()));
        while ((line = stderrReader.readLine()) != null) {
          System.out.println(line);
        }

        // get the return value from the executable. 0 means success, non-zero 
        // indicates a problem
        int retValue = cmdProc.waitFor();
        if (retValue != 0) {
          throw new Exception("SVM Rank crashed.");
        }
  }
}
