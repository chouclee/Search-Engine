                          QryEval, version 2.0
                            September 1, 2014


This software illustrates the architecture for the portion of a search
engine that evaluates queries.  It is a template for class homework
assignments, so it emphasizes simplicity over efficiency.  It has just
a few main components.

QryEval is the main class. Given a parameter file which specifies the
index path in a key value pair (index=path_to_index), it opens the
index and evaluates some hard-coded queries and prints the
results. You will need to modify this class so that it reads in more
parameters, reads an external query file, evaluates queries in the
file, and writes results to another file.  You will also need to
extend the query parser. This should be fairly simple, given that the
queries use prefix operators. Make sure to use the provided
tokenizeQuery(..)  method to process the raw query terms. Otherwise,
you may get zero results for queries that include stopwords or plural
words!

QryOp is an abstract class for all query operators (e.g., AND, OR, SYN,
NEAR/n, WINDOW/n, etc).  It has just a few data structures and methods
that are common to all query operators.  The rest of the class is
just abstract definitions of query operator capabilities.

QryopIl and QryopSl are extensions of Qryop that are specialized for
query opeators that return inverted lists (e.g., TERM, SYN, NEAR/n)
and query operators that return score lists (e.g., AND, SCORE).

QryopILTerm, QryopIlSyn, and QryopSlAnd are query operator
implementations for term (e.g., "apple"), synonym ("SYN"), and boolean
AND query operators.

This implementation contains 4 types of query operators:

  * The Term operator, which just fetches an inverted list from the index;

  * The Syn operator, which combines inverted lists;

  * The Score operator, which converts an inverted list into a score list; and

  * The And operator, which combines score lists.

It is convenient to treat query operators as members of one class that
return the same type of result, but some operators produce inverted
lists (e.g., Term, Syn), whereas others produce score lists (e.g.,
Score, And).  The solution is for all query operators to return
QryResult objects that encapsulate both types of result.  Some query
operators return populated inverted lists and empty score lists; other
query operators return empty inverted lists and populated score lists.

Query operator behavior depends upon the type of retrieval model being
used.  Some retrieval models have parameters.  RetrievalModel is an
abstract class for all retrieval models.  Its subclasses provide
places to store parameters and methods used to accomplish different
types of query evaluation.  This implementation contains a
RetrievalModelUnrankedBoolean that contains no parameters, but notice
how the behavior of QryopSlScore and QryopSlAnd can be altered
depending upon the specific retrieval model being used.

You will need to implement several other retrieval models.  For
example, to implement the Indri retrieval model, do the following.

  * Read the retrieval model name from the parameter file, and
    create the appropriate retrieval model.

  * Modify the QryopSlScore function to calculate a query likelihood
    score with Dirichlet smoothing, and to calculate default scores.

  * Modify the evaluate method of each query operator of type QryopSl
    to to implement the Indri score combinations.

This architecture makes it easy to support multiple retrieval models
within one implementation.

The ScoreList class provides a very simple implementation of a score
list.

The InvList class provides a very simple implementation of an inverted
list.

Query expansion and text mining operations require random access to
document term vectors. (Recall that a document term vector is a parsed
representation of a document. See lecture notes for details.)  The
TermVector class that provides a simple, Indri-like API that gives
access to the number of terms in a document, the vocabulary of terms
that occur in the document, the terms that occur at each position in
the document, and the frequency of each term.
