package pretty.bow.core

import java.io.PrintWriter
import java.io.File
import java.io.FileWriter

object BuildArff {

  def main(args: Array[String]): Unit = {
     
     BagOfWord.clearIndex
     BagOfWord.addIndex("1","how are you hotel",false)
     BagOfWord.addIndex("2","have a 7.7 nice trip hotel",false)
     BagOfWord.addIndex("3","Where is my bag , 0 your bag on my desk hotel",false)
     BagOfWord.addIndex("4", "Where is my bag , 0 your bag on my desk clip",false)
     BagOfWord.addIndex("5", "goold today bear building clip enough",false)
     BagOfWord.addIndex("6", "cook enough clip",false)

     BagOfWord.commitIndex
     
     BagOfWord.minDocFrequency = 0
     BagOfWord.featureModel = BagOfWord.AllFeatureModels._1
     
     val ids = List(("1","1"),("1","2"),("1","3"),("0","4"),("0","5"),("0","6"))
     buildSparseArff(ids, new File("resources/wikiclassifer1.arff"))
     buildNormalArff(ids, new File("resources/wikiclassifer2.arff"))
  }
  
  def buildNormalArff(ids: List[(String,String)], arffout: File): Unit = {
    val arffWriter = openWriter(arffout)
    writeHeader(arffWriter)
    writeNormalData(arffWriter, ids)
    closeWriter(arffWriter)
  }
  
  def buildSparseArff(ids: List[(String,String)], arffout: File): Unit = {
    val arffWriter = openWriter(arffout)
    writeHeader(arffWriter)
    writeSparseData(arffWriter, ids)
    closeWriter(arffWriter)
  }
  
  def openWriter(f: File): PrintWriter = new PrintWriter(new FileWriter(f))
  
  def writeHeader(w: PrintWriter): Unit = {
    val termDict = BagOfWord.initialTermDict
    println("termDict size:"+termDict.size)
    w.println("@relation Wikipedia")
    w.println()
    w.println("@attribute 'class' {0,1}")  //  the order is important, {1,0} will have problem when use sparse arff
    termDict.foreach(f => {
       w.println("@attribute '"+f+"' numeric ")
    })
    w.println()
    w.println("@data")
  }
  
  def writeNormalData(w: PrintWriter, ids: List[(String,String)]): Unit = {
    ids.foreach(id => {
       val feature = BagOfWord.getFeatureByIndexedId(id._2)
       if(feature.indexOf("1") != -1)
       w.println(id._1+","+feature)
    })
  }
  
  def writeSparseData(w: PrintWriter, ids: List[(String,String)]): Unit = {
    ids.foreach(id => {
       val record = id._1+","+BagOfWord.getFeatureByIndexedId(id._2)
       var indexRecord = record.split(",").toList.zipWithIndex.map{ case (e, i)  =>  (i)+" "+e }
       val sparseRecord = indexRecord.filter(p => !p.split(" ")(1).equals("0")).mkString(",")
       if(!sparseRecord.equals("") && !sparseRecord.equals("0 1"))
             w.println("{"+sparseRecord+"}")
       else println(id._2+"-"+sparseRecord)
       
    })
  }
   
  def closeWriter(w: PrintWriter): Unit = {
    w.flush()
    w.close()
  }
  
}