package org.monarchinitiative.ristretto.cli;

import org.monarchinitiative.ristretto.cli.commands.*;
import org.monarchinitiative.ristretto.core.curie.CurieUtil;
import org.monarchinitiative.ristretto.core.curie.CurieUtilBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.Map;


@Command(name = "ristretto",
        subcommands = {
                PrefixesCommand.class,
                InfoCommand.class,
                ListRootsCommand.class,
                AncestorsCommand.class,
                DescendantsCommand.class,
                SiblingsCommand.class,
                RelationshipsCommand.class,
        },
        mixinStandardHelpOptions = true)
public class RistrettoCommand implements Runnable {

    @Option(names = {"-i", "--input"}, description = "Input file", scope = CommandLine.ScopeType.INHERIT)
    private Path inputFile;

    @Option(names = {"--prefix"}, description = "", scope = CommandLine.ScopeType.INHERIT)
    private Map<String, String> prefix;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new RistrettoCommand()).setCaseInsensitiveEnumValuesAllowed(true).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        // This method can be left empty or used for default behavior
    }

    public Path getInputFile() {
        return inputFile;
    }

    public Map<String, String> getPrefix() {
        return prefix;
    }

    public CurieUtil curieUtil() {
        return prefix == null ? CurieUtilBuilder.defaultCurieUtil() : CurieUtilBuilder.just(prefix);
    }
}
