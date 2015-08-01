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

package script

import util._
import java.io.{FileInputStream, PrintStream, File}
import io.Source
import collection.mutable
import collection.mutable.ArrayBuffer
import scala.collection.immutable.ListMap


object Converter {
  
  var rel_frequency = new mutable.HashMap[String, Int] 
  var tuple_frequency = new mutable.HashMap[(String,String), Int] 
  

  def main(args: Array[String]) {
    if (Parameters.parse_arguments(args)>=0){
      convert();
    }
  }
  
  
  def convert(){
    
    val trainFile = Parameters.input_folder+"train_complete_shuffled.dat"
    val testFile = Parameters.input_folder+"test_subsample.dat"
    
    //STEP -1: LOAD STOP WORDS FROM FILE
    println("STEP -1/7: LOAD STOP WORDS FROM FILE")
    val stopWordFile = Parameters.input_folder+"eng_stopwords.txt"
    BagOfWords.loadStopWord(stopWordFile)
    
    //STEP 0: LOAD CONTEXT METADATA FROM FILE
    println("STEP 0/7: LOAD CONTEXT METADATA FROM FILE")
    val metaFile = Parameters.input_folder+"trainMetaComplete"
    NYTMetadata.loadMeta(metaFile)
 
    //STEP 1: BUILD MAPS WITH ATTRIBUTES AND BAG OF WORDS
    println("STEP 1/7: BUILD MAPS WITH ATTRIBUTES AND BAG OF WORDS")
    buildMaps(trainFile)
    BagOfWords.BuildMap()        
    println("#words: "+BagOfWords.next_word)
    
    //STEP 2: BUILD CONTEXT MAP
    println("STEP 2/7: BUILD CONTEXT MAP")
    buildContextMap()
    
    println( "#relations = "+next_rel)
    println( "#tuples = "+next_tup)
    println( "#entities = "+next_ent)
    
    //STEP 3: BUILD OFFSETS AND META
    val outputRelGroupsFile = Parameters.output_folder+"rel.groups" 
    val outputTupGroupsFile = Parameters.output_folder+"tup.groups"     
    println("STEP 3/7: BUILD OFFSETS AND META")
    buildOffsetsRel(outputRelGroupsFile)
    buildOffsetsTup(outputTupGroupsFile)
    
    //STEP 4: PRINT X FILES
    println("STEP 4/7: PRINT X FILES")
    val outputRelXFile = Parameters.output_folder+"rel.x" 
    val outputTupXFile = Parameters.output_folder+"tup.x"     
    printRel(outputRelXFile);
    printTup(outputTupXFile);    
    
    //STEP 5: PRINT TRAIN AND VALIDATION FILES
    println("STEP 5/7: PRINT TRAIN AND VALIDATION FILES")
    val outputTrainFile = Parameters.output_folder+"train.libfm"    
    val outputValidationFile = Parameters.output_folder+"validation.libfm"
    val outputRelTrainFile = Parameters.output_folder+"rel.train" 
    val outputTupTrainFile = Parameters.output_folder+"tup.train" 
    val outputRelValidationFile = Parameters.output_folder+"rel.validation" 
    val outputTupValidationFile = Parameters.output_folder+"tup.validation" 
    printTrainAndValidation(trainFile, outputTrainFile, outputValidationFile, outputRelTrainFile, outputTupTrainFile,outputRelValidationFile,outputTupValidationFile)    
    
    //STEP 6: PRINT TEST FILES
    println("STEP 6/7: PRINT TEST FILES")
    val outputTestFile = Parameters.output_folder+"test.libfm" 
    val outputRelTestFile = Parameters.output_folder+"rel.test" 
    val outputTupTestFile = Parameters.output_folder+"tup.test"     
    printTest(testFile, outputTestFile, outputRelTestFile, outputTupTestFile)  
    
    //STEP 7: PRINT CONVERSION MAP
    println("STEP 7/7: PRINT CONVERSION MAP")
    val outputConversionMap = Parameters.output_folder+"conversion_map.dat"
    printConversion(outputConversionMap)
  }
  
  var rel_conv_map = new mutable.HashMap[String, Int] 
  var ent_conv_map = new mutable.HashMap[String, Int]
  var sub_conv_map = new mutable.HashMap[String, Int]
  var obj_conv_map = new mutable.HashMap[String, Int] 
  var ctx_conv_map = new mutable.HashMap[String, Int] 
  var tup_conv_map = new mutable.LinkedHashMap[(String,String),Int]
  
  //counters 
  var next_rel = 0
  var next_ent = 0
  var next_sub = 0
  var next_obj = 0
  var next_ctx = 0
  var next_tup = 0
  
  //map to bind each tuple to a set of documents
  var Tuple2Document = new mutable.LinkedHashMap[(String,String), ArrayBuffer[NYTMetadata.NYTDocument]]
  var union_documents : ArrayBuffer[NYTMetadata.NYTDocument] = new ArrayBuffer[NYTMetadata.NYTDocument]();
  //map to bind each tuple to a set of sentences
  var Tuple2Sentence = new mutable.LinkedHashMap[(String,String), ArrayBuffer[String]]
  
  def buildMaps(trainFile : String){
    
    val lines = Source.fromFile(trainFile).getLines()    
    while (lines.hasNext) {
      val line = lines.next()
      val split = line.split("\\t")
      val sub = split(0)
      val obj = split(1)
      val rel = split(2)           
      
      if (sub_conv_map.getOrElseUpdate(sub, next_sub)==next_sub) next_sub+=1
      if (obj_conv_map.getOrElseUpdate(obj, next_obj)==next_obj) next_obj+=1
      if (rel_conv_map.getOrElseUpdate(rel, next_rel)==next_rel) next_rel+=1    
      if (tup_conv_map.getOrElseUpdate((sub,obj), next_tup)==next_tup) next_tup+=1      
       
      if (ent_conv_map.getOrElseUpdate(sub, next_ent)==next_ent) next_ent+=1
      if (ent_conv_map.getOrElseUpdate(obj, next_ent)==next_ent) next_ent+=1
      var document : NYTMetadata.NYTDocument = null;
      
      //CONTEXT
      try{      
        val document_str = split(3)    
        document = NYTMetadata.documents(document_str)           
        Tuple2Document.getOrElseUpdate((sub,obj),ArrayBuffer[NYTMetadata.NYTDocument]())
        Tuple2Document((sub,obj)) += document         
        union_documents += document               
        val sentence = split(5)
        val result = BagOfWords.addEvidence(sentence)    
        Tuple2Sentence.getOrElseUpdate((sub,obj),ArrayBuffer[String]())
        Tuple2Sentence((sub,obj)) += result;         
      } catch{
        case e: Exception => 
          if (rel.startsWith("REL$")){/*OK*/}
          else{println(line); println(e); return;}
      }; 
      //ATTRIBUTES FROM ner#[x]->[y]
      try{      
        var ner = split(4)   
        TupleTypes.updateMap(ner, sub, obj)
      } catch{
        case e: Exception => 
          if (rel.startsWith("REL$")){/*OK*/}
          else{println(line); println(e);}
      }; 
      //relations frequency
      var old = rel_frequency.getOrElseUpdate(rel, 0)
      rel_frequency(rel) = old + 1
      //tuples frequency
      old = tuple_frequency.getOrElseUpdate((sub,obj), 0)
      tuple_frequency((sub,obj)) = old + 1       
    }             
  }
  
  
  var offset_rel = 0
  var offset_tup = 0
  var offset_ent = 0  
  var offset_att = 0
  var offset_ctx = 0
  var offset_bag = 0
  
  def buildOffsetsRel(groupFileRel : String){
    offset_rel = 0
    val out = new PrintStream(new File(groupFileRel))
    //print groups
    var c = 0;
    for (i <- 0 until next_rel) out.println(c);
    out.close
  }  
  
  def buildOffsetsTup(groupFileTuple : String){
    
    val out = new PrintStream(new File(groupFileTuple))
    var c = 0;
    
    //TUPLE GROUP INLUDED (default)
    offset_tup = 0
    var current_offset = next_tup;
    for (i <- 0 until next_tup){ out.println(c) }
    c+=1
    
    //ENTITY GROUP INCLUDED (default)
    offset_ent = current_offset;    
    current_offset = offset_ent + next_ent;
    for (i <- 0 until next_ent){ out.println(c) }
    c+=1
    
    if (Parameters.TUPLE_TYPES){
      //TUPLE TYPES GROUP INCLUDED (t)
      offset_att = current_offset
      val next_typ = TupleTypes.buildOffset(offset_att)
      current_offset = offset_att + next_typ
      c = TupleTypes.printGroups(out, c)
    }
    
    if (Parameters.NYT_METADATA){
      //NYT METADATA GROUP INCLUDED (m)
      offset_ctx = current_offset;
      val next_ctx = Context.giveOffset()
      current_offset = offset_ctx + next_ctx
      c = Context.printGroups(out, c)
    }
    
    if (Parameters.BAG_OF_WORD){
      //BAG-OF-WORD GROUP INCLUDED (w)
      BagOfWords.printGroups(out, c)
      offset_bag = current_offset;
    }
    
    out.close
  }  
  
  def printRel(outputRelXFile : String){
    val out = new PrintStream(new File(outputRelXFile))
    for (i <- 0 until next_rel) out.println(i+":1");
    out.close
  }
  
  def buildContextMap(){
    Context.populateMap(union_documents);
    Context.buildMap()
    Context.buildOffsets()
  }
  
  def printTup(outputSubObjXFile : String){
    val out = new PrintStream(new File(outputSubObjXFile))
    for ((t,v) <- tup_conv_map){  
      val tup = v + offset_tup;
      val e1 = ent_conv_map(t._1) + offset_ent
      val e2 = ent_conv_map(t._2) + offset_ent
      var content : String = tup+":1 "+e1+":0.5 "+e2+":0.5"
      
      if (Parameters.TUPLE_TYPES){
        //TUPLE TYPES GROUP INCLUDED (t)
        content += TupleTypes.printAttributes(t._1, t._2)
      }
      
      if (Parameters.NYT_METADATA){
        //NYT METADATA GROUP INCLUDED (m)
        content += Context.printContext(Tuple2Document(t),offset_ctx)      
      }
      
      if (Parameters.BAG_OF_WORD){
        //BAG-OF-WORD GROUP INCLUDED (w)
        content += BagOfWords.print(Tuple2Sentence(t),offset_bag)
      }
      
      out.println(content);  
    }
    out.close
  }  
  
  def printTrainAndValidation(trainFile : String, outputTrainFile : String, outputValidationFile : String, outputRelTrainFile : String, outputTupTrainFile : String, outputRelValidationFile : String, outputTupValidationFile : String){
    val out_train = new PrintStream(new File(outputTrainFile))  
    val out_validation = new PrintStream(new File(outputValidationFile)) 
    val out_rel_train = new PrintStream(new File(outputRelTrainFile))  
    val out_subobjattrctx_train = new PrintStream(new File(outputTupTrainFile))      
    val out_rel_validation = new PrintStream(new File(outputRelValidationFile))  
    val out_subobjattrctx_validation = new PrintStream(new File(outputTupValidationFile))   
    val lines = Source.fromFile(trainFile).getLines() 
    var counter = 0;
    val limit_float : Float = 100f / Parameters.percentage_validation    
    val limit_int : Int =  math.round(limit_float)
    while (lines.hasNext) {
      val line = lines.next()
      val split = line.split("\\t")
      val sub = split(0)
      val obj = split(1)
      val rel = split(2)      
      //****BEGIN TRICK TO ADD AT LEAST ONE SAMPLE OF EACH 'IMPORTANT' RELATION IN THE VALIDATION SET****
      var condition : Boolean = false
      for (cons <- Parameters.relations_considered.keySet if (!condition)){
        if (rel.contains(cons)){ 
          if((Parameters.relations_considered(cons)>0) && (rel_frequency(rel) > Parameters.rel_cutoff) && (tuple_frequency((sub,obj)) > Parameters.tuple_cutoff)){
            condition = true
            var old = Parameters.relations_considered(cons)
            Parameters.relations_considered(cons) = old - 1            
            val old_stat = Parameters.relations_considered_stat(cons)
            Parameters.relations_considered_stat(cons) = (old_stat._1+1,old_stat._2)
          }
          else{
            val old_stat = Parameters.relations_considered_stat(cons)
            Parameters.relations_considered_stat(cons) = (old_stat._1,old_stat._2+1)
          }
        }
      }
      //****END TRICK****
      if (condition || ((counter%limit_int == 0) && (rel_frequency(rel) > Parameters.rel_cutoff) && (tuple_frequency((sub,obj)) > Parameters.tuple_cutoff))){
        out_rel_validation.println(rel_conv_map(rel))
        out_subobjattrctx_validation.println(tup_conv_map(sub,obj))   
        out_validation.println(1)
        var old = rel_frequency(rel)
        rel_frequency(rel) = old - 1
        old = tuple_frequency((sub,obj))
        tuple_frequency((sub,obj)) = old - 1
      }
      else{
        out_rel_train.println(rel_conv_map(rel))
        out_subobjattrctx_train.println(tup_conv_map(sub,obj))
        out_train.println(1)
      }
      counter+=1;
    }    
    out_train.close()
    out_validation.close();
    out_rel_train.close()
    out_subobjattrctx_train.close()
    out_rel_validation.close()
    out_subobjattrctx_validation.close()
  }
  
  def printTest(inputTestFile : String, outputTestFile : String, outputRelTestFile : String, outputTupTestFile : String){
    val lines = Source.fromFile(inputTestFile).getLines()
    val out_test = new PrintStream(new File(outputTestFile))  
    val out_rel = new PrintStream(new File(outputRelTestFile))  
    val out_subobjattrctx = new PrintStream(new File(outputTupTestFile))      
    val out = new PrintStream(new File(inputTestFile+"_pruned2"))      
    while (lines.hasNext) {
      val line = lines.next()
      val split = line.split("\\t")
      val sub = split(0)
      val obj = split(1)
      val rel = split(2) 
      if ( rel_conv_map.contains(rel) && tup_conv_map.contains((sub,obj))){
        out.println(split(0)+"\t"+split(1)+"\tREL$NA\t"+split(2));
        out_rel.println(rel_conv_map(rel))
        out_subobjattrctx.println(tup_conv_map(sub,obj))
        out_test.println(0);
      }
    }
    out.close();
    out_test.close();
    out_rel.close()
    out_subobjattrctx.close()
  } 
  
  
  def printConversion(outputConversionFile : String){
    val out = new PrintStream(new File(outputConversionFile))
    for ((k,v) <- rel_conv_map){ 
      val value : Int = v + offset_rel
      out.println(value+"\t"+k) 
    }
    for ((k,v) <- tup_conv_map){ 
      val value : Int = v + offset_tup + next_rel
      out.println(value+"\t"+k)      
    }
    for ((k,v) <- ent_conv_map){ 
      val value : Int = v + offset_ent + next_rel
      out.println(value+"\t"+k)      
    }
    out.close
  }  
  
}