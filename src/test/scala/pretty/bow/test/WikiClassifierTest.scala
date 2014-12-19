package pretty.bow.test

import java.io.File
import org.junit.Test
import storage.JDBCConnectionWrapper
import java.io.FileWriter
import java.io.PrintWriter
import pretty.bow.core.BagOfWord
import pretty.bow.core.BuildArff
import pretty.bow.core.TrainingModel

class WikiClassifierTest {

  @Test
  def prepareFeature() = {
    val jdbcConnection = new JDBCConnectionWrapper("com.mysql.jdbc.Driver",
      "jdbc:mysql://ec2-54-251-233-86.ap-southeast-1.compute.amazonaws.com:3306/business_graph?useUnicode=true&characterEncoding=UTF-8", "release4", "release4");
    BagOfWord.config(BagOfWord.AllFeatureModels._1, 5, 1, 2, false, false)
    BagOfWord.clearIndex
    val hotel = jdbcConnection.select("select id,text from crawl_wiki where title like '%hotel%' ")
    var i = 0
    var ids = scala.collection.mutable.MutableList[(String, String)]()
    while (hotel.next() && i < 3000) {

      val text = hotel.getString("text").trim().replaceAll("\\s+", "")
      if (text.length() > 50) {
        i += 1
        println(i)
        val id = hotel.getString("id")
        ids += (("1", id))
        BagOfWord.addIndex(id, text, false)
      }
    }
    hotel.close()
    BagOfWord.commitIndex
    val pw = new java.io.PrintWriter(new FileWriter("ids", true))
    try ids.foreach(f => { pw.write(f._1 + "," + f._2 + "\n") }) finally pw.close()

    println("count:" + BagOfWord.lucene.findIndexDbCount())
    i = 0
    val nothotel = jdbcConnection.select("select id,text from crawl_wiki where title not like '%hotel%' ")
    while (nothotel.next() && i < 30000) {
      val text = nothotel.getString("text").trim().replaceAll("\\s+", "")
      if (text.length() > 50) {
        i += 1
        println(i)
        val id = nothotel.getString("id")
        ids += (("0", id))
        BagOfWord.addIndex(id, text, false)
        if (i % 1000 == 0) {
          BagOfWord.commitIndex
          val pw = new java.io.PrintWriter(new FileWriter("ids", true))
          try ids.foreach(f => { pw.write(f._1 + "," + f._2 + "\n") }) finally pw.close()
        }
      }
    }
    nothotel.close()
    BagOfWord.commitIndex
    println("count:" + BagOfWord.lucene.findIndexDbCount())
    BuildArff.buildSparseArff(ids.toList, new File("resources/wikifeature.arff"))
  }

  @Test
  def training() = {
    val trainer = new TrainingModel("resources/trainingdata.arff", "resources/mymodel.model")
    trainer.currentModel = trainer.models._3
    trainer.evaluate
    val model = trainer.training
    val writer = new PrintWriter(new FileWriter(new File("resources/modelinstringformat.model")))
    writer.println(model)
    writer.close()
    trainer.saveModel(model)
  }

  @Test
  def predicting() = {
    val trainer = new TrainingModel(modelPath = "resources/mymodel.model")
    val result = trainer.predict("hotel bbq dreamS")
    println(result)
  }

}