package pretty.bow.core

import scala.io.Source
import java.io.File
import java.io.PrintWriter
import java.io.FileWriter


object SortSVMWeightUtil {

  def main(args: Array[String]): Unit = {
    
    var lines = List[(Float,String)]()
    for(line <- Source.fromFile(new File("resources/svmweight.txt")).getLines()){
       if(!line.trim().equals("")){
    	   	val cells = line.split("\\s+")
            if(cells.length == 4 ) lines ::= (cells(2).toFloat, cells(3))
            else lines ::= (cells(2).toFloat, cells(3)+" "+cells(4))
       }
    }
    val writer = new PrintWriter(new FileWriter(new File("resources/sortsvmweight.txt")))
    lines.sortBy(f => f._1).reverse.zipWithIndex.foreach{case(e, i) => {
           writer.println(e)
    }}
    writer.close()

  }

}