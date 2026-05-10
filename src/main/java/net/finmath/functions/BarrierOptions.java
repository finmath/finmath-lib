package net.finmath.functions;

/**
 * Analytic helper formulas for barrier-style products under Black-Scholes.
 *
 * <p>
 * This class is intentionally kept in {@code it.univr.fima.correction}
 * and serves as the analytic regression backbone for the finite-difference
 * barrier product layer in this project.
 * </p>
 *
 * <p>
 * Covered formulas include:
 * </p>
 * <ul>
 *   <li>standard single-barrier vanilla options,</li>
 *   <li>single-barrier binary options,</li>
 *   <li>single-barrier one-touch / no-touch style binaries,</li>
 *   <li>double-barrier vanilla options,</li>
 *   <li>double-barrier cash binaries, including KIKO / KOKI,</li>
 *   <li>soft barriers.</li>
 * </ul>
 *
 * <p>
 * The class remains in this package on purpose, to avoid name clashes
 * with the upstream dependency class {@code net.finmath.functions.BarrierOptions}.
 * </p>
 *
 * @author Alessandro Gnoatto
 * @version 1.0
 * @date 23.03.2026
 */
public final class BarrierOptions {

    public enum BarrierType {
        /**
         * The down in.
         */
        DOWN_IN,
        /**
         * The up in.
         */
        UP_IN,
        /**
         * The down out.
         */
        DOWN_OUT,
        /**
         * The up out.
         */
        UP_OUT
    }

    /**
     * Binary payoff style used by single-barrier binary formulas.
     */
    public enum BinaryPayoffType {
        /** Pays a fixed cash amount at expiry when the terminal condition is met. */
        CASH_OR_NOTHING,
        /** Pays one unit of the underlying at expiry when the terminal condition is met. */
        ASSET_OR_NOTHING
    }

    /**
     * Double-barrier monitoring styles supported by the closed-form routines.
     */
    public enum DoubleBarrierType {
        /** Knock-in if either barrier is hit. */
        KNOCK_IN,
        /** Knock-out if either barrier is hit. */
        KNOCK_OUT,
        /** Knock-in at the lower barrier and knock-out at the upper barrier. */
        KIKO,
        /** Knock-out at the lower barrier and knock-in at the upper barrier. */
        KOKI
    }

    /**
     * The pi.
     */
    private static final double PI = 3.14159265358979323846264338327950;

    private BarrierOptions() {}

    /**
     * Prices a standard continuously monitored single-barrier European option
     * with possible rebate under Black-Scholes.
     *
     * <p>This is the Haug-style single-barrier formula already present in the
     * original helper class.</p>
     *
     * @param initialStockValue Spot price {@code S0}.
     * @param riskFreeRate Continuously compounded risk-free rate {@code r}.
     * @param dividendYield Continuously compounded dividend yield {@code q}.
     * @param volatility Black-Scholes volatility {@code sigma}.
     * @param optionMaturity Time to maturity {@code T}.
     * @param optionStrike Strike {@code K}.
     * @param isCall {@code true} for a call, {@code false} for a put.
     * @param rebate Cash rebate associated with the barrier event.
     * @param barrierValue Barrier level {@code H}.
     * @param barrierType Barrier orientation and knock style.
     * @return The single-barrier option value.
     */
    public static double blackScholesBarrierOptionValue(
            final double initialStockValue,
            final double riskFreeRate,
            final double dividendYield,
            final double volatility,
            final double optionMaturity,
            final double optionStrike,
            final boolean isCall,
            final double rebate,
            final double barrierValue,
            final BarrierType barrierType) {

        final int phi = isCall ? +1 : -1;
        final int eta = getEta(barrierType);

        final double volSq = volatility * volatility;
        final double volTime = volatility * Math.sqrt(optionMaturity);
        final double mu = (riskFreeRate - dividendYield - 0.5 * volSq) / volSq;
        final double lambda = Math.sqrt(mu * mu + (2.0 * riskFreeRate) / volSq);
        final double z = Math.log(barrierValue / initialStockValue) / volTime + lambda * volTime;

        final double muVolTime = (1.0 + mu) * volTime;

        final double x1 = Math.log(initialStockValue / optionStrike) / volTime + muVolTime;
        final double x2 = Math.log(initialStockValue / barrierValue) / volTime + muVolTime;
        final double y1 = Math.log(barrierValue * barrierValue / (initialStockValue * optionStrike)) / volTime + muVolTime;
        final double y2 = Math.log(barrierValue / initialStockValue) / volTime + muVolTime;

        final double A = phi * initialStockValue * Math.exp(-dividendYield * optionMaturity)
                * n(phi * x1)
                - phi * optionStrike * Math.exp(-riskFreeRate * optionMaturity)
                * n(phi * (x1 - volTime));
        final double B = phi * initialStockValue * Math.exp(-dividendYield * optionMaturity)
                * n(phi * x2)
                - phi * optionStrike * Math.exp(-riskFreeRate * optionMaturity)
                * n(phi * (x2 - volTime));
        final double C = phi * initialStockValue * Math.exp(-dividendYield * optionMaturity)
                * Math.pow(barrierValue / initialStockValue, 2.0 * (mu + 1.0))
                * n(eta * y1)
                - phi * optionStrike * Math.exp(-riskFreeRate * optionMaturity)
                * Math.pow(barrierValue / initialStockValue, 2.0 * mu)
                * n(eta * (y1 - volTime));
        final double D = phi * initialStockValue * Math.exp(-dividendYield * optionMaturity)
                * Math.pow(barrierValue / initialStockValue, 2.0 * (mu + 1.0))
                * n(eta * y2)
                - phi * optionStrike * Math.exp(-riskFreeRate * optionMaturity)
                * Math.pow(barrierValue / initialStockValue, 2.0 * mu)
                * n(eta * (y2 - volTime));
        final double E = rebate * Math.exp(-riskFreeRate * optionMaturity)
                * (n(eta * (x2 - volTime))
                - Math.pow(barrierValue / initialStockValue, 2.0 * mu)
                * n(eta * (y2 - volTime)));
        final double F = rebate * (Math.pow(barrierValue / initialStockValue, mu + lambda) * n(eta * z)
                + Math.pow(barrierValue / initialStockValue, mu - lambda) * n(eta * (z - 2.0 * lambda * volTime)));

        switch (barrierType) {
        case DOWN_IN:
            if (isCall) {
                return optionStrike >= barrierValue ? C + E : A - B + D + E;
            }
            return optionStrike >= barrierValue ? B - C + D + E : A + E;
        case UP_IN:
            if (isCall) {
                return optionStrike >= barrierValue ? A + E : B - C + D + E;
            }
            return optionStrike >= barrierValue ? A - B + D + E : C + E;
        case DOWN_OUT:
            if (isCall) {
                return optionStrike >= barrierValue ? A - C + F : B - D + F;
            }
            return optionStrike >= barrierValue ? A - B + C - D + F : F;
        case UP_OUT:
            if (isCall) {
                return optionStrike >= barrierValue ? F : A - B + C - D + F;
            }
            return optionStrike >= barrierValue ? B - D + F : A - C + F;
        default:
            throw new IllegalArgumentException("Invalid barrier type.");
        }
    }

    /**
     * Prices a continuously monitored single-barrier binary option under
     * Black-Scholes.
     *
     * @param initialStockValue Spot price {@code S0}.
     * @param riskFreeRate Continuously compounded risk-free rate {@code r}.
     * @param dividendYield Continuously compounded dividend yield {@code q}.
     * @param volatility Black-Scholes volatility {@code sigma}.
     * @param optionMaturity Time to maturity {@code T}.
     * @param optionStrike Strike {@code K}.
     * @param isCall {@code true} for a call, {@code false} for a put.
     * @param barrierValue Barrier level {@code H}.
     * @param barrierType Barrier orientation and knock style.
     * @param binaryPayoffType Cash-or-nothing or asset-or-nothing payoff.
     * @param cashPayoff Cash amount used when {@code binaryPayoffType} is cash-or-nothing.
     * @return The single-barrier binary option value.
     */
    public static double blackScholesBinaryBarrierOptionValue(
            final double initialStockValue,
            final double riskFreeRate,
            final double dividendYield,
            final double volatility,
            final double optionMaturity,
            final double optionStrike,
            final boolean isCall,
            final double barrierValue,
            final BarrierType barrierType,
            final BinaryPayoffType binaryPayoffType,
            final double cashPayoff) {

        validatePositive(initialStockValue, "initialStockValue");
        validatePositive(optionStrike, "optionStrike");
        validatePositive(barrierValue, "barrierValue");
        validateNonNegative(volatility, "volatility");
        validateNonNegative(optionMaturity, "optionMaturity");

        final boolean knockedOut =
                (barrierType == BarrierType.DOWN_OUT && initialStockValue <= barrierValue)
                || (barrierType == BarrierType.UP_OUT && initialStockValue >= barrierValue);
        if (knockedOut) {
            return 0.0;
        }

        final boolean knockedIn =
                (barrierType == BarrierType.DOWN_IN && initialStockValue <= barrierValue)
                || (barrierType == BarrierType.UP_IN && initialStockValue >= barrierValue);
        if (knockedIn) {
            return vanillaBinaryValue(
                    initialStockValue,
                    riskFreeRate,
                    dividendYield,
                    volatility,
                    optionMaturity,
                    optionStrike,
                    isCall,
                    binaryPayoffType,
                    cashPayoff);
        }

        final double variance = volatility * volatility * optionMaturity;
        final double stdDev = Math.sqrt(variance);
        final double discount = Math.exp(-riskFreeRate * optionMaturity);
        final double dividendDiscount = Math.exp(-dividendYield * optionMaturity);

        double mu = Math.log(dividendDiscount / discount) / variance - 0.5;
        double K = 0.0;

        if (binaryPayoffType == BinaryPayoffType.CASH_OR_NOTHING) {
            K = cashPayoff;
        } else {
            mu += 1.0;
            K = initialStockValue * dividendDiscount / discount;
        }

        final double logSX = Math.log(initialStockValue / optionStrike);
        final double logSH = Math.log(initialStockValue / barrierValue);
        final double logHS = Math.log(barrierValue / initialStockValue);
        final double logH2SX = Math.log(barrierValue * barrierValue / (initialStockValue * optionStrike));
        final double hs2mu = Math.pow(barrierValue / initialStockValue, 2.0 * mu);

        final double eta = getEta(barrierType);
        final double phi = isCall ? 1.0 : -1.0;

        final double cumX1;
        final double cumX2;
        final double cumY1;
        final double cumY2;

        if (variance >= 1E-16) {
            final double x1 = phi * (logSX / stdDev + mu * stdDev);
            final double x2 = phi * (logSH / stdDev + mu * stdDev);
            final double y1 = eta * (logH2SX / stdDev + mu * stdDev);
            final double y2 = eta * (logHS / stdDev + mu * stdDev);

            cumX1 = n(x1);
            cumX2 = n(x2);
            cumY1 = n(y1);
            cumY2 = n(y2);
        } else {
            cumX1 = logSX > 0.0 ? 1.0 : 0.0;
            cumX2 = logSH > 0.0 ? 1.0 : 0.0;
            cumY1 = logH2SX > 0.0 ? 1.0 : 0.0;
            cumY2 = logHS > 0.0 ? 1.0 : 0.0;
        }

        final double alpha;
        switch (barrierType) {
        case DOWN_IN:
            if (isCall) {
                alpha = optionStrike >= barrierValue
                        ? hs2mu * cumY1
                        : cumX1 - cumX2 + hs2mu * cumY2;
            } else {
                alpha = optionStrike >= barrierValue
                        ? cumX2 + hs2mu * (-cumY1 + cumY2)
                        : cumX1;
            }
            break;
        case UP_IN:
            if (isCall) {
                alpha = optionStrike >= barrierValue
                        ? cumX1
                        : cumX2 + hs2mu * (-cumY1 + cumY2);
            } else {
                alpha = optionStrike >= barrierValue
                        ? cumX1 - cumX2 + hs2mu * cumY2
                        : hs2mu * cumY1;
            }
            break;
        case DOWN_OUT:
            if (isCall) {
                alpha = optionStrike >= barrierValue
                        ? cumX1 - hs2mu * cumY1
                        : cumX2 - hs2mu * cumY2;
            } else {
                alpha = optionStrike >= barrierValue
                        ? cumX1 - cumX2 + hs2mu * (cumY1 - cumY2)
                        : 0.0;
            }
            break;
        case UP_OUT:
            if (isCall) {
                alpha = optionStrike >= barrierValue
                        ? 0.0
                        : cumX1 - cumX2 + hs2mu * (cumY1 - cumY2);
            } else {
                alpha = optionStrike >= barrierValue
                        ? cumX2 - hs2mu * cumY2
                        : cumX1 - hs2mu * cumY1;
            }
            break;
        default:
            throw new IllegalArgumentException("Invalid barrier type.");
        }

        return discount * K * alpha;
    }

    /**
     * Prices a continuously monitored double-barrier vanilla option under
     * Black-Scholes using the truncated series also used by QuantLib.
     *
     * @param initialStockValue Spot price {@code S0}.
     * @param riskFreeRate Continuously compounded risk-free rate {@code r}.
     * @param dividendYield Continuously compounded dividend yield {@code q}.
     * @param volatility Black-Scholes volatility {@code sigma}.
     * @param optionMaturity Time to maturity {@code T}.
     * @param optionStrike Strike {@code K}.
     * @param isCall {@code true} for a call, {@code false} for a put.
     * @param lowerBarrier Lower barrier {@code L}.
     * @param upperBarrier Upper barrier {@code U}.
     * @param barrierType Double-barrier knock style.
     * @param series Number of image terms retained on each side of zero.
     * @return The double-barrier vanilla option value.
     */
    public static double blackScholesDoubleBarrierOptionValue(
            final double initialStockValue,
            final double riskFreeRate,
            final double dividendYield,
            final double volatility,
            final double optionMaturity,
            final double optionStrike,
            final boolean isCall,
            final double lowerBarrier,
            final double upperBarrier,
            final DoubleBarrierType barrierType,
            final int series) {

        validatePositive(initialStockValue, "initialStockValue");
        validatePositive(optionStrike, "optionStrike");
        validatePositive(lowerBarrier, "lowerBarrier");
        validatePositive(upperBarrier, "upperBarrier");
        validatePositive(volatility, "volatility");
        validatePositive(optionMaturity, "optionMaturity");
        if (lowerBarrier >= upperBarrier) {
            throw new IllegalArgumentException("lowerBarrier must be < upperBarrier");
        }
        if (barrierType == DoubleBarrierType.KIKO || barrierType == DoubleBarrierType.KOKI) {
            throw new IllegalArgumentException("KIKO/KOKI are not supported for vanilla double-barrier options.");
        }

        final boolean triggered = initialStockValue <= lowerBarrier || initialStockValue >= upperBarrier;
        if (triggered) {
            return barrierType == DoubleBarrierType.KNOCK_IN
                    ? vanillaBlackScholesValue(initialStockValue, riskFreeRate, dividendYield, volatility,
                            optionMaturity, optionStrike, isCall)
                    : 0.0;
        }

        final double volatilitySquared = volatility * volatility;
        final double stdDeviation = volatility * Math.sqrt(optionMaturity);
        final double costOfCarry = riskFreeRate - dividendYield;
        final double mu1 = 2.0 * costOfCarry / volatilitySquared + 1.0;
        final double bsigma = (costOfCarry + 0.5 * volatilitySquared) * optionMaturity / stdDeviation;
        final double dividendDiscount = Math.exp(-dividendYield * optionMaturity);
        final double riskFreeDiscount = Math.exp(-riskFreeRate * optionMaturity);

        final double knockOutValue;
        if (isCall) {
            double acc1 = 0.0;
            double acc2 = 0.0;
            for (int n = -series; n <= series; n++) {
                final double L2n = Math.pow(lowerBarrier, 2.0 * n);
                final double U2n = Math.pow(upperBarrier, 2.0 * n);
                final double d1 = Math.log(initialStockValue * U2n / (optionStrike * L2n)) / stdDeviation + bsigma;
                final double d2 = Math.log(initialStockValue * U2n / (upperBarrier * L2n)) / stdDeviation + bsigma;
                final double d3 = Math.log(Math.pow(lowerBarrier, 2.0 * n + 2.0)
                        / (optionStrike * initialStockValue * U2n)) / stdDeviation + bsigma;
                final double d4 = Math.log(Math.pow(lowerBarrier, 2.0 * n + 2.0)
                        / (upperBarrier * initialStockValue * U2n)) / stdDeviation + bsigma;

                acc1 += Math.pow(Math.pow(upperBarrier, n) / Math.pow(lowerBarrier, n), mu1)
                        * (n(d1) - n(d2))
                        - Math.pow(Math.pow(lowerBarrier, n + 1.0) / (Math.pow(upperBarrier, n) * initialStockValue), mu1)
                        * (n(d3) - n(d4));

                acc2 += Math.pow(Math.pow(upperBarrier, n) / Math.pow(lowerBarrier, n), mu1 - 2.0)
                        * (n(d1 - stdDeviation) - n(d2 - stdDeviation))
                        - Math.pow(Math.pow(lowerBarrier, n + 1.0) / (Math.pow(upperBarrier, n) * initialStockValue), mu1 - 2.0)
                        * (n(d3 - stdDeviation) - n(d4 - stdDeviation));
            }
            knockOutValue = Math.max(0.0,
                    initialStockValue * dividendDiscount * acc1 - optionStrike * riskFreeDiscount * acc2);
        } else {
            double acc1 = 0.0;
            double acc2 = 0.0;
            for (int n = -series; n <= series; n++) {
                final double L2n = Math.pow(lowerBarrier, 2.0 * n);
                final double U2n = Math.pow(upperBarrier, 2.0 * n);
                final double y1 = Math.log(initialStockValue * U2n / Math.pow(lowerBarrier, 2.0 * n + 1.0)) / stdDeviation + bsigma;
                final double y2 = Math.log(initialStockValue * U2n / (optionStrike * L2n)) / stdDeviation + bsigma;
                final double y3 = Math.log(Math.pow(lowerBarrier, 2.0 * n + 2.0)
                        / (lowerBarrier * initialStockValue * U2n)) / stdDeviation + bsigma;
                final double y4 = Math.log(Math.pow(lowerBarrier, 2.0 * n + 2.0)
                        / (optionStrike * initialStockValue * U2n)) / stdDeviation + bsigma;

                acc1 += Math.pow(Math.pow(upperBarrier, n) / Math.pow(lowerBarrier, n), mu1 - 2.0)
                        * (n(y1 - stdDeviation) - n(y2 - stdDeviation))
                        - Math.pow(Math.pow(lowerBarrier, n + 1.0) / (Math.pow(upperBarrier, n) * initialStockValue), mu1 - 2.0)
                        * (n(y3 - stdDeviation) - n(y4 - stdDeviation));

                acc2 += Math.pow(Math.pow(upperBarrier, n) / Math.pow(lowerBarrier, n), mu1)
                        * (n(y1) - n(y2))
                        - Math.pow(Math.pow(lowerBarrier, n + 1.0) / (Math.pow(upperBarrier, n) * initialStockValue), mu1)
                        * (n(y3) - n(y4));
            }
            knockOutValue = Math.max(0.0,
                    optionStrike * riskFreeDiscount * acc1 - initialStockValue * dividendDiscount * acc2);
        }

        if (barrierType == DoubleBarrierType.KNOCK_OUT) {
            return knockOutValue;
        }

        return Math.max(0.0,
                vanillaBlackScholesValue(initialStockValue, riskFreeRate, dividendYield, volatility,
                        optionMaturity, optionStrike, isCall) - knockOutValue);
    }

    /**
     * Prices a continuously monitored double-barrier cash binary option under
     * Black-Scholes.
     *
     * @param initialStockValue Spot price {@code S0}.
     * @param riskFreeRate Continuously compounded risk-free rate {@code r}.
     * @param dividendYield Continuously compounded dividend yield {@code q}.
     * @param volatility Black-Scholes volatility {@code sigma}.
     * @param optionMaturity Time to maturity {@code T}.
     * @param cashPayoff Cash amount paid when the contract condition is satisfied.
     * @param lowerBarrier Lower barrier {@code L}.
     * @param upperBarrier Upper barrier {@code U}.
     * @param barrierType Double-barrier binary style.
     * @param maxIteration Maximum number of series terms.
     * @param requiredConvergence Absolute convergence tolerance for the last term.
     * @return The double-barrier cash binary value.
     */
    public static double blackScholesDoubleBarrierCashBinaryValue(
            final double initialStockValue,
            final double riskFreeRate,
            final double dividendYield,
            final double volatility,
            final double optionMaturity,
            final double cashPayoff,
            final double lowerBarrier,
            final double upperBarrier,
            final DoubleBarrierType barrierType,
            final int maxIteration,
            final double requiredConvergence) {

        validatePositive(initialStockValue, "initialStockValue");
        validatePositive(lowerBarrier, "lowerBarrier");
        validatePositive(upperBarrier, "upperBarrier");
        validatePositive(volatility, "volatility");
        validatePositive(optionMaturity, "optionMaturity");
        if (lowerBarrier >= upperBarrier) {
            throw new IllegalArgumentException("lowerBarrier must be < upperBarrier");
        }

        switch (barrierType) {
        case KNOCK_OUT:
            if (initialStockValue <= lowerBarrier || initialStockValue >= upperBarrier) {
                return 0.0;
            }
            break;
        case KNOCK_IN:
            if (initialStockValue <= lowerBarrier || initialStockValue >= upperBarrier) {
                return cashPayoff;
            }
            break;
        case KIKO:
            if (initialStockValue >= upperBarrier) {
                return 0.0;
            } else if (initialStockValue <= lowerBarrier) {
                return cashPayoff;
            }
            break;
        case KOKI:
            if (initialStockValue <= lowerBarrier) {
                return 0.0;
            } else if (initialStockValue >= upperBarrier) {
                return cashPayoff;
            }
            break;
        default:
            throw new IllegalArgumentException("Unsupported double barrier type.");
        }

        final double variance = volatility * volatility * optionMaturity;
        if (barrierType == DoubleBarrierType.KIKO || barrierType == DoubleBarrierType.KOKI) {
            return doubleBarrierBinaryKikoKokiValue(initialStockValue, riskFreeRate, dividendYield, variance,
                    optionMaturity, cashPayoff, lowerBarrier, upperBarrier, barrierType,
                    maxIteration, requiredConvergence);
        }
        return doubleBarrierBinaryExpiryValue(initialStockValue, riskFreeRate, dividendYield, variance,
                optionMaturity, cashPayoff, lowerBarrier, upperBarrier, barrierType,
                maxIteration, requiredConvergence);
    }

    /**
     * Prices a soft-barrier option under Black-Scholes.
     *
     * <p>The barrier is smoothed over the interval between {@code lowerBarrier}
     * and {@code upperBarrier}. When the two levels coincide, the method falls
     * back to the standard single-barrier formula.</p>
     *
     * @param initialStockValue Spot price {@code S0}.
     * @param riskFreeRate Continuously compounded risk-free rate {@code r}.
     * @param dividendYield Continuously compounded dividend yield {@code q}.
     * @param volatility Black-Scholes volatility {@code sigma}.
     * @param optionMaturity Time to maturity {@code T}.
     * @param optionStrike Strike {@code K}.
     * @param isCall {@code true} for a call, {@code false} for a put.
     * @param lowerBarrier Lower end of the soft barrier band.
     * @param upperBarrier Upper end of the soft barrier band.
     * @param barrierType Soft knock-in or knock-out direction.
     * @return The soft-barrier option value.
     */
    public static double blackScholesSoftBarrierOptionValue(
            final double initialStockValue,
            final double riskFreeRate,
            final double dividendYield,
            final double volatility,
            final double optionMaturity,
            final double optionStrike,
            final boolean isCall,
            final double lowerBarrier,
            final double upperBarrier,
            final BarrierType barrierType) {

        validatePositive(initialStockValue, "initialStockValue");
        validatePositive(optionStrike, "optionStrike");
        validatePositive(lowerBarrier, "lowerBarrier");
        validatePositive(upperBarrier, "upperBarrier");
        validatePositive(volatility, "volatility");
        validatePositive(optionMaturity, "optionMaturity");
        if (upperBarrier < lowerBarrier) {
            throw new IllegalArgumentException("upperBarrier must be >= lowerBarrier");
        }

        double r = riskFreeRate;
        final double q = dividendYield;
        if (Math.abs(r - q) < 1E-10) {
            r = q + 1E-6;
        }

        final boolean knockedIn = (barrierType == BarrierType.DOWN_IN && initialStockValue <= lowerBarrier)
                || (barrierType == BarrierType.UP_IN && initialStockValue >= upperBarrier);
        if (knockedIn) {
            return vanillaBlackScholesValue(initialStockValue, r, q, volatility, optionMaturity, optionStrike, isCall);
        }

        final boolean knockedOut = (barrierType == BarrierType.DOWN_OUT && initialStockValue <= lowerBarrier)
                || (barrierType == BarrierType.UP_OUT && initialStockValue >= upperBarrier);
        if (knockedOut) {
            return 0.0;
        }

        if (Math.abs(upperBarrier - lowerBarrier) < 1E-4) {
            return blackScholesBarrierOptionValue(initialStockValue, r, q, volatility, optionMaturity,
                    optionStrike, isCall, 0.0, upperBarrier, barrierType);
        }

        final int eta = isCall ? 1 : -1;
        final double b = r - q;
        final double mu = (b + 0.5 * volatility * volatility) / (volatility * volatility);
        final double sqrtT = Math.sqrt(optionMaturity);
        final double lambda1 = Math.exp(-0.5 * volatility * volatility * optionMaturity * (mu + 0.5) * (mu - 0.5));
        final double lambda2 = Math.exp(-0.5 * volatility * volatility * optionMaturity * (mu - 0.5) * (mu - 1.5));
        final double SX = initialStockValue * optionStrike;
        final double logU2SX = Math.log((upperBarrier * upperBarrier) / SX);
        final double logL2SX = Math.log((lowerBarrier * lowerBarrier) / SX);

        final double d1 = logU2SX / (volatility * sqrtT) + mu * volatility * sqrtT;
        final double d2 = d1 - (mu + 0.5) * volatility * sqrtT;
        final double d3 = logU2SX / (volatility * sqrtT) + (mu - 1.0) * volatility * sqrtT;
        final double d4 = d3 - (mu - 0.5) * volatility * sqrtT;

        final double e1 = logL2SX / (volatility * sqrtT) + mu * volatility * sqrtT;
        final double e2 = e1 - (mu + 0.5) * volatility * sqrtT;
        final double e3 = logL2SX / (volatility * sqrtT) + (mu - 1.0) * volatility * sqrtT;
        final double e4 = e3 - (mu - 0.5) * volatility * sqrtT;

        double term1 = eta * initialStockValue * Math.exp((b - r) * optionMaturity)
                * Math.pow(initialStockValue, -2.0 * mu)
                * Math.pow(SX, mu + 0.5)
                / (2.0 * (mu + 0.5));
        term1 *= Math.pow((upperBarrier * upperBarrier) / SX, mu + 0.5) * n(eta * d1)
                - lambda1 * n(eta * d2)
                - Math.pow((lowerBarrier * lowerBarrier) / SX, mu + 0.5) * n(eta * e1)
                + lambda1 * n(eta * e2);

        double term2 = eta * optionStrike * Math.exp(-r * optionMaturity)
                * Math.pow(initialStockValue, -2.0 * (mu - 1.0))
                * Math.pow(SX, mu - 0.5)
                / (2.0 * (mu - 0.5));
        term2 *= Math.pow((upperBarrier * upperBarrier) / SX, mu - 0.5) * n(eta * d3)
                - lambda2 * n(eta * d4)
                - Math.pow((lowerBarrier * lowerBarrier) / SX, mu - 0.5) * n(eta * e3)
                + lambda2 * n(eta * e4);

        final double knockInValue = (term1 - term2) / (upperBarrier - lowerBarrier);
        if (barrierType == BarrierType.DOWN_IN || barrierType == BarrierType.UP_IN) {
            return knockInValue;
        }

        return vanillaBlackScholesValue(initialStockValue, r, q, volatility, optionMaturity, optionStrike, isCall)
                - knockInValue;
    }

    private static double doubleBarrierBinaryExpiryValue(
            final double initialStockValue,
            final double riskFreeRate,
            final double dividendYield,
            final double variance,
            final double optionMaturity,
            final double cashPayoff,
            final double lowerBarrier,
            final double upperBarrier,
            final DoubleBarrierType barrierType,
            final int maxIteration,
            final double requiredConvergence) {

        final double sigmaSq = variance / optionMaturity;
        final double b = riskFreeRate - dividendYield;
        final double alpha = -0.5 * (2.0 * b / sigmaSq - 1.0);
        final double beta = -0.25 * Math.pow(2.0 * b / sigmaSq - 1.0, 2.0) - 2.0 * riskFreeRate / sigmaSq;
        final double Z = Math.log(upperBarrier / lowerBarrier);
        final double factor = (2.0 * PI * cashPayoff) / (Z * Z);
        final double loAlpha = Math.pow(initialStockValue / lowerBarrier, alpha);
        final double hiAlpha = Math.pow(initialStockValue / upperBarrier, alpha);

        double total = 0.0;
        double term = 0.0;
        for (int i = 1; i < maxIteration; i++) {
            final double term1 = (loAlpha - Math.pow(-1.0, i) * hiAlpha)
                    / (alpha * alpha + Math.pow(i * PI / Z, 2.0));
            final double term2 = Math.sin(i * PI / Z * Math.log(initialStockValue / lowerBarrier));
            final double term3 = Math.exp(-0.5 * (Math.pow(i * PI / Z, 2.0) - beta) * variance);
            term = factor * i * term1 * term2 * term3;
            total += term;
        }
        if (Math.abs(term) >= requiredConvergence) {
            throw new IllegalArgumentException("series did not converge sufficiently fast");
        }

        if (barrierType == DoubleBarrierType.KNOCK_OUT) {
            return Math.max(total, 0.0);
        }
        final double discount = Math.exp(-riskFreeRate * optionMaturity);
        return Math.max(cashPayoff * discount - total, 0.0);
    }

    private static double doubleBarrierBinaryKikoKokiValue(
            final double initialStockValue,
            final double riskFreeRate,
            final double dividendYield,
            final double variance,
            final double optionMaturity,
            final double cashPayoff,
            final double lowerBarrier,
            final double upperBarrier,
            final DoubleBarrierType barrierType,
            final int maxIteration,
            final double requiredConvergence) {

        double barrierLo = lowerBarrier;
        double barrierHi = upperBarrier;
        if (barrierType == DoubleBarrierType.KOKI) {
            final double tmp = barrierLo;
            barrierLo = barrierHi;
            barrierHi = tmp;
        }

        final double sigmaSq = variance / optionMaturity;
        final double b = riskFreeRate - dividendYield;
        final double alpha = -0.5 * (2.0 * b / sigmaSq - 1.0);
        final double beta = -0.25 * Math.pow(2.0 * b / sigmaSq - 1.0, 2.0) - 2.0 * riskFreeRate / sigmaSq;
        final double Z = Math.log(barrierHi / barrierLo);
        final double logSL = Math.log(initialStockValue / barrierLo);

        double total = 0.0;
        double term = 0.0;
        for (int i = 1; i < maxIteration; i++) {
            final double factor = Math.pow(i * PI / Z, 2.0) - beta;
            final double term1 = (beta - Math.pow(i * PI / Z, 2.0) * Math.exp(-0.5 * factor * variance)) / factor;
            final double term2 = Math.sin(i * PI / Z * logSL);
            term = (2.0 / (i * PI)) * term1 * term2;
            total += term;
        }
        total += 1.0 - logSL / Z;
        total *= cashPayoff * Math.pow(initialStockValue / barrierLo, alpha);

        if (Math.abs(term) >= requiredConvergence) {
            throw new IllegalArgumentException("series did not converge sufficiently fast");
        }
        return Math.max(total, 0.0);
    }

    private static double vanillaBinaryValue(
            final double initialStockValue,
            final double riskFreeRate,
            final double dividendYield,
            final double volatility,
            final double optionMaturity,
            final double optionStrike,
            final boolean isCall,
            final BinaryPayoffType binaryPayoffType,
            final double cashPayoff) {

        if (optionMaturity <= 0.0 || volatility <= 0.0) {
            final double intrinsicIndicator = isCall
                    ? (initialStockValue > optionStrike ? 1.0 : 0.0)
                    : (initialStockValue < optionStrike ? 1.0 : 0.0);
            if (binaryPayoffType == BinaryPayoffType.CASH_OR_NOTHING) {
                return Math.exp(-riskFreeRate * Math.max(optionMaturity, 0.0)) * cashPayoff * intrinsicIndicator;
            }
            return initialStockValue * Math.exp(-dividendYield * Math.max(optionMaturity, 0.0)) * intrinsicIndicator;
        }

        final double sqrtT = Math.sqrt(optionMaturity);
        final double d1 = (Math.log(initialStockValue / optionStrike)
                + (riskFreeRate - dividendYield + 0.5 * volatility * volatility) * optionMaturity)
                / (volatility * sqrtT);
        final double d2 = d1 - volatility * sqrtT;
        final double phi = isCall ? 1.0 : -1.0;

        if (binaryPayoffType == BinaryPayoffType.CASH_OR_NOTHING) {
            return cashPayoff * Math.exp(-riskFreeRate * optionMaturity) * n(phi * d2);
        }
        return initialStockValue * Math.exp(-dividendYield * optionMaturity) * n(phi * d1);
    }

    /**
     * Prices a vanilla European option under Black-Scholes by delegating to
     * {@link AnalyticFormulas}.
     *
     * <p>This helper is used only for parity reductions in the barrier formulas.
     * It intentionally avoids re-implementing the vanilla closed form locally.</p>
     *
     * @param initialStockValue Spot price {@code S0}.
     * @param riskFreeRate Continuously compounded risk-free rate {@code r}.
     * @param dividendYield Continuously compounded dividend yield {@code q}.
     * @param volatility Black-Scholes volatility {@code sigma}.
     * @param optionMaturity Time to maturity {@code T}.
     * @param optionStrike Strike {@code K}.
     * @param isCall {@code true} for a call, {@code false} for a put.
     * @return The Black-Scholes value of the corresponding vanilla European option.
     */
    private static double vanillaBlackScholesValue(
            final double initialStockValue,
            final double riskFreeRate,
            final double dividendYield,
            final double volatility,
            final double optionMaturity,
            final double optionStrike,
            final boolean isCall) {

        if (optionMaturity <= 0.0) {
            return Math.max(isCall ? initialStockValue - optionStrike : optionStrike - initialStockValue, 0.0);
        }

        final double forward = initialStockValue * Math.exp((riskFreeRate - dividendYield) * optionMaturity);
        final double payoffUnit = Math.exp(-riskFreeRate * optionMaturity);
        final double callValue = AnalyticFormulas.blackScholesGeneralizedOptionValue(
                forward,
                volatility,
                optionMaturity,
                optionStrike,
                payoffUnit);

        if (isCall) {
            return callValue;
        }

        return callValue - payoffUnit * (forward - optionStrike);
    }

    private static int getEta(final BarrierType barrierType) {
        switch (barrierType) {
        case DOWN_IN:
        case DOWN_OUT:
            return 1;
        case UP_IN:
        case UP_OUT:
            return -1;
        default:
            throw new IllegalArgumentException("Invalid barrier type.");
        }
    }

    private static double n(final double x) {
        return NormalDistribution.cumulativeDistribution(x);
    }

    private static void validatePositive(final double value, final String name) {
        if (!(value > 0.0)) {
            throw new IllegalArgumentException(name + " must be > 0");
        }
    }

    private static void validateNonNegative(final double value, final String name) {
        if (value < 0.0) {
            throw new IllegalArgumentException(name + " must be >= 0");
        }
    }

    /**
     * Settlement timing for the binary barrier payoff.
     */
    public enum BinaryBarrierSettlement {
        /** Pays immediately when the barrier is first hit. */
        AT_HIT,
        /** Pays at expiry if the barrier event condition is satisfied. */
        AT_EXPIRY
    }

    /**
     * Barrier-event style for strike-free binary barriers.
     */
    public enum BinaryBarrierEventType {
        /** Pays if the barrier has been hit before maturity. */
        HIT,
        /** Pays if the barrier has not been hit before maturity. */
        NO_HIT
    }

    /**
     * Prices a continuously monitored single-barrier binary paying at the first hit time.
     *
     * <p>This covers the Reiner-Rubinstein / Haug formulas:
     * <ul>
     *   <li>down-and-in cash-(at-hit)-or-nothing,</li>
     *   <li>up-and-in cash-(at-hit)-or-nothing,</li>
     *   <li>down-and-in asset-(at-hit)-or-nothing,</li>
     *   <li>up-and-in asset-(at-hit)-or-nothing.</li>
     * </ul>
     *
     * <p>For asset-at-hit, the payoff at hit is the barrier level {@code H}, since the hit occurs exactly at {@code S=H}.</p>
     *
     * @param initialStockValue Spot price {@code S0}.
     * @param riskFreeRate Continuously compounded risk-free rate {@code r}.
     * @param dividendYield Continuously compounded dividend yield {@code q}.
     * @param volatility Black-Scholes volatility {@code sigma}.
     * @param optionMaturity Time to maturity {@code T}.
     * @param barrierValue Barrier level {@code H}.
     * @param barrierType Must be {@code DOWN_IN} or {@code UP_IN}.
     * @param binaryPayoffType Cash-or-nothing or asset-or-nothing payoff.
     * @param cashPayoff Cash amount used for cash-or-nothing.
     * @return The at-hit binary barrier value.
     */
    public static double blackScholesBinaryBarrierAtHitValue(
            final double initialStockValue,
            final double riskFreeRate,
            final double dividendYield,
            final double volatility,
            final double optionMaturity,
            final double barrierValue,
            final BarrierType barrierType,
            final BinaryPayoffType binaryPayoffType,
            final double cashPayoff) {

        validatePositive(initialStockValue, "initialStockValue");
        validatePositive(barrierValue, "barrierValue");
        validateNonNegative(volatility, "volatility");
        validateNonNegative(optionMaturity, "optionMaturity");

        if (barrierType != BarrierType.DOWN_IN && barrierType != BarrierType.UP_IN) {
            throw new IllegalArgumentException("At-hit formulas only apply to DOWN_IN or UP_IN.");
        }

        final boolean alreadyHit =
                (barrierType == BarrierType.DOWN_IN && initialStockValue <= barrierValue)
                || (barrierType == BarrierType.UP_IN && initialStockValue >= barrierValue);

        if (alreadyHit) {
            if (binaryPayoffType == BinaryPayoffType.CASH_OR_NOTHING) {
                return cashPayoff;
            }
            return barrierValue;
        }

        if (optionMaturity <= 0.0) {
            return 0.0;
        }

        if (volatility <= 0.0) {
            return 0.0;
        }

        final double sigmaSq = volatility * volatility;
        final double volTime = volatility * Math.sqrt(optionMaturity);
        final double mu = (riskFreeRate - dividendYield - 0.5 * sigmaSq) / sigmaSq;
        final double lambda = Math.sqrt(mu * mu + 2.0 * riskFreeRate / sigmaSq);
        final int eta = getEta(barrierType);

        final double z = Math.log(barrierValue / initialStockValue) / volTime + lambda * volTime;
        final double payoffAmount = binaryPayoffType == BinaryPayoffType.CASH_OR_NOTHING ? cashPayoff : barrierValue;
        final double hs = barrierValue / initialStockValue;

        return payoffAmount
                * (Math.pow(hs, mu + lambda) * n(eta * z)
                + Math.pow(hs, mu - lambda) * n(eta * (z - 2.0 * lambda * volTime)));
    }

    /**
     * Prices a continuously monitored single-barrier binary paying at expiry,
     * depending only on whether the barrier has been hit or not.
     *
     * <p>This covers the Reiner-Rubinstein / Haug formulas:
     * <ul>
     *   <li>down/up-and-in cash-(at-expiration)-or-nothing,</li>
     *   <li>down/up-and-in asset-(at-expiration)-or-nothing,</li>
     *   <li>down/up-and-out cash-or-nothing,</li>
     *   <li>down/up-and-out asset-or-nothing.</li>
     * </ul>
     *
     * @param initialStockValue Spot price {@code S0}.
     * @param riskFreeRate Continuously compounded risk-free rate {@code r}.
     * @param dividendYield Continuously compounded dividend yield {@code q}.
     * @param volatility Black-Scholes volatility {@code sigma}.
     * @param optionMaturity Time to maturity {@code T}.
     * @param barrierValue Barrier level {@code H}.
     * @param barrierType Barrier orientation and knock style.
     * @param eventType HIT for knock-in style payoff, NO_HIT for knock-out style payoff.
     * @param binaryPayoffType Cash-or-nothing or asset-or-nothing.
     * @param cashPayoff Cash amount used when {@code binaryPayoffType} is cash-or-nothing.
     * @return The expiry binary barrier value.
     */
    public static double blackScholesBinaryBarrierStatusAtExpiryValue(
            final double initialStockValue,
            final double riskFreeRate,
            final double dividendYield,
            final double volatility,
            final double optionMaturity,
            final double barrierValue,
            final BarrierType barrierType,
            final BinaryBarrierEventType eventType,
            final BinaryPayoffType binaryPayoffType,
            final double cashPayoff) {

        validatePositive(initialStockValue, "initialStockValue");
        validatePositive(barrierValue, "barrierValue");
        validateNonNegative(volatility, "volatility");
        validateNonNegative(optionMaturity, "optionMaturity");

        final boolean isKnockIn = barrierType == BarrierType.DOWN_IN || barrierType == BarrierType.UP_IN;
        final boolean isKnockOut = barrierType == BarrierType.DOWN_OUT || barrierType == BarrierType.UP_OUT;

        if ((eventType == BinaryBarrierEventType.HIT && !isKnockIn)
                || (eventType == BinaryBarrierEventType.NO_HIT && !isKnockOut)) {
            throw new IllegalArgumentException(
                    "Use HIT with DOWN_IN/UP_IN and NO_HIT with DOWN_OUT/UP_OUT.");
        }

        final boolean alreadyHit =
                ((barrierType == BarrierType.DOWN_IN || barrierType == BarrierType.DOWN_OUT) && initialStockValue <= barrierValue)
                || ((barrierType == BarrierType.UP_IN || barrierType == BarrierType.UP_OUT) && initialStockValue >= barrierValue);

        if (alreadyHit) {
            if (eventType == BinaryBarrierEventType.HIT) {
                if (binaryPayoffType == BinaryPayoffType.CASH_OR_NOTHING) {
                    return cashPayoff * Math.exp(-riskFreeRate * optionMaturity);
                }
                return initialStockValue * Math.exp(-dividendYield * optionMaturity);
            }
            return 0.0;
        }

        if (optionMaturity <= 0.0) {
            return 0.0;
        }

        if (volatility <= 0.0) {
            return 0.0;
        }

        final double sigmaSq = volatility * volatility;
        final double volTime = volatility * Math.sqrt(optionMaturity);
        final double mu = (riskFreeRate - dividendYield - 0.5 * sigmaSq) / sigmaSq;
        final double eta = getEta(barrierType);
        final double phi = isKnockIn ? -eta : eta;

        final double x2 = Math.log(initialStockValue / barrierValue) / volTime + (1.0 + mu) * volTime;
        final double y2 = Math.log(barrierValue / initialStockValue) / volTime + (1.0 + mu) * volTime;
        final double hs2mu = Math.pow(barrierValue / initialStockValue, 2.0 * mu);

        if (binaryPayoffType == BinaryPayoffType.CASH_OR_NOTHING) {
            final double K = cashPayoff;
            final double B2 = K * Math.exp(-riskFreeRate * optionMaturity) * n(phi * (x2 - volTime));
            final double B4 = K * Math.exp(-riskFreeRate * optionMaturity) * hs2mu * n(eta * (y2 - volTime));
            return isKnockIn ? B2 + B4 : B2 - B4;
        } else {
            final double A2 = initialStockValue * Math.exp(-dividendYield * optionMaturity) * n(phi * x2);
            final double A4 = initialStockValue * Math.exp(-dividendYield * optionMaturity)
                    * Math.pow(barrierValue / initialStockValue, 2.0 * (mu + 1.0))
                    * n(eta * y2);
            return isKnockIn ? A2 + A4 : A2 - A4;
        }
    }

    /**
     * One-touch / asset-touch convenience wrapper.
     *
     * <p>For cash-or-nothing this is a standard one-touch paying {@code cashPayoff} at hit.
     * For asset-or-nothing this is an asset-touch paying the barrier level at hit.</p>
      * @param initialStockValue The value.
      * @param riskFreeRate The value.
      * @param dividendYield The value.
      * @param volatility The value.
      * @param optionMaturity The value.
      * @param barrierValue The value.
      * @param barrierType The value.
      * @param binaryPayoffType The value.
      * @param cashPayoff The value.
      * @return The value.
     */
    public static double blackScholesOneTouchValue(
            final double initialStockValue,
            final double riskFreeRate,
            final double dividendYield,
            final double volatility,
            final double optionMaturity,
            final double barrierValue,
            final BarrierType barrierType,
            final BinaryPayoffType binaryPayoffType,
            final double cashPayoff) {

        return blackScholesBinaryBarrierAtHitValue(
                initialStockValue,
                riskFreeRate,
                dividendYield,
                volatility,
                optionMaturity,
                barrierValue,
                barrierType,
                binaryPayoffType,
                cashPayoff);
    }

    /**
     * No-touch / hit-at-expiry convenience wrapper.
     *
     * <p>Use DOWN_OUT or UP_OUT with eventType NO_HIT for no-touch,
     * and DOWN_IN or UP_IN with eventType HIT for hit-by-expiry.</p>
      * @param initialStockValue The value.
      * @param riskFreeRate The value.
      * @param dividendYield The value.
      * @param volatility The value.
      * @param optionMaturity The value.
      * @param barrierValue The value.
      * @param barrierType The value.
      * @param eventType The value.
      * @param binaryPayoffType The value.
      * @param cashPayoff The value.
      * @return The value.
     */
    public static double blackScholesBarrierStatusBinaryValue(
            final double initialStockValue,
            final double riskFreeRate,
            final double dividendYield,
            final double volatility,
            final double optionMaturity,
            final double barrierValue,
            final BarrierType barrierType,
            final BinaryBarrierEventType eventType,
            final BinaryPayoffType binaryPayoffType,
            final double cashPayoff) {

        return blackScholesBinaryBarrierStatusAtExpiryValue(
                initialStockValue,
                riskFreeRate,
                dividendYield,
                volatility,
                optionMaturity,
                barrierValue,
                barrierType,
                eventType,
                binaryPayoffType,
                cashPayoff);
    }
}
