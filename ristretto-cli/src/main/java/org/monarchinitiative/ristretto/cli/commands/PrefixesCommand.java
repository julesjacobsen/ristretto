package org.monarchinitiative.ristretto.cli.commands;

import org.monarchinitiative.ristretto.cli.RistrettoCommand;
import org.monarchinitiative.ristretto.core.curie.CurieUtil;
import picocli.CommandLine;

import java.util.Map;

@CommandLine.Command(name = "prefixes", description = "Display the known prefix mappings")
public class PrefixesCommand implements Runnable {

    @CommandLine.ParentCommand
    RistrettoCommand parent;

    @CommandLine.Parameters(index = "0..*")
    String[] prefixes;

    @Override
    public void run() {
        CurieUtil curieUtil = parent.curieUtil();
        Map<String, String> prefixToIri = curieUtil.prefixToIri();
        if (prefixes == null || prefixes.length == 0) {
            prefixToIri.forEach((key, value) -> System.out.println(key + "\t" + value));
        } else {
            for (String prefix : prefixes) {
                if (prefixToIri.containsKey(prefix)) {
                    System.out.println(prefix + "\t" + prefixToIri.get(prefix));
                } else {
                    System.err.println(prefix + "\tnot found");
                }
            }
        }
    }
}
