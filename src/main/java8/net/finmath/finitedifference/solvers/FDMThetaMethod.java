package net.finmath.finitedifference.solvers;

import java.util.function.DoubleUnaryOperator;

import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import net.finmath.finitedifference.models.FiniteDifference1DBoundary;
import net.finmath.finitedifference.models.FiniteDifference1DModel;

/**
 * One dimensional finite difference solver.
 * Theta method for local volatility PDE.
 * This is where the real stuff happens.
 *
 * @author Ralph Rudd
 * @author Christian Fries
 * @author Jörg Kienitz
 */


public class FDMThetaMethod {
	private final FiniteDifference1DModel model;
	private final FiniteDifference1DBoundary boundaryCondition;
	private final double theta;
	private final double center;
	private final double timeHorizon;

	public FDMThetaMethod(FiniteDifference1DModel model, FiniteDifference1DBoundary boundaryCondition, double timeHorizon, double center, double theta) {
		this.model = model;
		this.boundaryCondition = boundaryCondition;
		this.timeHorizon = timeHorizon;
		this.center = center;
		this.theta = theta;
	}

	public double[][] getValue(double evaluationTime, double time, DoubleUnaryOperator valueAtMaturity) {
		if(evaluationTime != 0) {
			throw new IllegalArgumentException("Evaluation time != 0 not supported.");
		}
		if(time != timeHorizon) {
			throw new IllegalArgumentException("Given time != timeHorizon not supported.");
		}

		// Grid Generation
		final double maximumStockPriceOnGrid = model.getForwardValue(timeHorizon)
				+ model.getNumStandardDeviations() * Math.sqrt(model.varianceOfStockPrice(timeHorizon));
		final double minimumStockPriceOnGrid = Math.max(model.getForwardValue(timeHorizon)
				- model.getNumStandardDeviations() * Math.sqrt(model.varianceOfStockPrice(timeHorizon)), 0);
		final double deltaStock = (maximumStockPriceOnGrid - minimumStockPriceOnGrid) / model.getNumSpacesteps();
		final double deltaTau = timeHorizon / model.getNumTimesteps();

		// Create interior spatial array of stock prices
		final int spaceLength = model.getNumSpacesteps() - 1;
		final double[] stock = new double[spaceLength];
		for (int i= 0; i < spaceLength; i++) {
			stock[i] = minimumStockPriceOnGrid + (i + 1) * deltaStock;
		}

		// Create time-reversed tau array
		final int timeLength = model.getNumTimesteps() + 1;
		final double[] tau = new double[timeLength];
		for (int i = 0; i < timeLength; i++) {
			tau[i] = i * deltaTau;
		}

		// Create constant matrices
		final RealMatrix eye = MatrixUtils.createRealIdentityMatrix(spaceLength);
		final RealMatrix D1 = MatrixUtils.createRealMatrix(spaceLength, spaceLength);
		final RealMatrix D2 = MatrixUtils.createRealMatrix(spaceLength, spaceLength);
		final RealMatrix T1 = MatrixUtils.createRealMatrix(spaceLength, spaceLength);
		final RealMatrix T2 = MatrixUtils.createRealMatrix(spaceLength, spaceLength);
		for (int i = 0; i < spaceLength; i++) {
			for (int j = 0; j < spaceLength; j++) {
				if (i == j) {
					D1.setEntry(i, j, minimumStockPriceOnGrid / deltaStock + (i + 1));
					D2.setEntry(i, j, Math.pow(minimumStockPriceOnGrid / deltaStock + (i + 1), 2));
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
		final RealMatrix F1 = eye.scalarMultiply(1 - model.getRiskFreeRate() * deltaTau);
		final RealMatrix F2 = D1.scalarMultiply(0.5 * model.getRiskFreeRate() * deltaTau).multiply(T1);
		final RealMatrix F3 = D2.scalarMultiply(0.5 * deltaTau).multiply(T2);
		final RealMatrix G1 = eye.scalarMultiply(1 + model.getRiskFreeRate() * deltaTau);
		final RealMatrix G2 = F2.scalarMultiply(-1);
		final RealMatrix G3 = F3.scalarMultiply(-1);

		// Initialize boundary and solution vectors
		final RealMatrix b = MatrixUtils.createRealMatrix(spaceLength, 1);
		final RealMatrix b2 = MatrixUtils.createRealMatrix(spaceLength, 1);
		RealMatrix U = MatrixUtils.createRealMatrix(spaceLength, 1);
		for (int i = 0; i < spaceLength; i++) {
			b.setEntry(i, 0, 0);
			b2.setEntry(i, 0, 0);
			U.setEntry(i, 0, valueAtMaturity.applyAsDouble(stock[i]));
		}

		// Theta finite difference method
		for (int m = 0; m < model.getNumTimesteps(); m++) {
			final double[] sigma = new double[spaceLength];
			final double[] sigma2 = new double[spaceLength];
			for (int i = 0; i < spaceLength; i++) {
				sigma[i] = Math.pow(model.getLocalVolatility(minimumStockPriceOnGrid + (i + 1) * deltaStock,
						timeHorizon - m * deltaTau), 2);
				sigma2[i] = Math.pow(model.getLocalVolatility(minimumStockPriceOnGrid + (i + 1) * deltaStock,
						timeHorizon - (m + 1) * deltaTau), 2);
			}
			final RealMatrix Sigma = MatrixUtils.createRealDiagonalMatrix(sigma);
			final RealMatrix Sigma2 = MatrixUtils.createRealDiagonalMatrix(sigma2);
			final RealMatrix F = F1.add(F2).add(Sigma.multiply(F3));
			final RealMatrix G = G1.add(G2).add(Sigma2.multiply(G3));
			final RealMatrix H = G.scalarMultiply(theta).add(eye.scalarMultiply(1 - theta));
			final DecompositionSolver solver = new LUDecomposition(H).getSolver();

			final double Sl = (minimumStockPriceOnGrid / deltaStock + 1);
			final double Su = (maximumStockPriceOnGrid / deltaStock - 1);
			final double vl = Math.pow(model.getLocalVolatility(minimumStockPriceOnGrid + deltaStock,
					timeHorizon - m * deltaTau), 2);
			final double vu = Math.pow(model.getLocalVolatility(maximumStockPriceOnGrid - deltaStock,
					timeHorizon - m * deltaTau), 2);
			final double vl2 = Math.pow(model.getLocalVolatility(minimumStockPriceOnGrid + deltaStock,
					timeHorizon - (m + 1) * deltaTau), 2);
			final double vu2 = Math.pow(model.getLocalVolatility(maximumStockPriceOnGrid - deltaStock,
					timeHorizon - (m + 1) * deltaTau), 2);

			final double test = timeReversedUpperBoundary(maximumStockPriceOnGrid, tau[m]);
			b.setEntry(0, 0,
					0.5 * deltaTau * Sl * (vl * Sl - model.getRiskFreeRate()) * timeReversedLowerBoundary(minimumStockPriceOnGrid, tau[m]));
			b.setEntry(spaceLength - 1, 0,
					0.5 * deltaTau * Su * (vu * Su + model.getRiskFreeRate()) * timeReversedUpperBoundary(maximumStockPriceOnGrid, tau[m]));
			b2.setEntry(0, 0,
					0.5 * deltaTau * Sl * (vl2 * Sl - model.getRiskFreeRate()) * timeReversedLowerBoundary(minimumStockPriceOnGrid, tau[m + 1]));
			b2.setEntry(spaceLength - 1, 0,
					0.5 * deltaTau * Su * (vu2 * Su + model.getRiskFreeRate()) * timeReversedUpperBoundary(maximumStockPriceOnGrid, tau[m + 1]));
			final RealMatrix U1 = (F.scalarMultiply(1 - theta).add(eye.scalarMultiply(theta))).multiply(U);
			final RealMatrix U2 = b.scalarMultiply(1 - theta).add(b2.scalarMultiply(theta));
			U = solver.solve(U1.add(U2));
		}
		final double[] optionPrice = U.getColumn(0);
		final double[][] stockAndOptionPrice = new double[2][spaceLength];
		stockAndOptionPrice[0] = stock;
		stockAndOptionPrice[1] = optionPrice;
		return stockAndOptionPrice;
	}

	// Time-reversed Boundary Conditions
	//    private double U_initial(double stockPrice, double tau) {
	//        return valueAtMaturity
	//    }
	private double timeReversedLowerBoundary(double stockPrice, double tau) {
		return boundaryCondition.getValueAtLowerBoundary(model, timeHorizon - tau, stockPrice);
	}

	private double timeReversedUpperBoundary(double stockPrice, double tau) {
		return boundaryCondition.getValueAtUpperBoundary(model, timeHorizon - tau, stockPrice);
	}

}


