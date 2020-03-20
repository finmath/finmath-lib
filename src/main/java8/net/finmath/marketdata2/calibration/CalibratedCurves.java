/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 30.11.2012
 */
package net.finmath.marketdata2.calibration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.finmath.marketdata2.model.AnalyticModel;
import net.finmath.marketdata2.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata2.model.curves.Curve;
import net.finmath.marketdata2.model.curves.DiscountCurveInterface;
import net.finmath.marketdata2.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata2.model.curves.ForwardCurveFromDiscountCurve;
import net.finmath.marketdata2.model.curves.ForwardCurveInterface;
import net.finmath.marketdata2.model.curves.ForwardCurveInterpolation;
import net.finmath.marketdata2.products.AnalyticProduct;
import net.finmath.marketdata2.products.Deposit;
import net.finmath.marketdata2.products.ForwardRateAgreement;
import net.finmath.marketdata2.products.Swap;
import net.finmath.marketdata2.products.SwapLeg;
import net.finmath.optimizer.SolverException;
import net.finmath.time.RegularSchedule;
import net.finmath.time.Schedule;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Generate a collection of calibrated curves (discount curves, forward curves)
 * from a vector of calibration products.
 *
 * An object of this class provides a calibration of curves (using multi-curves, forward curve, discount curve).
 * Sometimes this is referred as curve bootstrapping, however the algorithm used here is not a bootstrap.
 *
 * The calibration products have to be provided via a vector of <code>CalibrationSpec</code>s.
 *
 * The products provides are
 * <table>
 * <caption>List of calibration products types</caption>
 * 	<tr>
 * 		<td>Value of Type String</td>
 * 		<td>Classes</td>
 * 		<td>Note</td>
 * 	</tr>
 * 	<tr>
 * 		<td>swap</td>
 * 		<td>{@link net.finmath.marketdata2.products.Swap}</td>
 * 		<td></td>
 * 	</tr>
 * 	<tr>
 * 		<td>swapleg</td>
 * 		<td>{@link net.finmath.marketdata2.products.SwapLeg}</td>
 * 		<td>Only the receiver part of <code>CalibrationSpec</code> is used.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>swapwithresetonreceiver</td>
 * 		<td>{@link net.finmath.marketdata2.products.SwapLeg}</td>
 * 		<td></td>
 * 	</tr>
 * 	<tr>
 * 		<td>swapwithresetonpayer</td>
 * 		<td>{@link net.finmath.marketdata2.products.SwapLeg}</td>
 * 		<td></td>
 * 	</tr>
 * 	<tr>
 * 		<td>deposit</td>
 * 		<td>{@link net.finmath.marketdata2.products.Deposit}</td>
 * 		<td>Only the receiver part of <code>CalibrationSpec</code> is used.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>fra</td>
 * 		<td>{@link net.finmath.marketdata2.products.ForwardRateAgreement}</td>
 * 		<td>Only the receiver part of <code>CalibrationSpec</code> is used.</td>
 * 	</tr>
 * </table>
 *
 * For a demo spreadsheet using this class see <a href="http://finmath.net/topics/curvecalibration/">finmath.net/topics/curvecalibration/</a>.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class CalibratedCurves {

	private static final boolean isUseForwardCurve;
	private static final boolean isCreateDefaultCurvesForMissingCurves;
	static {
		// Default value is true
		isUseForwardCurve = Boolean.parseBoolean(System.getProperty("net.finmath.analytic.calibration.CalibratedCurves.isUseForwardCurve","true"));
		// Default value is false
		isCreateDefaultCurvesForMissingCurves = Boolean.parseBoolean(System.getProperty("net.finmath.analytic.calibration.CalibratedCurves.isCreateDefaultCurvesForMissingCurves","false"));
	}

	/**
	 * Specification of calibration product.
	 *
	 * @author Christian Fries
	 */
	public static class CalibrationSpec {

		private String				symbol;

		private final String				type;

		private final	Schedule	swapTenorDefinitionReceiver;
		private final String				forwardCurveReceiverName;
		private final double				spreadReceiver;
		private final String				discountCurveReceiverName;

		private Schedule	swapTenorDefinitionPayer;
		private String				forwardCurvePayerName;
		private double				spreadPayer;
		private String				discountCurvePayerName;

		private final String				calibrationCurveName;
		private final double				calibrationTime;

		/**
		 * Calibration specification.
		 *
		 * @param symbol A string identifying the calibration product. This string can be used in sensitivity calculation, allowing to bump the spread in a finite difference approximation. See <code>getCloneShifted</code> method.
		 * @param type The type of the calibration product.
		 * @param swapTenorDefinitionReceiver The schedule of periods of the receiver leg.
		 * @param forwardCurveReceiverName The forward curve of the receiver leg (may be null).
		 * @param spreadReceiver The spread or fixed coupon of the receiver leg.
		 * @param discountCurveReceiverName The discount curve of the receiver leg.
		 * @param swapTenorDefinitionPayer The schedule of periods of the payer leg.
		 * @param forwardCurvePayerName The forward curve of the payer leg (may be null).
		 * @param spreadPayer The spread or fixed coupon of the payer leg.
		 * @param discountCurvePayerName The discount curve of the payer leg.
		 * @param calibrationCurveName The curve to calibrate, by this product.
		 * @param calibrationTime The time point in calibrationCurveName used to calibrate, by this product.
		 */
		public CalibrationSpec(
				final String symbol,
				final String type,
				final Schedule swapTenorDefinitionReceiver,
				final String forwardCurveReceiverName, final double spreadReceiver,
				final String discountCurveReceiverName,
				final Schedule swapTenorDefinitionPayer,
				final String forwardCurvePayerName, final double spreadPayer,
				final String discountCurvePayerName,
				final String calibrationCurveName,
				final double calibrationTime) {
			super();
			this.symbol = symbol;
			this.type = type;
			this.swapTenorDefinitionReceiver = swapTenorDefinitionReceiver;
			this.forwardCurveReceiverName = forwardCurveReceiverName;
			this.spreadReceiver = spreadReceiver;
			this.discountCurveReceiverName = discountCurveReceiverName;
			this.swapTenorDefinitionPayer = swapTenorDefinitionPayer;
			this.forwardCurvePayerName = forwardCurvePayerName;
			this.spreadPayer = spreadPayer;
			this.discountCurvePayerName = discountCurvePayerName;
			this.calibrationCurveName = calibrationCurveName;
			this.calibrationTime = calibrationTime;
		}

		/**
		 * Calibration specification.
		 *
		 * @param type The type of the calibration product.
		 * @param swapTenorDefinitionReceiver The schedule of periods of the receiver leg.
		 * @param forwardCurveReceiverName The forward curve of the receiver leg (may be null).
		 * @param spreadReceiver The spread or fixed coupon of the receiver leg.
		 * @param discountCurveReceiverName The discount curve of the receiver leg.
		 * @param swapTenorDefinitionPayer The schedule of periods of the payer leg.
		 * @param forwardCurvePayerName The forward curve of the payer leg (may be null).
		 * @param spreadPayer The spread or fixed coupon of the payer leg.
		 * @param discountCurvePayerName The discount curve of the payer leg.
		 * @param calibrationCurveName The curve to calibrate, by this product.
		 * @param calibrationTime The time point in calibrationCurveName used to calibrate, by this product.
		 */
		public CalibrationSpec(
				final String type,
				final Schedule swapTenorDefinitionReceiver,
				final String forwardCurveReceiverName, final double spreadReceiver,
				final String discountCurveReceiverName,
				final Schedule swapTenorDefinitionPayer,
				final String forwardCurvePayerName, final double spreadPayer,
				final String discountCurvePayerName,
				final String calibrationCurveName,
				final double calibrationTime) {
			this(null, type, swapTenorDefinitionReceiver, forwardCurveReceiverName, spreadReceiver, discountCurveReceiverName, swapTenorDefinitionPayer, forwardCurvePayerName, spreadPayer, discountCurvePayerName, calibrationCurveName, calibrationTime);
		}

		/**
		 * Calibration specification.
		 *
		 * @param type The type of the calibration product.
		 * @param swapTenorDefinitionReceiver The schedule of periods of the receiver leg.
		 * @param forwardCurveReceiverName The forward curve of the receiver leg (may be null).
		 * @param spreadReceiver The spread or fixed coupon of the receiver leg.
		 * @param discountCurveReceiverName The discount curve of the receiver leg.
		 * @param swapTenorDefinitionPayer The schedule of periods of the payer leg.
		 * @param forwardCurvePayerName The forward curve of the payer leg (may be null).
		 * @param spreadPayer The spread or fixed coupon of the payer leg.
		 * @param discountCurvePayerName The discount curve of the payer leg.
		 * @param calibrationCurveName The curve to calibrate, by this product.
		 * @param calibrationTime The time point in calibrationCurveName used to calibrate, by this product.
		 */
		public CalibrationSpec(
				final String type,
				final double[] swapTenorDefinitionReceiver,
				final String forwardCurveReceiverName, final double spreadReceiver,
				final String discountCurveReceiverName,
				final double[] swapTenorDefinitionPayer,
				final String forwardCurvePayerName, final double spreadPayer,
				final String discountCurvePayerName,
				final String calibrationCurveName,
				final double calibrationTime) {
			super();
			this.type = type;
			this.swapTenorDefinitionReceiver = new RegularSchedule(new TimeDiscretizationFromArray(swapTenorDefinitionReceiver[0] /* initial */, swapTenorDefinitionReceiver[1] /* numberOfTimeSteps */, swapTenorDefinitionReceiver[2] /* deltaT */, TimeDiscretizationFromArray.ShortPeriodLocation.SHORT_PERIOD_AT_START));
			this.forwardCurveReceiverName = forwardCurveReceiverName;
			this.spreadReceiver = spreadReceiver;
			this.discountCurveReceiverName = discountCurveReceiverName;
			this.swapTenorDefinitionPayer = new RegularSchedule(new TimeDiscretizationFromArray(swapTenorDefinitionPayer[0] /* initial */, swapTenorDefinitionPayer[1] /* numberOfTimeSteps */, swapTenorDefinitionPayer[2] /* deltaT */, TimeDiscretizationFromArray.ShortPeriodLocation.SHORT_PERIOD_AT_START));
			this.forwardCurvePayerName = forwardCurvePayerName;
			this.spreadPayer = spreadPayer;
			this.discountCurvePayerName = discountCurvePayerName;
			this.calibrationCurveName = calibrationCurveName;
			this.calibrationTime = calibrationTime;
		}

		/**
		 * Calibration specification.
		 *
		 * @param type The type of the calibration product.
		 * @param swapTenorDefinitionReceiver The schedule of periods of the receiver leg.
		 * @param forwardCurveReceiverName The forward curve of the receiver leg (may be null).
		 * @param spreadReceiver The spread or fixed coupon of the receiver leg.
		 * @param discountCurveReceiverName The discount curve of the receiver leg.
		 * @param calibrationCurveName The curve to calibrate, by this product.
		 * @param calibrationTime The time point in calibrationCurveName used to calibrate, by this product.
		 */
		public CalibrationSpec(
				final String type,
				final double[] swapTenorDefinitionReceiver,
				final String forwardCurveReceiverName, final double spreadReceiver,
				final String discountCurveReceiverName,
				final String calibrationCurveName,
				final double calibrationTime) {
			super();
			this.type = type;
			this.swapTenorDefinitionReceiver = new RegularSchedule(new TimeDiscretizationFromArray(swapTenorDefinitionReceiver[0] /* initial */, swapTenorDefinitionReceiver[1] /* numberOfTimeSteps */, swapTenorDefinitionReceiver[2] /* deltaT */, TimeDiscretizationFromArray.ShortPeriodLocation.SHORT_PERIOD_AT_START));
			this.forwardCurveReceiverName = forwardCurveReceiverName;
			this.spreadReceiver = spreadReceiver;
			this.discountCurveReceiverName = discountCurveReceiverName;
			this.calibrationCurveName = calibrationCurveName;
			this.calibrationTime = calibrationTime;
		}

		public CalibrationSpec getCloneShifted(final double shift) {
			if(discountCurvePayerName == null || type.toLowerCase().equals("swapleg")  || type.toLowerCase().equals("deposit")  || type.toLowerCase().equals("fra")) {
				return new CalibrationSpec(symbol, type, swapTenorDefinitionReceiver, forwardCurveReceiverName, spreadReceiver+shift, discountCurveReceiverName, swapTenorDefinitionPayer, forwardCurvePayerName, spreadPayer, discountCurvePayerName, calibrationCurveName, calibrationTime);
			}
			else {
				return new CalibrationSpec(symbol, type, swapTenorDefinitionReceiver, forwardCurveReceiverName, spreadReceiver, discountCurveReceiverName, swapTenorDefinitionPayer, forwardCurvePayerName, spreadPayer+shift, discountCurvePayerName, calibrationCurveName, calibrationTime);
			}
		}

		@Override
		public String toString() {
			return "CalibrationSpec [symbol=" + symbol + ", type=" + type + ", swapTenorDefinitionReceiver="
					+ swapTenorDefinitionReceiver + ", forwardCurveReceiverName=" + forwardCurveReceiverName
					+ ", spreadReceiver=" + spreadReceiver + ", discountCurveReceiverName=" + discountCurveReceiverName
					+ ", swapTenorDefinitionPayer=" + swapTenorDefinitionPayer + ", forwardCurvePayerName="
					+ forwardCurvePayerName + ", spreadPayer=" + spreadPayer + ", discountCurvePayerName="
					+ discountCurvePayerName + ", calibrationCurveName=" + calibrationCurveName + ", calibrationTime="
					+ calibrationTime + "]";
		}
	}

	private AnalyticModel				model				= new AnalyticModelFromCurvesAndVols();
	private final Set<ParameterObject>		objectsToCalibrate	= new LinkedHashSet<>();
	private final Vector<AnalyticProduct>	calibrationProducts			= new Vector<>();
	private final Vector<String>						calibrationProductsSymbols	= new Vector<>();

	private final List<CalibrationSpec>				calibrationSpecs	= new ArrayList<>();

	private final double evaluationTime;
	private final double calibrationAccuracy;

	private final int lastNumberOfInterations;
	private double lastAccuracy;

	/**
	 * Generate a collection of calibrated curves (discount curves, forward curves)
	 * from a vector of calibration products and a given model.
	 *
	 * If the model already contains a curve referenced as calibration curve that
	 * curve is replaced by a clone, retaining the given curve information and
	 * adding a new calibration point.
	 *
	 * If the model does not contain the curve referenced as calibration curve, the
	 * curve will be added to the model.
	 *
	 * Use case: You already have a discount curve as part of the model and like
	 * to calibrate an additional curve to an additional set of instruments.
	 *
	 * @param calibrationSpecs Array of calibration specs.
	 * @param calibrationModel A given model used to value the calibration products.
	 * @param evaluationTime Evaluation time applied to the calibration products.
	 * @param calibrationAccuracy Error tolerance of the solver. Set to 0 if you need machine precision.
	 * @throws net.finmath.optimizer.SolverException May be thrown if the solver does not cannot find a solution of the calibration problem.
	 * @throws CloneNotSupportedException Thrown, when a curve could not be cloned.
	 */
	public CalibratedCurves(final List<CalibrationSpec> calibrationSpecs, final AnalyticModel calibrationModel, final double evaluationTime, final double calibrationAccuracy) throws SolverException, CloneNotSupportedException {
		if(calibrationModel != null) {
			model	= calibrationModel.getCloneForParameter(null);
		}
		this.evaluationTime = evaluationTime;
		this.calibrationAccuracy = calibrationAccuracy;

		for(final CalibrationSpec calibrationSpec : calibrationSpecs) {
			add(calibrationSpec);
		}

		lastNumberOfInterations = calibrate(calibrationAccuracy);
	}

	/**
	 * Generate a collection of calibrated curves (discount curves, forward curves)
	 * from a vector of calibration products and a given model.
	 *
	 * If the model already contains a curve referenced as calibration curve that
	 * curve is replaced by a clone, retaining the given curve information and
	 * adding a new calibration point.
	 *
	 * If the model does not contain the curve referenced as calibration curve, the
	 * curve will be added to the model.
	 *
	 * Use case: You already have a discount curve as part of the model and like
	 * to calibrate an additional curve to an additional set of instruments.
	 *
	 * @param calibrationSpecs Array of calibration specs.
	 * @param calibrationModel A given model used to value the calibration products.
	 * @param evaluationTime Evaluation time applied to the calibration products.
	 * @param calibrationAccuracy Error tolerance of the solver. Set to 0 if you need machine precision.
	 * @throws net.finmath.optimizer.SolverException May be thrown if the solver does not cannot find a solution of the calibration problem.
	 * @throws CloneNotSupportedException Thrown, when a curve could not be cloned.
	 */
	public CalibratedCurves(final CalibrationSpec[] calibrationSpecs, final AnalyticModelFromCurvesAndVols calibrationModel, final double evaluationTime, final double calibrationAccuracy) throws SolverException, CloneNotSupportedException {
		if(calibrationModel != null) {
			model	= calibrationModel.getCloneForParameter(null);
		}
		this.evaluationTime = evaluationTime;
		this.calibrationAccuracy = calibrationAccuracy;

		for(final CalibrationSpec calibrationSpec : calibrationSpecs) {
			add(calibrationSpec);
		}

		lastNumberOfInterations = calibrate(calibrationAccuracy);
	}

	/**
	 * Generate a collection of calibrated curves (discount curves, forward curves)
	 * from a vector of calibration products and a given model.
	 *
	 * If the model already contains a curve referenced as calibration curve that
	 * curve is replaced by a clone, retaining the given curve information and
	 * adding a new calibration point.
	 *
	 * If the model does not contain the curve referenced as calibration curve, the
	 * curve will be added to the model.
	 *
	 * Use case: You already have a discount curve as part of the model and like
	 * to calibrate an additional curve to an additional set of instruments.
	 *
	 * @param calibrationSpecs Array of calibration specs.
	 * @param calibrationModel A given model used to value the calibration products.
	 * @param calibrationAccuracy Error tolerance of the solver. Set to 0 if you need machine precision.
	 * @throws net.finmath.optimizer.SolverException May be thrown if the solver does not cannot find a solution of the calibration problem.
	 * @throws CloneNotSupportedException Thrown, when a curve could not be cloned.
	 */
	public CalibratedCurves(final CalibrationSpec[] calibrationSpecs, final AnalyticModelFromCurvesAndVols calibrationModel, final double calibrationAccuracy) throws SolverException, CloneNotSupportedException {
		this(calibrationSpecs, calibrationModel, 0.0, calibrationAccuracy);
	}

	/**
	 * Generate a collection of calibrated curves (discount curves, forward curves)
	 * from a vector of calibration products and a given model.
	 *
	 * If the model already contains a curve referenced as calibration curve that
	 * curve is replaced by a clone, retaining the given curve information and
	 * adding a new calibration point.
	 *
	 * If the model does not contain the curve referenced as calibration curve, the
	 * curve will be added to the model.
	 *
	 * Use case: You already have a discount curve as part of the model and like
	 * to calibrate an additional curve to an additional set of instruments.
	 *
	 * @param calibrationSpecs Array of calibration specs.
	 * @param calibrationModel A given model used to value the calibration products.
	 * @throws net.finmath.optimizer.SolverException May be thrown if the solver does not cannot find a solution of the calibration problem.
	 * @throws CloneNotSupportedException Thrown, when a curve could not be cloned.
	 */
	public CalibratedCurves(final CalibrationSpec[] calibrationSpecs, final AnalyticModelFromCurvesAndVols calibrationModel) throws SolverException, CloneNotSupportedException {
		this(calibrationSpecs, calibrationModel, 0.0);
	}

	/**
	 * Generate a collection of calibrated curves (discount curves, forward curves)
	 * from a vector of calibration products.
	 *
	 * @param calibrationSpecs Array of calibration specs.
	 * @throws net.finmath.optimizer.SolverException May be thrown if the solver does not cannot find a solution of the calibration problem.
	 * @throws CloneNotSupportedException Thrown, when a curve could not be cloned.
	 */
	public CalibratedCurves(final Collection<CalibrationSpec> calibrationSpecs) throws SolverException, CloneNotSupportedException {
		this(calibrationSpecs.toArray(new CalibrationSpec[calibrationSpecs.size()]), null);
	}

	/**
	 * Generate a collection of calibrated curves (discount curves, forward curves)
	 * from a vector of calibration products.
	 *
	 * @param calibrationSpecs Array of calibration specs.
	 * @throws net.finmath.optimizer.SolverException May be thrown if the solver does not cannot find a solution of the calibration problem.
	 * @throws CloneNotSupportedException Thrown, when a curve could not be cloned.
	 */
	public CalibratedCurves(final CalibrationSpec[] calibrationSpecs) throws SolverException, CloneNotSupportedException {
		this(calibrationSpecs, null, 0.0);
	}

	public AnalyticProduct getCalibrationProductForSpec(final CalibrationSpec calibrationSpec) {
		String forwardCurveReceiverName = calibrationSpec.forwardCurveReceiverName;
		String forwardCurvePayerName	= calibrationSpec.forwardCurvePayerName;

		/*
		 * If required, default curves are created if missing.
		 */
		if(isCreateDefaultCurvesForMissingCurves) {
			createDiscountCurve(calibrationSpec.discountCurveReceiverName);
			createDiscountCurve(calibrationSpec.discountCurvePayerName);

			forwardCurveReceiverName	= createForwardCurve(calibrationSpec.swapTenorDefinitionReceiver, calibrationSpec.forwardCurveReceiverName);
			forwardCurvePayerName		= createForwardCurve(calibrationSpec.swapTenorDefinitionPayer, calibrationSpec.forwardCurvePayerName);
		}
		else {
			final Predicate<String> discountCurveMissing = new Predicate<String>() {
				@Override
				public boolean test(final String curveName) {
					return curveName != null && curveName.length() > 0 && model.getDiscountCurve(curveName) == null;
				}
			};
			final Predicate<String> forwardCurveMissing = new Predicate<String>() {
				@Override
				public boolean test(final String curveName) {
					return curveName != null && curveName.length() > 0 && model.getForwardCurve(curveName) == null;
				}
			};
			if(discountCurveMissing.test(calibrationSpec.discountCurveReceiverName)) {
				throw new IllegalArgumentException("Discount curve " + calibrationSpec.discountCurveReceiverName + " missing. Needs to be part of model " + model + ".");
			}
			if(discountCurveMissing.test(calibrationSpec.discountCurvePayerName)) {
				throw new IllegalArgumentException("Discount curve " + calibrationSpec.discountCurvePayerName + " missing. Needs to be part of model " + model + ".");
			}
			if(forwardCurveMissing.test(calibrationSpec.forwardCurveReceiverName)) {
				throw new IllegalArgumentException("Forward curve " + calibrationSpec.forwardCurveReceiverName + " missing. Needs to be part of model " + model + ".");
			}
			if(forwardCurveMissing.test(calibrationSpec.forwardCurvePayerName)) {
				throw new IllegalArgumentException("Forward curve " + calibrationSpec.forwardCurvePayerName + " missing. Needs to be part of model " + model + ".");
			}
		}

		final Schedule tenorReceiver = calibrationSpec.swapTenorDefinitionReceiver;
		final Schedule tenorPayer	= calibrationSpec.swapTenorDefinitionPayer;

		AnalyticProduct product = null;
		if(calibrationSpec.type.toLowerCase().equals("deposit")){
			product = new Deposit(tenorReceiver, calibrationSpec.spreadReceiver, calibrationSpec.discountCurveReceiverName);
		}
		else if(calibrationSpec.type.toLowerCase().equals("fra")){
			product = new ForwardRateAgreement(tenorReceiver, calibrationSpec.spreadReceiver, forwardCurveReceiverName, calibrationSpec.discountCurveReceiverName);
		}
		else if(calibrationSpec.type.toLowerCase().equals("future")){
			// like a fra but future price needs to be translated into rate
			product = new ForwardRateAgreement(calibrationSpec.swapTenorDefinitionReceiver, 1.0-calibrationSpec.spreadReceiver/100.0, forwardCurveReceiverName, calibrationSpec.discountCurveReceiverName);
		}
		else if(calibrationSpec.type.toLowerCase().equals("swapleg")) {
			// note that a swapLeg is always assumed to have a notional reset
			product = new SwapLeg(tenorReceiver, forwardCurveReceiverName, calibrationSpec.spreadReceiver, calibrationSpec.discountCurveReceiverName, true);
		}
		else if(calibrationSpec.type.toLowerCase().equals("swap")) {
			final SwapLeg	legReceiver	= new SwapLeg(tenorReceiver, forwardCurveReceiverName, calibrationSpec.spreadReceiver, calibrationSpec.discountCurveReceiverName, true);
			final SwapLeg	legPayer	= new SwapLeg(tenorPayer, forwardCurvePayerName, calibrationSpec.spreadPayer, calibrationSpec.discountCurvePayerName, true);
			product = new Swap(legReceiver, legPayer);
		}
		else if(calibrationSpec.type.toLowerCase().equals("swapwithresetonreceiver")) {
			final String discountCurveForNotionalResetName = calibrationSpec.discountCurvePayerName;
			final SwapLeg	legReceiver	= new SwapLeg(tenorReceiver, forwardCurveReceiverName, calibrationSpec.spreadReceiver, calibrationSpec.discountCurveReceiverName, discountCurveForNotionalResetName, true);
			final SwapLeg	legPayer	= new SwapLeg(tenorPayer, forwardCurvePayerName, calibrationSpec.spreadPayer, calibrationSpec.discountCurvePayerName, true);
			product = new Swap(legReceiver, legPayer);
		}
		else if(calibrationSpec.type.toLowerCase().equals("swapwithresetonpayer")) {
			final String discountCurveForNotionalResetName = calibrationSpec.discountCurveReceiverName;
			final SwapLeg	legReceiver	= new SwapLeg(tenorReceiver, forwardCurveReceiverName, calibrationSpec.spreadReceiver, calibrationSpec.discountCurveReceiverName, true);
			final SwapLeg	legPayer	= new SwapLeg(tenorPayer, forwardCurvePayerName, calibrationSpec.spreadPayer, calibrationSpec.discountCurvePayerName, discountCurveForNotionalResetName, true);
			product = new Swap(legReceiver, legPayer);
		}

		else {
			throw new RuntimeException("Product of type " + calibrationSpec.type + " unknown.");
		}

		return product;
	}

	/**
	 * Return the calibrated model, i.e., the model maintaining a collection of curves calibrated to the
	 * given calibration specifications.
	 *
	 * @return The calibrated model.
	 */
	public AnalyticModel getModel() {
		return model;
	}

	/**
	 * Get a curve for a given name.
	 *
	 * @param name Name of the curve
	 * @return The curve model.
	 */
	public Curve getCurve(final String name) {
		return model.getCurve(name);
	}

	/**
	 * Return the number of iterations needed to calibrate the model.
	 *
	 * @return The number of iterations needed to calibrate the model.
	 */
	public int getLastNumberOfInterations() {
		return lastNumberOfInterations;
	}

	/**
	 * Returns the set curves calibrated to "shifted" market data, that is,
	 * the market date of <code>this</code> object, modified by the shifts
	 * provided to this methods.
	 *
	 * @param symbol The symbol to shift. All other symbols remain unshifted.
	 * @param shift The shift to apply to the symbol.
	 * @return A new set of calibrated curves, calibrated to shifted market data.
	 * @throws SolverException The likely cause of this exception is a failure of the solver used in the calibration.
	 * @throws CloneNotSupportedException The likely cause of this exception is the inability to clone or modify a curve.
	 */
	public CalibratedCurves getCloneShifted(final String symbol, final double shift) throws SolverException, CloneNotSupportedException {
		// Clone calibration specs, shifting the desired symbol
		final List<CalibrationSpec> calibrationSpecsShifted = new ArrayList<>();
		for(final CalibrationSpec calibrationSpec : calibrationSpecs) {
			if(calibrationSpec.symbol.equals(symbol)) {
				calibrationSpecsShifted.add(calibrationSpec.getCloneShifted(shift));
			}
			else {
				calibrationSpecsShifted.add(calibrationSpec);
			}
		}

		return new CalibratedCurves(calibrationSpecsShifted, model, evaluationTime, calibrationAccuracy);
	}

	/**
	 * Returns the set curves calibrated to "shifted" market data, that is,
	 * the market date of <code>this</code> object, modified by the shifts
	 * provided to this methods.
	 *
	 * @param shifts A map of shifts associating each symbol with a shifts. If symbols are not part of this map, they remain unshifted.
	 * @return A new set of calibrated curves, calibrated to shifted market data.
	 * @throws SolverException The likely cause of this exception is a failure of the solver used in the calibration.
	 * @throws CloneNotSupportedException The likely cause of this exception is the inability to clone or modify a curve.
	 */
	public CalibratedCurves getCloneShifted(final Map<String,Double> shifts) throws SolverException, CloneNotSupportedException {
		// Clone calibration specs, shifting the desired symbol
		final List<CalibrationSpec> calibrationSpecsShifted = new ArrayList<>();
		for(final CalibrationSpec calibrationSpec : calibrationSpecs) {
			if(shifts.containsKey(calibrationSpec)) {
				calibrationSpecsShifted.add(calibrationSpec.getCloneShifted(shifts.get(calibrationSpec)));
			}
			else {
				calibrationSpecsShifted.add(calibrationSpec);
			}
		}

		return new CalibratedCurves(calibrationSpecsShifted, model, evaluationTime, calibrationAccuracy);
	}

	/**
	 * Returns the set curves calibrated to "shifted" market data, that is,
	 * the market date of <code>this</code> object, modified by the shifts
	 * provided to this methods.
	 *
	 * This method will shift all symbols matching a given regular expression <code>Pattern</code>.
	 *
	 * @see java.util.regex.Pattern
	 *
	 * @param symbolRegExp A pattern, identifying the symbols to shift.
	 * @param shift The shift to apply to the symbol(s).
	 * @return A new set of calibrated curves, calibrated to shifted market data.
	 * @throws SolverException The likely cause of this exception is a failure of the solver used in the calibration.
	 * @throws CloneNotSupportedException The likely cause of this exception is the inability to clone or modify a curve.
	 */
	public CalibratedCurves getCloneShifted(final Pattern symbolRegExp, final double shift) throws SolverException, CloneNotSupportedException {
		// Clone calibration specs, shifting the desired symbol
		final List<CalibrationSpec> calibrationSpecsShifted = new ArrayList<>();

		for(final CalibrationSpec calibrationSpec : calibrationSpecs) {
			final Matcher matcher = symbolRegExp.matcher(calibrationSpec.symbol);
			if(matcher.matches()) {
				calibrationSpecsShifted.add(calibrationSpec.getCloneShifted(shift));
			}
			else {
				calibrationSpecsShifted.add(calibrationSpec);
			}
		}

		return new CalibratedCurves(calibrationSpecsShifted, model, evaluationTime, calibrationAccuracy);
	}

	/**
	 * Returns the set curves calibrated to "shifted" market data, that is,
	 * the market date of <code>this</code> object, modified by the shifts
	 * provided to this methods.
	 *
	 * This method will shift all symbols matching a given regular expression.
	 *
	 * @see java.util.regex.Pattern
	 *
	 * @param symbolRegExp A string representing a regular expression, identifying the symbols to shift.
	 * @param shift The shift to apply to the symbol(s).
	 * @return A new set of calibrated curves, calibrated to shifted market data.
	 * @throws SolverException The likely cause of this exception is a failure of the solver used in the calibration.
	 * @throws CloneNotSupportedException The likely cause of this exception is the inability to clone or modify a curve.
	 */
	public CalibratedCurves getCloneShiftedForRegExp(final String symbolRegExp, final double shift) throws SolverException, CloneNotSupportedException {
		return getCloneShifted(Pattern.compile(symbolRegExp), shift);
	}

	/**
	 * Return the accuracy achieved in the last calibration.
	 *
	 * @return The accuracy achieved in the last calibration.
	 */
	public double getLastAccuracy() {
		return lastAccuracy;
	}

	/**
	 * Returns the first product found in the vector of calibration products
	 * which matches the given symbol, where symbol is the String set in
	 * the calibrationSpecs.
	 *
	 * @param symbol A given symbol string.
	 * @return The product associated with that symbol.
	 */
	public AnalyticProduct getCalibrationProductForSymbol(final String symbol) {

		/*
		 * The internal data structure is not optimal here (a map would make more sense here),
		 * if the user does not require access to the products, we would allow non-unique symbols.
		 * Hence we store both in two side by side vectors.
		 */
		for(int i=0; i<calibrationProductsSymbols.size(); i++) {
			final String calibrationProductSymbol = calibrationProductsSymbols.get(i);
			if(calibrationProductSymbol.equals(symbol)) {
				return calibrationProducts.get(i);
			}
		}

		return null;
	}

	private int calibrate(final double accuracy) throws SolverException {
		final Solver solver = new Solver(model, calibrationProducts, evaluationTime, accuracy);
		model = solver.getCalibratedModel(objectsToCalibrate);

		lastAccuracy = solver.getAccuracy();

		return solver.getIterations();
	}

	/**
	 * Add a calibration product to the set of calibration instruments.
	 *
	 * @param calibrationSpec The spec of the calibration product.
	 * @throws CloneNotSupportedException Thrown if a curve could not be cloned / created.
	 */
	private String add(final CalibrationSpec calibrationSpec) throws CloneNotSupportedException
	{
		calibrationSpecs.add(calibrationSpec);

		/*
		 * Add one point to the calibration curve and one new objective function
		 */

		// Create calibration product (will also create the curve if necessary)
		calibrationProducts.add(getCalibrationProductForSpec(calibrationSpec));
		calibrationProductsSymbols.add(calibrationSpec.symbol);

		// Create parameter to calibrate

		// Fetch old curve
		final Curve calibrationCurveOld = model.getCurve(calibrationSpec.calibrationCurveName);
		if(calibrationCurveOld == null) {
			throw new IllegalArgumentException("Calibration curve " + calibrationSpec.calibrationCurveName + " does not exist. This should not happen. Possible reason: The given calibration product does not depend on the given calibration curve.");
		}

		// Remove old curve
		objectsToCalibrate.remove(calibrationCurveOld);

		// Create and add new curve
		Curve calibrationCurve = null;
		if(DiscountCurveInterface.class.isInstance(calibrationCurveOld)) {
			@SuppressWarnings("unused")
			final
			double paymentTime	= calibrationSpec.swapTenorDefinitionReceiver.getPayment(calibrationSpec.swapTenorDefinitionReceiver.getNumberOfPeriods()-1);

			// Build new curve with one additional point
			calibrationCurve = calibrationCurveOld
					.getCloneBuilder()
					.addPoint(calibrationSpec.calibrationTime, model.getRandomVariableForConstant(1.0), true)
					.build();
		}
		else if(ForwardCurveInterface.class.isInstance(calibrationCurveOld)) {
			// Build new curve with one additional point
			calibrationCurve = calibrationCurveOld
					.getCloneBuilder()
					.addPoint(calibrationSpec.calibrationTime, model.getRandomVariableForConstant(0.1), true)
					.build();
		}
		else {
			// Build new curve with one additional point
			calibrationCurve = calibrationCurveOld
					.getCloneBuilder()
					.addPoint(calibrationSpec.calibrationTime, model.getRandomVariableForConstant(1.0), true)
					.build();
		}
		model = model.addCurves(calibrationCurve);
		objectsToCalibrate.add(calibrationCurve);

		return calibrationSpec.type;
	}

	/**
	 * Get a discount curve from the model, if not existing create a discount curve.
	 *
	 * @param discountCurveName The name of the discount curve to create.
	 * @return The discount factor curve associated with the given name.
	 */
	private DiscountCurveInterface createDiscountCurve(final String discountCurveName) {
		DiscountCurveInterface discountCurve	= model.getDiscountCurve(discountCurveName);
		if(discountCurve == null) {
			discountCurve = DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors(discountCurveName, new double[] { 0.0 }, new double[] { 1.0 });
			model = model.addCurves(discountCurve);
		}

		return discountCurve;
	}

	/**
	 * Get a forward curve from the model, if not existing create a forward curve.
	 *
	 * @param swapTenorDefinition The swap tenor associated with the forward curve.
	 * @param forwardCurveName The name of the forward curve to create.
	 * @return The forward curve associated with the given name.
	 */
	private String createForwardCurve(final Schedule swapTenorDefinition, final String forwardCurveName) {

		/*
		 * Temporary "hack" - we try to infer index maturity codes from curve name.
		 */
		String indexMaturityCode = null;
		if(forwardCurveName.contains("_12M") || forwardCurveName.contains("-12M") || forwardCurveName.contains(" 12M")) {
			indexMaturityCode = "12M";
		}
		if(forwardCurveName.contains("_1M")	|| forwardCurveName.contains("-1M")	|| forwardCurveName.contains(" 1M")) {
			indexMaturityCode = "1M";
		}
		if(forwardCurveName.contains("_6M")	|| forwardCurveName.contains("-6M")	|| forwardCurveName.contains(" 6M")) {
			indexMaturityCode = "6M";
		}
		if(forwardCurveName.contains("_3M") || forwardCurveName.contains("-3M") || forwardCurveName.contains(" 3M")) {
			indexMaturityCode = "3M";
		}

		if(forwardCurveName == null || forwardCurveName.isEmpty()) {
			return null;
		}

		// Check if the curves exists, if not create it
		Curve	curve = model.getCurve(forwardCurveName);

		Curve	forwardCurve = null;
		if(curve == null) {
			// Create a new forward curve
			if(isUseForwardCurve) {
				curve = new ForwardCurveInterpolation(forwardCurveName, swapTenorDefinition.getReferenceDate(), indexMaturityCode, ForwardCurveInterpolation.InterpolationEntityForward.FORWARD, null);
			}
			else {
				// Alternative: Model the forward curve through an underlying discount curve.
				curve = DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors(forwardCurveName, new double[] { 0.0 }, new double[] { 1.0 });
				model = model.addCurves(curve);
			}
		}

		// Check if the curve is a discount curve, if yes - create a forward curve wrapper.
		if(DiscountCurveInterface.class.isInstance(curve)) {
			/*
			 *  If the specified forward curve exits as a discount curve, we generate a forward curve
			 *  by wrapping the discount curve and calculating the
			 *  forward from discount factors using the formula (df(T)/df(T+Delta T) - 1) / Delta T).
			 *
			 *  If no index maturity is used, the forward curve is interpreted "single curve", i.e.
			 *  T+Delta T is always the payment.
			 */
			forwardCurve = new ForwardCurveFromDiscountCurve(curve.getName(), swapTenorDefinition.getReferenceDate(), indexMaturityCode);
		}
		else {
			// Use a given forward curve
			forwardCurve = curve;
		}

		model = model.addCurves(forwardCurve);

		return forwardCurve.getName();
	}
}
