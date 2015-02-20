# ClearWSD
Java verb sense disambiguation system for OntoNotes groupings and VerbNet classes

[OntoNotes WSD Data and Models](https://www.dropbox.com/s/szxgrfjegx7x375/ondata.zip?dl=0)  
[VerbNet Classification Data and Models](https://www.dropbox.com/s/3z53gx0fnzy4l8t/vndata.zip?dl=0)  

#### usage for edu.colorado.clear.wsd.run.ONClassify and edu.colorado.clear.wsd.run.VNClassify
-i <inputPath> : path to input file (one word per line, sentences separated by blank lines)  
-d <dataPath> : path to data directory  
-o <outputName> : name of output file (e.g. out.vn)  
-v <verboseOutput> : include ClearNLP output (optional)  

##### Example input:
He  
ran  
to  
the  
store  
.

##### Example output:
He  
ran	51.3.2-2-1  
to  
the  
store  
.	

##### Example verbose ([ClearNLP](clearnlp.com)) output:
1	He	he	PRP	vnrole=theme	2	nsubj  
2	ran	run	VBD	pb=run.02|vn=51.3.2-2-1	0	root  
3	to	to	IN	_	2	prep  
4	the	the	DT	_	5	det  
5	store	store	NN	_	3	pobj  
6	.	.	.	_	2	punct  
