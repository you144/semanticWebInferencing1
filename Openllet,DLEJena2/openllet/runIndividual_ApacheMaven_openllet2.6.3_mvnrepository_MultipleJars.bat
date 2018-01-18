@rem parameters are foldername javafilename javaparameters
@rem javaparameters is optional and can be omitted if not needed
@rem for example runIndividual examples BnodeQueryExample
@rem for example runIndividual examples ClassTree <ontology URI>
@rem note %1 (parameters) have one percentage and variables are enclosed with a percentage sign on either side

set "libraries=lib\ApacheMaven_openllet2.6.3_mvnrepository_MultipleJars\*"
set "output=.\output_ApacheMaven_openllet2.6.3_mvnrepository_MultipleJars"
set "javac="C:\Program Files\Java\jdk1.8.0_144\bin\javac""
set "java="C:\Program Files\Java\jdk1.8.0_144\bin\java""
@rem set "javaargs=-Xms1G -Xmx8G"

%javac% -classpath "%libraries%" openllet\%1\%2.java 2> %output%\%2_compile.txt
%java% -classpath ".;%libraries%" %javaargs% openllet.%1.%2 %3 > %output%\%2_output1.txt 2> %output%\%2_output2.txt
