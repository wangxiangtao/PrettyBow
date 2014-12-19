PrettyBow
=========

About the library

The library provides facilities for:

Recursively descending directories, finding text files.
Finding `document' boundaries when there are multiple documents per file.
Tokenizing a text file, according to several different methods.
Including N-grams among the tokens.
Mapping strings to integers and back again, very efficiently.
Building a sparse matrix of document/token counts.
Pruning vocabulary by word counts or by information gain.
Building and manipulating word vectors.
Setting word vector weights according to Naive Bayes, TFIDF, and several other methods.
Smoothing word probabilities according to Laplace (Dirichlet uniform), M-estimates, Witten-Bell, and Good-Turning.
Scoring queries for retrieval or classification.
Writing all data structures to disk in a compact format.
Reading the document/token matrix from disk in an efficient, sparse fashion.
Performing test/train splits, and automatic classification tests.
Operating in server mode, receiving and answering queries over a socket.
