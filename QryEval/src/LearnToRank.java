import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class LearnToRank {
  private ArrayList<Integer> queriesID;
  private HashMap<Integer, String> queries;
  private HashMap<Integer, String[]> relevance;
  private Scanner scan;
  public LearnToRank(Map<String, String> params) {
    if (!params.containsKey("letor:trainingQueryFile")) {
      System.err.println("Error: training query file was missing.");
      System.exit(1);
    }
    queriesID = new ArrayList<Integer>();
    // use a hashmap to relate query IDs and queries
    queries = new HashMap<Integer, String>();
    try {
      scan = new Scanner(new File(params.get("letor:trainingQueryFile")));
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
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
    relevance = new HashMap<Integer, String[]>();
    try {
      scan = new Scanner(new File(params.get("letor:trainingQrelsFile")));
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    do {
      line = scan.nextLine();
      String[] pair = line.split("\\s+");
      String[] val = new String[2];
      val[0] = pair[2].trim(); // put external doc id
      val[1] = pair[3].trim(); // put relevance value
      relevance.put(Integer.parseInt(pair[0].trim()), val);
    } while (scan.hasNext());
    scan.close();
    
  }
  
  public void generateTrainingData() {
    for (Integer queryID : queriesID) {
      
    }
  }
}
