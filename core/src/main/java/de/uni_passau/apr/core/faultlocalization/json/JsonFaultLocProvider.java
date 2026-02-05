package de.uni_passau.apr.core.faultlocalization.json;

import de.uni_passau.apr.core.faultlocalization.FaultLocalization;
import de.uni_passau.apr.core.faultlocalization.FaultLocalizationProvider;

public class JsonFaultLocProvider implements FaultLocalizationProvider {

    private static final double EPSILON = 1e-9;

    @Override
    public FaultLocalization loadFor(de.uni_passau.apr.core.benchmark.BenchmarkConfig benchmarkConfig) throws java.io.IOException {

        java.nio.file.Path faultLocFilePath = benchmarkConfig.getFaultLocFilePath();

        // Read the JSON file and validate required fields
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        FaultLocalization faultLocalization = objectMapper.readValue(faultLocFilePath.toFile(), FaultLocalization.class);
        if (faultLocalization.getFile() == null || faultLocalization.getStatements() == null || faultLocalization.getStatements().isEmpty()) {
            throw new IllegalArgumentException("Invalid fault localization file: missing required fields.");
        }

        java.util.Set<Integer> seenLines = new java.util.HashSet<>();
        //get line count of buggy program using already loaded buggy program string and File.lines()
        String buggyProgram = benchmarkConfig.getBuggyProgram();
        long lineCount = buggyProgram.lines().count();

        for (de.uni_passau.apr.core.faultlocalization.WeightedLocation location : faultLocalization.getStatements()) {

            double weight = location.getWeight();
            // each location must have a valid line number and weight
            if (location.getLine() <= 0 || weight < -EPSILON) {
                throw new IllegalArgumentException("Invalid fault localization file: invalid line number or weight.");
            }

            // line range is valid 1 <= line <= number of lines in the file
            if ( location.getLine() > lineCount) {
                throw new IllegalArgumentException("Invalid fault localization file: line number out of range.");
            }

            // weight is either 0.0, 0.1, or 1.0
            // setting weight to exact values to avoid floating point issues
            boolean isValidWeight = false;
            if (Math.abs(weight - 0.0) <= EPSILON) {
                location.setWeight(0.0);
                isValidWeight = true;
            } else if (Math.abs(weight - 0.1) <= EPSILON) {
                location.setWeight(0.1);
                isValidWeight = true;
            } else if (Math.abs(weight - 1.0) <= EPSILON) {
                location.setWeight(1.0);
                isValidWeight = true;
            }
            if (!isValidWeight) {
                throw new IllegalArgumentException("Invalid fault localization file: invalid weight value: " + weight);
            }

            // if lines are duplicated, throw exception
            if (!seenLines.add(location.getLine())) {
                throw new IllegalArgumentException("Invalid fault localization file: duplicate line numbers found.");
            }

        }

        // filter out locations with weight 0.0
        faultLocalization.setStatements(
                faultLocalization.getStatements().stream()
                        .filter(loc -> Math.abs(loc.getWeight()) > EPSILON)
                        .toList()
        );

        // if list of locations is empty after filtering, throw exception
        if (faultLocalization.getStatements().isEmpty()) {
            throw new IllegalArgumentException("Invalid fault localization file: no locations with weight > 0.0.");
        }

        return faultLocalization;
    }

}
