package de.uni_passau.apr.core.faultlocalization;

public class WeightedLocation {

    int lineNumber;
    double weight;

    public WeightedLocation() {
    }

    public WeightedLocation(int lineNumber, double weight) {
        this.lineNumber = lineNumber;
        this.weight = weight;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
