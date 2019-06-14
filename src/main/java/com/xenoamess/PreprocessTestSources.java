package com.xenoamess;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.xenoamess.PreprocessSources.preprocessFolder;

/**
 * preprocess test sources in sourceDirectory.
 */
@Mojo(name = "preprocess-test-sources", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES)
public class PreprocessTestSources extends AbstractMojo {

    @Parameter(defaultValue = "${basedir}/src/test/java-templates", property = "sourceDir", required = true)
    private File sourceDirectory;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/src/test/java", property = "outputDir",
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

    @Override
    public void execute() throws MojoExecutionException {
        project.addTestCompileSourceRoot(outputDirectory.getAbsolutePath());
        if (this.args == null) {
            this.args = new String[0];
        }
        Set<String> libPaths = new HashSet<>(Arrays.asList(this.libPaths));
        libPaths.add(sourceDirectory.getAbsolutePath());
        preprocessFolder(this.sourceDirectory, outputDirectory, args, encoding, libPaths);
    }
}
