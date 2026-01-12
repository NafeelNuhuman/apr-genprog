package de.uni_passau.apr.core.algorithm;

public record RunConfig(int maxGenerations, int populationSize, int timeoutSeconds) {
    public RunConfig {
        if (maxGenerations <= 0) {
            throw new IllegalArgumentException("maxGenerations must be > 0");
        }
        if (populationSize <= 0) {
            throw new IllegalArgumentException("populationSize must be > 0");
        }
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("timeoutSeconds must be > 0");
        }
    }
}
