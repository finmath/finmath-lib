package net.finmath.finitedifference.solvers;

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import net.finmath.finitedifference.models.FDMBlackScholesModel;
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
 */
public class FDMThetaMethod {
	private FiniteDifference1DModel model;
	private FiniteDifference1DBoundary boundaryCondition;
	private double alpha;
	private double beta;
	private double gamma;
	private double theta;
	private double center;
	private double timeHorizon;

	public FDMThetaMethod(FDMBlackScholesModel model, FiniteDifference1DBoundary boundaryCondition, double timeHorizon, double center, double theta) {
		this.model = model;
		this.boundaryCondition = boundaryCondition;
		this.timeHorizon = timeHorizon;
		this.center = center;
		this.theta = theta;

		this.gamma = (2 * model.getRiskFreeRate()) / Math.pow(model.getVolatility(), 2);
		this.alpha = -0.5 * (gamma - 1);
		this.beta = -0.25 * Math.pow((gamma + 1), 2);
	}

	public double[][] getValue(double evaluationTime, double time, DoubleUnaryOperator valueAtMaturity) {
		if(evaluationTime != 0) {
			throw new IllegalArgumentException("Evaluation time != 0 not supported.");
		}
		if(time != timeHorizon) {
			throw new IllegalArgumentException("Given time != timeHorizonn not supported.");
		}

		// Grid Generation
		double maximumStockPriceOnGrid = model.getForwardValue(timeHorizon)
				+ model.getNumStandardDeviations() * Math.sqrt(model.varianceOfStockPrice(timeHorizon));
		double minimumStockPriceOnGrid = Math.max(model.getForwardValue(timeHorizon)
				- model.getNumStandardDeviations() * Math.sqrt(model.varianceOfStockPrice(timeHorizon)), 0);
		double maximumX = f_x(maximumStockPriceOnGrid);
		double minimumX = f_x(Math.max(minimumStockPriceOnGrid, center/50.0));	// Previously there was a floor at 1 here. The floor at 1 is problematic. It does not scale with the spot! @TODO: There should be a more intelligent method to set the floor (do we need this?)
		double dx = (maximumX - minimumX) / (model.getNumSpacesteps() - 2);
		int N_pos = (int) Math.ceil((maximumX / dx) + 1);
		int N_neg = (int) Math.floor((minimumX / dx) - 1);

		// Create interior spatial vector for heat equation
		int len = N_pos - N_neg - 1;
		double[] x = new double[len];
		for (int i = 0; i < len; i++) {
			x[i] = (N_neg + 1) * dx + dx * i;
		}

		// Create time vector for heat equation
		double dtau = Math.pow(model.getVolatility(), 2) * timeHorizon / (2 * model.getNumSpacesteps());
		double[] tau = new double[model.getNumSpacesteps() + 1];
		for (int i = 0; i < model.getNumSpacesteps() + 1; i++) {
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
			double state = x[i];
			U[i] = f(valueAtMaturity.applyAsDouble(f_s(state)), state, 0);
		}
		RealMatrix UVector = MatrixUtils.createColumnRealMatrix(U);

		// Solve system
		for (int m = 0; m < model.getNumSpacesteps(); m++) {
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
			optionPrice[i] = U[i] * center *
					Math.exp(alpha * x[i] + beta * tau[model.getNumSpacesteps()]);
			stockPrice[i] = f_s(x[i]);
		}

		double[][] stockAndOptionPrice = new double[2][len];
		stockAndOptionPrice[0] = stockPrice;
		stockAndOptionPrice[1] = optionPrice;
		return stockAndOptionPrice;
	}

	// State Space Transformations
	private double f_x(double value) {return Math.log(value / center); }
	private double f_s(double x) { return center * Math.exp(x); }
	private double f_t(double tau) { return timeHorizon - (2 * tau) / Math.pow(model.getVolatility(), 2); }
	private double f(double value, double x, double tau) { return (value / center) * Math.exp(-alpha * x - beta * tau); }

	// Heat Equation Boundary Conditions
	private double u_neg_inf(double x, double tau) {
		return f(boundaryCondition.getValueAtLowerBoundary(model, f_t(tau), f_s(x)), x, tau);
	}
	private double u_pos_inf(double x, double tau) {
		return f(boundaryCondition.getValueAtUpperBoundary(model, f_t(tau), f_s(x)), x, tau);
	}
}
