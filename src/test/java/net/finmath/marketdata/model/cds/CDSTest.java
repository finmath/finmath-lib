package net.finmath.marketdata.model.cds;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.marketdata.calibration.ParameterObject;
import net.finmath.marketdata.calibration.Solver;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.curves.CurveInterpolation;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.products.AnalyticProduct;
import net.finmath.optimizer.SolverException;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.businessdaycalendar.AbstractBusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarAny;


/**
 * Description of the Test.
 *
 */
public class CDSTest {

	@Test
	public void testCDS() throws SolverException {

		final double errorTolerance = 1E-14;

		// Create a discount curve (no discounting)
		final DiscountCurveInterpolation discountCurveInterpolation					= DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors(
				"discountCurve"								/* name */,
				new double[] {0.0, 1.0, 2.0, 3.0, 4.0, 5.0}	/* maturities */,
				new double[] {1.0, 1.0, 1.0, 1.0, 1.0, 1.0}	/* discount factors */,
				CurveInterpolation.InterpolationMethod.LINEAR,
				CurveInterpolation.ExtrapolationMethod.LINEAR,
				CurveInterpolation.InterpolationEntity.LOG_OF_VALUE
				);


		// Create a survival probability curve with a conditional default probability of 10% each period
		final CurveInterpolation survivalProbabilityCurve	= DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors(
				"survivalProbabilityCurve"								/* name */,
				new double[] {0.0, 1.0, 2.0, 3.0, 4.0, 5.0}	/* maturities */,
				new double[] {1.0, 0.9, 0.8, 0.7, 0.6, 0.5}	/* survival probabilities factors */,
				CurveInterpolation.InterpolationMethod.LINEAR,
				CurveInterpolation.ExtrapolationMethod.LINEAR,
				CurveInterpolation.InterpolationEntity.LOG_OF_VALUE
				);


		// Define a recovery rate curve (flat at 40%)
		final CurveInterpolation recoveryRateCurve= DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors(
				"recoveryRateCurve"								/* name */,
				new double[] {0.0, 1.0, 2.0, 3.0, 4.0, 5.0}	    /* maturities */,
				new double[]{ 1.0, 0.4, 0.4, 0.4, 0.4, 0.4}     /* recovery rates */,
				CurveInterpolation.InterpolationMethod.LINEAR,
				CurveInterpolation.ExtrapolationMethod.LINEAR,
				CurveInterpolation.InterpolationEntity.LOG_OF_VALUE
				);

		final CDS.DirtyCleanPrice dirtyCleanPrice = CDS.DirtyCleanPrice.CLEAN;
		final CDS.ValuationModel pricingModel_DISCRETE = CDS.ValuationModel.DISCRETE;
		final CDS.ValuationModel pricingModel_JPM = CDS.ValuationModel.JPM;
		final CDS.ValuationModel pricingModel_JPM_NOACCFEE = CDS.ValuationModel.JPM_NOACCFEE;

		final DateTimeFormatter Formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

		final LocalDate referenceDate = LocalDate.parse("20.03.2018", Formatter);
		final LocalDate startDate = referenceDate;
		final LocalDate maturityDate1 = LocalDate.parse("20.03.2019", Formatter);
		final LocalDate maturityDate2 = LocalDate.parse("20.03.2020", Formatter);
		final LocalDate maturityDate3 = LocalDate.parse("20.03.2021", Formatter);
		final LocalDate maturityDate4 = LocalDate.parse("20.03.2022", Formatter);
		final LocalDate maturityDate5 = LocalDate.parse("20.03.2023", Formatter);

		final ScheduleGenerator.Frequency frequency = ScheduleGenerator.Frequency.ANNUAL;
		final ScheduleGenerator.DaycountConvention daycountConvention = ScheduleGenerator.DaycountConvention.ACT_360;  // Same DCC as for the Discount CurveFromInterpolationPoints (OIS CurveFromInterpolationPoints)?
		final ScheduleGenerator.ShortPeriodConvention shortPeriodConvention = ScheduleGenerator.ShortPeriodConvention.FIRST;
		final AbstractBusinessdayCalendar.DateRollConvention dateRollConvention = AbstractBusinessdayCalendar.DateRollConvention.UNADJUSTED;
		final BusinessdayCalendarAny businessdayCalendar = new BusinessdayCalendarAny();
		final int fixingOffsetDays = 0;
		final int paymentOffsetDays = 0;
		final boolean isUseEndOfMonth = false;

		final Schedule  schedule1 =  ScheduleGenerator.createScheduleFromConventions(referenceDate, startDate, maturityDate1, frequency, daycountConvention, shortPeriodConvention, dateRollConvention, businessdayCalendar, fixingOffsetDays, paymentOffsetDays, isUseEndOfMonth);
		final Schedule  schedule2 =  ScheduleGenerator.createScheduleFromConventions(referenceDate, startDate, maturityDate2, frequency, daycountConvention, shortPeriodConvention, dateRollConvention, businessdayCalendar, fixingOffsetDays, paymentOffsetDays, isUseEndOfMonth);
		final Schedule  schedule3 =  ScheduleGenerator.createScheduleFromConventions(referenceDate, startDate, maturityDate3, frequency, daycountConvention, shortPeriodConvention, dateRollConvention, businessdayCalendar, fixingOffsetDays, paymentOffsetDays, isUseEndOfMonth);
		final Schedule  schedule4 =  ScheduleGenerator.createScheduleFromConventions(referenceDate, startDate, maturityDate4, frequency, daycountConvention, shortPeriodConvention, dateRollConvention, businessdayCalendar, fixingOffsetDays, paymentOffsetDays, isUseEndOfMonth);
		final Schedule  schedule5 =  ScheduleGenerator.createScheduleFromConventions(referenceDate, startDate, maturityDate5, frequency, daycountConvention, shortPeriodConvention, dateRollConvention, businessdayCalendar, fixingOffsetDays, paymentOffsetDays, isUseEndOfMonth);

		final LocalDate tradeDate = LocalDate.parse("20.03.2018", Formatter);

		final boolean useFinerDiscretization = false;

		final double[] cdsFees = new double[]{0.1, 0.1, 0.1, 0.1, 0.1};

		// Create a collection of objective functions (calibration products = CDS with fixed fee of 10% and no upfront)
		// CDS with PricingModel=DISCRETE
		final CDS cds1 = new CDS(schedule1 , "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[0], tradeDate, pricingModel_DISCRETE, dirtyCleanPrice, useFinerDiscretization);
		final CDS cds2 = new CDS(schedule2, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[1], tradeDate, pricingModel_DISCRETE, dirtyCleanPrice, useFinerDiscretization);
		final CDS cds3 = new CDS(schedule3, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[2], tradeDate, pricingModel_DISCRETE, dirtyCleanPrice, useFinerDiscretization);
		final CDS cds4 = new CDS(schedule4, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[3], tradeDate, pricingModel_DISCRETE, dirtyCleanPrice, useFinerDiscretization);
		final CDS cds5 = new CDS(schedule5, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[4], tradeDate, pricingModel_DISCRETE, dirtyCleanPrice, useFinerDiscretization);

		// CDS with PricingModel=JPM
		final CDS cds_JPM1 = new CDS(schedule1, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[0], tradeDate, pricingModel_JPM, dirtyCleanPrice,useFinerDiscretization);
		final CDS cds_JPM2 = new CDS(schedule2, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[1], tradeDate, pricingModel_JPM, dirtyCleanPrice, useFinerDiscretization);
		final CDS cds_JPM3 = new CDS(schedule3, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[2], tradeDate, pricingModel_JPM, dirtyCleanPrice, useFinerDiscretization);
		final CDS cds_JPM4 = new CDS(schedule4, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[3], tradeDate, pricingModel_JPM, dirtyCleanPrice, useFinerDiscretization);
		final CDS cds_JPM5 = new CDS(schedule5, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[4], tradeDate, pricingModel_JPM, dirtyCleanPrice, useFinerDiscretization);

		// CDS with PricingModel=JPM_NOACCFEE
		final CDS cds_JPM_NOACCFEE1 = new CDS(schedule1, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[0], tradeDate, pricingModel_JPM_NOACCFEE, dirtyCleanPrice, useFinerDiscretization);
		final CDS cds_JPM_NOACCFEE2 = new CDS(schedule2, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[1], tradeDate, pricingModel_JPM_NOACCFEE, dirtyCleanPrice, useFinerDiscretization);
		final CDS cds_JPM_NOACCFEE3 = new CDS(schedule3, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[2], tradeDate, pricingModel_JPM_NOACCFEE, dirtyCleanPrice, useFinerDiscretization);
		final CDS cds_JPM_NOACCFEE4 = new CDS(schedule4, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[3], tradeDate, pricingModel_JPM_NOACCFEE, dirtyCleanPrice, useFinerDiscretization);
		final CDS cds_JPM_NOACCFEE5 = new CDS(schedule5, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[4], tradeDate, pricingModel_JPM_NOACCFEE, dirtyCleanPrice, useFinerDiscretization);


		final Vector<AnalyticProduct> calibrationProductsDISCRETE = new Vector<AnalyticProduct>();
		calibrationProductsDISCRETE.add(cds1);
		calibrationProductsDISCRETE.add(cds2);
		calibrationProductsDISCRETE.add(cds3);
		calibrationProductsDISCRETE.add(cds4);
		calibrationProductsDISCRETE.add(cds5);

		final Vector<AnalyticProduct> calibrationProductsJPM = new Vector<AnalyticProduct>();
		calibrationProductsJPM.add(cds_JPM1);
		calibrationProductsJPM.add(cds_JPM2);
		calibrationProductsJPM.add(cds_JPM3);
		calibrationProductsJPM.add(cds_JPM4);
		calibrationProductsJPM.add(cds_JPM5);

		final Vector<AnalyticProduct> calibrationProductsJPMNOACCFEE = new Vector<AnalyticProduct>();
		calibrationProductsJPMNOACCFEE.add(cds_JPM_NOACCFEE1);
		calibrationProductsJPMNOACCFEE.add(cds_JPM_NOACCFEE2);
		calibrationProductsJPMNOACCFEE.add(cds_JPM_NOACCFEE3);
		calibrationProductsJPMNOACCFEE.add(cds_JPM_NOACCFEE4);
		calibrationProductsJPMNOACCFEE.add(cds_JPM_NOACCFEE5);



		// A model is a collection of curves (curves and products find other curves by looking up their name in the model)
		final AnalyticModelFromCurvesAndVols modelDISCRETE = new AnalyticModelFromCurvesAndVols(new CurveInterpolation[] { discountCurveInterpolation, survivalProbabilityCurve, recoveryRateCurve });
		final AnalyticModelFromCurvesAndVols modelJPM = new AnalyticModelFromCurvesAndVols(new CurveInterpolation[] { discountCurveInterpolation, survivalProbabilityCurve, recoveryRateCurve });
		final AnalyticModelFromCurvesAndVols modelJPMNOACCFEE = new AnalyticModelFromCurvesAndVols(new CurveInterpolation[] { discountCurveInterpolation, survivalProbabilityCurve, recoveryRateCurve });

		// Create a collection of curves to calibrate
		final Set<ParameterObject> curvesToCalibrate = new HashSet<ParameterObject>();
		curvesToCalibrate.add(survivalProbabilityCurve);

		// Calibrate the curve (All CDS should be valued at 0 initially ~ conventional CDS spread or with upfront)
		final ArrayList<Double> targetList = new ArrayList<Double>();
		targetList.add(0.0);
		targetList.add(0.0);
		targetList.add(0.0);
		targetList.add(0.0);
		targetList.add(0.0);

		// ----------------------------- DISCRETE --------------------------------------------

		System.out.println("CDS Calibration - DISCRETE ---------------------");

		// Output Prices before Calibration
		for(int i=0;i<calibrationProductsDISCRETE.size();i++){
			System.out.println("Implemented value at t=0 of CDS (DISCRETE) with "+ (i+1) +" payments:"+" "+ calibrationProductsDISCRETE.get(i).getValue(0, modelDISCRETE));
		}

		// Calibration of the survivalProbabilityCurve
		final Solver solverDISCRETE = new Solver(modelDISCRETE, calibrationProductsDISCRETE, targetList, 0.0, errorTolerance);
		final AnalyticModel calibratedModelDISCRETE = solverDISCRETE.getCalibratedModel(curvesToCalibrate);

		// Output Results
		System.out.println("The solver required " + solverDISCRETE.getIterations() + " iterations.");
		System.out.println("survivalProbabilityCurve");
		System.out.println(calibratedModelDISCRETE.getCurve("survivalProbabilityCurve").toString());
		System.out.println(" ");

		System.out.println("CDS prices");
		for(int i=0;i<calibrationProductsDISCRETE.size();i++){
			System.out.println("Model Price of CDS "+ (i+1) +" is:"+" "+ calibrationProductsDISCRETE.get(i).getValue(0.0, calibratedModelDISCRETE));
			System.out.println("Market Price of CDS "+ (i+1) +" is:"+" "+ 0.0);
		}
		System.out.println(" ");


		// Test method getConventionalSpread()
		double conventionalSpread = cds5.getConventionalSpread(0.0, calibratedModelDISCRETE);
		System.out.println("Conventional Spread of CDS 5: " + conventionalSpread);
		System.out.println(" ");

		// Check if error < errorTolerance
		System.out.print("Calibration check: ");
		double evaluationTime = 0.0;
		double error = 0;
		for(int calibrationProductIndex = 0; calibrationProductIndex < calibrationProductsDISCRETE.size(); calibrationProductIndex++) {
			final AnalyticProduct	calibrationProduct		= calibrationProductsDISCRETE.get(calibrationProductIndex);
			final double						calibrationProductValue	= calibrationProduct.getValue(evaluationTime, calibratedModelDISCRETE);

			error += (calibrationProductValue-targetList.get(calibrationProductIndex))*(calibrationProductValue-targetList.get(calibrationProductIndex));
		}
		error = Math.sqrt(error);
		if(error < errorTolerance){
			System.out.println("Successful");
		}else{
			System.out.println("Failed");
		}
		Assert.assertTrue(error < errorTolerance);
		System.out.println("");

		// ----------------------------- JPM -----------------------------------------------------

		System.out.println("CDS Calibration - JPM ---------------------");

		// Output Prices before Calibration
		for(int i=0;i<calibrationProductsJPM.size();i++){
			System.out.println("Implemented value at t=0 of CDS (JPM) with "+ (i+1) +" payments:"+" "+ calibrationProductsJPM.get(i).getValue(0, modelJPM));
		}

		// Calibration of the survivalProbabilityCurve
		final Solver solverJPM = new Solver(modelJPM, calibrationProductsJPM, targetList, 0.0, errorTolerance);
		final AnalyticModel calibratedModelJPM = solverJPM.getCalibratedModel(curvesToCalibrate);

		// Output Results
		System.out.println("The solver required " + solverJPM.getIterations() + " iterations.");
		System.out.println("survivalProbabilityCurve");
		System.out.println(calibratedModelJPM.getCurve("survivalProbabilityCurve").toString());
		System.out.println(" ");

		System.out.println("CDS prices");
		for(int i=0;i<calibrationProductsJPM.size();i++){
			System.out.println("Model Price of CDS "+ (i+1) +" is:"+" "+ calibrationProductsJPM.get(i).getValue(0.0, calibratedModelJPM));
			System.out.println("Market Price of CDS "+ (i+1) +" is:"+" "+ 0.0);
		}
		System.out.println(" ");


		// Test method getConventionalSpread()
		conventionalSpread = cds5.getConventionalSpread(0.0, calibratedModelJPM);
		System.out.println("Conventional Spread of CDS 5: " + conventionalSpread);
		System.out.println(" ");

		// Check if error < errorTolerance
		System.out.print("Calibration check: ");
		evaluationTime = 0.0;
		error = 0;
		for(int calibrationProductIndex = 0; calibrationProductIndex < calibrationProductsJPM.size(); calibrationProductIndex++) {
			final AnalyticProduct	calibrationProduct		= calibrationProductsJPM.get(calibrationProductIndex);
			final double						calibrationProductValue	= calibrationProduct.getValue(evaluationTime, calibratedModelJPM);

			error += (calibrationProductValue-targetList.get(calibrationProductIndex))*(calibrationProductValue-targetList.get(calibrationProductIndex));
		}
		error = Math.sqrt(error);
		if(error < errorTolerance){
			System.out.println("Successful");
		}else{
			System.out.println("Failed");
		}
		Assert.assertTrue(error < errorTolerance);
		System.out.println("");

		// ----------------------------- JPM_NOACCFEE -----------------------------------------------------

		System.out.println("CDS Calibration - JPM ---------------------");

		// Output Prices before Calibration
		for(int i=0;i<calibrationProductsJPMNOACCFEE.size();i++){
			System.out.println("Implemented value at t=0 of CDS (JPM_NOACCFEE) with "+ (i+1) +" payments:"+" "+ calibrationProductsJPMNOACCFEE.get(i).getValue(0, modelJPMNOACCFEE));
		}

		// Calibration of the survivalProbabilityCurve
		final Solver solverJPM_NOACCFEE = new Solver(modelJPMNOACCFEE, calibrationProductsJPMNOACCFEE, targetList, 0.0, errorTolerance);
		final AnalyticModel calibratedModelJPM_NOACCFEE = solverJPM_NOACCFEE.getCalibratedModel(curvesToCalibrate);

		// Output Results
		System.out.println("The solver required " + solverJPM_NOACCFEE.getIterations() + " iterations.");
		System.out.println("survivalProbabilityCurve");
		System.out.println(calibratedModelJPM_NOACCFEE.getCurve("survivalProbabilityCurve").toString());
		System.out.println(" ");

		System.out.println("CDS prices");
		for(int i=0;i<calibrationProductsJPMNOACCFEE.size();i++){
			System.out.println("Model Price of CDS "+ (i+1) +" is:"+" "+ calibrationProductsJPMNOACCFEE.get(i).getValue(0.0, calibratedModelJPM_NOACCFEE));
			System.out.println("Market Price of CDS "+ (i+1) +" is:"+" "+ 0.0);
		}
		System.out.println(" ");


		// Test method getConventionalSpread()
		conventionalSpread = cds5.getConventionalSpread(0.0, calibratedModelJPM_NOACCFEE);
		System.out.println("Conventional Spread of CDS 5: " + conventionalSpread);
		System.out.println(" ");

		// Check if error < errorTolerance
		System.out.print("Calibration check: ");
		evaluationTime = 0.0;
		error = 0;
		for(int calibrationProductIndex = 0; calibrationProductIndex < calibrationProductsJPMNOACCFEE.size(); calibrationProductIndex++) {
			final AnalyticProduct	calibrationProduct		= calibrationProductsJPMNOACCFEE.get(calibrationProductIndex);
			final double						calibrationProductValue	= calibrationProduct.getValue(evaluationTime, calibratedModelJPM_NOACCFEE);

			error += (calibrationProductValue-targetList.get(calibrationProductIndex))*(calibrationProductValue-targetList.get(calibrationProductIndex));
		}
		error = Math.sqrt(error);
		if(error < errorTolerance){
			System.out.println("Successful");
		}else{
			System.out.println("Failed");
		}
		Assert.assertTrue(error < errorTolerance);

	}

}

