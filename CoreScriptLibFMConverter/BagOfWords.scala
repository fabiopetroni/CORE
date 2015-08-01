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

package util

import collection.mutable
import java.io.PrintStream
import io.Source
import collection.mutable.ArrayBuffer

object BagOfWords { 
  
  val CUTOFF = 5;
  
  var missing_words = 0;
  
  var word_conv_map = new mutable.HashMap[String, Int] 
  var word_frequency = new mutable.HashMap[String, Int] 
  var next_word = 0
  var stop_words : ArrayBuffer[String] = new ArrayBuffer[String]()
  
  def printStat(total : Int){
    var x : Double = 0;
    x = missing_words;
    x/= total;
    x*= 100;
    println("missing_words:"+missing_words+"("+x+"%)")
  }
  
  def print(sentences : ArrayBuffer[String], baseOffset:Int) : String = {
    var freq = new mutable.HashMap[Int, Int]
    var n = 0
    
    for (sentence <- sentences){     
      val split = sentence.split(" ");
      for (word <- split){
        try{
          val index = word_conv_map(word)
          var old = freq.getOrElseUpdate(index, 0)
          freq(index) = old + 1
          n+=1
        } catch{ case e: Exception => }
      }
    }
    
    var result : String = "";
    for ((k,v) <- freq){
        var key = k + baseOffset
        var value : Float = v
        value /= n
        result += " "+key+":"+value;
    }    
    if (freq.isEmpty){ missing_words+=1 }
    result
  }
  
  def printGroups(out: PrintStream, base : Int){
    var c = base
    for (i <- 0 until next_word){ out.println(c) }
  }
  
  def addEvidence(sentence : String) : String = {
    var result : String = "";
    //step 1- remove 'sen#' at the beginning of the sentence
    var line = sentence.substring(4,sentence.length());    
    //step 2 - remove puncuation and numbers
    line = line.replaceAll("[^a-zA-Z ]", "");    
    //step 3 - split line
    val split = line.split(" ");
    for (x <- split){
      val word = x.toLowerCase();
      if (!word.isEmpty() && !stop_words.contains(word)){         
        var old = word_frequency.getOrElseUpdate(word, 0)
        word_frequency(word) = old + 1
      }
      result += word +" ";
    }
    result
  }
  
  def BuildMap(){
//    val out = new PrintStream(new File("/Users/fabio/Dropbox/workspaces/scala/UniversalSchema/resources/bag_u.dat"))
    for ((word,c) <- word_frequency){
//      out.println(c+" "+word)
      if (c > CUTOFF){
        if (word_conv_map.getOrElseUpdate(word, next_word)==next_word) next_word+=1  
      }
    }
//    out.close
  }
  
  def loadStopWord(file: String){
    val lines = Source.fromFile(file).getLines()    
    while (lines.hasNext) {
      val line = lines.next()
      if (!stop_words.contains(line.toLowerCase())){
        stop_words += line.toLowerCase()
      }
    }
  }
//    
//  def main(args: Array[String]) {
//    val stopWordFile = "../UniversalSchema/resources/eng_stopwords.txt"  
//    loadStopWord(stopWordFile)
//    
//    val sentence = "sen#I wanted to head 27 miles up the Mediterranean coast to Rafah , a divided city that sprawls from Egypt 's Sinai region into the Gaza Strip ."
//    addEvidence(sentence)
//    
//  }
}