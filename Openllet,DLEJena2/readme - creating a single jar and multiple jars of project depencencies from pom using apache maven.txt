Readme - Creating a single jar and multiple jars of project depencencies from pom using Apache Maven

Written by Jevan Pipitone
18 January 2018

Contact Details
Website: http://www.jevan.com.au
LinkedIn: http://www.linkedin.com/in/jevanpipitone
Facebook: http://www.facebook.com/jevanpipitone
GitHub: http://www.github.com/jevanpipitone

------------------------------------------------------------

1. To pom.xml add under project tag:

<build>
  <plugins>
    <plugin>
      <!-- NOTE: We don't need a groupId specification because the group is
         org.apache.maven.plugins ...which is assumed by default.
       -->
      <artifactId>maven-assembly-plugin</artifactId>
      <version>3.1.0</version>
      <configuration>
        <descriptorRefs>
          <descriptorRef>jar-with-dependencies</descriptorRef>
        </descriptorRefs>
      </configuration>
      <executions>
        <execution>
          <id>make-assembly</id> <!-- this is used for inheritance merges -->
          <phase>package</phase> <!-- bind to the packaging phase -->
          <goals>
            <goal>single</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-dependency-plugin</artifactId>
      <version>3.0.2</version>
      <executions>
        <execution>
          <id>copy-dependencies</id>
          <phase>package</phase>
          <goals>
            <goal>copy-dependencies</goal>
          </goals>
          <configuration>
            <outputDirectory>${project.build.directory}/alternateLocation</outputDirectory>
            <overWriteReleases>false</overWriteReleases>
            <overWriteSnapshots>false</overWriteSnapshots>
            <overWriteIfNewer>true</overWriteIfNewer>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
</project>

2. To create a project assembly, simple execute the normal package phase from the default lifecycle:

    mvn package

from a cmd.exe command prompt in the same folder as pom.xml

When this build completes, you should see a file in the target directory with a name similar to the following:

    target/sample-1.0-SNAPSHOT-jar-with-dependencies.jar

An easy way to do this is:

Copy runApacheMaven.bat to

_github\Openllet,DLEJena2\openllet maven\openllet-2.6.2\examples\

and after editing pom.xml as per step 1 above then run the batch file.

Do similarly for dlejena in

_github\Openllet,DLEJena2\dlejena maven\dlejena\

You will find all the jars that are needed in the target\alternateLocation\
folder and there will be a jar containing the words "-jar-with-dependencies.jar"
in the target folder that contains all the needed information in one file.

Unfortunately it's not perfect - DLEJena does not run using all of its jars that
apache maven collects, nor with the single jar it creates, so I had to use the previous
set of jars that I had found works, see
_github\Openllet,DLEJena2\dlejena_v2.0\lib

However this could be because the maven project for dlejena that we were supplied with
appears to be for DLEJena v1 but the example java programs are for DLEJena V2.

Openllet works with the multiple jars that apache maven collects, but not with the single
jar it creates. See D:\Data\Applications\GitHub\semanticWebInferencing\Openllet,DLEJena2\openllet 2.6.2\lib\

