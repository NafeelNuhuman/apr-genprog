package de.uni_passau.apr.core.faultlocalization;

public class WeightedLocation {

    private int line;
    private double weight;

    public WeightedLocation() {
    }

    public WeightedLocation(int lineNumber, double weight) {
        this.line = lineNumber;
        this.weight = weight;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
