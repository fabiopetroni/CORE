scalaVersion := "2.11.5"

fork in run := true

fork in runMain := true

connectInput in run := true

javaOptions in run += "-Xmx60G"

javaOptions in runMain += "-Xmx60G"

showSuccess := false