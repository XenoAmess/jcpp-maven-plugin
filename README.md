# jcpp-maven-plugin

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.xenoamess/jcpp-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.xenoamess/jcpp-maven-plugin)

jcpp-maven-plugin, A plugin for C pre processing in maven project.

## Goal:
Use a c pre processor ([org.anarres.jcpp](https://github.com/shevek/jcpp)) to process your java templates in a specified folder before build.

## Usage:

1. include it in your `<build>`

    ```pom
    <build>
        <plugins>
            <plugin>
                <groupId>com.xenoamess</groupId>
                <artifactId>jcpp-maven-plugin</artifactId>
                <version>0.2.0</version>
                <executions>
                    <execution>
                        <id>preprocess-sources</id>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>preprocess-sources</goal>
                        </goals>
                        <configuration>
                            <sourceDirectory>
                                ${basedir}/src/main/java-templates
                            </sourceDirectory>
                            <outputDirectory>
                                ${project.build.directory}/generated-sources/src/main/java
                            </outputDirectory>
                            <libPaths>
                                <param>${basedir}/src/main/resources/templates
                                </param>
                            </libPaths>
                        </configuration>
                    </execution>
                    <execution>
                        <id>preprocess-test-sources</id>
                        <phase>generate-test-sources</phase>
                        <goals>
                            <goal>preprocess-test-sources</goal>
                        </goals>
                        <configuration>
                            <sourceDirectory>
                                ${basedir}/src/test/java-templates
                            </sourceDirectory>
                            <outputDirectory>
                                ${project.build.directory}/generated-test-sources/src/test/java
                            </outputDirectory>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
    ```

2. write your code template.
put source template code into `<sourceDirectory>`
put additional libPaths for your template codes into `<libPaths>`

3. mvn compile and get the result.

## Params:

All params and their default value are listed here.
```
    @Parameter(defaultValue = "${basedir}/src/main/java-templates", property = "sourceDir", required = true)
    private File sourceDirectory;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/src/main/java", property = "outputDir",
            required = true)
    private File outputDirectory;

    @Parameter(property = "args")
    private String[] args;

    @Parameter(defaultValue = "${project.build.sourceEncoding}", property = "encoding")
    private String encoding;

    @Parameter(property = "libPaths")
    private String[] libPaths;
    
    @Parameter(defaultValue = "${project}")
    private MavenProject project;
```

**sourceDirectory** means a directory which contains your templates.

**outputDirectory** means a directory to put preprocessed outputDirectory.

**args** means additional args for org.anarres.jcpp.Main

**encoding** means encoding of your source files.

**libPaths** means additional libraries paths for preprocessing your templates.
libPaths will be uniqued before the plugin use libPaths, so don't worry if you have duplicated strings in libPaths.
sourceDirectory will always be added to libPaths automatically before the plugin use libPaths.

**project** means your project. I don't think it shall be changed but if you insisted, then you are free to do what you want.

## Live Demo:

Projects using this:
https://github.com/XenoAmess/commonx
