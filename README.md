# CORE
Context-Aware Open Relation Extraction with Factorization Machines

Here you find the source code and data used in the following publication:

-Fabio Petroni, Luciano Del Corro and Rainer Gemulla (2015): "CORE: Context-Aware Open Relation Extraction with Factorization Machines". EMNLP, 2015.

If you use them please cite the paper.

In the following we assume that you are in the top level project directory. We also assume that you have received and unpacked the CoreData.zip archive into a CoreData subdirectory. 

###Convert the input data in a libFM compliant format

The first step is to convert the row data in a libFM compliant format.
To do so you can use the CoreScriptLibFMConverter.
You can find the detail of the script in the folder.

For example, if you want to recreate the input data for the CORE+mtw model, you have to launch the following command:

```
cd CoreScriptLibFMConverter
mkdir ../CORE+mtw
sbt “run ../CoreData/ ../CORE+mtw mtw”
cd ..
```

This will create in the top level project directory a folder CORE+mtw/ with the data for the CORE+mtw model.



###Training the CORE Model

To train the CORE model (e.g., the CORE+mtw model created in the previous step) you need to download a modified version of libFM. You can find it here: https://github.com/fabiopetroni/libfm.

Please see the [libFM - Manual for the BPR extension](http://www.fabiopetroni.com/Download/manual_libFM_with_BPR_extension.pdf) for details about how to use the BPR extension.

For example, if you want to train the CORE+mtw model created so far (with the parameters described in the paper), run the following:

```
./libFM -task r -dim 1,1,100 -regular 0.01 -learn_rate 0.05 -iter 1000 -neg_sample 1 -method bpr -train CORE+mtw/train.libfm -test CORE+mtw/test.libfm -relation CORE+mtw/rel,CORE+mtw/tup -out CORE+mtw_onlyScores.txt
```

This will train the model and provide in output the prediction for the considered evaluation set.

The output file (i.e., CORE+mtw_onlyScores.txt) contains only numbers (one for each line). To be analyzed it must be equipped with the corresponding facts and sorted. To do so run the following:

```
paste CORE+mtw_onlyScores.txt CoreData/test_subsample_prediction.dat > CORE+mtw_unsorted.txt
export LC_NUMERIC=en_US.utf-8
sort -g -r CORE+mtw_unsorted.txt > CORE+mtw.txt
```

Now the file is ready to be analyzed.

###Annotations

Previously annotations are available in the AnnotationManager/annotations/ directory.

The AnnotationManager script allows to annotate new facts in the output file (e.g., CORE+mtw.txt), that have not been already annotated.

You can find the detail of the script in the folder.

For example, if you want to annotate the facts in the CORE+mtw.txt file, for the relation "person/company$", you have to launch the following command:

```
cd AnnotationManager
sbt “run ../CORE+mtw.txt ../CoreData/ annotations person/company$”
cd ..
```

###Evaluation

The EvaluationManager directory contains the two tables presented in the paper (in two files table_Freebase_relations.tex and table_surface_relation.tex) and all the output files created by the considered models (in the results/ subfolder).

The EvaluationManager script allows to evaluate the performance of several output file (included your own solution).

You can find the detail of the script in the folder.

For example, if you want to recreate the two tables in the paper you have to launch the following commands:

```
cd EvaluationManager
```

for surface relations

```
sbt "run surface ../AnnotationManager/annotations/ results/PITF.txt:PITS results/NFE.txt:NFE results/CORE.txt:CORE results/CORE+m.txt:CORE+m results/CORE+t.txt:CORE+t results/CORE+w.txt:CORE+w results/CORE+mt.txt:CORE+mt results/CORE+mtw.txt:CORE+mtw"
```

for Freebase relations

```
sbt "run Freebase ../AnnotationManager/annotations/ results/PITF.txt:PITS results/NFE.txt:NFE results/CORE.txt:CORE results/CORE+m.txt:CORE+m results/CORE+t.txt:CORE+t results/CORE+w.txt:CORE+w results/CORE+mt.txt:CORE+mt results/CORE+mtw.txt:CORE+mtw"
```


####Final notes

If you have problems while executing the sbt commands, insert the " characted with your keyboard (and not with copy and paste).

