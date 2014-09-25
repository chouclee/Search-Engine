import java.util.HashMap;


public class RetrievalModelIndri extends RetrievalModel {
  private HashMap<String, Float> paramMap;
  
  /**
   * Constructor for RetrievalModelBMxx
   */
  public RetrievalModelIndri() {
    paramMap = new HashMap<String, Float>();
  }
  
  @Override
  /**
   * Set a retrieval model parameter.
   * @param parameterName
   * @param parametervalue
   * @return false (doesn't support this function)
   */
  public boolean setParameter(String parameterName, double value) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  /**
   * Set a retrieval model parameter.
   * @param parameterName
   * @param parametervalue
   * @return true if parameter could be set
   */
  public boolean setParameter(String parameterName, String value) {
    float num = Float.parseFloat(value);
    // TODO Auto-generated method stub
    if (parameterName.equalsIgnoreCase("mu") && num >= 0) {
      paramMap.put("mu", num);
    }
    else if (parameterName.equalsIgnoreCase("lambda") && num >=0 && num <= 1)
      paramMap.put("lambda", num);
    else
      return false;
    return true;
  }
  
  public double getParameter(String parameterName) throws IllegalArgumentException {
    if (!paramMap.containsKey(parameterName))
      throw new IllegalArgumentException();
    return paramMap.get(parameterName); 
  }

}

