@rem parameters are foldername javafilename javaparameters
@rem javaparameters is optional and can be omitted if not needed
@rem for example runIndividual examplesModifiedByJevan test1
@rem note %1 (parameters) have one percentage and variables are enclosed with a percentage sign on either side

set "libraries=lib\ApacheMavenSingleJar\*"
set "output=.\output_ApacheMavenSingleJar"
set "javac="C:\Program Files\Java\jdk1.8.0_144\bin\javac""
set "java="C:\Program Files\Java\jdk1.8.0_144\bin\java""
@rem set "javaargs=-Xms1G -Xmx8G"

%javac% -classpath %libraries% %1\%2.java 2> %output%\%2_compile.txt
%java% -classpath %libraries%;. %javaargs% %1.%2 %3 > %output%\%2_output1.txt 2> %output%\%2_output2.txt
