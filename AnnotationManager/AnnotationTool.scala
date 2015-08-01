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

package annotation

import java.util.Calendar
import java.text.SimpleDateFormat
import collection.mutable.{HashSet, HashMap, ArrayBuffer}
import io.{Source,StdIn}
import java.io.{File,InputStream,PrintStream,FileInputStream}
import collection.mutable

/**
 * @author Sebastian Riedel adn Fabio Petroni
 */
object AnnotationTool {

  case class Annotation(tuple: Seq[Any], label: String, correct: Boolean) {
    override def toString = (Seq(if (correct) "1" else "0", label) ++ tuple).mkString("\t")

    def fact = tuple -> label
  }

  def loadAnnotations(in: InputStream, out: Option[PrintStream] = None) = {
    println("Reading in annotations...")
    val result = new mutable.HashMap[(Seq[Any], String), Annotation]()
    for (line <- Source.fromInputStream(in).getLines()) {
      val fields = line.split("\\t")
      val correct = fields(0) == "1"
      val label = fields(1)
      val tuple = fields.drop(2).toSeq
      result(tuple -> label) = Annotation(tuple, label, correct)
      for (o <- out) o.println(line)
    }
    result
  }

  def loadMentions(trainMentionFileName: String, testMentionFileName: String) = {
    val pair2sen = new HashMap[Seq[Any], HashSet[String]] // arg1 -> rel arg1 arg2
    
    //TRAIN
    val source_train = Source.fromFile(trainMentionFileName,"ISO-8859-1")
    println("Loading TRAIN mention file...")
    for (line <- source_train.getLines(); if (!line.startsWith("#Document"))) {
      val fields = line.split("\t")
      val sen = fields(fields.length - 1)
      val sens = pair2sen.getOrElseUpdate(Seq(fields(1), fields(2)), new HashSet[String])
      sens += sen
    }
    source_train.close()
    
    //TEST
    val source_test = Source.fromFile(testMentionFileName,"ISO-8859-1")
    println("Loading TEST mention file...")
    for (line <- source_test.getLines(); if (!line.startsWith("#Document"))) {
      val fields = line.split("\t")
      val sen = fields(fields.length - 1)
      val sens = pair2sen.getOrElseUpdate(Seq(fields(1), fields(2)), new HashSet[String])
      sens += sen
    }
    source_test.close()
    
    pair2sen
  }

  def main(args: Array[String]){
    if (Parameters.parse_arguments(args)>=0){
      annotate()
    }    
  }
  
  var Linked_Tuple = new mutable.LinkedHashMap[(String,String),Int]
  
  def loadTestTuple(inputTestFile : String){
    val lines = Source.fromFile(inputTestFile).getLines()
    while (lines.hasNext) {
      val line = lines.next()
      val split = line.split("\\t")
      val sub = split(0)
      val obj = split(1) 
      Linked_Tuple.getOrElseUpdate((sub,obj), 1)
    }
    println("Linked_Tuple.size="+Linked_Tuple.size)
  }

  def annotate() {
    
    val not_linked : Boolean = true;

    if (!not_linked){
      val linkedTestTuple = "resources/test_subsample.dat_linkedTestTuple.txt"
      loadTestTuple(linkedTestTuple);
    }
    
    val sourceName = Parameters.target_file
    val projDirName = Parameters.annotation_folder
    val TrainMentionFileName = Parameters.input_folder+"/nyt-freebase.train.triples.universal.mention.txt" 
    val TestMentionFileName = Parameters.input_folder+"/nyt-freebase.test.triples.universal.mention.txt" 
      
    /*
     * STRUCTURED
     * person/company
     * location/containedby
     * author/works_written
     * person/nationality     
     * parent/child
     * person/place_of_death
     * person/place_of_birth
     * neighborhood/neighborhood_of
     * person/parents
     * company/founders
     * film/directed_by
     * sports_team/league
     * team/arena_stadium
     * team_owner/teams_owned
     * roadcast/area_served
     * structure/architect
     * composer/compositions
     * person/religion
     * film/produced_by
     *      
     * PATTERNS
     * head
     * visit
     * base
     * attend
     * scientist
     * support
     * adviser
     * criticize
     * praise
     * vote
     */
    val relation = Parameters.pattern ;
    val pattern = relation.r //company
    val previousFileName = "latest.tsv" 
    
    val newFileName = {
      val cal = Calendar.getInstance()
      val sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
      sdf.format(cal.getTime) + ".tsv"
    }
    val projDir = new File(projDirName)
    projDir.mkdirs()

    val sourceFile = new File(sourceName)
    val previousFile = new File(projDir, previousFileName)
    val newFile = new File(projDir, newFileName)
    val out = new PrintStream(newFile)

    //read in mention file
    val pair2sen = loadMentions(TrainMentionFileName, TestMentionFileName)

    //read in previous file if exists
    //Format: Tuple, System,
    val annotations = if (previousFile.exists())
      loadAnnotations(new FileInputStream(previousFile), Some(out))
    else
      new mutable.HashMap[(Seq[Any], String), Annotation]
    println("Previous Annotations: " + annotations.size)

    //set up new softlink
    setupSoftlink(new File(projDir, "latest.tsv"), newFile)

    var labelled = 0

    //go through ranked file, and find tuples not yet annotated
    for (line <- Source.fromFile(sourceFile).getLines()) {
      val Array(score, arg1, arg2, freebase, predicted) = line.split("\\t")
      if (pattern.findFirstIn(predicted).isDefined) {
        val tuple = Seq(arg1, arg2)
        if (not_linked || Linked_Tuple.contains((arg1,arg2))){
          annotations.get(tuple -> predicted) match {
            case None =>
              //get sentences
              val sentences = pair2sen.getOrElse(tuple, Set.empty)
              //ask user
              println("*************************************************")
              println("Asking for annotation of: " + tuple.mkString(" | "))
              println("Number of annotations:    " + labelled)
              println("Prediction:               " + predicted)
              println("Score:                    " + score)
              println("Freebase:                 " + freebase)
              println("Sentences: ")
              for (sentence <- sentences) {
                var current : String = sentence
                for (arg <- tuple) {
                  if (current.contains(arg)) {
                    current = current.replaceAll(arg, "["+arg+"]")
                  } else if (current.contains(arg.toUpperCase)) {
                    current = current.replaceAll(arg.toUpperCase, "[" + arg.toUpperCase + "]")
                  } else if (current.contains(arg.toLowerCase)) {
                    current = current.replaceAll(arg.toLowerCase, "[" + arg.toLowerCase + "]")
                  }
                }
                println("   " + current)
              }
              println("Correct (y/N)?: ")
              val line = StdIn.readLine()
              val correct = line.trim.toLowerCase == "y"
              val annotation = Annotation(tuple, predicted, correct)
              out.println(annotation)
              out.flush()
  
            case Some(annotation) =>
              println(annotation)            
              val sentences = pair2sen.getOrElse(tuple, Set.empty)
              for (sentence <- sentences) {
                var current : String = sentence
                for (arg <- tuple) {
                  if (current.contains(arg)) {
                    current = current.replaceAll(arg, "["+arg+"]")
                  } else if (current.contains(arg.toUpperCase)) {
                    current = current.replaceAll(arg.toUpperCase, "[" + arg.toUpperCase + "]")
                  } else if (current.contains(arg.toLowerCase)) {
                    current = current.replaceAll(arg.toLowerCase, "[" + arg.toLowerCase + "]")
                  }
                }
                println("   " + current)
              }
              println()
          }
          labelled += 1
        }
      }
    }


  }

  def setupSoftlink(latest: File, newFile: File) {
    if (latest.exists()) {
      //remove latest file, assuming it's a softlink
      latest.delete()
    }
    Runtime.getRuntime.exec("/bin/ln -s %s %s".format(newFile.getAbsolutePath, latest.getAbsolutePath))
  }
}
