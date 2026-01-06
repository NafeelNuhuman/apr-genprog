package de.uni_passau.apr.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "apr",
        mixinStandardHelpOptions = true,
        description = "GenProg-style APR tool (course project).",
        subcommands = {
                ValidateCommand.class
        }
)
public class Main implements Runnable {

    @Override
    public void run() {
        System.out.println("APR tool bootstrapped ✅");
    }

    public static void main(String[] args) {
        int exit = new CommandLine(new Main()).execute(args);
        System.exit(exit);
    }
}