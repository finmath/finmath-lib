package net.finmath.finitedifference.experimental;
import java.util.Arrays;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * Implementation of the theta schemes for the Black-Scholes model (still experimental).
 *
 * @author Ralph Rudd
 * @author Christian Fries
 * @author Jörg Kienitz
 */
public class BlackScholesTheta {

	// Option Parameters
	private double volatility = 0.4;
	private double riskFreeRate = 0.06;
	private double optionStrike = 50;
	private double optionMaturity = 1;

	// Mesh Parameters
	private int numberOfPointsNegative = -100;
	private int numberOfPointsPositive = 20;
	private int numTimesteps = 35;
	private double dx = 0.06;

	// Algorithm Parameters
	private double theta = 0.5;

	// Derived Parameters
	private double gamma = (2 * riskFreeRate) / Math.pow(volatility, 2);
	private double alpha = -0.5 * (gamma - 1);
	private double beta = -0.25 * Math.pow((gamma + 1), 2);
	private double dtau = Math.pow(volatility, 2) * optionMaturity / (2 * numTimesteps);
	private double kappa = dtau / Math.pow(dx, 2);

	// Call Option Boundary Conditions
	private double V_T(double stockPrice) {
		return Math.max(stockPrice - optionStrike, 0);
	}
	private double V_0(double stockPrice, double currentTime) {
		return 0;
	}
	private double V_inf(double stockPrice, double currentTime) {
		return stockPrice - optionStrike * Math.exp(-riskFreeRate*(optionMaturity - currentTime));
	}

	// Transformations
	private double f_s(double x) {
		return optionStrike * Math.exp(x);
	}
	private double f_t(double tau) {
		return optionMaturity - (2 * tau) / Math.pow(volatility, 2);
	}
	private double f(double value, double x, double tau) {
		return (value / optionStrike) * Math.exp(-alpha * x - beta * tau);
	}

	// Heat Equation Boundary Conditions
	private double u_0(double x) {
		return f(V_T(f_s(x)), x, 0);
	}
	private double u_neg_inf(double x, double tau) {
		return f(V_0(f_s(x), f_t(tau)), x, tau);
	}
	private double u_pos_inf(double x, double tau) {
		return f(V_inf(f_s(x), f_t(tau)), x, tau);
	}

	public double[][] solve() {
		// Create interior spatial vector for heat equation
		int len = numberOfPointsPositive - numberOfPointsNegative - 1;
		double[] x = new double[len];
		for (int i = 0; i < len; i++) {
			x[i] = (numberOfPointsNegative + 1) * dx + dx * i;
		}

		// Create time vector for heat equation
		double[] tau = new double[numTimesteps + 1];
		for (int i = 0; i < numTimesteps + 1; i++) {
			tau[i] = i * dtau;
		}

		// Create necessary matrices
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
		for (int m = 0; m < numTimesteps; m++) {
			b[0] = (u_neg_inf(numberOfPointsNegative * dx, tau[m]) * (1 - theta) * kappa)
					+ (u_neg_inf(numberOfPointsNegative * dx, tau[m + 1]) * theta * kappa);
			b[len-1] = (u_pos_inf(numberOfPointsPositive * dx, tau[m]) * (1 - theta) * kappa)
					+ (u_pos_inf(numberOfPointsPositive * dx, tau[m + 1]) * theta * kappa);

			RealMatrix bVector = MatrixUtils.createColumnRealMatrix(b);
			RealMatrix constantsMatrix = (DMatrix.multiply(UVector)).add(bVector);
			UVector = solver.solve(constantsMatrix);
		}
		U = UVector.getColumn(0);

		// Transform x to stockPrice and U to optionPrice
		double[] optionPrice = new double[len];
		double[] stockPrice = new double[len];
		for (int i = 0; i < len; i++ ){
			optionPrice[i] = U[i] * optionStrike *
					Math.exp(alpha * x[i] + beta * tau[numTimesteps]);
			stockPrice[i] = f_s(x[i]);
		}

		double[][] stockAndOptionPrice = new double[2][len];
		stockAndOptionPrice[0] = stockPrice;
		stockAndOptionPrice[1] = optionPrice;
		return stockAndOptionPrice;
	}

}
