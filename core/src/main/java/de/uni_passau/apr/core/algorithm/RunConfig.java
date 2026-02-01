package de.uni_passau.apr.core.algorithm;

import java.util.Random;

public record RunConfig(int maxGenerations, int populationSize, int timeoutSeconds, Random random) {
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
        if (random == null) {
            throw new IllegalArgumentException("random cannot be null");
        }
    }
}
