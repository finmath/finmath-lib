package net.finmath.finitedifference.solvers;

import net.finmath.finitedifference.models.FDMBlackScholesModel;
import net.finmath.finitedifference.products.FDMEuropeanCallOption;
import org.apache.commons.math3.linear.*;

import java.util.Arrays;

public class FDMThetaMethod {
    private FDMBlackScholesModel model;
    private FDMEuropeanCallOption option;
    private double alpha;
    private double beta;
    private double gamma;
    private double theta;


    // State Space Transformations
    private double f_x(double S) {return Math.log(S / option.strike); }
    private double f_s(double x) { return option.strike * Math.exp(x); }
    private double f_t(double tau) { return option.maturity - (2 * tau) / Math.pow(model.volatility, 2); }
    private double f(double V, double x, double tau) { return (V / option.strike) * Math.exp(-alpha * x - beta * tau); }

    // Heat Equation Boundary Conditions
    private double u_0(double x) {
        return f(option.valueAtMaturity(f_s(x)), x, 0);
    }
    private double u_neg_inf(double x, double tau) {
        return f(option.valueAtLowerStockPriceBoundary(f_s(x), f_t(tau)), x, tau);
    }
    private double u_pos_inf(double x, double tau) {
        return f(option.valueAtUpperStockPriceBoundary(f_s(x), f_t(tau)), x, tau);
    }

    public FDMThetaMethod(FDMBlackScholesModel model, FDMEuropeanCallOption option, double theta) {
        this.model = model;
        this.option = option;
        this.theta = theta;
        this.gamma = (2 * model.riskFreeRate) / Math.pow(model.volatility, 2);
        this.alpha = -0.5 * (gamma - 1);
        this.beta = -0.25 * Math.pow((gamma + 1), 2);
    }

    public double[][] valueOption() {
        // Grid Generation
        double maximumStockPriceOnGrid = model.expectedValueOfStockPrice(option.maturity)
                + model.numStandardDeviations * Math.sqrt(model.varianceOfStockPrice(option.maturity));
        double minimumStockPriceOnGrid = Math.max(model.expectedValueOfStockPrice(option.maturity)
                - model.numStandardDeviations * Math.sqrt(model.varianceOfStockPrice(option.maturity)), 0);
        double maximumX = f_x(maximumStockPriceOnGrid);
        double minimumX = f_x(Math.max(minimumStockPriceOnGrid, 1));
        double dx = (maximumX - minimumX) / (model.numSpacesteps - 2);
        int N_pos = (int) Math.ceil((maximumX / dx) + 1);
        int N_neg = (int) Math.floor((minimumX / dx) - 1);

        // Create interior spatial vector for heat equation
        int len = N_pos - N_neg - 1;
        double[] x = new double[len];
        for (int i = 0; i < len; i++) {
            x[i] = (N_neg + 1) * dx + dx * i;
        }

        // Create time vector for heat equation
        double dtau = Math.pow(model.volatility, 2) * option.maturity / (2 * model.numTimesteps);
        double[] tau = new double[model.numTimesteps + 1];
        for (int i = 0; i < model.numTimesteps + 1; i++) {
            tau[i] = i * dtau;
        }

        // Create necessary matrices
        double kappa = dtau / Math.pow(dx, 2);
        double[][] C = new double[len][len];
        double[][] D = new double[len][len];
        for (int i = 0; i < len; i++) {
            for (int j = 0; j < len; j++) {
                if (i == j) {
                    C[i][j] = 1 + 2 * theta * kappa;
                    D[i][j] = 1 - 2 * (1 - theta) * kappa;
                } else if ((i == j - 1) || (i == j + 1)) {
                    C[i][j] = - theta * kappa;
                    D[i][j] = (1 - theta) * kappa;
                } else {
                    C[i][j] = 0;
                    D[i][j] = 0;
                }
            }
        }
        RealMatrix CMatrix = new Array2DRowRealMatrix(C);
        RealMatrix DMatrix = new Array2DRowRealMatrix(D);
        DecompositionSolver solver = new LUDecomposition(CMatrix).getSolver();

        // Create spatial boundary vector
        double[] b = new double[len];
        Arrays.fill(b, 0);

        // Initialize U
        double[] U = new double[len];
        for (int i = 0; i < U.length; i++) {
            U[i] = u_0(x[i]);
        }
        RealMatrix UVector = MatrixUtils.createColumnRealMatrix(U);

        // Solve system
        for (int m = 0; m < model.numTimesteps; m++) {
            b[0] = (u_neg_inf(N_neg * dx, tau[m]) * (1 - theta) * kappa)
                    + (u_neg_inf(N_neg * dx, tau[m + 1]) * theta * kappa);
            b[len-1] = (u_pos_inf(N_pos * dx, tau[m]) * (1 - theta) * kappa)
                    + (u_pos_inf(N_pos * dx, tau[m + 1]) * theta * kappa);

            RealMatrix bVector = MatrixUtils.createColumnRealMatrix(b);
            RealMatrix constantsMatrix = (DMatrix.multiply(UVector)).add(bVector);
            UVector = solver.solve(constantsMatrix);
        }
        U = UVector.getColumn(0);

        // Transform x to stockPrice and U to optionPrice
        double[] optionPrice = new double[len];
        double[] stockPrice = new double[len];
        for (int i = 0; i < len; i++ ){
            optionPrice[i] = U[i] * option.strike *
                    Math.exp(alpha * x[i] + beta * tau[model.numTimesteps]);
            stockPrice[i] = f_s(x[i]);
        }

        double[][] stockAndOptionPrice = new double[2][len];
        stockAndOptionPrice[0] = stockPrice;
        stockAndOptionPrice[1] = optionPrice;
        return stockAndOptionPrice;
    }

}
