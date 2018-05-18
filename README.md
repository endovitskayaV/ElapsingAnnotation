# elapsingAnnotation
An annotation that shows method elapsed time

example https://github.com/endovitskayaV/AnnotationExample/tree/master/src

Enabling:

1. Enable annotation processing in IDE
For examle, in Intelij Idea File->Settings->Build,Execituion,Deployment->Annotation processors->Enable annotation processing

2.
a) For maven projects
<repositories>
+        <repository>
+            <id>elapsingAnnotation-mvn-repo</id>
+            <url>https://raw.github.com/endovitskayaV/elapsingAnnotation/mvn-repo/</url>
+            <snapshots>
+                <enabled>true</enabled>
+                <updatePolicy>always</updatePolicy>
+            </snapshots>
+        </repository>
     
     <!--your other repositories-->
     
+    </repositories>


    <dependencies>
        <dependency>
            <groupId>ru.vsu</groupId>
            <artifactId>elapsingAnnotation</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>

       <!--your other dependencies-->
       
    </dependencies>

b) For gradle projects:
repositories {
    maven{
        url 'https://raw.github.com/endovitskayaV/elapsingAnnotation/mvn-repo/'
    }
     //your other repositories
}

dependencies {
    compile group: 'ru.vsu', name: 'elapsingAnnotation', version:'1.0-SNAPSHOT'
    //your other dependencies
}

c) For other projects dowload https://github.com/endovitskayaV/elapsingAnnotation/blob/master/elapsingAnnotation-1.0-SNAPSHOT-jar-with-dependencies.jar
and include in project classpath

