package pretty.bow.core

import java.io.BufferedReader
import weka.core.Instances
import java.io.FileReader
import java.io.File
import weka.attributeSelection.Ranker
import weka.attributeSelection.InfoGainAttributeEval
import java.util.Arrays
import weka.attributeSelection.AttributeSelection
import weka.filters.unsupervised.instance.NonSparseToSparse
import weka.core.converters.ArffSaver
import weka.filters.unsupervised.attribute.Remove
import weka.filters.Filter

object FeatureSelection {

  def main(args: Array[String]): Unit = {

    val reader = new BufferedReader(new FileReader(new File("resources/prettymatch.data")))
    var _instances = new Instances(reader)
    println(_instances.numInstances())
    reader.close()
    _instances.setClassIndex(_instances.numAttributes() - 1)
    val ranker = new Ranker();
    val ig = new InfoGainAttributeEval();
    ig.buildEvaluator(_instances);

    val firstAttributes = ranker.search(ig, _instances);
    val candidates = Arrays.copyOfRange(firstAttributes, 0, 50);

    //	 for(i <- 0 to 50)	println( _instances.attribute(firstAttributes(i)))

    ranker.setNumToSelect(5000)
    val trainSelector = new AttributeSelection();
    trainSelector.setSearch(ranker);
    trainSelector.setEvaluator(ig);
    trainSelector.SelectAttributes(_instances);
    val Results = trainSelector.toResultsString()
//    println(Results)
    _instances = trainSelector.reduceDimensionality(_instances)
//    println(_instances.numAttributes())
    val saver = new ArffSaver();
    saver.setInstances(_instances);
    saver.setFile(new File("resources/trainingdata.arff"));
    saver.writeBatch();

  }

}