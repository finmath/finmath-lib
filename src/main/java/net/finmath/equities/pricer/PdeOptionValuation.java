package net.finmath.equities.pricer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import net.finmath.equities.marketdata.FlatYieldCurve;
import net.finmath.equities.models.EquityForwardStructure;
import net.finmath.equities.models.FlatVolatilitySurface;
import net.finmath.equities.models.VolatilitySurface;
import net.finmath.equities.pricer.EquityValuationRequest.CalculationRequestType;
import net.finmath.equities.products.EuropeanOption;
import net.finmath.equities.products.Option;
import net.finmath.rootfinder.BisectionSearch;
import net.finmath.rootfinder.SecantMethod;
import net.finmath.time.daycount.DayCountConvention;

/**
 * This class implements a finite difference pricer under a Black-Scholes process or a
 * local volatility process in the presence of Buehler dividends.
 * It supports European and American options. Greeks are calculated inside the grid to the extent possible.
 * The implementation uses a Crank-Nicolson scheme.
 * Payoffs are smoothed using the modified timestepping from Rannacher's 1984 paper.
 * The American exercise feature is priced using the penalty approach from Forsyth's 2001 paper.
 *
 * TODO The linear algebra framework used (apache.commons.math3) is not optimized for the discretized PDE.
 * More performant linear algebra algorithms should be used that take account of
 * the tridiagonal matrix structure of the problem, e.g. direct diagonal operations instead of full-blown
 * matrix multiplication, and the Thomas algorithm instead of LU decomposition for solving equations.
 *
 * @author Andreas Grotz
 */
public class PdeOptionValuation implements OptionValuation
{

	private final int timeStepsPerYear;
	private final double spaceMinForwardMultiple;
	private final double spaceMaxForwardMultiple;
	private final int spaceNbOfSteps;
	private final double spaceStepSize;
	private final ArrayList<Double> spots;
	private final int spotIndex;
	private final DayCountConvention dayCounter;
	private final boolean isLvPricer;
	private final boolean includeDividendDatesInGrid;


	public PdeOptionValuation(
			double spaceMinForwardMultiple,
			double spaceMaxForwardMultiple,
			int spaceNbPoints,
			final int timeStepsPerYear,
			DayCountConvention dcc,
			final boolean isLvPricer,
			final boolean includeDividendDatesInGrid)
	{
		assert spaceMinForwardMultiple < 1.0 : "min multiple of forward must be below 1.0";
		assert spaceMaxForwardMultiple > 1.0 : "max multiple of forward must be below 1.0";

		this.timeStepsPerYear = timeStepsPerYear;
		this.dayCounter = dcc;
		this.isLvPricer = isLvPricer;
		this.includeDividendDatesInGrid = includeDividendDatesInGrid;

		// Set up the space grid for the pure volatility process
		var tmpSpaceStepSize = (spaceMaxForwardMultiple - spaceMinForwardMultiple) / spaceNbPoints;
		var tmpSpaceNbPoints = spaceNbPoints;
		var tmpSpots  = new ArrayList<Double>();
		for (int i = 0; i < tmpSpaceNbPoints; i++) {
			tmpSpots.add(spaceMinForwardMultiple + tmpSpaceStepSize * i);
		}
		// The space grid needs to include the forward level 1.0 for the pure volatility process
		// Hence if necessary, we increase the step size slightly to include it
		final var lowerBound = Math.abs(Collections.binarySearch(tmpSpots, 1.0)) - 2;
		if (!(tmpSpots.get(lowerBound) == 1.0))
		{
			tmpSpaceStepSize += (1.0 - tmpSpots.get(lowerBound)) / lowerBound;
			tmpSpots  = new ArrayList<Double>();
			tmpSpaceNbPoints = 0;
			var tmpSpot = 0.0;
			while (tmpSpot < spaceMaxForwardMultiple)
			{
				tmpSpot = spaceMinForwardMultiple + tmpSpaceStepSize * tmpSpaceNbPoints;
				tmpSpots.add(tmpSpot);
				tmpSpaceNbPoints++;
			}
		}

		this.spaceMinForwardMultiple = spaceMinForwardMultiple;
		this.spaceMaxForwardMultiple = tmpSpots.get(tmpSpots.size() - 1);
		this.spaceNbOfSteps = tmpSpaceNbPoints;
		spots = tmpSpots;
		spaceStepSize = tmpSpaceStepSize;
		spotIndex = lowerBound;
	}

	@Override
	public EquityValuationResult calculate(
			EquityValuationRequest request,
			EquityForwardStructure forwardStructure,
			FlatYieldCurve discountCurve,
			VolatilitySurface volaSurface)
	{
		final var results = new HashMap<CalculationRequestType, Double>();
		if(request.getCalcsRequested().isEmpty()) {
			return new EquityValuationResult(request, results);
		}

		double price = 0.0;
		if(request.getCalcsRequested().contains(CalculationRequestType.EqDelta)
				|| request.getCalcsRequested().contains(CalculationRequestType.EqGamma ))
		{
			final var spotSensis = getPdeSensis(
					request.getOption(),
					forwardStructure,
					discountCurve,
					volaSurface);
			price = spotSensis[0];
			if(request.getCalcsRequested().contains(CalculationRequestType.EqDelta)) {
				results.put(CalculationRequestType.EqDelta, spotSensis[1]);
			}
			if(request.getCalcsRequested().contains(CalculationRequestType.EqGamma)) {
				results.put(CalculationRequestType.EqGamma, spotSensis[2]);
			}
		}
		else
		{
			price = getPrice(
					request.getOption(),
					forwardStructure,
					discountCurve,
					volaSurface);
		}

		if(request.getCalcsRequested().contains(CalculationRequestType.Price)) {
			results.put(CalculationRequestType.Price, price);
		}

		if(request.getCalcsRequested().contains(CalculationRequestType.EqVega))
		{
			final var volShift = 0.0001; // TODO Make part of class members
			final var priceShifted = getPrice(
					request.getOption(),
					forwardStructure,
					discountCurve,
					volaSurface.getShiftedSurface(volShift));
			results.put(CalculationRequestType.EqVega, (priceShifted - price) / volShift);
		}

		return new EquityValuationResult(request, results);
	}

	public double getPrice(
			Option option,
			EquityForwardStructure forwardStructure,
			FlatYieldCurve discountCurve,
			VolatilitySurface volSurface)
	{
		return evolvePde(option, forwardStructure, discountCurve, volSurface, false)[0];
	}

	public double[] getPdeSensis(
			Option option,
			EquityForwardStructure forwardStructure,
			FlatYieldCurve discountCurve,
			VolatilitySurface volSurface)
	{
		return evolvePde(option, forwardStructure, discountCurve, volSurface, true);
	}

	public double getVega(
			Option option,
			EquityForwardStructure forwardStructure,
			FlatYieldCurve discountCurve,
			VolatilitySurface volSurface,
			double basePrice,
			double volShift)
	{
		final var shiftedPrice = getPrice(option, forwardStructure, discountCurve, volSurface.getShiftedSurface(volShift));
		return (shiftedPrice - basePrice) / volShift;
	}

	public double getTheta(
			Option option,
			EquityForwardStructure forwardStructure,
			FlatYieldCurve discountCurve,
			VolatilitySurface volSurface,
			double basePrice)
	{
		final var valDate = forwardStructure.getValuationDate();
		final var thetaDate = valDate.plusDays(1);
		final var thetaSpot = forwardStructure.getForward(thetaDate);
		final var shiftedFwdStructure = forwardStructure.cloneWithNewSpot(thetaSpot).cloneWithNewDate(thetaDate);
		final var shiftedPrice = getPrice(option, shiftedFwdStructure, discountCurve, volSurface);
		return (shiftedPrice - basePrice) / dayCounter.getDaycountFraction(valDate, thetaDate);
	}

	private double[] evolvePde(
			Option option,
			EquityForwardStructure forwardStructure,
			FlatYieldCurve discountCurve,
			VolatilitySurface volSurface,
			boolean calculateSensis)
	{
		// Get data
		final var valDate = forwardStructure.getValuationDate();
		final var expiryDate = option.getExpiryDate();
		final var expiryTime = dayCounter.getDaycountFraction(valDate, expiryDate);
		assert !forwardStructure.getValuationDate().isAfter(expiryDate)
		: "Valuation date must not be after option expiry";
		final var impliedVol = volSurface.getVolatility(option.getStrike(), expiryDate, forwardStructure);
		var forward = forwardStructure.getForward(expiryDate);
		var fdf = forwardStructure.getFutureDividendFactor(expiryDate);

		// Build matrices
		final RealMatrix idMatrix = MatrixUtils.createRealIdentityMatrix(spaceNbOfSteps);
		final RealMatrix tridiagMatrix = MatrixUtils.createRealMatrix(spaceNbOfSteps, spaceNbOfSteps);
		final double spaceStepSq = spaceStepSize * spaceStepSize;
		for (int i = 0; i < spaceNbOfSteps; i++) {
			for (int j = 0; j < spaceNbOfSteps; j++) {
				if (i == j)
				{
					tridiagMatrix.setEntry(i, j, Math.pow(spots.get(i), 2) / spaceStepSq);
				}
				else if (i == j - 1 || i == j + 1)
				{
					tridiagMatrix.setEntry(i, j, -0.5 * Math.pow(spots.get(i), 2) / spaceStepSq);
				}
				else
				{
					tridiagMatrix.setEntry(i, j, 0);
				}
			}
		}

		// Set initial values
		var prices = MatrixUtils.createRealVector(new double[spaceNbOfSteps]);
		for (int i = 0; i < spaceNbOfSteps; i++)
		{
			prices.setEntry(i, option.getPayoff((forward - fdf) * spots.get(i) + fdf));
		}

		// Set time intervals to evolve the PDE (i.e. from dividend to dividend)
		final var diviDates = forwardStructure.getDividendStream().getDividendDates();
		final var anchorTimes = new ArrayList<Double> ();
		anchorTimes.add(0.0);
		if(includeDividendDatesInGrid)
		{
			for (final var date : diviDates)
			{
				if (date.isAfter(valDate) && date.isBefore(expiryDate))
				{
					anchorTimes.add(dayCounter.getDaycountFraction(valDate, date));
				}
			}
		}
		anchorTimes.add(expiryTime);
		anchorTimes.sort(Comparator.comparing(pt -> pt));
		var lastAtmPrice = 0.0;
		var dt = 0.0;

		// Evolve PDE
		for (int a = anchorTimes.size() - 1; a > 0; a--)
		{
			// Set time steps
			final var timeInterval = anchorTimes.get(a) - anchorTimes.get(a - 1);
			int timeNbOfSteps;
			double timeStepSize;
			if (timeStepsPerYear == 0) // Use optimal ratio of time and space step size
			{
				timeNbOfSteps = (int)Math.ceil(2 * impliedVol * Math.pow(timeInterval, 1.5) / spaceStepSize);
				timeStepSize = timeInterval / timeNbOfSteps;
			}
			else // Use time step size provided externally
			{
				timeNbOfSteps = (int)Math.floor(timeInterval * timeStepsPerYear);
				timeStepSize = timeInterval / timeNbOfSteps;
			}

			final var times = new ArrayList<Double>();
			for (int i = 0; i <= 4; i++) {
				times.add(anchorTimes.get(a) - i * 0.25 * timeStepSize);
			}
			for (int i = timeNbOfSteps - 2; i >= 0; i--) {
				times.add(anchorTimes.get(a - 1) + i * timeStepSize);
			}

			// Evolve PDE in current time interval
			for (int i = 1; i < times.size(); i++)
			{
				lastAtmPrice = prices.getEntry(spotIndex);
				dt = times.get(i-1) - times.get(i);
				double theta = 0.5;
				if (i <= 4) {
					theta = 1.0;
				}
				final var theta1 = 1.0 - theta;
				final var volSq = impliedVol * impliedVol;

				RealMatrix implicitMatrix, explicitMatrix;
				if (isLvPricer)
				{
					implicitMatrix = tridiagMatrix.scalarMultiply(theta * dt);
					explicitMatrix = tridiagMatrix.scalarMultiply(-theta1 * dt);
					final var localVol = new double[spaceNbOfSteps];
					for (int s = 0; s < spaceNbOfSteps; s++)
					{
						final var lv = volSurface.getLocalVolatility(
								Math.log(spots.get(s)), times.get(i-1), forwardStructure, spaceStepSize, dt);
						localVol[s] = lv * lv;
					}

					final var volaMatrix = MatrixUtils.createRealDiagonalMatrix(localVol);
					implicitMatrix = volaMatrix.multiply(implicitMatrix);
					explicitMatrix = volaMatrix.multiply(explicitMatrix);
				}
				else
				{
					implicitMatrix = tridiagMatrix.scalarMultiply(theta * dt * volSq);
					explicitMatrix = tridiagMatrix.scalarMultiply(-theta1 * dt * volSq);
				}

				implicitMatrix = idMatrix.add(implicitMatrix);
				explicitMatrix = idMatrix.add(explicitMatrix);

				if (option.isAmericanOption())
				{
					// Use the penalty algorithm from Forsyth's 2001 paper to solve the
					// linear complementary problem for the American exercise feature.
					final var penaltyFactor = 1 / Math.min(timeStepSize * timeStepSize, spaceStepSize * spaceStepSize);
					forward = forwardStructure.getForward(times.get(i));
					fdf = forwardStructure.getFutureDividendFactor(times.get(i));
					final var discountFactor = discountCurve.getForwardDiscountFactor(times.get(i), expiryTime);
					final var payoffs = MatrixUtils.createRealVector(new double[spaceNbOfSteps]);
					final var penaltyMatrix = MatrixUtils.createRealMatrix(spaceNbOfSteps, spaceNbOfSteps);
					for (int j = 1; j < spaceNbOfSteps - 1; j++)
					{
						final var payoff = option.getPayoff((forward - fdf) * spots.get(j) + fdf)
								/ discountFactor;
						payoffs.setEntry(j, payoff);
						penaltyMatrix.setEntry(j, j, prices.getEntry(j) < payoff ? penaltyFactor : 0);
					}

					final var b = explicitMatrix.operate(prices);
					var oldPrices = prices.copy();
					final var oldPenaltyMatrix = penaltyMatrix.copy();
					final var tol = 1 / penaltyFactor;
					int iterations = 0;
					while (true)
					{
						assert iterations++ < 100 : "Penalty algorithm for american exercise did not converge in 100 steps";
						final var c = b.add(penaltyMatrix.operate(payoffs));
						final var A = implicitMatrix.add(penaltyMatrix);
						final DecompositionSolver solver = new LUDecomposition(A).getSolver();
						prices = solver.solve(c);
						for (int j = 1; j < spaceNbOfSteps - 1; j++)
						{
							penaltyMatrix.setEntry(j, j, prices.getEntry(j) < payoffs.getEntry(j) ? penaltyFactor : 0);
						}

						if (penaltyMatrix.equals(oldPenaltyMatrix)
								|| (prices.subtract(oldPrices).getLInfNorm())
								/ Math.max(oldPrices.getLInfNorm(), 1.0) < tol)
						{
							break;
						}
						oldPrices = prices.copy();
					}

				}
				else
				{
					// Solve the PDE step directly
					prices = explicitMatrix.operate(prices);
					final DecompositionSolver solver = new LUDecomposition(implicitMatrix).getSolver();
					prices = solver.solve(prices);
				}

				// Set boundary conditions
				prices.setEntry(0, option.getPayoff((forward - fdf) * spaceMinForwardMultiple + fdf));
				prices.setEntry(spaceNbOfSteps - 1,  option.getPayoff((forward - fdf) * spaceMaxForwardMultiple + fdf));
			}
		}

		final var discountFactor = discountCurve.getDiscountFactor(expiryDate);
		final var price = discountFactor * prices.getEntry(spotIndex);

		if (calculateSensis)
		{
			final var dFdX = forwardStructure.getDividendAdjustedStrike(
					forwardStructure.getForward(expiryDate), expiryDate);
			final var dFdS = forwardStructure.getGrowthDiscountFactor(valDate, expiryDate);
			final var delta = discountFactor * 0.5
					* (prices.getEntry(spotIndex + 1) - prices.getEntry(spotIndex - 1)) / spaceStepSize
					* dFdS / dFdX;
			final var gamma = discountFactor * (prices.getEntry(spotIndex + 1) + prices.getEntry(spotIndex - 1)
			- 2 * prices.getEntry(spotIndex)) / spaceStepSq * dFdS * dFdS / dFdX / dFdX;
			final var theta = (discountFactor * lastAtmPrice - price) / dt;
			return new double[] {price, delta, gamma, theta};
		}
		else
		{
			return new double[] {price, Double.NaN, Double.NaN, Double.NaN};
		}
	}


	public double getImpliedVolatility(
			Option option,
			EquityForwardStructure forwardStructure,
			FlatYieldCurve discountCurve,
			double price)
	{
		double initialGuess = 0.25;
		final var forward = forwardStructure.getForward(option.getExpiryDate());
		// Use analytic pricer as initial guess for Europeans and OTM Americans
		// Use two bisection steps for ITM Americans
		if(option.isAmericanOption() && option.getPayoff(forward) > 0.0)
		{
			final var bisectionSolver = new BisectionSearch(0.00001,1.0);
			for (int i = 0; i < 3; i++)
			{
				final double currentVol = bisectionSolver.getNextPoint();
				final double currentPrice = getPrice(
						option,
						forwardStructure,
						discountCurve,
						new FlatVolatilitySurface(currentVol));

				bisectionSolver.setValue(currentPrice - price);
			}
			initialGuess = bisectionSolver.getBestPoint();
		}
		else
		{
			final var anaPricer = new AnalyticOptionValuation(dayCounter);
			Option testOption;
			if(option.isAmericanOption()) {
				testOption = new EuropeanOption(option.getExpiryDate(), option.getStrike(), option.isCallOption());
			} else {
				testOption = option;
			}
			initialGuess = anaPricer.getImpliedVolatility(testOption, forwardStructure, discountCurve, price);

		}

		// Solve for implied vol
		final var solver = new SecantMethod(initialGuess, initialGuess * 1.01);
		while(solver.getAccuracy() / price > 1e-3 && !solver.isDone()) {
			final double currentVol = solver.getNextPoint();
			final double currentPrice = getPrice(
					option,
					forwardStructure,
					discountCurve,
					new FlatVolatilitySurface(currentVol));

			solver.setValue(currentPrice - price);
		}

		return Math.abs(solver.getBestPoint()); // Note that the PDE only uses sigma^2
	}
}
