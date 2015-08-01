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
import collection.mutable.ArrayBuffer
import java.io.PrintStream

object TupleTypes {
  
  var Tuple2Attributes = new mutable.LinkedHashMap[(String,String), ArrayBuffer[String]]

  def printAttributes(sub : String, obj: String) : String = {
    var freq = new mutable.HashMap[String, Int]
    var n = 0;
    for (attr <- Tuple2Attributes((sub,obj))){
      val old = freq.getOrElseUpdate(attr, 0)
      freq(attr) = old + 1
      n += 1
    }
    
    var result : String = ""
    //ATTRIBUTES
    for ((k,v) <- freq){
      val key = att_map(k) + offset_attr
      var value : Float = v
      value /= n
      result += " "+key+":"+value;
    }       
    
    result
  }
  
  var att_map = new mutable.HashMap[String, Int]
  var freq_att = new mutable.HashMap[String, Int]
  var next_att = 0
  
  def updateMap(ner : String, sub : String, obj: String){
    if (att_map.getOrElseUpdate(ner, next_att)==next_att) next_att+=1  
    val old_attributes = Tuple2Attributes.getOrElseUpdate((sub,obj),ArrayBuffer[String]())
    Tuple2Attributes((sub,obj)) += ner
    val old = freq_att.getOrElseUpdate(ner, 0)
    freq_att(ner) = old + 1
  }
  
  var offset_attr = 0
  
  def buildOffset(base : Int) : Int = {
    offset_attr = base;
//    //DEBUG
//    for  ((k,v) <- freq_att){
//      println(v+" "+k)
//    }
    next_att
  }
  
  def printGroups(out: PrintStream, base : Int) : Int  ={
    var c = base
    for (i <- 0 until next_att){ out.println(c) }
    c+=1
    c
  }
  
  def printTuple2Attribute(){
    for ((k,v) <- Tuple2Attributes){
      print(k._1+"\t"+k._2)
      for (e <- v){
        print("\t"+e)
      }
      println()
    }
  }
  
}