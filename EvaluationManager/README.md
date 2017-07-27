# EvaluationManager

Based on the publication:

-Fabio Petroni, Luciano Del Corro and Rainer Gemulla (2015): "CORE: Context-Aware Open Relation Extraction with Factorization Machines". EMNLP, 2015.

If you use the application please cite the paper.

### Usage:

to execute the script:

1. install sbt
2. enter in this directory 
3. execute the following: (a tex file with the output will be created)

```
sbt "run configuration annotation_folder OUTFILE1:NAME1 OUTFILE2:NAME2 …”
```

Parameters:
- `configuration`: relations considered. 'Freebase' or 'surface'. default is Freebase.
- `annotation_folder`: the directory from which the tool will read your new annotations.
- `OUTFILE1:NAME1 OUTFILE2:NAME2 ...`: the output files of the competing solutions to evaluate and a corresponding name.


### Example

for surface relations

```
sbt "run surface ../AnnotationManager/annotations/ results/PITF.txt:PITS results/NFE.txt:NFE results/CORE.txt:CORE results/CORE+m.txt:CORE+m results/CORE+t.txt:CORE+t results/CORE+w.txt:CORE+w results/CORE+mt.txt:CORE+mt results/CORE+mtw.txt:CORE+mtw"
```

for Freebase relations

```
sbt "run Freebase ../AnnotationManager/annotations/ results/PITF.txt:PITS results/NFE.txt:NFE results/CORE.txt:CORE results/CORE+m.txt:CORE+m results/CORE+t.txt:CORE+t results/CORE+w.txt:CORE+w results/CORE+mt.txt:CORE+mt results/CORE+mtw.txt:CORE+mtw"
```

(if the sbt shell starts insert “ with your keyboard)

If you want to compare these results to your own predictions, simply append your results to the list above
