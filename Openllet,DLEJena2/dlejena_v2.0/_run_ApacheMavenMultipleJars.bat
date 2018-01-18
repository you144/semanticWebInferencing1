@rem note that set "batchFileName = runIndividual.bat" does not work
@rem so you can see it is necessary to remove spaces when setting a variable
@rem in a batch file
@rem https://social.technet.microsoft.com/Forums/ie/en-US/9cec8313-5e1f-47ed-99fa-a01a96e6530c/batch-file-variables-are-blank-after-being-defined-?forum=ITCG
set "batchFileName=runIndividual_ApacheMavenMultipleJars.bat"

call %batchFileName% examplesModifiedByJevan PropertyChainJevan
call %batchFileName% examplesModifiedByJevan SparqlQueryJevan1
call %batchFileName% examplesModifiedByJevan SparqlQueryJevan2
call %batchFileName% examplesModifiedByJevan test1

call %batchFileName% examples ABoxValidation
call %batchFileName% examples CreateOntology
call %batchFileName% examples CustomTemplate
call %batchFileName% examples LoadOntology
call %batchFileName% examples PropertyChain
call %batchFileName% examples SparqlQuery
call %batchFileName% examples TBoxInconsistency
call %batchFileName% examples Updates
