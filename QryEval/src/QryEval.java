/**
 *  QryEval illustrates the architecture for the portion of a search
 *  engine that evaluates queries.  It is a template for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class QryEval {

  static String usage = "Usage:  java " + System.getProperty("sun.java.command") + " paramFile\n\n";

  // The index file reader is accessible via a global variable. This
  // isn't great programming style, but the alternative is for every
  // query operator to store or pass this value, which creates its
  // own headaches.

  public static IndexReader READER;

  // Create and configure an English analyzer that will be used for
  // query parsing.

  public static EnglishAnalyzerConfigurable analyzer = new EnglishAnalyzerConfigurable(
          Version.LUCENE_43);
  static {
    analyzer.setLowercase(true);
    analyzer.setStopwordRemoval(true);
    analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
    //analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.PORTER);
  }

  /**
   * @param args
   *          The only argument is the path to the parameter file.
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    long startTime = System.currentTimeMillis();
    
    // must supply parameter file
    if (args.length < 1) {
      System.err.println(usage);
      System.exit(1);
    }

    // read in the parameter file; one parameter per line in format of key=value
    Map<String, String> params = new HashMap<String, String>();
    Scanner scan = new Scanner(new File(args[0]));
    String line = null;
    do {
      line = scan.nextLine();
      String[] pair = line.split("=");
      params.put(pair[0].trim(), pair[1].trim());
    } while (scan.hasNext());
    scan.close();

    // parameters required for this example to run
    if (!params.containsKey("indexPath")) {
      System.err.println("Error: Parameters were missing.");
      System.exit(1);
    }

    // open the index
    READER = DirectoryReader.open(FSDirectory.open(new File(params.get("indexPath"))));

    if (READER == null) {
      System.err.println(usage);
      System.exit(1);
    }

    // DocLengthStore s = new DocLengthStore(READER);
    // set model
    RetrievalModel model = null;
    if (params.get("retrievalAlgorithm").equalsIgnoreCase("UnrankedBoolean")) {
      model = new RetrievalModelUnrankedBoolean();
    } else if (params.get("retrievalAlgorithm").equalsIgnoreCase("RankedBoolean")) {
      model = new RetrievalModelRankedBoolean();
    } else {
      System.err.println(usage);
      System.exit(1);
    }

    // load all queries
    if (!params.containsKey("queryFilePath")) {
      System.err.println("Error: Parameters were missing.");
      System.exit(1);
    }
    ArrayList<Integer> queriesID = new ArrayList<Integer>();
    // use a hashmap to relate query IDs and queries
    HashMap<Integer, String> queries = new HashMap<Integer, String>();
    scan = new Scanner(new File(params.get("queryFilePath")));
    line = null;
    do {
      line = scan.nextLine();
      String[] pair = line.split(":");
      queriesID.add(Integer.parseInt(pair[0].trim()));
      queries.put(Integer.parseInt(pair[0].trim()), pair[1].trim());
    } while (scan.hasNext());
    scan.close();

    // evaluate retrieval algorithm
    for (Integer queryID : queriesID) {
      Qryop operation = parseQuery(queries.get(queryID));// retrieve first operation
      System.out.println(queryID + "\t" + queries.get(queryID));
      // printResults(queryID, operation.evaluate(model));
      writeTrecEvalFile(params.get("trecEvalOutputPath"), queryID, operation.evaluate(model));
    }

    /*
     * The code below is an unorganized set of examples that show you different ways of accessing
     * the index. Some of these are only useful in HW2 or HW3.
     */
    /*
     * // Lookup the document length of the body field of doc 0.
     * System.out.println(s.getDocLength("body", 0));
     * 
     * // How to use the term vector. TermVector tv = new TermVector(1, "body");
     * System.out.println(tv.stemString(100)); // get the string for the 100th stem
     * System.out.println(tv.stemDf(100)); // get its df System.out.println(tv.totalStemFreq(100));
     * // get its ctf
     * 
     * /** The index is open. Start evaluating queries. The examples below show query trees for two
     * simple queries. These are meant to illustrate how query nodes are created and connected.
     * However your software will not create queries like this. Your software will use a query
     * parser. See parseQuery.
     * 
     * The general pattern is to tokenize the query term (so that it gets converted to lowercase,
     * stopped, stemmed, etc), create a Term node to fetch the inverted list, create a Score node to
     * convert an inverted list to a score list, evaluate the query, and print results.
     * 
     * Modify the software so that you read a query from a file, parse it, and form the query tree
     * automatically.
     */

    // Later HW assignments will use more RAM, so you want to be aware
    // of how much memory your program uses.

    printMemoryUsage(false);
    long endTime = System.currentTimeMillis();
    System.out.println("Total running time: " + (endTime - startTime)/1000 + "s.");

  }

  /**
   * Write an error message and exit. This can be done in other ways, but I wanted something that
   * takes just one statement so that it is easy to insert checks without cluttering the code.
   * 
   * @param message
   *          The error message to write before exiting.
   * @return void
   */
  static void fatalError(String message) {
    System.err.println(message);
    System.exit(1);
  }

  /**
   * Get the external document id for a document specified by an internal document id. If the
   * internal id doesn't exists, returns null.
   * 
   * @param iid
   *          The internal document id of the document.
   * @throws IOException
   */
  static String getExternalDocid(int iid) throws IOException {
    Document d = QryEval.READER.document(iid);
    String eid = d.get("externalId");
    return eid;
  }

  /**
   * Finds the internal document id for a document specified by its external id, e.g.
   * clueweb09-enwp00-88-09710. If no such document exists, it throws an exception.
   * 
   * @param externalId
   *          The external document id of a document.s
   * @return An internal doc id suitable for finding document vectors etc.
   * @throws Exception
   */
  static int getInternalDocid(String externalId) throws Exception {
    Query q = new TermQuery(new Term("externalId", externalId));

    IndexSearcher searcher = new IndexSearcher(QryEval.READER);
    TopScoreDocCollector collector = TopScoreDocCollector.create(1, false);
    searcher.search(q, collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;

    if (hits.length < 1) {
      throw new Exception("External id not found.");
    } else {
      return hits[0].doc;
    }
  }

  /**
   * parseQuery converts a query string into a query tree.
   * 
   * @param qString
   *          A string containing a query.
   * @param qTree
   *          A query tree
   * @throws IOException
   */
  static Qryop parseQuery(String qString) throws IOException {

    Qryop currentOp = null;
    Stack<Qryop> stack = new Stack<Qryop>();

    // Add a default query operator to an unstructured query. This
    // is a tiny bit easier if unnecessary whitespace is removed.

    qString = qString.trim();

    if (qString.charAt(0) != '#') {
      qString = "#or(" + qString + ")";
    }

    // Tokenize the query.

    StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()", true);
    String token = null, field = null;

    // Each pass of the loop processes one token. To improve
    // efficiency and clarity, the query operator on the top of the
    // stack is also stored in currentOp.
    boolean isFirstOp = true; // used to check whether this op is the left most op
    while (tokens.hasMoreTokens()) {

      token = tokens.nextToken();

      if (token.matches("[ ,(\t\n\r]")) {
        // Ignore most delimiters.
      } else if (token.equalsIgnoreCase("#and")) { // AND
        currentOp = new QryopSlAnd();
        stack.push(currentOp);
        isFirstOp = false;
      } else if (token.equalsIgnoreCase("#syn")) { // SYN
        currentOp = new QryopIlSyn();
        stack.push(currentOp);
        isFirstOp = false;
      } else if (token.equalsIgnoreCase("#or")) { // OR
        currentOp = new QryopSlOr();
        stack.push(currentOp);
        isFirstOp = false;
      } else if (token.matches("(?i)#near/\\d+")) {// NEAR
        // if the query's highest level is near
        // we should warp this query with QryopSlScore
        if (isFirstOp) {
          stack.push(new QryopSlScore());
          isFirstOp = false;
        }
        int dist = Integer.parseInt(token.split("/")[1]);
        currentOp = new QryopIlNear(dist);
        stack.push(currentOp);
      } else if (token.startsWith(")")) { // Finish current query operator.
        // If the current query operator is not an argument to
        // another query operator (i.e., the stack is empty when it
        // is removed), we're done (assuming correct syntax - see
        // below). Otherwise, add the current operator as an
        // argument to the higher-level operator, and shift
        // processing back to the higher-level operator.
        // if (!noIlOperator)
        // noIlOperator = true;
        stack.pop();

        if (stack.empty())
          break;

        Qryop arg = currentOp;
        currentOp = stack.peek();
        currentOp.add(arg);
      } else {
        // NOTE: You should do lexical processing of the token before
        // creating the query term, and you should check to see whether
        // the token specifies a particular field (e.g., apple.title).
        String[] tokenized = tokenizeQuery(token);
        // System.out.println(tokenized.length);
        if (tokenized != null && tokenized.length != 0) {
          token = tokenized[0];
          if (token.matches("(?i).+(\\.)(body|url|keywords|title|inlink)")) {
            String[] splited = token.split("\\.");
            token = splited[0];
            field = splited[1];
            currentOp.add(new QryopIlTerm(token, field));
          } else if (tokenized.length > 1
                  && tokenized[1].matches("(body|url|keyword|title|inlink)")) {
            // Edge case :
            // if the term is a number dot field (like 2.keywords), after being tokenized, 
            // number and field would be separated into two columns, and "keywords" would 
            // be truncated to "keyword"
            currentOp.add(new QryopIlTerm(token, tokenized[1]));
          } else
            currentOp.add(new QryopIlTerm(token));

        }
      }
    }

    // A broken structured query can leave unprocessed tokens on the
    // stack, so check for that.

    if (tokens.hasMoreTokens()) {
      System.err.println("Error:  Query syntax is incorrect.  " + qString);
      return null;
    }

    return currentOp;
  }

  /**
   * Print a message indicating the amount of memory used. The caller can indicate whether garbage
   * collection should be performed, which slows the program but reduces memory usage.
   * 
   * @param gc
   *          If true, run the garbage collector before reporting.
   * @return void
   */
  public static void printMemoryUsage(boolean gc) {

    Runtime runtime = Runtime.getRuntime();

    if (gc) {
      runtime.gc();
    }

    System.out.println("Memory used:  "
            + ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)) + " MB");
  }

  /**
   * Print the query results.
   * 
   * THIS IS NOT THE CORRECT OUTPUT FORMAT. YOU MUST CHANGE THIS METHOD SO THAT IT OUTPUTS IN THE
   * FORMAT SPECIFIED IN THE HOMEWORK PAGE, WHICH IS:
   * 
   * QueryID Q0 DocID Rank Score RunID
   * 
   * @param queryName
   *          Original query.
   * @param result
   *          Result object generated by {@link Qryop#evaluate()}.
   * @throws IOException
   */
  static void printResults(int queryID, QryResult result) throws IOException {
    for (int i = 0; i < result.docScores.scores.size(); i++) {
      System.out.print(queryID + "\t" + "Q0" + "\t");
      if (result.docScores.scores.size() == 0)
        System.out.println("dummy" + "\t" + "1" + "\t" + "0" + "\t" + "run-1");
      else {
        System.out.println(getExternalDocid(result.docScores.getDocid(i)) + "\t" + (i + 1) + "\t"
                + result.docScores.getDocidScore(i) + "\t" + "run-1");
      }
    }
  }

  /**
   * Write the query results to TrecEval file
   * 
   * The TrecEval file has the following format:
   * QueryID Q0 DocID Rank Score RunID
   * 
   * @param filePath
   *          fiel path
   * @param queryID
   *          query ID for this query
   * @param result
   *          Result object generated by {@link Qryop#evaluate()}.
   */
  static void writeTrecEvalFile(String filePath, int queryID, QryResult result) {

    BufferedWriter writer = null;
    File file = new File(filePath);
    try {
      writer = new BufferedWriter(new FileWriter(file, true));
      //writer = new BufferedWriter(new FileWriter(file, false));
      // use false and true to control append function. false is used for debugging
      // false : overwrite the file; true : append mode, no overwrite
      int numDocs = Math.min(100, result.docScores.scores.size());
      if (result.docScores.scores.size() == 0)
        writer.write(queryID + "\t" + "Q0" + "\t" + "dummy" + "\t" + "1" + "\t" + "0" + "\t"
                + "run-1\n");
      else {
        // get best 100 match
        // put 100 matches in a PriorityQueue, traverse the rest matches, if any
        // match is bigger than the smallest one in the PQ, remove the head of PQ
        // add this bigger one
        //result.docScores.initalExtDocID(); //initialize all external doc id
        Comparator<ScoreList.ScoreListEntry> SCORE_ORDER = new ScoreList.ScoreOrder();
        PriorityQueue<ScoreList.ScoreListEntry> pq = new PriorityQueue<ScoreList.ScoreListEntry>(
                numDocs, SCORE_ORDER);
        int cnt = 0;
        for (; cnt < numDocs; cnt++) {
          pq.add(result.docScores.scores.get(cnt));
        }
        for (; cnt < result.docScores.scores.size(); cnt++) {
          if (result.docScores.scores.get(cnt).compareTo(pq.peek()) > 0) {
            pq.poll();
            pq.add(result.docScores.scores.get(cnt));
          }
        }
        ScoreList.ScoreListEntry[] topRank = new ScoreList.ScoreListEntry[numDocs];
        for (int i = numDocs; i > 0; i--) {
          topRank[i - 1] = pq.poll();
        }
        // Long endTime = System.currentTimeMillis();
        // System.out.println("sort result : " + (endTime - startTime)/1000 + "s");
        for (int i = 0; i < numDocs; i++) {
          writer.write(queryID + "\t" + "Q0" + "\t");
          writer.write(getExternalDocid(topRank[i].getDocid()) + "\t" + (i + 1) + "\t"
                  + topRank[i].getScore() + "\t" + "run-1\n");
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        writer.close();
      } catch (Exception e) {
      }
    }
  }

  /**
   * Given a query string, returns the terms one at a time with stopwords removed and the terms
   * stemmed using the Krovetz stemmer.
   * 
   * Use this method to process raw query terms.
   * 
   * @param query
   *          String containing query
   * @return Array of query tokens
   * @throws IOException
   */
  static String[] tokenizeQuery(String query) throws IOException {

    TokenStreamComponents comp = analyzer.createComponents("body", new StringReader(query));
    TokenStream tokenStream = comp.getTokenStream();

    CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
    tokenStream.reset();

    List<String> tokens = new ArrayList<String>();
    while (tokenStream.incrementToken()) {
      String term = charTermAttribute.toString();
      tokens.add(term);
    }
    return tokens.toArray(new String[tokens.size()]);
  }
}
