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

package evaluation

object Parameters {
  
  //    val rankFileNamesAndLabels = Array(    
  //      "results/PITF.txt:PITF",
  //      "results/NFE.txt:NFE",
  //      "results/CORE.txt:CORE",
  //      "results/CORE+m.txt:CORE+m",
  //      "results/CORE+t.txt:CORE+t",
  //      "results/CORE+w.txt:CORE+w",
  //      "results/CORE+mt.txt:CORE+mt",
  //      "results/CORE+mtw.txt:CORE+mtw"
  //    )

  var configuration : Boolean = true // true = Freebase_relation; false = surface_relations
  var annotation_folder = "annotations/" //the name of the folder where to read and store the annotation    
  var rankFileNamesAndLabels : Array[String] = null;
  
  def parse_arguments(args: Array[String]) : Int = {
    var result = 0;
    try{  
      if (args(0).contains("Freebase")){
        configuration = true;
      }
      if (args(0).contains("surface")){
        configuration = false;
      }
      annotation_folder = args(1)+"/"
      rankFileNamesAndLabels = args.lift(2).get +: args.drop(3)
      for (s <- rankFileNamesAndLabels)
        println(s)
    } catch{
      case e: Exception => 
        println("\nInvalid arguments ["+args.length+"]. Aborting.\n");
        println("Usage:\n EvaluationManager configuration annotation_folder OUTFILE1:NAME1 OUTFILE2:NAME2 ...\n");
        println("Parameters:");
        println(" configuration: relations considered. 'Freebase' or 'surface'. default is Freebase");
        println(" annotation_folder: the directory from and into which the tool will read/write your new annotations.");
        println(" OUTFILE1:NAME1 OUTFILE2:NAME2 ...: the output files of the competing solutions to evaluate and a corresponding name.\n");
        result = -1
    }; 
    result
  }
  
}