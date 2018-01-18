@rem note that set "batchFileName = runIndividual.bat" does not work
@rem so you can see it is necessary to remove spaces when setting a variable
@rem in a batch file
@rem https://social.technet.microsoft.com/Forums/ie/en-US/9cec8313-5e1f-47ed-99fa-a01a96e6530c/batch-file-variables-are-blank-after-being-defined-?forum=ITCG
set "batchFileName=runIndividual_ApacheMaven_openllet2.6.3_mvnrepository_SingleJar.bat"

call %batchFileName% examples BnodeQueryExample
call %batchFileName% examples ClassTree "http://www.co-ode.org/ontologies/pizza"
call %batchFileName% examples ExplanationExample
call %batchFileName% examples IncrementalClassifierExample
call %batchFileName% examples IncrementalConsistencyExample
call %batchFileName% examples IndividualsExample
call %batchFileName% examples InterruptReasoningExample
call %batchFileName% examples JenaOwl2_Example
call %batchFileName% examples JenaReasoner
call %batchFileName% examples ModularityExample
call %batchFileName% examples OWLAPIExample
call %batchFileName% examples OWLAPIExample2
call %batchFileName% examples PersistenceExample
call %batchFileName% examples QuerySubsumptionExample
call %batchFileName% examples RulesExample
call %batchFileName% examples SPARQLDLExample
call %batchFileName% examples TerpExample

call %batchFileName% examples263 InterruptReasoningExample263
call %batchFileName% examples263 OWLAPIExample2263
@rem do not compile or execute SPARQLDLExample263 since it runs a server which will stop this script
@rem call %batchFileName% examples263 SPARQLDLExample263

call %batchFileName% examplesModifiedByJevan SPARQLDLExampleJevan1

