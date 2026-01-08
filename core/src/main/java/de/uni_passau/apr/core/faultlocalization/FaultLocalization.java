package de.uni_passau.apr.core.faultlocalization;

import java.util.List;

public class FaultLocalization {

    private String file;
    private List<WeightedLocation> statements;

    public FaultLocalization() {
    }

    public FaultLocalization(String file, List<WeightedLocation> statements) {
        this.file = file;
        this.statements = statements;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public List<WeightedLocation> getStatements() {
        return statements;
    }

    public void setStatements(List<WeightedLocation> statements) {
        this.statements = statements;
    }
}
