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

import io.{Source,Codec}
import java.nio.charset.CodingErrorAction
import java.io.{FileInputStream, PrintStream, File}
import collection.mutable

object NYTMetadata {
  
//  
//  def main(args: Array[String]) {
//    val metaFile = "resources/trainMeta"
//    loadMeta(metaFile)
//    stats()
//  }
  
  class NYTDocument(
    var ArticleId: String = "", 
    var Day: String = "", 
    var DayOfWeek: String = "", 
    var Descriptions: String = "", 
    var Month: String = "", 
    var NewsDesk: String = "", 
    var OnlineSection: String = "", 
    var Page: String = "", 
    var Section: String = "", 
    var TaxonomicClassifiers: String = "", 
    var Year: String = "", 
    var GeneralOnlineDescriptors: String = "", 
    var Locations: String = "", 
    var People: String = "", 
    var TypesOfMaterial: String = "", 
    var Organizations: String = "", 
    var FiledLocation: String = "", 
    var SeriesName: String = "", 
    var BiographicalCategories: String = "", 
    var Names: String = "", 
    var FeaturePage: String = "", 
    var OnlineDescriptors: String = "", 
    var OnlineOrganizations: String = "", 
    var OnlinePeople: String = "", 
    var OnlineLocations: String = "", 
    var OnlineTitles: String = ""
    ) {}
  
  var documents = new mutable.LinkedHashMap[String, NYTDocument] 
  
  def loadMeta(metaFile : String){
    val lines = Source.fromFile(metaFile, "ISO-8859-1").getLines()    
    var document : String = "";
    var n_document : Int = 0;
    while (lines.hasNext) {
      try{
        val line = lines.next()
        if (document.isEmpty()){ document = line; n_document+=1; }
        else{
          if (line.isEmpty()){
            document = lines.next()
            n_document+=1;
          }
          else{
            val split = line.split("\\t")
            val attribute = split(0)
            val value = split.drop(1).mkString("\t")
            toYesOrNo(attribute,value,documents.getOrElseUpdate( document, new NYTDocument ))            
            if (documents(document).ArticleId.isEmpty()){
              documents(document).ArticleId = document
            }
            
          }
        }
      } catch{
        case e: Exception => //println(e);
      };
    }
//    println("n_document: "+n_document)    
  }

  def toYesOrNo(attribute : String, value : String, document : NYTDocument): String = attribute match {
    case "ArticleId" => document.ArticleId = value; value 
    case "Day" => document.Day = value; value 
    case "DayOfWeek" => document.DayOfWeek = value; value 
    case "Descriptions" => document.Descriptions = value; value 
    case "Month" => document.Month = value; value 
    case "NewsDesk" => document.NewsDesk = value; value 
    case "OnlineSection" => document.OnlineSection = value; value 
    case "Page" => document.Page = value; value 
    case "Section" => document.Section = value; value 
    case "TaxonomicClassifiers" => document.TaxonomicClassifiers = value; value 
    case "Year" => document.Year = value; value 
    case "GeneralOnlineDescriptors" => document.GeneralOnlineDescriptors = value; value 
    case "Locations" => document.Locations = value; value 
    case "People" => document.People = value; value 
    case "TypesOfMaterial" => document.TypesOfMaterial = value; value 
    case "Organizations" => document.Organizations = value; value 
    case "FiledLocation" => document.FiledLocation = value; value 
    case "SeriesName" => document.SeriesName = value; value 
    case "BiographicalCategories" => document.BiographicalCategories = value; value 
    case "Names" => document.Names = value; value 
    case "FeaturePage" => document.FeaturePage = value; value 
    case "OnlineDescriptors" => document.OnlineDescriptors = value; value 
    case "OnlineOrganizations" => document.OnlineOrganizations = value; value 
    case "OnlinePeople" => document.OnlinePeople = value; value 
    case "OnlineLocations" => document.OnlineLocations = value; value 
    case "OnlineTitles" => document.OnlineTitles = value; value 
    case _ => /*println("ERROR! "+attribute+" "+value);*/  "ERROR! "+attribute+" "+value
  }

  def stats(){
    val OnlineSection2Documents = documents.values.groupBy { _.OnlineSection };
    println("OnlineSections: "+OnlineSection2Documents.size)
    for ((k,v) <- OnlineSection2Documents){
      println(k+ " " + v.size)
    }
  }
}