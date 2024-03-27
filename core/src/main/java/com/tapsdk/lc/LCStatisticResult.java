package com.tapsdk.lc;

import java.util.ArrayList;
import java.util.List;

public class LCStatisticResult {
    private List<LCStatistic> results = new ArrayList<>();

    /**
     * getter
     * @return statistic list.
     */
    public List<LCStatistic> getResults() {
        return results;
    }

    /**
     * setter
     * @param results statistic list.
     */
    public void setResults(List<LCStatistic> results) {
        this.results = results;
    }
}
