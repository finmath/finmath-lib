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
 * @version 1.0
 */
public class BlackScholesTheta {

	// Option Parameters
	private final double volatility = 0.4;
	private final double riskFreeRate = 0.06;
	private final double optionStrike = 50;
	private final double optionMaturity = 1;

	// Mesh Parameters
	private final int numberOfPointsNegative = -100;
	private final int numberOfPointsPositive = 20;
	private final int numTimesteps = 35;
	private final double dx = 0.06;

	// Algorithm Parameters
	private final double theta = 0.5;

	// Derived Parameters
	private final double gamma = (2 * riskFreeRate) / Math.pow(volatility, 2);
	private final double alpha = -0.5 * (gamma - 1);
	private final double beta = -0.25 * Math.pow((gamma + 1), 2);
	private final double dtau = Math.pow(volatility, 2) * optionMaturity / (2 * numTimesteps);
	private final double kappa = dtau / Math.pow(dx, 2);

	// Call Option Boundary Conditions
	private double V_T(final double stockPrice) {
		return Math.max(stockPrice - optionStrike, 0);
	}
	private double V_0(final double stockPrice, final double currentTime) {
		return 0;
	}
	private double V_inf(final double stockPrice, final double currentTime) {
		return stockPrice - optionStrike * Math.exp(-riskFreeRate*(optionMaturity - currentTime));
	}

	// Transformations
	private double f_s(final double x) {
		return optionStrike * Math.exp(x);
	}
	private double f_t(final double tau) {
		return optionMaturity - (2 * tau) / Math.pow(volatility, 2);
	}
	private double f(final double value, final double x, final double tau) {
		return (value / optionStrike) * Math.exp(-alpha * x - beta * tau);
	}

	// Heat Equation Boundary Conditions
	private double u_0(final double x) {
		return f(V_T(f_s(x)), x, 0);
	}
	private double u_neg_inf(final double x, final double tau) {
		return f(V_0(f_s(x), f_t(tau)), x, tau);
	}
	private double u_pos_inf(final double x, final double tau) {
		return f(V_inf(f_s(x), f_t(tau)), x, tau);
	}

	public double[][] solve() {
		// Create interior spatial vector for heat equation
		final int len = numberOfPointsPositive - numberOfPointsNegative - 1;
		final double[] x = new double[len];
		for (int i = 0; i < len; i++) {
			x[i] = (numberOfPointsNegative + 1) * dx + dx * i;
		}

		// Create time vector for heat equation
		final double[] tau = new double[numTimesteps + 1];
		for (int i = 0; i < numTimesteps + 1; i++) {
			tau[i] = i * dtau;
		}

		// Create necessary matrices
		final double[][] C = new double[len][len];
		final double[][] D = new double[len][len];
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

		final RealMatrix CMatrix = new Array2DRowRealMatrix(C);
		final RealMatrix DMatrix = new Array2DRowRealMatrix(D);
		final DecompositionSolver solver = new LUDecomposition(CMatrix).getSolver();

		// Create spatial boundary vector
		final double[] b = new double[len];
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

			final RealMatrix bVector = MatrixUtils.createColumnRealMatrix(b);
			final RealMatrix constantsMatrix = (DMatrix.multiply(UVector)).add(bVector);
			UVector = solver.solve(constantsMatrix);
		}
		U = UVector.getColumn(0);

		// Transform x to stockPrice and U to optionPrice
		final double[] optionPrice = new double[len];
		final double[] stockPrice = new double[len];
		for (int i = 0; i < len; i++ ){
			optionPrice[i] = U[i] * optionStrike *
					Math.exp(alpha * x[i] + beta * tau[numTimesteps]);
			stockPrice[i] = f_s(x[i]);
		}

		final double[][] stockAndOptionPrice = new double[2][len];
		stockAndOptionPrice[0] = stockPrice;
		stockAndOptionPrice[1] = optionPrice;
		return stockAndOptionPrice;
	}

}
