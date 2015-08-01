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

import collection.mutable.ArrayBuffer
import collection.mutable
import java.io.PrintStream
import java.util.StringTokenizer
import scala.collection.immutable.ListMap

object Context {
  
  val CUTOFF = 2;
  
  var missing_NewsDesk = 0
  var missing_OnlineSection = 0
  var missing_Section = 0
  var missing_Descriptions = 0  
  var missing_Year = 0
  
  def printFreq(){
    
    val K = 10;
    
    println("NEWS DESK:")
    var i = 0;
    for ((k,v) <- ListMap(freq_NewsDesk.toSeq.sortWith(_._2 > _._2):_*) if i<K){
      println(k+" "+v);
      i+=1
    }
    
    println("ONLINE SECTION:")
    i = 0;
    for ((k,v) <- ListMap(freq_OnlineSection.toSeq.sortWith(_._2 > _._2):_*) if i<K){
      println(k+" "+v);
      i+=1
    }
    
    println("SECTION:")
    i = 0;
    for ((k,v) <- ListMap(freq_Section.toSeq.sortWith(_._2 > _._2):_*) if i<K){
      println(k+" "+v);
      i+=1
    }
    
    println("GENERAL ONLINE DESCRIPTION  DESCRIPTIONS:")
    i = 0;
    for ((k,v) <- ListMap(freq__Descriptions.toSeq.sortWith(_._2 > _._2):_*) if i<K){
      println(k+" "+v);
      i+=1
    }
    
//    println("YEAR")
//    for ((k,v) <- ListMap(freq_Year.toSeq.sortWith(_._2 > _._2):_*)){
//      println(k+" "+v);
//    }
  }
  
  
  def printStat(total : Int){
    var x : Double = 0;
    x = missing_NewsDesk;
    x/= total;
    x*= 100;
    println("missing_NewsDesk:"+missing_NewsDesk+"("+x+"%)")
    x = missing_OnlineSection;
    x/= total;
    x*= 100;
    println("missing_OnlineSection:"+missing_OnlineSection+"("+x+"%)")
    x = missing_Section;
    x/= total;
    x*= 100;
    println("missing_Section:"+missing_Section+"("+x+"%)")
    x = missing_Descriptions;
    x/= total;
    x*= 100;
    println("missing_Descriptions:"+missing_Descriptions+"("+x+"%)")
    x = missing_Year;
    x/= total;
    x*= 100;
    println("missing_Year:"+missing_Year+"("+x+"%)")
  }
      
  def printContext(documents : ArrayBuffer[NYTMetadata.NYTDocument], baseOffset:Int) : String = {       
    
    var freq_ArticleId = new mutable.HashMap[String, Int]
    var freq_NewsDesk = new mutable.HashMap[Int, Int]
    var freq_OnlineSection = new mutable.HashMap[Int, Int]
    var freq_Section = new mutable.HashMap[Int, Int]
    var freq_Descriptions = new mutable.HashMap[Int, Int]    
    var freq_Year = new mutable.HashMap[Int, Int]
    var n_NewsDesk = 0
    var n_OnlineSection = 0
    var n_Section = 0
    var n_Descriptions = 0    
    var n_Year  = 0
    
    for (doc <- documents){      
      var subj : String = "";
      var split : Array[String] = null
      
      //1- ArticleId
      subj = doc.ArticleId;
      if (!subj.isEmpty){
        var old = freq_ArticleId.getOrElseUpdate(subj, 0)
        freq_ArticleId(subj) = old + 1
      } 
      //TRICK TO CONSIDER ONLY UNIQUE ELEMENTS
      if (!subj.isEmpty && freq_ArticleId.getOrElse(subj, 0)==1){        
        //1- NEWS DESK
        subj = doc.NewsDesk
        if (!subj.isEmpty){  
          try{
            val index = map_NewsDesk(subj)
            var old = freq_NewsDesk.getOrElseUpdate(index, 0)
            freq_NewsDesk(index) = old + 1
            n_NewsDesk+=1
          } catch{ case e: Exception => }
        }
        
        //2- ONLINE SECTION
        split = doc.OnlineSection.split("; ");
        for ( subj <- split){
          if (!subj.isEmpty){
            try{
              val index = map_OnlineSection(subj)
              var old = freq_OnlineSection.getOrElseUpdate(index, 0)
              freq_OnlineSection(index) = old + 1
              n_OnlineSection+=1
            } catch{ case e: Exception => }
          }
        }
        
        //3- SECTION
        subj = doc.Section
        if (!subj.isEmpty){ 
          try{
            val index = map_Section(subj)
            var old = freq_Section.getOrElseUpdate(index, 0)
            freq_Section(index) = old + 1
            n_Section+=1
          } catch{ case e: Exception => }
        }         
        
        var aux_Descriptions : ArrayBuffer[String] = new ArrayBuffer[String]();
        
        //4- DESCRIPTIONS
        split = doc.Descriptions.split("\\t");
        for ( subj <- split){     
          if (!subj.isEmpty){
            if (!aux_Descriptions.contains(subj)){
              aux_Descriptions += subj;
            }            
          }
        }      
        //4- GENERAL ONLINE DESCRIPTION -> DESCRIPTIONS
        split = doc.GeneralOnlineDescriptors.split("\\t");
        for ( subj <- split){     
          if (!subj.isEmpty){
            if (!aux_Descriptions.contains(subj)){
              aux_Descriptions += subj;
            }
          }
        }      
        for (subj <- aux_Descriptions){
          try{
            val index = map_Descriptions(subj)
            var old = freq_Descriptions.getOrElseUpdate(index, 0)
            freq_Descriptions(index) = old + 1
            n_Descriptions+=1
          } catch{ case e: Exception => }
        }
        aux_Descriptions.clear()
        
        //5- YEAR
        subj = doc.Year
        if (!subj.isEmpty){
          try{
            val index = map_Year(subj)
            var old = freq_Year.getOrElseUpdate(index, 0)
            freq_Year(index) = old + 1
            n_Year+=1
          } catch{ case e: Exception => }
        }
      }
    }
    
    var denominator = 1;
//    if(!freq_NewsDesk.isEmpty){ denominator+= 1 }
//    if(!freq_OnlineSection.isEmpty){ denominator+= 1 }
//    if(!freq_Section.isEmpty){ denominator+= 1 }
//    if(!freq_Descriptions.isEmpty){ denominator+= 1 }
    
    if (denominator == 0){
      println("ERROR: tuple without context!!!")
    }
    
    var result : String = "";
    
    //1- NEWS DESK
    for ((k,v) <- freq_NewsDesk){
      var key = k + offset_NewsDesk + baseOffset
      var value : Float = v
      value /= n_NewsDesk
      value /= denominator
      result += " "+key+":"+value;
//      println("ND "+key+":"+value)
    }    
    if (freq_NewsDesk.isEmpty){missing_NewsDesk+=1}
    
    //2- ONLINE SECTION
    for ((k,v) <- freq_OnlineSection){
      var key = k + offset_OnlineSection + baseOffset
      var value : Float = v
      value /= n_OnlineSection
      value /= denominator
      result += " "+key+":"+value;
//      println("OS "+key+":"+value)
    }
    if(freq_OnlineSection.isEmpty){missing_OnlineSection+=1}
    
    //3- SECTION
    for ((k,v) <- freq_Section){
      var key = k + offset_Section + baseOffset
      var value : Float = v
      value /= n_Section
      value /= denominator
      result += " "+key+":"+value;
//      println("SEC "+key+":"+value)
    }
    if(freq_Section.isEmpty){missing_Section+=1}
    
    //4- DESCRIPTIONS
    for ((k,v) <- freq_Descriptions){
        var key = k + offset_Descriptions + baseOffset
        var value : Float = v
        value /= n_Descriptions
        value /= denominator
        result += " "+key+":"+value;
//      println("DES "+key+":"+value)
    }    
    if(freq_Descriptions.isEmpty){missing_Descriptions+=1}
    
    //5- YEAR
    for ((k,v) <- freq_Year){
      var key = k + offset_Year + baseOffset
      var value : Float = v
      value /= n_Year
      value /= denominator
      result += " "+key+":"+value;
//      println("GOD "+key+":"+value)
    }
    if(freq_Year.isEmpty){missing_Year+=1}
    result
  }
  
  //RESTRICTED SET OF VALUES FOR CONTEXT
  var offset_NewsDesk = 0;
  var offset_OnlineSection = 0;
  var offset_Section = 0;
  var offset_Descriptions = 0;
  var offset_Year = 0;
  
  def buildOffsets(){
    offset_NewsDesk = 0;
    offset_OnlineSection = offset_NewsDesk + next_NewsDesk;
    offset_Section = offset_OnlineSection + next_OnlineSection;
    offset_Descriptions = offset_Section + next_Section;
    offset_Year = offset_Descriptions + next_Descriptions;
  }
  
  def giveOffset() : Int = {
    offset_Year + next_Year
  }
  
  def printGroups(out: PrintStream, base : Int) : Int ={
    var c = base
    for (i <- 0 until next_NewsDesk){ out.println(c) }
    c+=1
    for (i <- 0 until next_OnlineSection){ out.println(c) }
    c+=1
    for (i <- 0 until next_Section){ out.println(c) }
    c+=1
    for (i <- 0 until next_Descriptions){ out.println(c) }
    c+=1
    for (i <- 0 until next_Year){ out.println(c) }
    c+=1
    c
  }
   
  //1- NEWS DESK
  var map_NewsDesk = new mutable.HashMap[String, Int]
  var next_NewsDesk = 0;
  //2- ONLINE SECTION
  var map_OnlineSection = new mutable.HashMap[String, Int]
  var next_OnlineSection = 0;
  //3- SECTION
  var map_Section = new mutable.HashMap[String, Int]
  var next_Section = 0;
   //4- GENERAL ONLINE DESCRIPTION -> DESCRIPTIONS
  var map_Descriptions = new mutable.HashMap[String, Int]
  var next_Descriptions = 0; 
  //5- YEAR
  var map_Year = new mutable.HashMap[String, Int]
  var next_Year = 0;
  
  //1- NEWS DESK
  var freq_NewsDesk = new mutable.HashMap[String, Int]
  //2- ONLINE SECTION
  var freq_OnlineSection = new mutable.HashMap[String, Int]
  //3- SECTION
  var freq_Section = new mutable.HashMap[String, Int]
   //4- GENERAL ONLINE DESCRIPTION -> DESCRIPTIONS
  var freq__Descriptions = new mutable.HashMap[String, Int]
  //5- YEAR
  var freq_Year = new mutable.HashMap[String, Int]
  
  //to test
  var freq_ArticleId = new mutable.HashMap[String, Int]
  
  def buildMap(){
    //1- NEWS DESK
    for ((subj,c) <- freq_NewsDesk){      
      if (c > CUTOFF){
        if (map_NewsDesk.getOrElseUpdate(subj,next_NewsDesk)==next_NewsDesk) next_NewsDesk +=1
      }
    }    
    //2- ONLINE SECTION
    for ((subj,c) <- freq_OnlineSection){
      if (c > CUTOFF){
        if (map_OnlineSection.getOrElseUpdate(subj,next_OnlineSection)==next_OnlineSection) next_OnlineSection +=1
      }
    }    
    //3- SECTION
    for ((subj,c) <- freq_Section){
      if (c > CUTOFF){
        if (map_Section.getOrElseUpdate(subj,next_Section)==next_Section) next_Section +=1
      }
    }    
    //4- GENERAL ONLINE DESCRIPTION -> DESCRIPTIONS
    for ((subj,c) <- freq__Descriptions){
      if (c > CUTOFF){
        if (map_Descriptions.getOrElseUpdate(subj,next_Descriptions)==next_Descriptions) next_Descriptions +=1
      }
    }    
    //5- YEAR
    for ((subj,c) <- freq_Year){
      if (c > CUTOFF){
        if (map_Year.getOrElseUpdate(subj,next_Year)==next_Year) next_Year +=1
      }
    }    
  }
  
  def populateMap(documents : ArrayBuffer[NYTMetadata.NYTDocument]){
    var num_documents = 0
    for (doc <- documents){      
      var subj : String = "";
      var split : Array[String] = null      
      //1- ArticleId
      subj = doc.ArticleId;
      if (!subj.isEmpty){
        var old = freq_ArticleId.getOrElseUpdate(subj, 0)
        freq_ArticleId(subj) = old + 1
      } 
      //TRICK TO CONSIDER ONLY UNIQUE ELEMENTS
      if (!subj.isEmpty && freq_ArticleId.getOrElse(subj, 0)==1){
        num_documents+=1
        //1- NEWS DESK
        //6- NewsDesk
        subj = doc.NewsDesk
        if (!subj.isEmpty){
          var old = freq_NewsDesk.getOrElseUpdate(subj, 0)
          freq_NewsDesk(subj) = old + 1
        }
        //2- ONLINE SECTION
        //7*- OnlineSection
        split = doc.OnlineSection.split("; ");
        for ( subj <- split){
          if (!subj.isEmpty){
            var old = freq_OnlineSection.getOrElseUpdate(subj, 0)
            freq_OnlineSection(subj) = old + 1
          }  
        }
        //3- SECTION
        //9- Section
        subj = doc.Section
        if (!subj.isEmpty){
          var old = freq_Section.getOrElseUpdate(subj, 0)
          freq_Section(subj) = old + 1
        }
        var aux_Descriptions : ArrayBuffer[String] = new ArrayBuffer[String]();      
        //4- GENERAL ONLINE DESCRIPTION -> DESCRIPTIONS
        //4*- Descriptions
        split = doc.Descriptions.split("\\t");
        for ( subj <- split){     
          if (!subj.isEmpty){
            if (!aux_Descriptions.contains(subj)){
              aux_Descriptions += subj;
            }            
          }
        }      
        //12*- GeneralOnlineDescriptors
        split = doc.GeneralOnlineDescriptors.split("\\t");
        for ( subj <- split){     
          if (!subj.isEmpty){
            if (!aux_Descriptions.contains(subj)){
              aux_Descriptions += subj;
            }
          }
        }    
        for (subj <- aux_Descriptions){
          var old = freq__Descriptions.getOrElseUpdate(subj, 0)
          freq__Descriptions(subj) = old + 1
        }
        aux_Descriptions.clear()      
        //5- YEAR
        //11- Year
        subj = doc.Year
        if (!subj.isEmpty){
          var old = freq_Year.getOrElseUpdate(subj, 0)
          freq_Year(subj) = old + 1
        }
      }
    }    
    println("#documents:"+num_documents)
  }
  
//  def truncateAt(n: Double, p: Int): Double = { val s = math pow (10, p); (math floor n * s) / s }
//  
//  var missing_ArticleId  = 0;
//  var missing_Day = 0;
//  var missing_DayOfWeek = 0;
//  var missing_Descriptions = 0;
//  var missing_Month = 0;
//  var missing_NewsDesk = 0;
//  var missing_OnlineSection = 0;
//  var missing_Page = 0;
//  var missing_Section = 0;
//  var missing_TaxonomicClassifiers = 0;
//  var missing_Year = 0;
//  var missing_Locations = 0;
//  var missing_People = 0;
//  var missing_TypesOfMaterial = 0;
//  var missing_Organizations = 0;
//  var missing_FiledLocation = 0;
//  var missing_SeriesName = 0;
//  var missing_BiographicalCategories = 0;
//  var missing_Names = 0;
//  var missing_FeaturePage = 0;
//  var missing_OnlineDescriptors = 0;
//  var missing_OnlineOrganizations = 0;
//  var missing_OnlinePeople = 0;
//  var missing_OnlineLocations = 0;
//  var missing_OnlineTitles = 0;
//  var n = 0;
//  var map_ArticleId = new mutable.HashMap[String, Int]
//  var next_ArticleId = 0;
//  var map_Day = new mutable.HashMap[String, Int]
//  var next_Day = 0;
//  var map_DayOfWeek = new mutable.HashMap[String, Int]
//  var next_DayOfWeek = 0;
//  var map_Month = new mutable.HashMap[String, Int]
//  var next_Month = 0;
//  var map_Page = new mutable.HashMap[String, Int]
//  var next_Page = 0;
//  var map_TaxonomicClassifiers = new mutable.HashMap[String, Int]
//  var next_TaxonomicClassifiers = 0;
//  var map_Locations = new mutable.HashMap[String, Int]
//  var next_Locations = 0;
//  var map_People = new mutable.HashMap[String, Int]
//  var next_People = 0;
//  var map_TypesOfMaterial = new mutable.HashMap[String, Int]
//  var next_TypesOfMaterial = 0;
//  var map_Organizations = new mutable.HashMap[String, Int]
//  var next_Organizations = 0;
//  var map_FiledLocation = new mutable.HashMap[String, Int]
//  var next_FiledLocation = 0;
//  var map_SeriesName = new mutable.HashMap[String, Int]
//  var next_SeriesName = 0;
//  var map_BiographicalCategories = new mutable.HashMap[String, Int]
//  var next_BiographicalCategories = 0; 
//  var map_Names = new mutable.HashMap[String, Int]
//  var next_Names = 0;
//  var map_FeaturePage = new mutable.HashMap[String, Int]
//  var next_FeaturePage = 0;
//  var map_OnlineDescriptors = new mutable.HashMap[String, Int]
//  var next_OnlineDescriptors = 0;
//  var map_OnlineOrganizations = new mutable.HashMap[String, Int]
//  var next_OnlineOrganizations = 0;
//  var map_OnlinePeople = new mutable.HashMap[String, Int]
//  var next_OnlinePeople = 0;
//  var map_OnlineLocations = new mutable.HashMap[String, Int]
//  var next_OnlineLocations = 0;
//  var map_OnlineTitles = new mutable.HashMap[String, Int]
//  var next_OnlineTitles = 0;      
//      
//      
//      //2- Day
//      subj = doc.Day
//      if (!subj.isEmpty && map_Day.getOrElseUpdate(subj,next_Day)==next_Day) next_Day +=1
//      
//      //3- DayOfWeek
//      subj = doc.DayOfWeek
//      if (!subj.isEmpty && map_DayOfWeek.getOrElseUpdate(subj,next_DayOfWeek)==next_DayOfWeek) next_DayOfWeek +=1,
//      
//      //5- Month      
//      subj = doc.Month
//      if (!subj.isEmpty && map_Month.getOrElseUpdate(subj,next_Month)==next_Month) next_Month +=1
//      
//      //8- Page
//      subj = doc.Page
//      if (!subj.isEmpty && map_Page.getOrElseUpdate(subj,next_Page)==next_Page) next_Page +=1
//      
//      //10*- TaxonomicClassifiers
//      split = doc.TaxonomicClassifiers.split("\\t")
//      for ( subj <- split){      
//        if (!subj.isEmpty && map_TaxonomicClassifiers.getOrElseUpdate(subj,next_TaxonomicClassifiers)==next_TaxonomicClassifiers) next_TaxonomicClassifiers +=1
//      }
//      
//      //13*- Locations
//      split = doc.Locations.split("\\t")
//      for ( subj <- split){ 
//        if (!subj.isEmpty && map_Locations.getOrElseUpdate(subj,next_Locations)==next_Locations) next_Locations +=1
//      }
//      
//      //14*- People
//      split = doc.People.split("\\t")
//      for ( subj <- split){ 
//        if (!subj.isEmpty && map_People.getOrElseUpdate(subj,next_People)==next_People) next_People +=1
//      }
//      
//      //15- TypesOfMaterial
//      subj = doc.TypesOfMaterial
//      if (!subj.isEmpty && map_TypesOfMaterial.getOrElseUpdate(subj,next_TypesOfMaterial)==next_TypesOfMaterial) next_TypesOfMaterial +=1
//      
//      //16*- Organizations
//      split = doc.Organizations.split("\\t")
//      for ( subj <- split){ 
//        if (!subj.isEmpty && map_Organizations.getOrElseUpdate(subj,next_Organizations)==next_Organizations) next_Organizations +=1
//      }
//      
//      //17- FiledLocation
//      subj = doc.FiledLocation
//      if (!subj.isEmpty && map_FiledLocation.getOrElseUpdate(subj,next_FiledLocation)==next_FiledLocation) next_FiledLocation +=1
//      
//      //18- SeriesName
//      subj = doc.SeriesName
//      if (!subj.isEmpty && map_SeriesName.getOrElseUpdate(subj,next_SeriesName)==next_SeriesName) next_SeriesName +=1
//      
//      //19- BiographicalCategories
//      subj = doc.BiographicalCategories
//      if (!subj.isEmpty && map_BiographicalCategories.getOrElseUpdate(subj,next_BiographicalCategories)==next_BiographicalCategories) next_BiographicalCategories +=1 
//      
//      //20- Names
//      subj = doc.Names
//      if (!subj.isEmpty && map_Names.getOrElseUpdate(subj,next_Names)==next_Names) next_Names +=1
//      
//      //21- FeaturePage
//      subj = doc.FeaturePage
//      if (!subj.isEmpty && map_FeaturePage.getOrElseUpdate(subj,next_FeaturePage)==next_FeaturePage) next_FeaturePage +=1
//      
//      //22- OnlineDescriptors?
//      subj = doc.OnlineDescriptors
//      if (!subj.isEmpty && map_OnlineDescriptors.getOrElseUpdate(subj,next_OnlineDescriptors)==next_OnlineDescriptors) next_OnlineDescriptors +=1
//      
//      //23*- OnlineOrganizations
//      split = doc.OnlineOrganizations.split("\\t")
//      for ( subj <- split){ 
//        if (!subj.isEmpty && map_OnlineOrganizations.getOrElseUpdate(subj,next_OnlineOrganizations)==next_OnlineOrganizations) next_OnlineOrganizations +=1
//      }
//      
//      //24*- OnlinePeople
//      split = doc.OnlinePeople.split("\\t")
//      for ( subj <- split){ 
//        if (!subj.isEmpty && map_OnlinePeople.getOrElseUpdate(subj,next_OnlinePeople)==next_OnlinePeople) next_OnlinePeople +=1
//      }
//      
//      //25- OnlineLocations
//      subj = doc.OnlineLocations
//      if (!subj.isEmpty && map_OnlineLocations.getOrElseUpdate(subj,next_OnlineLocations)==next_OnlineLocations) next_OnlineLocations +=1
//      
//      //26- OnlineTitles
//      subj = doc.OnlineTitles
//      if (!subj.isEmpty && map_OnlineTitles.getOrElseUpdate(subj,next_OnlineTitles)==next_OnlineTitles) next_OnlineTitles +=1
//    
//  def collectStat(documents : ArrayBuffer[NYTMetadata.NYTDocument]){
//    n+=1;
//    
//    var present_ArticleId  = false;
//    var present_Day = false;
//    var present_DayOfWeek = false;
//    var present_Descriptions = false;
//    var present_Month = false;
//    var present_NewsDesk = false;
//    var present_OnlineSection = false;
//    var present_Page = false;
//    var present_Section = false;
//    var present_TaxonomicClassifiers = false;
//    var present_Year = false;
//    var present_Locations = false;
//    var present_People = false;
//    var present_TypesOfMaterial = false;
//    var present_Organizations = false;
//    var present_FiledLocation = false;
//    var present_SeriesName = false;
//    var present_BiographicalCategories = false;
//    var present_Names = false;
//    var present_FeaturePage = false;
//    var present_OnlineDescriptors = false;
//    var present_OnlineOrganizations = false;
//    var present_OnlinePeople = false;
//    var present_OnlineLocations = false;
//    var present_OnlineTitles = false;
//  
//    for (doc <- documents){
//      
//      if (!doc.ArticleId.isEmpty){ present_ArticleId = true;}
//      if (!doc.Day.isEmpty){ present_Day = true; }
//      if (!doc.DayOfWeek.isEmpty){ present_DayOfWeek = true; }
//      if (!doc.Descriptions.isEmpty){ present_Descriptions = true; }
//      if (!doc.Month.isEmpty){ present_Month = true; }
//      if (!doc.NewsDesk.isEmpty){ present_NewsDesk = true; }
//      if (!doc.OnlineSection.isEmpty){ present_OnlineSection = true; }
//      if (!doc.Page.isEmpty){ present_Page = true; }
//      if (!doc.Section.isEmpty){ present_Section = true; }
//      if (!doc.TaxonomicClassifiers.isEmpty){ present_TaxonomicClassifiers = true; }
//      if (!doc.Year.isEmpty){ present_Year = true; }
//      if (!doc.GeneralOnlineDescriptors.isEmpty){ present_Descriptions = true; }
//      if (!doc.Locations.isEmpty){ present_Locations = true; }
//      if (!doc.People.isEmpty){ present_People = true; }
//      if (!doc.TypesOfMaterial.isEmpty){ present_TypesOfMaterial = true; }
//      if (!doc.Organizations.isEmpty){ present_Organizations = true; }
//      if (!doc.FiledLocation.isEmpty){ present_FiledLocation = true; }
//      if (!doc.SeriesName.isEmpty){ present_SeriesName = true; }
//      if (!doc.BiographicalCategories.isEmpty){ present_BiographicalCategories = true; }
//      if (!doc.Names.isEmpty){ present_Names = true; }
//      if (!doc.FeaturePage.isEmpty){ present_FeaturePage = true; }
//      if (!doc.OnlineDescriptors.isEmpty){ present_OnlineDescriptors = true; }
//      if (!doc.OnlineOrganizations.isEmpty){ present_OnlineOrganizations = true; }
//      if (!doc.OnlinePeople.isEmpty){ present_OnlinePeople = true; }
//      if (!doc.OnlineLocations.isEmpty){ present_OnlineLocations = true; }
//      if (!doc.OnlineTitles.isEmpty){ present_OnlineTitles = true; }
//  
//    }
//    
//    if (!present_ArticleId){ missing_ArticleId += 1 }
//    if (!present_Day){ missing_Day += 1 }
//    if (!present_DayOfWeek){ missing_DayOfWeek += 1 }
//    if (!present_Descriptions){ missing_Descriptions += 1 }
//    if (!present_Month){ missing_Month += 1 }
//    if (!present_NewsDesk){ missing_NewsDesk += 1 }
//    if (!present_OnlineSection){ missing_OnlineSection += 1 }
//    if (!present_Page){ missing_Page += 1 }
//    if (!present_Section){ missing_Section += 1 }
//    if (!present_TaxonomicClassifiers){ missing_TaxonomicClassifiers += 1 }
//    if (!present_Year){ missing_Year += 1 }
//    if (!present_Locations){ missing_Locations += 1 }
//    if (!present_People){ missing_People += 1 }
//    if (!present_TypesOfMaterial){ missing_TypesOfMaterial += 1 }
//    if (!present_Organizations){ missing_Organizations += 1 }
//    if (!present_FiledLocation){ missing_FiledLocation += 1 }
//    if (!present_SeriesName){ missing_SeriesName += 1 }
//    if (!present_BiographicalCategories){ missing_BiographicalCategories += 1 }
//    if (!present_Names){ missing_Names += 1 }
//    if (!present_FeaturePage){ missing_FeaturePage += 1 }
//    if (!present_OnlineDescriptors){ missing_OnlineDescriptors += 1 }
//    if (!present_OnlineOrganizations){ missing_OnlineOrganizations += 1 }
//    if (!present_OnlinePeople){ missing_OnlinePeople += 1 }
//    if (!present_OnlineLocations){ missing_OnlineLocations += 1 }
//    if (!present_OnlineTitles){ missing_OnlineTitles += 1 }
//    
//  }
//  
//  def printStat(){
//    println("#name,missing,next  n="+n+"\n");
//    println("ArticleId ,"+missing_ArticleId+","+next_ArticleId)
//    println("Day,"+missing_Day+","+next_Day)
//    println("DayOfWeek,"+missing_DayOfWeek+","+next_DayOfWeek)
//    println("Descriptions,"+missing_Descriptions+","+next_Descriptions)
//    println("Month,"+missing_Month+","+next_Month)
//    println("NewsDesk,"+missing_NewsDesk+","+next_NewsDesk)
//    println("OnlineSection,"+missing_OnlineSection+","+next_OnlineSection)
//    println("Page,"+missing_Page+","+next_Page)
//    println("Section,"+missing_Section+","+next_Section)
//    println("TaxonomicClassifiers,"+missing_TaxonomicClassifiers+","+next_TaxonomicClassifiers)
//    println("Year,"+missing_Year+","+next_Year)
//    println("Locations,"+missing_Locations+","+next_Locations)
//    println("People,"+missing_People+","+next_People)
//    println("TypesOfMaterial,"+missing_TypesOfMaterial+","+next_TypesOfMaterial)
//    println("Organizations,"+missing_Organizations+","+next_Organizations)
//    println("FiledLocation,"+missing_FiledLocation+","+next_FiledLocation)
//    println("SeriesName,"+missing_SeriesName+","+next_SeriesName)
//    println("BiographicalCategories,"+missing_BiographicalCategories+","+next_BiographicalCategories)
//    println("Names,"+missing_Names+","+next_Names)
//    println("FeaturePage,"+missing_FeaturePage+","+next_FeaturePage)
//    println("OnlineDescriptors,"+missing_OnlineDescriptors+","+next_OnlineDescriptors)
//    println("OnlineOrganizations,"+missing_OnlineOrganizations+","+next_OnlineOrganizations)
//    println("OnlinePeople,"+missing_OnlinePeople+","+next_OnlinePeople)
//    println("OnlineLocations,"+missing_OnlineLocations+","+next_OnlineLocations)
//    println("OnlineTitles,"+missing_OnlineTitles+","+next_OnlineTitles)
//  }
}