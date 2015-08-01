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

object Parameters {

    /*
     * file needed in the input_folder
     * 1. nyt-freebase.test.triples.universal.mention.txt
     * 2. nyt-freebase.train.triples.universal.mention.txt
     */
  var input_folder = "../UniversalSchema/naacl2013/" //the name of the folder with the input files
  
  var annotation_folder = "annotations/" //the name of the folder where to read and store the annotation
    
  var target_file : String = "";
  
  var pattern : String = "";
  
  def parse_arguments(args: Array[String]) : Int = {
    var result = 0;
    try{  
      target_file = args(0)
      input_folder = args(1)+"/"
      annotation_folder = args(2)+"/"
      pattern = args(3)
    } catch{
      case e: Exception => 
        println("\nInvalid arguments ["+args.length+"]. Aborting.\n");
        println("Usage:\n AnnotationManager target_file input_folder annotation_folder pattern\n");
        println("Parameters:");
        println(" target_file: the system output (the file with the predicted fact to be annotated).");
        println(" input_folder: the name of the folder with the input mentions files.");
        println(" annotation_folder: the directory from and into which the tool will read/write your new annotations.");
        println(" pattern: a regular expression that determines which relations should be included in the predictions to be annotated.\n");
        result = -1
    }; 
    result
  }
  
}