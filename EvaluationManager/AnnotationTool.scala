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
}
