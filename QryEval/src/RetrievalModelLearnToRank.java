import java.util.HashMap;

public class RetrievalModelLearnToRank extends RetrievalModel {
  private HashMap<String, Float> paramMap;
  public int N;
  
  public RetrievalModelLearnToRank() {
    paramMap = new HashMap<String, Float>();
    N = QryEval.READER.numDocs(); // total number of documents
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
