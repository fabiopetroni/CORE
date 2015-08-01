# AnnotationManager

Based on the publication:

-Fabio Petroni, Luciano Del Corro and Rainer Gemulla (2015): "CORE: Context-Aware Open Relation Extraction with Factorization Machines". EMNLP, 2015.

If you use the application please cite the paper.

###Usage:

to execute the script:

1. install sbt
2. enter in this directory 
3. execute the following

```
sbt “run target_file input_folder annotation_folder pattern”
```

Parameters:
- `target_file`: the system output (the file with the predicted fact to be annotated).
- `input_folder`: the name of the folder with the input mentions files.
- `annotation_folder`: the directory from and into which the tool will read/write your new annotations.
- `pattern`: a regular expression that determines which relations should be included in the predictions to be annotated.


###Example

```
sbt “run ../EvaluationManager/results/CORE+mtw.txt ../CoreData/ annotations person/company$”
```

(if the sbt shell starts insert “ with your keyboard)
