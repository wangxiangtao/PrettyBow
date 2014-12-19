package pretty.bow.nlp
import scala.collection.JavaConversions._
import java.io.StringReader
import scala.Option.option2Iterable
import scala.collection.JavaConversions._
import scala.collection.JavaConversions._
import edu.stanford.nlp.parser.lexparser.LexicalizedParser
import edu.stanford.nlp.process.CoreLabelTokenFactory
import edu.stanford.nlp.process.PTBTokenizer
import edu.stanford.nlp.process.Tokenizer
import edu.stanford.nlp.process.TokenizerFactory
import edu.stanford.nlp.trees.Tree
import edu.stanford.nlp.trees.TreeGraph
import edu.stanford.nlp.trees.TreeGraphNode
import edu.stanford.nlp.trees.TreePrint
import edu.stanford.nlp.trees.Trees
import java.util.ArrayList
import edu.stanford.nlp.trees.PennTreebankLanguagePack
import edu.stanford.nlp.semgraph.SemanticGraph
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import edu.stanford.nlp.pipeline.Annotation
import java.util.Properties
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation
import edu.stanford.nlp.ling.Word
import scala.collection.mutable.ListBuffer
import edu.stanford.nlp.util.CoreMap
import edu.stanford.nlp.semgraph.SemanticGraphEdge
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation
object Parser {

  //      def main(args: Array[String]) {
  //            Parser.createCoreMap(" Adobe Media Optimizer (AMO)")
  //      }

  val props = new Properties();
  props.put("annotators", "tokenize,ssplit");
  props.put("parse.model", "edu/stanford/nlp/models/lexparser/englishRNN.ser.gz");

  val pipeline = new StanfordCoreNLP(props);

  def createCoreMap(sentence: String): CoreMap = {
    val document = new Annotation(sentence)
    try {
      pipeline.annotate(document)
    } catch {
      case e: Exception => {
        println("Error:" + sentence)
        return null
      }
    }
    document.get(classOf[SentencesAnnotation]).toList(0)
  }

  def sentTokenize(para: String): List[String] = {
    val doc = new Annotation(para)
    val props = new Properties();
    props.put("annotators", "tokenize, ssplit")
    val pipeline = new StanfordCoreNLP(props);
    pipeline.annotate(doc)
    doc.get(classOf[SentencesAnnotation])
      .map(coremap => coremap.get(classOf[TextAnnotation])).toList
  }

  def wordTokenize(text: String): List[String] = {
    val sent = new Annotation(text)
    val props = new Properties();
    props.put("annotators", "tokenize, ssplit")
    val pipeline = new StanfordCoreNLP(props);
    sent.get(classOf[SentencesAnnotation]).map { sentence =>
      sentence.get(classOf[TokensAnnotation])
        .map(corelabel => corelabel.get(classOf[TextAnnotation]))
        .toList
    }.flatten.toList
  }

  def wordTokenizeWithLemma(text: String): List[String] = {
    val sent = new Annotation(text)
    val props = new Properties();
    props.put("annotators", "tokenize, ssplit, pos, lemma")
    val pipeline = new StanfordCoreNLP(props);
    sent.get(classOf[SentencesAnnotation]).map { sentence =>
      sentence.get(classOf[TokensAnnotation])
        .map(corelabel => corelabel.get(classOf[LemmaAnnotation]))
    }.flatten.toList
  }

  def buildTree(coreMap: CoreMap): Tree = {
    coreMap.get(classOf[TreeAnnotation])
  }

  def buildSemanticGraph(coreMap: CoreMap): SemanticGraph = {
    val SemanticGraph = coreMap.get(classOf[CollapsedCCProcessedDependenciesAnnotation])
    SemanticGraph
  }

  def createTreeGraphReturnRoot(tree: Tree): TreeGraphNode = {
    val treegraph = new TreeGraph(tree)
    val root = treegraph.root()
    root
  }

  def extractPath(semanticGraph: SemanticGraph, FirWord: Int, SecWord: Int): String = {
    val relationMap = scala.collection.mutable.Map[(String, String), String]()
    var edges: java.util.List[SemanticGraphEdge] = null
    try {
      edges = semanticGraph.getShortestUndirectedPathEdges(semanticGraph.getNodeByIndex(FirWord), semanticGraph.getNodeByIndex(SecWord))
    } catch {
      case e: Exception => {
        return null
      }
    }
    if (edges == null) return null
    edges.foreach(f => {
      relationMap.put((f.getSource().word(), f.getTarget().word()), "-->" + f.getRelation() + "-->")
      relationMap.put((f.getTarget().word(), f.getSource().word()), "<--" + f.getRelation() + "<--")
    })
    val nodes = semanticGraph.getShortestUndirectedPathNodes(semanticGraph.getNodeByIndex(FirWord), semanticGraph.getNodeByIndex(SecWord)).toList
    nodes.sliding(2, 1).toList.map(f => { f(0).word() + "/" + f(0).tag() + " (" + relationMap.get(f(0).word(), f(1).word()).getOrElse("") + ")" })
      .filter(p => p.indexOf("conj_and") == -1).mkString(" ")
      .split(" ").toList.patch(0, Nil, 1).mkString(" ") // remove header of list
  }

  def splitSentence(tree: TreeGraphNode, sentence: String): ListBuffer[String] = {
    val children = tree.children
    if (children == null) return ListBuffer.empty
    var subSentence = scala.collection.mutable.ListBuffer[String]()
    children.foreach(f => {
      val result = splitSentence(f, sentence)
      subSentence = result
    })
    if (tree.value().equals("SBAR") || tree.value().equals("SINV") || tree.value().equals("S")) {
      var s = sentence.substring(
        tree.yieldWords().toList.head.beginPosition(),
        tree.yieldWords().toList.last.endPosition())
      subSentence.foreach(p => { s = s.replace(p, "") })
      if (subSentence.size == 0 || s.length != 0)
        subSentence = s +: subSentence
    }
    return subSentence
  }

  def extractNP(coreMap: CoreMap): ListBuffer[String] = {
    val tree = Parser.buildTree(coreMap)
    val words = tree.yieldWords().map(f => { f.toString() }).toList
    val root = Parser.createTreeGraphReturnRoot(tree)
    extractNP(root)
  }

  def extractNP(tree: TreeGraphNode): ListBuffer[String] = {
    val tags = tree.labeledYield().map(f => { f.tag().value() }).toList
    val tagForSubject = List("NNP", "NN", "NNPS", "NNS", "PRP$", "PRP")
    var resultlist = scala.collection.mutable.ListBuffer[String]()
    if (tree.value().equals("NP") && tags.intersect(tagForSubject).size > 0) {
      tree.children().foreach(f => {
        val NPs = extractNP(f)
        resultlist = resultlist ++ NPs
      })
      if (tree.yieldWords().size() > 0 && resultlist.size == 0) {
        resultlist.add(tree.yieldWords().mkString(" "))
      }
    } else {
      val children = tree.children
      if (children == null) return resultlist
      children.foreach(f => {
        val NPs = extractNP(f)
        resultlist = resultlist ++ NPs
      })
    }
    return resultlist
  }

  def getCommonParentByProduct(tree: TreeGraphNode, product: String, root: TreeGraphNode): Tree = {
    val nodeListWithProduct = new ArrayList[Tree]()
    val productPhrase = product.split("\\s+").toList
    val words = tree.yieldWords().map(f => { f.toString() }).toList
    val productIndex = containsSubSequenceIndex(words, productPhrase)
    println(productIndex)
    getNodeWithProduct(tree, productIndex, nodeListWithProduct)
    Trees.getLowestCommonAncestor(nodeListWithProduct, root)
  }

  def getNodeWithProduct(tree: TreeGraphNode, productIndex: List[Int], nodeListWithProduct: ArrayList[Tree]) {
    if (productIndex.contains(tree.index())) {
      nodeListWithProduct.add(tree)
    }
    val children = tree.children
    children.foreach(child => {
      getNodeWithProduct(child, productIndex, nodeListWithProduct)
    })
  }

  def LongestCommonSubsequence[A, C](a: C, b: C)(
    implicit c2i: C => Iterable[A], cbf: collection.generic.CanBuildFrom[C, A, C]): C = {
    val builder = cbf()
    def ListLCS(a: Iterable[A], b: Iterable[A]): List[A] = {
      if (a.isEmpty || b.isEmpty) Nil
      else if (a.head == b.head) a.head :: ListLCS(a.tail, b)
      else {
        val case1 = ListLCS(a.tail, b)
        val case2 = ListLCS(a, b.tail)
        if (case1.length > case2.length) case1 else case2
      }
    }
    builder ++= ListLCS(c2i(a), c2i(b))
    builder.result()
  }

  def containsSubSequenceIndex(A: List[String], B: List[String]): List[Int] = { // ex: List(ADOMAST, LTD) , List(ADOMAST, MANUFACTURING, LTD) => List(ADOMAST, LTD)
    if (A.size < B.size) return List.empty
    val result = scala.collection.mutable.Set[Int]()
    var i = 1
    var j = 1
    for (i <- 1 to A.size) {
      if (A(i - 1).equals(B(j - 1))) {
        if (j == B.size) {
          for (t <- 0 to B.size - 1) result.add(i - t)
          j = 1
        } else j = j + 1
      } else if (A(i - 1).equals(B.head)) j = 1
    }
    result.toList.sorted
  }
}


