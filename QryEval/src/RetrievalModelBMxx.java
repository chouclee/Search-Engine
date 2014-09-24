import java.util.HashMap;


public class RetrievalModelBMxx extends RetrievalModel {
  private HashMap<String, Double> paramMap;
  
  /**
   * Constructor for RetrievalModelBMxx
   */
  public RetrievalModelBMxx(double k_1, double b, double k_3) {
    paramMap = new HashMap<String, Double>();
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
    if (parameterName.equalsIgnoreCase("k_1"))
      paramMap.put("k_1", Double.parseDouble(value));
    else if (parameterName.equalsIgnoreCase("b"))
      paramMap.put("b", Double.parseDouble(value));
    else if (parameterName.equalsIgnoreCase("k_3"))
      paramMap.put("k_3", Double.parseDouble(value));
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
