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


object Conf {
  def targets_Freebase_relations : List[String] = List("person/company$","location/containedby$","person/nationality$","author/works_written$","parent/child$","person/place_of_death$","person/place_of_birth$","neighborhood/neighborhood_of$","person/parents$","company/founders$","film/directed_by$","sports_team/league$","team/arena_stadium$","team_owner/teams_owned$","roadcast/area_served$","structure/architect$","composer/compositions$","person/religion$","film/produced_by$")
  def targets_surface_relations : List[String] = List("head","attend","base","visit","scientist","support","adviser","criticize","praise","vote")
  def K = 100
}