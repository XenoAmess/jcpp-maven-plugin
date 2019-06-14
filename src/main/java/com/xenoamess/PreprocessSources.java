package com.xenoamess;


import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.anarres.cpp.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * preprocess sources in sourceDirectory.
 */
@Mojo(name = "preprocess-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class PreprocessSources extends AbstractMojo {

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

    @Override
    public void execute() throws MojoExecutionException {
        if (this.args == null) {
            this.args = new String[0];
        }
        Set<String> libPaths = new HashSet<>(Arrays.asList(this.libPaths));
        libPaths.add(sourceDirectory.getAbsolutePath());
        preprocessFolder(this.sourceDirectory, outputDirectory, args, encoding, libPaths);
    }

    public static void preprocessFolder(File sourceDirectory, File outputDirectory, String[] args, String encoding,
                                        Set<String> libPaths) throws MojoExecutionException {
        System.out.println("enter preprocessFolder():");
        System.out.println(sourceDirectory);
        System.out.println(outputDirectory);
        System.out.println("----------");

        if (!sourceDirectory.isDirectory()) {
            throw new IllegalArgumentException("!sourceDirectory.isDirectory()");
        }
        outputDirectory.mkdirs();
        for (File newSourceFile : sourceDirectory.listFiles()) {
            File newOutputFile =
                    new File(outputDirectory.getAbsolutePath() + File.separatorChar + newSourceFile.getName());
            if (newSourceFile.isDirectory()) {
                preprocessFolder(newSourceFile, newOutputFile, args, encoding, libPaths);
            }
            if (newSourceFile.isFile()) {
                try {
                    preprocessFile(newSourceFile, newOutputFile, args, encoding, libPaths);
                } catch (Exception e) {
                    throw new MojoExecutionException("fail to preprocess File:preprocessFolder(File sourceDirectory, " +
                            "File outputDirectory, String[] args) fails:" + newSourceFile + "," + newOutputFile, e);
                }
            }
        }
    }

    public static void preprocessFile(File sourceFile, File outputFile, String[] args, String encoding,
                                      Set<String> libPaths) throws Exception {
        System.out.println("enter preprocessFolder():");
        System.out.println(sourceFile);
        System.out.println(outputFile);
        System.out.println("----------");

        try (FileOutputStream outputFileOutputStream = new FileOutputStream((outputFile))) {
            if (!sourceFile.isFile()) {
                throw new IllegalArgumentException("!sourceDirectory.isDirectory()");
            }

            OptionParser parser = new OptionParser();
            OptionSpec<?> helpOption = parser.accepts("help",
                    "Displays command-line help.")
                    .forHelp();
            OptionSpec<?> versionOption = parser.acceptsAll(Arrays.asList("version"),
                    "Displays the product version (" + BuildMetadata.getInstance().getVersion() + ") and exits.")
                    .forHelp();

            OptionSpec<?> debugOption = parser.acceptsAll(Arrays.asList("debug"),
                    "Enables debug output.");

            OptionSpec<String> defineOption = parser.acceptsAll(Arrays.asList("define", "D"),
                    "Defines the given macro.")
                    .withRequiredArg().ofType(String.class).describedAs("name[=definition]");
            OptionSpec<String> undefineOption = parser.acceptsAll(Arrays.asList("undefine", "U"),
                    "Undefines the given macro, previously either builtin or defined using -D.")
                    .withRequiredArg().describedAs("name");
            OptionSpec<File> includeOption = parser.accepts("include",
                    "Process file as if \"#" + "include \"file\"\" appeared as the first line of the primary source " +
                            "file.")
                    .withRequiredArg().ofType(File.class).describedAs("file");
            OptionSpec<File> incdirOption = parser.acceptsAll(Arrays.asList("incdir", "I"),
                    "Adds the directory dir to the list of directories to be searched for header files.")
                    .withRequiredArg().ofType(File.class).describedAs("dir");
            OptionSpec<File> iquoteOption = parser.acceptsAll(Arrays.asList("iquote"),
                    "Adds the directory dir to the list of directories to be searched for header files included using" +
                            " " +
                            "\"\".")
                    .withRequiredArg().ofType(File.class).describedAs("dir");

            Method warningMethod = Main.class.getDeclaredMethod("getWarnings");
            warningMethod.setAccessible(true);
            String warningString = ((StringBuilder) warningMethod.invoke(null)).toString();
            OptionSpec<String> warningOption = parser.acceptsAll(Arrays.asList("warning", "W"),
                    "Enables the named warning class (" + warningString + ").")
                    .withRequiredArg().ofType(String.class).describedAs("warning");
            OptionSpec<Void> noWarningOption = parser.acceptsAll(Arrays.asList("no-warnings", "w"),
                    "Disables ALL warnings.");
            OptionSpec<File> inputsOption = parser.nonOptions()
                    .ofType(File.class).describedAs("Files to process.");

            OptionSet options = parser.parse(args);

            if (options.has(helpOption)) {
                parser.printHelpOn(outputFileOutputStream);
                return;
            }

            if (options.has(versionOption)) {
                Method versionMethod = Main.class.getDeclaredMethod("version", PrintStream.class);
                versionMethod.setAccessible(true);
                versionMethod.invoke(null, outputFileOutputStream);
                return;
            }

            Preprocessor pp = new Preprocessor();
            pp.addFeature(Feature.DIGRAPHS);
            pp.addFeature(Feature.TRIGRAPHS);
            pp.addFeature(Feature.LINEMARKERS);
            pp.addWarning(Warning.IMPORT);
            pp.setListener(new

                    DefaultPreprocessorListener());
            pp.addMacro("__JCPP__");
            pp.addMacro("__XENO_AMESS__");
//            pp.getSystemIncludePath().add("/usr/local/include");
            pp.getSystemIncludePath().addAll(libPaths);

            if (options.has(debugOption)) {
                pp.addFeature(Feature.DEBUG);
            }

            if (options.has(noWarningOption)) {
                pp.getWarnings().

                        clear();
            }

            for (
                    String warning : options.valuesOf(warningOption)) {
                warning = warning.toUpperCase();
                warning = warning.replace('-', '_');
                if (warning.equals("ALL")) {
                    pp.addWarnings(EnumSet.allOf(Warning.class));
                } else {
                    pp.addWarning(Enum.valueOf(Warning.class, warning));
                }
            }

            for (
                    String arg : options.valuesOf(defineOption)) {
                int idx = arg.indexOf('=');
                if (idx == -1) {
                    pp.addMacro(arg);
                } else {
                    pp.addMacro(arg.substring(0, idx), arg.substring(idx + 1));
                }
            }
            for (
                    String arg : options.valuesOf(undefineOption)) {
                pp.getMacros().remove(arg);
            }

            for (
                    File dir : options.valuesOf(incdirOption)) {
                pp.getSystemIncludePath().

                        add(dir.getAbsolutePath());
            }
            for (
                    File dir : options.valuesOf(iquoteOption)) {
                pp.getQuoteIncludePath().

                        add(dir.getAbsolutePath());
            }
            for (
                    File file : options.valuesOf(includeOption))
            // Comply exactly with spec.
            {
                pp.addInput(new

                        StringLexerSource("#" + "include \"" + file + "\"\n"));
            }

            List<File> inputs = options.valuesOf(inputsOption);
            if (inputs.isEmpty()) {
                pp.addInput(new FileLexerSource(sourceFile, encoding));
            } else {
                for (File input : inputs) {
                    pp.addInput(new FileLexerSource(input, encoding));
                }
            }

            if (pp.getFeature(Feature.DEBUG)) {
                System.out.println("#" + "include \"...\" search starts here:");
                for (String dir : pp.getQuoteIncludePath()) {
                    System.out.println("  " + dir);
                }
                System.out.println("#" + "include <...> search starts here:");
                for (String dir : pp.getSystemIncludePath()) {
                    System.out.println("  " + dir);
                }
                System.out.println("End of search list.");
            }

            try {
                for (; ; ) {
                    Token tok = pp.token();
                    if (tok == null) {
                        break;
                    }
                    if (tok.getType() == Token.EOF) {
                        break;
                    }
                    if (tok.getType() == Token.P_LINE) {
                        continue;
                    }
                    outputFileOutputStream.write((tok.getText()).getBytes());
                }
            } catch (
                    Exception e) {
                StringBuilder buf = new StringBuilder("Preprocessor failed:\n");

                Field ppSourceField = pp.getClass().getDeclaredField("source");
                ppSourceField.setAccessible(true);
                Source s = (Source) ppSourceField.get(pp);

                while (s != null) {
                    buf.append(" -> ").append(s).append("\n");
                    Field sParentField = s.getClass().getDeclaredField("parent");
                    sParentField.setAccessible(true);
                    s = (Source) sParentField.get(s);
                }
                System.err.println(buf.toString() + e);
            }
        }
    }
}
