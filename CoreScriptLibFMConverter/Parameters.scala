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

object Parameters {
  
  val c_TUPLE_TYPES = "t"
  val c_NYT_METADATA = "m"
  val c_BAG_OF_WORD = "w"
  
  var TUPLE_TYPES : Boolean = false
  var NYT_METADATA : Boolean = false
  var BAG_OF_WORD : Boolean = false

    /*
     * file needed in the input_folder
     * 1. train_complete_shuffled.dat
     * 2. test_subsample.dat
     * 3. eng_stopwords.txt
     * 4. trainMetaComplete
     */
    var input_folder = "../UniversalSchema/resources/" //the name of the folder with the input files
  
    /*
     * file that will be created in the output_folder
     * train.libfm
     * validation.libfm
     * test.libfm
     * rel.x
     * rel.train
     * rel.validation
     * rel.test
     * rel.groups
     * tup.x
     * tup.train
     * tup.validation
     * tup.test
     * tup.groups
     * conversion_map.dat
     */
  var output_folder = "output/" //the name of the folder where to store the conversion
    
  
  def parse_arguments(args: Array[String]) : Int = {
    var result = 0;
    try{  
      input_folder = args(0)+"/"
      output_folder = args(1)+"/"
      try{
        val context_str = args(2) //example mtw -> article metadata (m); tuple types (t); bag-of-words (w).
        if (context_str.contains(c_TUPLE_TYPES)){
          //TUPLE TYPES INCLUDED (t)
          TUPLE_TYPES = true
        }        
        if (context_str.contains(c_NYT_METADATA)){
          //NYT METADATA INCLUDED (m)
          NYT_METADATA = true   
        }        
        if (context_str.contains(c_BAG_OF_WORD)){
          //BAG-OF-WORD INCLUDED (w)
          BAG_OF_WORD = true;
        }       
      } catch{
        case e: Exception =>{ TUPLE_TYPES = false;
                              NYT_METADATA = false;
                              BAG_OF_WORD = false }
      }
      result = 1
    } catch{
      case e: Exception => 
        println("\nInvalid arguments ["+args.length+"]. Aborting.\n");
        println("Usage:\n CoreScript input_folder output_folder context_considered\n");
        println("Parameters:");
        println(" input_folder: the name of the folder with the input files.");
        println(" output_folder: the name of the folder where to store the conversion.");
        println(" context_considered: example mtw -> article metadata (m); tuple types (t); bag-of-words (w). default no context.\n");
        result = -1
    }; 
    result
  }

  //initial cutoff 
  val rel_cutoff = 10
  val tuple_cutoff = 2
  
  //validation set parameters
  val percentage_validation : Float = 8f //%
  val min_important_relations_in_validation = 5;
  val relations_considered = collection.mutable.HashMap[String, Int] (
    "person/company" -> min_important_relations_in_validation,
    "location/containedby"-> min_important_relations_in_validation,
    "author/works_written"-> min_important_relations_in_validation,
    "person/nationality"-> min_important_relations_in_validation,
    "parent/child"-> min_important_relations_in_validation,
    "person/place_of_death"-> min_important_relations_in_validation,
    "person/place_of_birth"-> min_important_relations_in_validation,
    "neighborhood/neighborhood_of"-> min_important_relations_in_validation,
    "person/parents"-> min_important_relations_in_validation,
    "company/founders"-> min_important_relations_in_validation,
    "film/directed_by"-> min_important_relations_in_validation,
    "sports_team/league"-> (min_important_relations_in_validation -2), //42 samples
    "team/arena_stadium"-> (min_important_relations_in_validation -2), //34 samples
    "team_owner/teams_owned"-> (min_important_relations_in_validation -2), //45 samples
    "roadcast/area_served"-> (min_important_relations_in_validation -1), //62 samples
    "structure/architect"-> (min_important_relations_in_validation -2), //29 samples
    "composer/compositions"-> (min_important_relations_in_validation -2), //28 samples
    "person/religion"-> (min_important_relations_in_validation -2), //34 samples
    "film/produced_by"-> (min_important_relations_in_validation- 1),  //97 samples
    "visit"-> min_important_relations_in_validation,
    "attend"-> min_important_relations_in_validation,
    "base"-> min_important_relations_in_validation,
    "head"-> min_important_relations_in_validation,
    "scientist"-> min_important_relations_in_validation,
    "support"-> min_important_relations_in_validation,
    "adviser"-> min_important_relations_in_validation,
    "criticize"-> min_important_relations_in_validation,
    "praise"-> min_important_relations_in_validation,
    "vote"-> min_important_relations_in_validation
  )
  val relations_considered_stat = collection.mutable.HashMap[String, (Int,Int)] (
    "person/company" -> (0,0),
    "location/containedby"-> (0,0),
    "author/works_written"-> (0,0),
    "person/nationality"-> (0,0),
    "parent/child"-> (0,0),
    "person/place_of_death"-> (0,0),
    "person/place_of_birth"-> (0,0),
    "neighborhood/neighborhood_of"-> (0,0),
    "person/parents"-> (0,0),
    "company/founders"-> (0,0),
    "film/directed_by"-> (0,0),
    "sports_team/league"-> (0,0),
    "team/arena_stadium"-> (0,0),
    "team_owner/teams_owned"-> (0,0),
    "roadcast/area_served"-> (0,0),
    "structure/architect"-> (0,0),
    "composer/compositions"-> (0,0),
    "person/religion"-> (0,0),
    "film/produced_by"-> (0,0),
    "visit"-> (0,0),
    "attend"-> (0,0),
    "base"-> (0,0),
    "head"-> (0,0),
    "scientist"-> (0,0),
    "support"-> (0,0),
    "adviser"-> (0,0),
    "criticize"-> (0,0),
    "praise"-> (0,0),
    "vote"-> (0,0)
  )
  
  
}