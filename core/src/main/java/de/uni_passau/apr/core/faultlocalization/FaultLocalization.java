package de.uni_passau.apr.core.faultlocalization;

import java.util.List;

public class FaultLocalization {

    String file;
    List<WeightedLocation> locations;

    public FaultLocalization() {
    }

    public FaultLocalization(String file, List<WeightedLocation> locations) {
        this.file = file;
        this.locations = locations;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public List<WeightedLocation> getLocations() {
        return locations;
    }

    public void setLocations(List<WeightedLocation> locations) {
        this.locations = locations;
    }
}
