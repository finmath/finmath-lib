package net.finmath.finitedifference.experimental;

import org.apache.commons.math3.linear.*;

/**
 * Implementation of the Theta-scheme for a one-dimensional local volatility model (still experimental).
 *
 * @author Ralph Rudd
 * @author Christian Fries
 * @author Jörg Kienitz
 * @version 1.0
 */


public class LocalVolatilityTheta {

    // Model Parameters
    private double volatility = 0.4;
    private double riskFreeRate = 0.06;
    private double alpha = 0.9;
    private double localVolatility(double stockPrice, double currentTime) {
        return volatility * Math.pow(stockPrice, alpha - 1);
    }

    // Option Parameters
    private double optionStrike = 40;
    private double optionMaturity = 1;

    // Mesh Parameters
    private double minimumStock = 0;
    private double maximumStock = 160;
    private int numStockSteps = 160;
    private int numTimeSteps = 80;
    private double deltaStock = (maximumStock - minimumStock) / numStockSteps;
    private double deltaTau = optionMaturity / numTimeSteps;

    // Algorithm Parameters
    private double theta = 0.5;

    // Call Option Boundary Conditions
    /*private double V_T(double stockPrice) {
        return Math.max(stockPrice - optionStrike, 0);
    }
    private double V_0(double stockPrice, double currentTime) {
        return 0;
    }
    private double V_inf(double stockPrice, double currentTime) {
        return stockPrice - optionStrike * Math.exp(-riskFreeRate*(optionMaturity - currentTime));
    }*/

    // Put Option Boundary Conditions
    private double V_terminal(double stockPrice) {
        return Math.max(optionStrike - stockPrice, 0);
    }
    private double V_minimumStock(double stockPrice, double currentTime) {
        return optionStrike * Math.exp(-riskFreeRate*(optionMaturity - currentTime)) - stockPrice;
    }
    private double V_maximumStock(double stockPrice, double currentTime) {
        return 0;
    }

    // Time-reversed Boundary Conditions
    private double U_initial(double stockPrice) {return V_terminal(stockPrice); }
    private double U_minimumStock(double stockPrice, double tau) {
        return V_minimumStock(stockPrice, optionMaturity - tau);
    }
    private double U_maximumStock(double stockPrice, double tau) {
        return V_maximumStock(stockPrice, optionMaturity - tau);
    }

    public double[][] solve() {
        // Create interior spatial array of stock prices
        int len = numStockSteps - 1;
        double[] stock = new double[len];
        for (int i = 0; i < len; i++){
            stock[i] = minimumStock + (i + 1) * deltaStock;
        }

        // Create time-reversed tau array
        double[] tau = new double[numTimeSteps + 1];
        for (int i = 0; i < numTimeSteps + 1; i++) {
            tau[i] = i * deltaTau;
        }

        // Create constant matrices
        RealMatrix eye = MatrixUtils.createRealIdentityMatrix(len);
        RealMatrix D1 = MatrixUtils.createRealMatrix(len, len);
        RealMatrix D2 = MatrixUtils.createRealMatrix(len, len);
        RealMatrix T1 = MatrixUtils.createRealMatrix(len, len);
        RealMatrix T2 = MatrixUtils.createRealMatrix(len, len);
        for (int i = 0; i < len; i++) {
            for (int j = 0; j < len; j++) {
                if (i == j) {
                    D1.setEntry(i, j, minimumStock/deltaStock + (i + 1));
                    D2.setEntry(i, j, Math.pow(minimumStock/deltaStock + (i + 1), 2));
                    T2.setEntry(i, j, -2);
                } else if (i == j - 1) {
                    T1.setEntry(i, j, 1);
                    T2.setEntry(i, j, 1);
                } else if (i == j + 1) {
                    T1.setEntry(i, j, -1);
                    T2.setEntry(i, j, 1);
                 } else {
                    D1.setEntry(i, j, 0);
                    D2.setEntry(i, j, 0);
                    T1.setEntry(i, j, 0);
                    T2.setEntry(i, j, 0);
                }
            }
        }
        RealMatrix F1 = eye.scalarMultiply(1 - riskFreeRate * deltaTau);
        RealMatrix F2 = D1.scalarMultiply(0.5 * riskFreeRate * deltaTau).multiply(T1);
        RealMatrix F3 = D2.scalarMultiply(0.5 * deltaTau).multiply(T2);
        RealMatrix G1 = eye.scalarMultiply(1 + riskFreeRate * deltaTau);
        RealMatrix G2 = F2.scalarMultiply(-1);
        RealMatrix G3 = F3.scalarMultiply(-1);

        // Initialize boundary and solution vectors
        RealMatrix b = MatrixUtils.createRealMatrix(len, 1);
        RealMatrix b2 = MatrixUtils.createRealMatrix(len, 1);
        RealMatrix U = MatrixUtils.createRealMatrix(len, 1);
        for (int i = 0; i < len; i++) {
            b.setEntry(i,0,0);
            b2.setEntry(i, 0, 0);
            U.setEntry(i,1, U_initial(stock[i]));
        }

        // Theta finite difference method
        for (int m = 0; m < numTimeSteps; m++) {
            double[] sigma = new double[len];
            double[] sigma2 = new double[len];
            for (int i = 0; i < len; i++) {
                sigma[i] = localVolatility(minimumStock + (i + 1) * deltaStock,
                        optionMaturity - m * deltaTau);
                sigma2[i] = localVolatility(minimumStock + (i + 1) * deltaStock,
                        optionMaturity - (m + 1) * deltaTau);
            }
            RealMatrix Sigma = MatrixUtils.createRealDiagonalMatrix(sigma);
            RealMatrix Sigma2 = MatrixUtils.createRealDiagonalMatrix(sigma2);
            RealMatrix F = F1.add(F2).add(Sigma.multiply(F3));
            RealMatrix G = G1.add(G2).add(Sigma2.multiply(G3));
            RealMatrix H = G.scalarMultiply(theta).add(eye.scalarMultiply(1 - theta));
            DecompositionSolver solver = new LUDecomposition(H).getSolver();

            double Sl = (minimumStock / deltaStock + 1);
            double Su = (maximumStock / deltaStock - 1);
            double vl = localVolatility(minimumStock + deltaStock,
                    optionMaturity - m * deltaTau);
            double vu = localVolatility(maximumStock - deltaStock,
                    optionMaturity - m * deltaTau);
            double vl2 = localVolatility(minimumStock + deltaStock,
                    optionMaturity - (m + 1) * deltaTau);
            double vu2 = localVolatility(maximumStock - deltaStock,
                    optionMaturity - (m + 1) * deltaTau);

            b.setEntry(0,0,
                    0.5 * deltaTau * Sl * (vl * Sl - riskFreeRate) * U_minimumStock(minimumStock, tau[m]));
            b.setEntry(len - 1, 0,
                    0.5 * deltaTau * Su * (vu * Su - riskFreeRate) * U_maximumStock(maximumStock, tau[m]));
            b2.setEntry(0,0,
                    0.5 * deltaTau * Sl * (vl2 * Sl - riskFreeRate) * U_minimumStock(minimumStock, tau[m + 1]));
            b2.setEntry(len - 1, 0,
                    0.5 * deltaTau * Su * (vu2 * Su - riskFreeRate) * U_maximumStock(maximumStock, tau[m + 1]));
            RealMatrix U1 = (F.scalarMultiply(1 - theta).add(eye.scalarMultiply(theta))).multiply(U);
            RealMatrix U2 = b.scalarMultiply(1 + theta).add(b2.scalarMultiply(theta));
            U = solver.solve(U1.add(U2));
        }
        double[] optionPrice = U.getColumn(0);
        double[][] stockAndOptionPrice = new double[2][len];
        stockAndOptionPrice[0] = stock;
        stockAndOptionPrice[1] = optionPrice;
        return stockAndOptionPrice;
    }



}
