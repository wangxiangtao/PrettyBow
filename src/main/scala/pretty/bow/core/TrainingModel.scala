package pretty.bow.core

import weka.classifiers.trees.J48
import java.io.BufferedReader
import weka.classifiers.functions.SMO
import weka.core.Instances
import weka.classifiers.bayes.NaiveBayesMultinomial
import weka.core.SparseInstance
import weka.classifiers.rules.PART
import java.io.File
import weka.classifiers.Classifier
import weka.core.Attribute
import weka.core.Instance
import java.io.FileReader
import weka.classifiers.Evaluation
import java.util.Random
import java.util.UUID

case class TrainedModel(
    classifier: Classifier, 
    dataset: Instances, 
    vocab: Map[Int,String])
    
class TrainingModel (arffIn: String = null, modelPath: String){
  
  val models = (new NaiveBayesMultinomial(),new J48(),SMO,new PART()) 
  var currentModel : Classifier = models._1
  val trainedModel  = readModel(modelPath).asInstanceOf[TrainedModel] 
  
  def predict(input: String): Double = {
    if( trainedModel.isInstanceOf[TrainedModel]) {
        val inst = buildInstance(input, trainedModel)
	    val pdist = trainedModel.classifier.distributionForInstance(inst)
	    pdist.zipWithIndex.maxBy(_._1)._2  // _1 is e, _2 is index,  so will return index 
    }
    else return -1
  }
  
    
  def buildInstance(input: String, model: TrainedModel): Instance = {
    val feature = BagOfWord.getFeatureMapForPredictedData(UUID.randomUUID.toString.replace("-", "") , input)
    val inst = new SparseInstance(model.vocab.size)
    model.vocab.foreach{ case(index,word) => {
       inst.setValue(index, feature.getOrElse(word, "0").toDouble)
    }}  
    inst.setDataset(model.dataset)
    inst
  }

  /////////////////// train models //////////////////
  
  def training = {
      trainModel(new File(arffIn),currentModel)
  }
  
  def SMO = {
      val smo = new SMO()
      smo.setC(0.3D)
      smo.setToleranceParameter(0.1D)
      smo
  }
  
  def trainModel(arff: File, classifier: Classifier): TrainedModel = {
    val _instances = readArffFile(arff)
    println("Be careful, you set the last attribute as class !")
    classifier.buildClassifier(_instances)
    val trainedModel = TrainedModel(classifier , _instances, readAttributes(_instances))
    trainedModel
  }
  
  def evaluate = {
    val _instances = readArffFile(new File(arffIn))
    val eval = new Evaluation(_instances);
	eval.crossValidateModel(currentModel, _instances, 10, new Random(1));
	println(eval.confusionMatrix()(0)(0))
		println(eval.confusionMatrix()(0)(1))
			println(eval.confusionMatrix()(1)(1))
				println(eval.confusionMatrix()(1)(0))

	println(eval.toSummaryString("Results\n ", false));
  }
  
  def saveModel(model: TrainedModel) = {
    weka.core.SerializationHelper.write(modelPath, model)
  }
  
  def readModel(path: String): TrainedModel = {
    try{
      weka.core.SerializationHelper.read(path).asInstanceOf[TrainedModel]
    }
    catch {
        case e: Exception => {
          println("no existed model file ")
          null
        }
    }
  }
  
  private def readArffFile(arff: java.io.File): weka.core.Instances = {
    val reader = new BufferedReader(new FileReader(arff))
    val _instances = new Instances(reader)
    reader.close()
    _instances.setClassIndex(_instances.numAttributes()-1)
    _instances
  }
  
  private def readAttributes(_instances: Instances) = {
    val _vocab = scala.collection.mutable.Map[Int,String]()
    val e = _instances.enumerateAttributes()
    while (e.hasMoreElements()) {
      val attrib = e.nextElement().asInstanceOf[Attribute]
      if (! "class".equals(attrib.name())) 
        _vocab += (( attrib.index(), attrib.name() ))
    }
    _vocab.toMap
  }
}