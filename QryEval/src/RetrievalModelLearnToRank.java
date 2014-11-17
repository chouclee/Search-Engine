import java.io.IOException;
import java.util.HashMap;

public class RetrievalModelLearnToRank extends RetrievalModel {
  private HashMap<String, Float> paramMap;
  public int N;
  public HashMap<String, Float> avgDocLenMap;
  public HashMap<String, Long> collectionLengthMap;
  public HashMap<String, Long> ctfMap;
  
  public RetrievalModelLearnToRank() throws IOException {
    paramMap = new HashMap<String, Float>();
    N = QryEval.READER.numDocs(); // total number of documents
    
    avgDocLenMap = new HashMap<String, Float>();
    
    avgDocLenMap.put("body", (float) QryEval.READER.getSumTotalTermFreq("body")
            / QryEval.READER.getDocCount("body")); // average doc length
    
    avgDocLenMap.put("title", (float) QryEval.READER.getSumTotalTermFreq("title")
            / QryEval.READER.getDocCount("title")); // average doc length
    
    avgDocLenMap.put("url", (float) QryEval.READER.getSumTotalTermFreq("url")
            / QryEval.READER.getDocCount("url")); // average doc length
    
    avgDocLenMap.put("inlink", (float) QryEval.READER.getSumTotalTermFreq("inlink")
            / QryEval.READER.getDocCount("inlink")); // average doc length
    
    collectionLengthMap = new HashMap<String, Long>();
    
    collectionLengthMap.put("body", QryEval.READER.getSumTotalTermFreq("body"));
    collectionLengthMap.put("title", QryEval.READER.getSumTotalTermFreq("title"));
    collectionLengthMap.put("url", QryEval.READER.getSumTotalTermFreq("url"));
    collectionLengthMap.put("inlink", QryEval.READER.getSumTotalTermFreq("inlink"));
    
    ctfMap = new HashMap<String, Long>();
  }
  @Override
  public boolean setParameter(String parameterName, double value) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean setParameter(String parameterName, String value) {
    // TODO Auto-generated method stub
    float num = Float.parseFloat(value);
    if (parameterName.equalsIgnoreCase("k_1") && num >= 0)
      paramMap.put("k_1", num);
    else if (parameterName.equalsIgnoreCase("b") && num >= 0 && num <= 1)
      paramMap.put("b", num);
    else if (parameterName.equalsIgnoreCase("k_3") && num >= 0)
      paramMap.put("k_3", num);
    else if (parameterName.equalsIgnoreCase("mu") && num >= 0)
      paramMap.put("mu", num);
    else if (parameterName.equalsIgnoreCase("lambda") && num >=0 && num <= 1)
      paramMap.put("lambda", num);
    else return false;
    return true;
  }
  
  public float getParameter(String parameterName) throws IllegalArgumentException {
    if (!paramMap.containsKey(parameterName))
      throw new IllegalArgumentException();
    return paramMap.get(parameterName); 
  }

}
