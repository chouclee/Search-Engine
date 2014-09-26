import java.util.HashMap;


public class RetrievalModelBMxx extends RetrievalModel {
  private HashMap<String, Float> paramMap;
  
  /**
   * Constructor for RetrievalModelBMxx
   */
  public RetrievalModelBMxx() {
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
    // TODO Auto-generated method stub
    float num = Float.parseFloat(value);
    if (parameterName.equalsIgnoreCase("k_1") && num >= 0)
      paramMap.put("k_1", num);
    else if (parameterName.equalsIgnoreCase("b") && num >= 0 && num <= 1)
      paramMap.put("b", num);
    else if (parameterName.equalsIgnoreCase("k_3") && num >= 0)
      paramMap.put("k_3", num);
    else 
      return false;
    return true;
  }
  
  public float getParameter(String parameterName) throws IllegalArgumentException {
    if (!paramMap.containsKey(parameterName))
      throw new IllegalArgumentException();
    return paramMap.get(parameterName); 
  }

}
