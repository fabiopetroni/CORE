# CoreScriptLibFMConverter

Based on the publication:

-Fabio Petroni, Luciano Del Corro and Rainer Gemulla (2015): "CORE: Context-Aware Open Relation Extraction with Factorization Machines". EMNLP, 2015.

If you use the application please cite the paper.

###Usage:

to execute the script:

1. install sbt
2. enter in this directory 
3. execute the following

```
sbt "run input_folder output_folder context_considered"
```

Parameters:
- `input_folder`: the name of the folder with the input files.
- `output_folder`: the name of the folder where to store the conversion.
- `context_considered`: example mtw -> article metadata (m); tuple types (t); bag-of-words (w). default no context


###Example

```
mkdir CORE+mtw
sbt “run ../CoreData/ CORE+mtw mtw”
```

(if the sbt shell starts insert “ with your keyboard)
