package pretty.bow.core

import scala.Array.canBuildFrom
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.mapAsScalaMap
import org.apache.lucene.util.Version
import org.apache.commons.lang3.math.NumberUtils
import lucene.LuceneIndexWrapper
import pretty.bow.nlp.Parser
import pretty.bow.nlp.Parser

object BagOfWord {
  var lucene = LuceneIndexWrapper.init("D://LuceneBIG", Version.LUCENE_4_9, false, false);
  val AllFeatureModels = ("Binary", "TF", "TFIDF")
  var featureModel = AllFeatureModels._1
  var termDict = List[String]()
  var minDocFrequency = 0
  var minGram = 1
  var maxGram = 1
  var lemma = false
  var stem  = false
  
  def config(featureModel: String, minDocFrequency: Int, minGram: Int, maxGram: Int ,
      lemma: Boolean , stem: Boolean) = {
    this.featureModel = featureModel
    this.minDocFrequency = minDocFrequency
    this.minGram = minGram
    this.maxGram = maxGram
    this.lemma = lemma
    this.stem  = stem
    this.lucene = lucene.initConfig(minGram, maxGram, stem)
  }

  def main(args: Array[String]): Unit = {
    BagOfWord.config(BagOfWord.AllFeatureModels._1, 0, 1, 2, false, false)
    BagOfWord.clearIndex
    println("ok")
    addIndex("1", "a wangxiangtao Hello good", false)
    addIndex("2", "the wangxiangtao dogs 2 dogs dream ddddddd 3.3 4.2", false)
    addIndex("3", "hotel address place park", false)
    BagOfWord.commitIndex()
    println(initialTermDict)
    println(getFeatureByIndexedId("1"))
    println(getFeatureByIndexedId("2"))
    println(getFeatureByIndexedId("3"))
//  val featureMapForPredictedData = getFeatureMapForPredictedData("4","wangxiangtao dogs tell")   //  will use the previous termDict.  
//  println(featureMapForPredictedData)
  }

  def getFeatureByIndexedId(id: String): String = {
    if (termDict.size == 0) initialTermDict()
    if (featureModel.equals(AllFeatureModels._1))
      return getBinaryVector(id)
    else if (featureModel.equals(AllFeatureModels._1))
      return getTFVector(id)
    else return getTFIDFVector(id)
  }

  def getFeatureMapForPredictedData(id: String, text: String): Map[String, String] = {
    if (termDict.size == 0) initialTermDict()
    addIndex(id, text, true)
    val feature = getFeatureByIndexedId(id).split(",")
    deleteIndexById(id, true)
    termDict.zip(feature).toMap
  }

  def addIndex(id: String, text: String, commit: Boolean) = {
    val normlizedText = tokenizeDoc(text,lemma).mkString(" ")
    lucene.indexDocWithDefinedField(id, normlizedText)
    if (commit) commitIndex
  }

  def commitIndex() = {
    lucene.commit(true)
  }

  def deleteIndexById(id: String, commit: Boolean) = {
    lucene.delete(id)
    if (commit) commitIndex
  }

  def clearIndex() = {
    lucene.clear()
    lucene.commit(true)
  }

  def initialTermDict(): List[String] = {
    termDict = lucene.getTerms().
      filter(p => !p.trim().equals("") && !p.equals("class")).
      filter(p => lucene.getDocumentFrequency(p) > minDocFrequency).toList
    termDict
  }

  def getTFVector(id: String): String = {
    if (termDict.size == 0) initialTermDict()
    val frequencyMap = lucene.getTermVectorById(id)
    termDict.map(f => {
      frequencyMap.getOrElse(f, 0)
    }).toList.mkString(",")
  }

  def getBinaryVector(id: String): String = {
    if (termDict.size == 0) initialTermDict()
    val frequencyMap = lucene.getTermVectorById(id)
    termDict.map(f => {
      if (frequencyMap.getOrElse(f, 0).asInstanceOf[Integer] > 0) 1 else 0
    }).toList.mkString(",")
  }

  def getTFIDFVector(id: String): String = {
    if (termDict.size == 0) initialTermDict()
    val frequencyMap = lucene.getTermVectorById(id)
    val numberOfUniqueToken = frequencyMap.size().toFloat
    termDict.map(f => {
      val termFrequency = frequencyMap.getOrElse(f, 0).asInstanceOf[Integer]
      val docFrequency = lucene.getDocumentFrequency(f).toFloat
      val tfidf: Float = if (termFrequency == 0) 0
      else (termFrequency / numberOfUniqueToken * Math.log(lucene.findIndexDbCount() / docFrequency)).toFloat
      tfidf
    }).toList.mkString(",")
  }

  def tokenizeDoc(text: String, lemma: Boolean) = {
    if(lemma) Parser.wordTokenizeWithLemma(text)
    else Parser.wordTokenize(text)
  }
}