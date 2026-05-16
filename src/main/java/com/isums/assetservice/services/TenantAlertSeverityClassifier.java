package com.isums.assetservice.services;

import lombok.Value;

public final class TenantAlertSeverityClassifier {

    public static final String CRITICAL = "CRITICAL";
    public static final String WARNING = "WARNING";
    public static final String INFO = "INFO";

    private TenantAlertSeverityClassifier() {}

    public static Classification classify(String alertType, String metric, String level, Double value) {
        String normalizedLevel = level == null ? "" : level.toUpperCase();
        String normalizedMetric = metric == null ? "" : metric.toLowerCase();
        String normalizedType = alertType == null ? "" : alertType.toUpperCase();

        boolean lifeSafety =
                normalizedMetric.contains("gas")
                || normalizedMetric.contains("smoke")
                || normalizedMetric.contains("fire")
                || normalizedMetric.contains("co2")
                || normalizedType.contains("FIRE")
                || normalizedType.contains("GAS")
                || normalizedType.contains("SMOKE")
                || (normalizedMetric.contains("temp") && value != null && value >= 70.0);

        if (normalizedType.contains("EIF_ANOMALY") || normalizedType.equals("EIF")
                || normalizedMetric.equals("eif_score") || normalizedMetric.equals("anomaly_score")) {
            return classifyEifAnomaly(normalizedType, value, lifeSafety);
        }

        if (CRITICAL.equals(normalizedLevel)) {
            return new Classification(CRITICAL, lifeSafety);
        }
        if (WARNING.equals(normalizedLevel)) {
            return new Classification(WARNING, lifeSafety);
        }
        if ("HIGH".equals(normalizedLevel)) {
            return new Classification(CRITICAL, lifeSafety);
        }
        if ("MEDIUM".equals(normalizedLevel) || "LOW".equals(normalizedLevel)) {
            return new Classification(WARNING, lifeSafety);
        }
        return new Classification(INFO, lifeSafety);
    }

    private static Classification classifyEifAnomaly(String normalizedType, Double score, boolean baseLifeSafety) {
        boolean powerAnomaly = normalizedType.contains("POWER") || normalizedType.contains("ELECTRIC");
        if (score == null) {
            return new Classification(WARNING, baseLifeSafety);
        }
        if (score >= 0.85) {
            return new Classification(CRITICAL, baseLifeSafety || powerAnomaly);
        }
        if (score >= 0.70) {
            return new Classification(CRITICAL, baseLifeSafety);
        }
        if (score >= 0.55) {
            return new Classification(WARNING, baseLifeSafety);
        }
        return new Classification(INFO, baseLifeSafety);
    }

    @Value
    public static class Classification {
        String severity;
        boolean lifeSafety;
    }
}
