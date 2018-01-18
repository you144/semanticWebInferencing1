Note: this has already been done. The description below is just for your information
so that you can know what I did to have these files as it is now.

1. "Openllet maven" to create openllet-examples-from-maven.jar using intellij maven
to place into "openllet 2.6.2\lib\" folder.

IntelliJ can be used if the original openllet-2.6.2.zip is extracted and then
open pom.xml in the examples openllet-2.6.2\examples folder.

In maven, after opening the pom.xml file in the examples folder

File, Project Structure
+, jar, from modules with dependencies
module: openllet-examples
extract to target jar
OK

output layout/openllet-examples.jar
right click on all available elements on right and select "put into output root"
OK

build, build artifacts, build

copy

openllet maven\openllet-2.6.2\examples\classes\artifacts\openllet_examples_jar\openllet-examples.jar

to

openllet 2.6.2\lib\openllet-examples-from-maven.jar

2. "dlejena maven" to create dlejena-from-maven.jar using intellij maven
to place into "dlejena_v2.0\lib\" folder.

Similar to situation of "openllet maven" but open the intellij maven project using

"dlejena maven\dlejena\pom.xml"

The compiled jar when built in intellij needs to be copied from

dlejena maven\dlejena\classes\artifacts\dlejena_jar\dlejena-maven.jar

to

dlejena_v2.0\lib\

--------------------------------------------------------------------------------

