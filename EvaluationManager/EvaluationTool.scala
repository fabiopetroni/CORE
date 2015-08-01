// Copyright (C) 2015 Fabio Petroni
// Contact: http://www.fabiopetroni.com
//
// This file is part of CoreScript (a script to covert row file in libFM compliant format).
//
// CoreScript is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// CoreScript is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with CoreScript.  If not, see <http://www.gnu.org/licenses/>.
//
// Based on the publication:
// - Fabio Petroni, Luciano Del Corro and Rainer Gemulla (2015): 
//   "CORE: Context-Aware Open Relation Extraction with Factorization Machines".
//   EMNLP, 2015.

package evaluation

import java.io.{FileInputStream, PrintStream, File}
import collection.JavaConversions._
import io.Source
import collection.mutable
import util.matching.Regex
import collection.mutable.ArrayBuffer

/**
 * @author Sebastian Riedel anf Fabio Petroni
 */


object EvaluationTool {
  def main(args: Array[String]) {    
    if (Parameters.parse_arguments(args)>=0){
      val rankFileNamesAndLabels = Parameters.rankFileNamesAndLabels;
      val rankFileNamesAndLabelsSplit = rankFileNamesAndLabels.map(name => if (name.contains(":")) name.split(":") else Array(name, new File(name).getName))      
      val rankFileNames = rankFileNamesAndLabelsSplit.map(_.apply(0))
      val labels = rankFileNamesAndLabelsSplit.map(_.apply(1))
      val rankFiles = rankFileNames.map(new File(_))
      var relPatterns : Seq[Regex] = null;
      if (Parameters.configuration){
        //Freebase relations
        println("Freebase relations evaluation")
        relPatterns = Conf.targets_Freebase_relations.toSeq.map(_.r)      
      }
      else{
        //Surface relations
        println("Surface relations evaluation")
        relPatterns = Conf.targets_surface_relations.toSeq.map(_.r)      
      }
      evaluate(rankFiles, relPatterns, labels)
    }
  }
  
  class Eval(val pattern: Regex) {
    var totalGuess = 0

    def totalGoldTrue = goldTuplesTrue.size

    def totalGoldFalse = goldTuplesFalse.size

    var name: String = "N/A"

    def totalGold = totalGoldTrue + totalGoldFalse

    //fabio
    var true_in_poolDepth = 0
    var n = 0
    
    var tp = 0
    var fp = 0
    var sumPrecision = 0.0
    val precisions = new ArrayBuffer[Double]()
    val recalls = new ArrayBuffer[Double]()
    val missings = new ArrayBuffer[Int]()

    def interpolate(curve: Seq[(Double, Double)]) = {
      for (((r, p), index) <- curve.zipWithIndex) yield r -> curve.view.drop(index).map(_._2).max
    }

    def precisionRecallCurve(recallLevels: Seq[Double]) = {
      val p = 1.0 +: precisions
      val r = 0.0 +: recalls
      val result = new ArrayBuffer[(Double, Double)]
      var currentLevelIndex = 0
      def precAt(index: Int) = if (index == -1) 0.0 else p(index)
      for (level <- recallLevels) yield level -> precAt(r.indexWhere(_ >= level))
      //      var currentLevel = recallLevels(currentLevelIndex)
      //      var currentIndex = 0
      //      while (currentIndex < p.size && currentLevelIndex < recallLevels.size) {
      //        currentLevel = recallLevels(currentLevelIndex)
      //        val prec = p(currentIndex)
      //        val rec = r(currentIndex)
      //        if (rec >= currentLevel) {
      //          currentLevelIndex += 1
      //          //          result += rec -> prec
      //          result += currentLevel -> prec
      //
      //        }
      //        currentIndex += 1
      //      }
      //      result
    }

    var precisionCount = 0
    var mapDone = false
    val precisionAtK = new ArrayBuffer[(Int, Int, Double)]
    val avgPrecisionForFact = new mutable.HashMap[(Seq[Any], String), Double]
    val precisionForFact = new mutable.HashMap[(Seq[Any], String), Double]


    val relations = new mutable.HashSet[String]
    val goldTuplesTrue = new mutable.HashSet[(Seq[Any], String)]
    val goldTuplesFalse = new mutable.HashSet[(Seq[Any], String)]
    val guessTuplesTrue = new mutable.HashSet[(Seq[Any], String)]

    def copyGold = {
      val result = new Eval(pattern)
      result.relations ++= relations
      result.goldTuplesTrue ++= goldTuplesTrue
      result.goldTuplesFalse ++= goldTuplesFalse
      result.guessTuplesTrue ++= guessTuplesTrue
      result
    }

    def meanAvgPrecision = {
//      println("\ngoldTuplesTrue.size "+goldTuplesTrue.size)
      var result = 0.0
//      var i = 0;
      for (fact <- goldTuplesTrue) {
        val avgPrec = precisionForFact.getOrElse(fact, 0.0)
        result += avgPrec
//        println(i + " "+fact._1)
//        println("- avgPrec : "+avgPrec)
//        println("- result : "+result)
//        i+=1
      }
      val den = Math.min(100, goldTuplesTrue.size)
      val x = result / den //100 //true_in_poolDepth //goldTuplesTrue.size
//      println("result "+x+"\n")
      x
    }
    
    def debugMeanAvgPrecision{
      println("\ngoldTuplesTrue.size "+goldTuplesTrue.size)
      var result = 0.0
      var counter = 0
      for (fact <- goldTuplesTrue) {
        val avgPrec = precisionForFact.getOrElse(fact, 0.0)
        
        if (avgPrec > 0){
          result += avgPrec
          println("- avgPrec : "+avgPrec)
          counter+=1
        }
      }
      println("counter: "+counter)
      val x = result / goldTuplesTrue.size
      println("result "+x+"\n")
    }

    def precisionAtRecall(recall: Double, depth: Int) = {
      if (recall == 0.0) 1.0
      else {
        val max = math.min(depth, precisions.size)
        val filtered = Range(0, max).filter(i => recalls(i) >= recall)
        if (filtered.size == 0) 0.0 else filtered.map(precisions(_)).max
      }
    }

    def precision = tp.toDouble / totalGuess

    def recall = tp.toDouble / totalGoldTrue

    def avgPrecision = sumPrecision / totalGuess

    def missingLabels = totalGuess - tp - fp

    override def toString = {
      """------------------
        |Pattern:       %s
        |Relations:     %s
        |Total Guess:   %d
        |Total Gold(T): %d
        |Total Gold(F): %d
        |True Pos:      %d
        |Precision:     %f
        |Recall:        %f
        |Avg Prec:      %f
        |Avg Prec#:     %d
        |MAP:           %f
        |Prec. at K:    %s""".stripMargin.format(pattern.toString(), relations.mkString(","),
        totalGuess, totalGoldTrue, totalGoldFalse, tp,
        precision, recall, avgPrecision, precisionCount, meanAvgPrecision, precisionAtK.mkString(", "))
    }
  }
  
  def extractFactFromLine(line: String): (String, String, String) = {
    val split = line.split("\\t")
    if (split.size == 4) {
      val Array(arg1, arg2) = split(1).split("\\|")
      (arg1, arg2, split(3))
    } else {
//        if (split(3)==split(4)){ 
//          println("WARNING split(3)==split(4) -> "+split(3)+" = "+split(4)+"\t ["+split(1)+","+split(2)+"]")
//        }
      (split(1), split(2), split(4))
    }
  }
  
  def evaluate(rankFiles: Seq[File], relPatterns: Seq[Regex], names: Seq[String]) {
   
    val projDirName = Parameters.annotation_folder
    val previousFileName = "latest.tsv" 
    val projDir = new File(projDirName)
    projDir.mkdirs()
    val previousFile = new File(projDir, previousFileName)
    val annotations = if (previousFile.exists())
      AnnotationTool.loadAnnotations(new FileInputStream(previousFile))
    else
      new mutable.HashMap[(Seq[Any], String), AnnotationTool.Annotation]
    println("Previous Annotations: " + annotations.size)
      
    val calculatePrecisionAtKs = Set(50, 100, 200, 300, 400)  

    case class PerFileEvals(file: File, name: String, evals: mutable.HashMap[Regex, Eval] = new mutable.HashMap[Regex, Eval]()) {
      def averageMap() = evals.map(_._2.meanAvgPrecision).sum / evals.size

      def globalMap() = {
        val sum = evals.view.map(e => e._2.meanAvgPrecision * e._2.totalGoldTrue).sum
        val normalizer = evals.view.map(_._2.totalGoldTrue).sum
        sum / normalizer
      }

      def totalGoldTrue() = evals.view.map(_._2.totalGoldTrue).sum

      def averagePrecisionAt(recall: Double, depth: Int) = {
        evals.view.map(_._2.precisionAtRecall(recall, depth)).sum / evals.size
      }
    }
    
    
    val perFileEvals = new ArrayBuffer[PerFileEvals]
    val globalEvals = new mutable.HashMap[Regex, Eval]()

    val allowedFacts = new mutable.HashMap[Regex, mutable.HashSet[(Seq[Any], String)]]()
    val poolDepth = Conf.K
    val details = false


    println("Collecting facts from rank files")
    for (rankFile <- rankFiles) {
      val counts = new mutable.HashMap[Regex, Int]()
      val missing = new mutable.HashSet[Regex]()
      missing ++= relPatterns
      val lines = Source.fromFile(rankFile).getLines()
      while (lines.hasNext && !missing.isEmpty) {
        val line = lines.next()
        if (line.trim != "") {
          val (arg1, arg2, predicted) = extractFactFromLine(line)
          
//          //DEBUG
//          println(extractFactFromLine(line))
              
          val tuple = Seq(arg1, arg2)
          val fact = tuple -> predicted

          for (pattern <- missing) {
            if (pattern.findFirstIn(predicted).isDefined) {
              allowedFacts.getOrElseUpdate(pattern, new mutable.HashSet[(Seq[Any], String)]()) += fact
              counts(pattern) = counts.getOrElse(pattern, 0) + 1
              if (counts(pattern) == poolDepth) missing -= pattern
            }
          }
        }
      }
    }

    println("Loading Annotations")
    for ((_, annotation) <- annotations) {
      for (pattern <- relPatterns; if (allowedFacts.get(pattern).map(_.apply(annotation.fact))).getOrElse(false)) {
        if (pattern.findFirstIn(annotation.label).isDefined) {
          val eval = globalEvals.getOrElseUpdate(pattern, new Eval(pattern))
          annotation.correct match {
            case true =>
              eval.goldTuplesTrue += annotation.tuple -> annotation.label
            case false =>
              eval.goldTuplesFalse += annotation.tuple -> annotation.label

          }
          eval.relations += annotation.label
        }
      }
    }
    
//    println("Loading Maps of existing relations")
//    val trainFile = "resources/train_complete_shuffled.dat"
//    var tup2rel = new mutable.LinkedHashMap[(String,String),ArrayBuffer[String]]   
//    val lines = Source.fromFile(trainFile).getLines()    
//    while (lines.hasNext) {
//      val line = lines.next()
//      val split = line.split("\\t")
//      val sub = split(0)
//      val obj = split(1)
//      val rel = split(2)           
//      tup2rel.getOrElseUpdate((sub,obj),ArrayBuffer[String]())
//      tup2rel((sub,obj)) += rel
//    }

    println("Loading Rank Files")
    //todo: first make sure that for each pattern and system we are using at most K
    //todo: annotations from that system

    for ((rankFile, index) <- rankFiles.zipWithIndex) {
      val perFile = PerFileEvals(rankFile, names(index))
      import perFile._
      val counts = new mutable.HashMap[Regex, Int]()
      val missing = new mutable.HashSet[Regex]()
      missing ++= relPatterns
      evals ++= globalEvals.mapValues(_.copyGold)

      //val outs = new mutable.HashMap[Regex, PrintStream] 
      
      val lines = Source.fromFile(rankFile).getLines()
      while (lines.hasNext && !missing.isEmpty) {
        val line = lines.next()
        val (arg1, arg2, predicted) = extractFactFromLine(line)
        val tuple = Seq(arg1, arg2)
        val fact = tuple -> predicted
        for (pattern <- relPatterns) {
          val eval = evals.getOrElseUpdate(pattern, new Eval(pattern))
          if (pattern.findFirstIn(predicted).isDefined) {
            eval.relations += predicted
            eval.totalGuess += 1
            
//            val out = outs.getOrElseUpdate(pattern, new PrintStream(new File(rankFile.getPath.split("\\.")(0)+"/"+pattern.toString().replaceAll("/", "_")+".dat")))
            
            eval.goldTuplesTrue(fact) -> eval.goldTuplesFalse(fact) match {
              case (true, _) =>               
                
                if (eval.n<poolDepth){
//                  out.print("TRUE - ")                  
                  eval.true_in_poolDepth += 1
                  eval.tp += 1
                  eval.guessTuplesTrue += fact
                }
              case (false, true) =>
                if (eval.n<poolDepth){ //out.print("FALSE - ") 
                  eval.fp += 1}
                
              case (false, false) =>
                if (eval.n<poolDepth){ /*out.print("UNKNOWN - ")*/ }
            }
            
//            if (eval.n<poolDepth){ 
////              out.print(fact._2+"("+fact._1(0)+","+fact._1(1)+")"); 
////              for ( x <- tup2rel.getOrElse((fact._1(0),fact._1(1)), Seq.empty) ){
////                out.print("\t"+x)
////              }
//              out.println()
//            }
            
            
            eval.sumPrecision += eval.precision
            eval.precisions += eval.precision
            eval.recalls += eval.recall
            eval.missings += eval.missingLabels
            if (eval.goldTuplesTrue(fact) && eval.n<poolDepth) {
              eval.avgPrecisionForFact(fact) = eval.avgPrecision
              eval.precisionForFact(fact) = eval.precision
//              println(fact._1+"\t"+fact._2+"\t"+eval.tp+"\t"+eval.totalGuess)
            }
            if (calculatePrecisionAtKs(eval.totalGuess)) {
              eval.precisionAtK += ((eval.totalGuess, eval.missingLabels, eval.precision))
            }
            counts(pattern) = counts.getOrElse(pattern, 0) + 1
//            if (counts(pattern) == runDepth) missing -= pattern
            eval.n += 1;

          }
        }
      }
//      for (out <- outs.values){
//        out.close()
//      }
      for (pattern <- relPatterns; eval <- evals.get(pattern)) {
        if (details) println(eval)
      }
      perFileEvals += perFile
    }


    //print overview table
    def printTextTable(out: PrintStream) {
      out.println("Summary:")
      out.print("%-30s%-10s%-10s".format("Pattern", "Gold+", "Gold+-"))
      for ((perFile, index) <- perFileEvals.zipWithIndex) {
        out.print("| %-10s%-10s".format("TRUE", "Missing"))
      }
      out.println()
      out.print("%50s".format(Range(0, 50).map(s => "-").mkString))
      for (perFile <- perFileEvals) {
        out.print("%22s".format(Range(0, 22).map(s => "-").mkString))
      }
      out.println()
      for (pattern <- relPatterns.sortBy(pattern => -perFileEvals.head.evals(pattern).totalGoldTrue)) {
        val first = perFileEvals.head
        out.print("%-30s%-10d%-10d".format(pattern.toString(), first.evals(pattern).goldTuplesTrue.size, first.evals(pattern).totalGold))
        for (perFile <- perFileEvals) {
          val eval = perFile.evals(pattern)
          out.print("| %-10d%-10d".format(
            eval.true_in_poolDepth,
            //          eval.precisionAtK.lift(1).map(_._3).getOrElse(-1.0)
            eval.missings.lift(math.min(poolDepth, eval.missings.size) - 1).getOrElse(-1)
          ))
        }
        out.println()
      }
      out.print("%-30s%-10d%-10d".format("Average", 0, 0))
      for (perFile <- perFileEvals) {
        out.print("| %-10.2f%-10d".format(perFile.averageMap(), -1))          
      }
      out.println()
      out.print("%-30s%-10d%-10d".format("Global", 0, 0))
      for (perFile <- perFileEvals) {
        out.print("| %-10.2f%-10d".format(perFile.globalMap(), -1))
      }
      out.println()
      
//      println();
//      for (perFile <- perFileEvals) {
//        println(perFile.name)
//        for ((k,v) <- perFile.evals){
//          println("key: \n"+k)
//          println("value: \n"+v)
//          println("********")
//          v.debugMeanAvgPrecision
//          println("********")
//        }
//        println();
//      }
    }

    //print latex table
    def printLatexTableCORE(out: PrintStream) {
      def norm(label: String) = label.replaceAll("\\$", "").replaceAll("_", "\\\\_")
      
      def truncateAt(n: Double, p: Int): Double = { val s = math pow (10, p); (math floor n * s) / s }
      
      val systemCount = perFileEvals.size
      
      out.println()
      out.println("\\begin{center}")
      out.println("\\begin{tabular}{ %s %s | %s }".format("l", "l", Seq.fill(systemCount)("c").mkString(" ")))
      out.println("  %20s & %s & %s \\\\".format("Relation", "\\#", perFileEvals.map(_.name).mkString(" & ")))
      out.println("\\hline")
      
      for (pattern <- relPatterns.sortBy(pattern => -perFileEvals.head.evals(pattern).totalGoldTrue)){
        val first = perFileEvals.head
        out.print(norm(pattern.toString())+ " & " + first.evals(pattern).totalGoldTrue);
       
        //BODY
        for ( solution <- perFileEvals){
          val value1 = solution.evals(pattern).true_in_poolDepth.toInt
          var content1 : String = "{\\bf "+value1+"}"
          
          val value2 = truncateAt(solution.evals(pattern).meanAvgPrecision,2)
          var content2 : String = "{\\bf "+value2+"}"
          
          var best1 = true;
          var best2 = true;
          //see if is the best: TIME CONSUMING CHECK
          for ( competitor <- perFileEvals){
            if (competitor.evals(pattern).true_in_poolDepth.toInt > value1){
              content1 = ""+value1+""
              best1 = false;
            }
            if (truncateAt(competitor.evals(pattern).meanAvgPrecision,2) > value2){
              content2 = ""+value2+""
              best2 = false;
            }
          }   
      
          if (best1){
            for ( competitor <- perFileEvals if competitor!=solution){
              if (competitor.evals(pattern).true_in_poolDepth.toInt == value1){
                content1 = "{\\em"+value1+"}"
              }
            }   
          }
          
          if(best2){
            for ( competitor <- perFileEvals if competitor!=solution){
              if (truncateAt(competitor.evals(pattern).meanAvgPrecision,2) == value2){
                content2 = "{\\em"+value2+"}"
              }
            }   
          }
          
          out.print(" & "+content1+" {\\small ("+content2+") }")         
        }  
        out.println("\\\\")
      }
      out.println("\\hline")
      out.println("  %20s & %4s & %s \\\\".format("Average $\\text{MAP}^{\\text{\\tiny{100}}}_\\#$",
        "",
        perFileEvals.map(e => "%6.2f".format(e.averageMap())).mkString(" & ")))
//      out.println("\\hline")
      out.println("  %20s & %4s & %s \\\\".format("Weighted Average $\\text{MAP}^{\\text{\\tiny{100}}}_\\#$",
        "",
        perFileEvals.map(e => "%6.2f".format(e.globalMap())).mkString(" & ")))
      out.println("\\end{tabular}")
      out.println("\\end{center}")
    }
    
    //print latex table
    def printLatexTable(out: PrintStream) {
      def norm(label: String) = label.replaceAll("\\$", "").replaceAll("_", "\\\\_")
      val systemCount = perFileEvals.size
      out.println("Latex:")
      out.println("\\begin{center}")
      out.println("\\begin{tabular}{ %s %s | %s }".format("l", "l", Seq.fill(systemCount)("c").mkString(" ")))
      out.println("  %20s & %s & %s \\\\".format("Relation", "\\#", perFileEvals.map(_.name).mkString(" & ")))
      out.println("\\hline")
      for (pattern <- relPatterns.sortBy(pattern => -perFileEvals.head.evals(pattern).totalGoldTrue)) {
        val first = perFileEvals.head
        val maps = perFileEvals.map(_.evals(pattern).true_in_poolDepth)// .meanAvgPrecision)
        val sorted = maps.sortBy(-_)
        def format(map: Double) = map match {
          case x if (x >= sorted.head && x <= sorted(1)) => "{\\em %6.2f}".format(map)
          case x if (x >= sorted.head) => "{\\bf %6.2f}".format(map)
          case _ => "%6.2f".format(map)
        }


        out.println("  %20s & %4d & %s \\\\".format(norm(pattern.toString()), first.evals(pattern).totalGoldTrue,
          maps.map(format(_)).mkString(" & ")))
      }
      out.println("\\hline")
      out.println("  %20s & %4s & %s \\\\".format("MAP",
        "",
        perFileEvals.map(e => "%6.2f".format(e.averageMap())).mkString(" & ")))
//      out.println("\\hline")
      out.println("  %20s & %4s & %s \\\\".format("Weighted MAP",
        "",
        perFileEvals.map(e => "%6.2f".format(e.globalMap())).mkString(" & ")))
      out.println("\\end{tabular}")
      out.println("\\end{center}")
    }

//    printLatexTable()
    printLatexTableCORE(System.out)
    //printTextTable()
    
    var filename = ""
    
    if (Parameters.configuration){
        //Freebase relations
        filename = "table_Freebase_relations.tex"
      }
      else{
        //Surface relations
        filename = "table_surface_relation.tex"    
      }
    
    val out = new PrintStream(new File(filename))
    printLatexTableCORE(out)
    out.close()

  }
}

