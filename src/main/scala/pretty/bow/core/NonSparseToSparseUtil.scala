package pretty.bow.core

import weka.core.converters.ArffSaver
import java.io.BufferedReader
import weka.core.Instances
import weka.filters.unsupervised.instance.NonSparseToSparse
import weka.filters.Filter
import java.io.FileReader
import java.io.File

object NonSparseToSparseUtil {

  def main(args: Array[String]): Unit = {
    val reader = new BufferedReader(new FileReader(new File("resources/nonsparse.arff")))
    val _instances = new Instances(reader)
    reader.close()
    val sp = new NonSparseToSparse();
    sp.setInputFormat(_instances);
    sp.batchFinished();
    val newdata = Filter.useFilter(_instances, sp)
    val saver = new ArffSaver();
    saver.setInstances(newdata);
    saver.setFile(new File("resources/sparse.arff"));
    saver.writeBatch();   
  }

}