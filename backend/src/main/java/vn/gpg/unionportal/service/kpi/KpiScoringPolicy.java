package vn.gpg.unionportal.service.kpi;

import vn.gpg.unionportal.dto.KpiModels.Detail;
import vn.gpg.unionportal.dto.KpiModels.ResultStatus;
import vn.gpg.unionportal.model.kpi.KpiClassificationRule;
import vn.gpg.unionportal.model.kpi.KpiDefinition;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

public final class KpiScoringPolicy {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final MathContext MC = MathContext.DECIMAL128;

    private KpiScoringPolicy() {
    }

    public static BigDecimal normalized(KpiDefinition definition, BigDecimal numerator,
                                        BigDecimal denominator) {
        if (numerator == null || definition.getDirection() == null) return null;
        return switch (definition.getDirection()) {
            case "RATING_1_5" -> clamp(numerator.subtract(BigDecimal.ONE, MC)
                    .divide(BigDecimal.valueOf(4), MC)).multiply(HUNDRED, MC);
            case "LOWER_BETTER" -> lowerIsBetter(definition, numerator, denominator);
            case "BOOLEAN" -> numerator.signum() > 0 ? HUNDRED : BigDecimal.ZERO;
            case "HIGHER_BETTER" -> higherIsBetter(definition, numerator, denominator);
            default -> null;
        };
    }

    private static BigDecimal higherIsBetter(KpiDefinition definition, BigDecimal numerator,
                                             BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0 || definition.getTargetValue() == null
                || definition.getTargetValue().signum() <= 0) return null;
        BigDecimal rate = numerator.divide(denominator, MC);
        return clamp(rate.divide(definition.getTargetValue(), MC)).multiply(HUNDRED, MC);
    }

    private static BigDecimal lowerIsBetter(KpiDefinition definition, BigDecimal numerator,
                                            BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0 || definition.getTargetValue() == null
                || definition.getMaxAllowedValue() == null) return null;
        BigDecimal span = definition.getMaxAllowedValue().subtract(definition.getTargetValue(), MC);
        if (span.signum() <= 0) return null;
        BigDecimal actualRate = numerator.divide(denominator, MC);
        return clamp(definition.getMaxAllowedValue().subtract(actualRate, MC).divide(span, MC))
                .multiply(HUNDRED, MC);
    }

    public static BigDecimal earnedPoints(BigDecimal normalizedScore, BigDecimal weight) {
        if (normalizedScore == null || weight == null) return BigDecimal.ZERO;
        return normalizedScore.divide(HUNDRED, MC).multiply(weight, MC);
    }

    public static BigDecimal baseScore(List<Detail> details) {
        BigDecimal earned = details.stream().map(Detail::earnedPoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal eligible = details.stream().filter(item -> item.resultStatus() != ResultStatus.NA)
                .map(Detail::eligibleWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
        return eligible.signum() == 0 ? BigDecimal.ZERO
                : earned.divide(eligible, MC).multiply(HUNDRED, MC);
    }

    public static BigDecimal clampScore(BigDecimal score, BigDecimal scoreScale) {
        if (score == null || score.signum() < 0) return BigDecimal.ZERO;
        return score.compareTo(scoreScale) > 0 ? scoreScale : score;
    }

    public static String classification(BigDecimal score, List<KpiClassificationRule> rules) {
        return rules.stream().filter(rule -> score.compareTo(rule.getMinimumScore()) >= 0)
                .map(KpiClassificationRule::getLabel).findFirst().orElse("Không đạt");
    }

    public static BigDecimal display(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
    }

    private static BigDecimal clamp(BigDecimal value) {
        if (value.signum() < 0) return BigDecimal.ZERO;
        return value.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : value;
    }
}
