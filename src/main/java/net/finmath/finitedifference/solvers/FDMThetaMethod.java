package net.finmath.finitedifference.solvers;

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import net.finmath.finitedifference.models.FiniteDifference1DBoundary;
import net.finmath.finitedifference.models.FiniteDifference1DModel;

/**
 * One dimensional finite difference solver.
 *
 * This is where the real stuff happens.
 *
 * @author Ralph Rudd
 * @author Christian Fries
 * @author Jörg Kienitz
 * @version 1.0
 */
public class FDMThetaMethod {
	private final FiniteDifference1DModel model;
	private final FiniteDifference1DBoundary boundaryCondition;
	private final double alpha;
	private final double beta;
	private final double gamma;
	private final double theta;
	private final double center;
	private final double timeHorizon;

	public FDMThetaMethod(final FiniteDifference1DModel model, final FiniteDifference1DBoundary boundaryCondition, final double timeHorizon, final double center, final double theta) {
		this.model = model;
		this.boundaryCondition = boundaryCondition;
		this.timeHorizon = timeHorizon;
		this.center = center;
		this.theta = theta;

		gamma = (2 * model.getRiskFreeRate()) / Math.pow(model.getVolatility(), 2);
		alpha = -0.5 * (gamma - 1);
		beta = -0.25 * Math.pow((gamma + 1), 2);
	}

	public double[][] getValue(final double evaluationTime, final double time, final DoubleUnaryOperator valueAtMaturity) {
		if(evaluationTime != 0) {
			throw new IllegalArgumentException("Evaluation time != 0 not supported.");
		}
		if(time != timeHorizon) {
			throw new IllegalArgumentException("Given time != timeHorizonn not supported.");
		}

		// Grid Generation
		final double maximumStockPriceOnGrid = model.getForwardValue(timeHorizon)
				+ model.getNumStandardDeviations() * Math.sqrt(model.varianceOfStockPrice(timeHorizon));
		final double minimumStockPriceOnGrid = Math.max(model.getForwardValue(timeHorizon)
				- model.getNumStandardDeviations() * Math.sqrt(model.varianceOfStockPrice(timeHorizon)), 0);
		final double maximumX = f_x(maximumStockPriceOnGrid);
		final double minimumX = f_x(Math.max(minimumStockPriceOnGrid, center/50.0));	// Previously there was a floor at 1 here. The floor at 1 is problematic. It does not scale with the spot! @TODO There should be a more intelligent method to set the floor (do we need this?)
		final double dx = (maximumX - minimumX) / (model.getNumSpacesteps() - 2);
		final int N_pos = (int) Math.ceil((maximumX / dx) + 1);
		final int N_neg = (int) Math.floor((minimumX / dx) - 1);

		// Create interior spatial vector for heat equation
		final int len = N_pos - N_neg - 1;
		final double[] x = new double[len];
		for (int i = 0; i < len; i++) {
			x[i] = (N_neg + 1) * dx + dx * i;
		}

		// Create time vector for heat equation
		final double dtau = Math.pow(model.getVolatility(), 2) * timeHorizon / (2 * model.getNumSpacesteps());
		final double[] tau = new double[model.getNumSpacesteps() + 1];
		for (int i = 0; i < model.getNumSpacesteps() + 1; i++) {
			tau[i] = i * dtau;
		}

		// Create necessary matrices
		final double kappa = dtau / Math.pow(dx, 2);
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
			final double state = x[i];
			U[i] = f(valueAtMaturity.applyAsDouble(f_s(state)), state, 0);
		}
		RealMatrix UVector = MatrixUtils.createColumnRealMatrix(U);

		// Solve system
		for (int m = 0; m < model.getNumSpacesteps(); m++) {
			b[0] = (u_neg_inf(N_neg * dx, tau[m]) * (1 - theta) * kappa)
					+ (u_neg_inf(N_neg * dx, tau[m + 1]) * theta * kappa);
			b[len-1] = (u_pos_inf(N_pos * dx, tau[m]) * (1 - theta) * kappa)
					+ (u_pos_inf(N_pos * dx, tau[m + 1]) * theta * kappa);

			final RealMatrix bVector = MatrixUtils.createColumnRealMatrix(b);
			final RealMatrix constantsMatrix = (DMatrix.multiply(UVector)).add(bVector);
			UVector = solver.solve(constantsMatrix);
		}
		U = UVector.getColumn(0);

		// Transform x to stockPrice and U to optionPrice
		final double[] optionPrice = new double[len];
		final double[] stockPrice = new double[len];
		for (int i = 0; i < len; i++ ){
			optionPrice[i] = U[i] * center *
					Math.exp(alpha * x[i] + beta * tau[model.getNumSpacesteps()]);
			stockPrice[i] = f_s(x[i]);
		}

		final double[][] stockAndOptionPrice = new double[2][len];
		stockAndOptionPrice[0] = stockPrice;
		stockAndOptionPrice[1] = optionPrice;
		return stockAndOptionPrice;
	}

	// State Space Transformations
	private double f_x(final double value) {return Math.log(value / center); }
	private double f_s(final double x) { return center * Math.exp(x); }
	private double f_t(final double tau) { return timeHorizon - (2 * tau) / Math.pow(model.getVolatility(), 2); }
	private double f(final double value, final double x, final double tau) { return (value / center) * Math.exp(-alpha * x - beta * tau); }

	// Heat Equation Boundary Conditions
	private double u_neg_inf(final double x, final double tau) {
		return f(boundaryCondition.getValueAtLowerBoundary(model, f_t(tau), f_s(x)), x, tau);
	}
	private double u_pos_inf(final double x, final double tau) {
		return f(boundaryCondition.getValueAtUpperBoundary(model, f_t(tau), f_s(x)), x, tau);
	}
}
