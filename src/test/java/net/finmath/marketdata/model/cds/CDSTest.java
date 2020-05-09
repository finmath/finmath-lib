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
import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.curves.CurveInterpolation;
import net.finmath.marketdata.model.curves.CurveInterpolation;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.products.AnalyticProduct;
import net.finmath.optimizer.SolverException;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleFromPeriods;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarAny;
import net.finmath.time.businessdaycalendar.AbstractBusinessdayCalendar;


/**
 * Description of the Test.
 *
 */
public class CDSTest {

	@Test
	public void testCDS() throws SolverException {

		final double errorTolerance = 1E-14;

		// Create a discount curve (no discounting)
		DiscountCurveInterpolation discountCurveInterpolation					= DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors(
				"discountCurve"								/* name */,
				new double[] {0.0, 1.0, 2.0, 3.0, 4.0, 5.0}	/* maturities */,
				new double[] {1.0, 1.0, 1.0, 1.0, 1.0, 1.0}	/* discount factors */,
				CurveInterpolation.InterpolationMethod.LINEAR,
				CurveInterpolation.ExtrapolationMethod.LINEAR,
				CurveInterpolation.InterpolationEntity.LOG_OF_VALUE
				);


		// Create a survival probability curve with a conditional default probability of 10% each period
		CurveInterpolation survivalProbabilityCurve	= DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors(
				"survivalProbabilityCurve"								/* name */,
				new double[] {0.0, 1.0, 2.0, 3.0, 4.0, 5.0}	/* maturities */,
				new double[] {1.0, 0.9, 0.8, 0.7, 0.6, 0.5}	/* survival probabilities factors */,
				CurveInterpolation.InterpolationMethod.LINEAR,
				CurveInterpolation.ExtrapolationMethod.LINEAR,
				CurveInterpolation.InterpolationEntity.LOG_OF_VALUE
				);


		// Define a recovery rate curve (flat at 40%)
		CurveInterpolation recoveryRateCurve= DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors(
				"recoveryRateCurve"								/* name */,
				new double[] {0.0, 1.0, 2.0, 3.0, 4.0, 5.0}	    /* maturities */,
				new double[]{ 1.0, 0.4, 0.4, 0.4, 0.4, 0.4}     /* recovery rates */,
				CurveInterpolation.InterpolationMethod.LINEAR,
				CurveInterpolation.ExtrapolationMethod.LINEAR,
				CurveInterpolation.InterpolationEntity.LOG_OF_VALUE
				);

		CDS.DirtyCleanPrice dirtyCleanPrice = CDS.DirtyCleanPrice.CLEAN;
		CDS.PricingModel pricingModel_DISCRETE = CDS.PricingModel.DISCRETE;
		CDS.PricingModel pricingModel_JPM = CDS.PricingModel.JPM;
		CDS.PricingModel pricingModel_JPM_NOACCFEE = CDS.PricingModel.JPM_NOACCFEE;

		DateTimeFormatter Formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

		LocalDate referenceDate = LocalDate.parse("20.03.2018", Formatter);
		LocalDate startDate = referenceDate;
		LocalDate maturityDate1 = LocalDate.parse("20.03.2019", Formatter);
		LocalDate maturityDate2 = LocalDate.parse("20.03.2020", Formatter);
		LocalDate maturityDate3 = LocalDate.parse("20.03.2021", Formatter);
		LocalDate maturityDate4 = LocalDate.parse("20.03.2022", Formatter);
		LocalDate maturityDate5 = LocalDate.parse("20.03.2023", Formatter);

		ScheduleGenerator.Frequency frequency = ScheduleGenerator.Frequency.ANNUAL;
		ScheduleGenerator.DaycountConvention daycountConvention = ScheduleGenerator.DaycountConvention.ACT_360;  // Same DCC as for the Discount CurveFromInterpolationPoints (OIS CurveFromInterpolationPoints)?
		ScheduleGenerator.ShortPeriodConvention shortPeriodConvention = ScheduleGenerator.ShortPeriodConvention.FIRST;
		AbstractBusinessdayCalendar.DateRollConvention dateRollConvention = AbstractBusinessdayCalendar.DateRollConvention.UNADJUSTED;
		BusinessdayCalendarAny businessdayCalendar = new BusinessdayCalendarAny();
		int fixingOffsetDays = 0;
		int paymentOffsetDays = 0;
		boolean isUseEndOfMonth = false;

		Schedule  schedule1 =  ScheduleGenerator.createScheduleFromConventions(referenceDate, startDate, maturityDate1, frequency, daycountConvention, shortPeriodConvention, dateRollConvention, businessdayCalendar, fixingOffsetDays, paymentOffsetDays, isUseEndOfMonth);
		Schedule  schedule2 =  ScheduleGenerator.createScheduleFromConventions(referenceDate, startDate, maturityDate2, frequency, daycountConvention, shortPeriodConvention, dateRollConvention, businessdayCalendar, fixingOffsetDays, paymentOffsetDays, isUseEndOfMonth);
		Schedule  schedule3 =  ScheduleGenerator.createScheduleFromConventions(referenceDate, startDate, maturityDate3, frequency, daycountConvention, shortPeriodConvention, dateRollConvention, businessdayCalendar, fixingOffsetDays, paymentOffsetDays, isUseEndOfMonth);
		Schedule  schedule4 =  ScheduleGenerator.createScheduleFromConventions(referenceDate, startDate, maturityDate4, frequency, daycountConvention, shortPeriodConvention, dateRollConvention, businessdayCalendar, fixingOffsetDays, paymentOffsetDays, isUseEndOfMonth);
		Schedule  schedule5 =  ScheduleGenerator.createScheduleFromConventions(referenceDate, startDate, maturityDate5, frequency, daycountConvention, shortPeriodConvention, dateRollConvention, businessdayCalendar, fixingOffsetDays, paymentOffsetDays, isUseEndOfMonth);

		LocalDate tradeDate = LocalDate.parse("20.03.2018", Formatter);

		boolean useFinerDiscretization = false;

		double[] cdsFees = new double[]{0.1, 0.1, 0.1, 0.1, 0.1};

		// Create a collection of objective functions (calibration products = CDS with fixed fee of 10% and no upfront)
		// CDS with PricingModel=DISCRETE
		CDS cds1 = new CDS(schedule1 , "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[0], tradeDate, pricingModel_DISCRETE, dirtyCleanPrice, useFinerDiscretization);
		CDS cds2 = new CDS(schedule2, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[1], tradeDate, pricingModel_DISCRETE, dirtyCleanPrice, useFinerDiscretization);
		CDS cds3 = new CDS(schedule3, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[2], tradeDate, pricingModel_DISCRETE, dirtyCleanPrice, useFinerDiscretization);
		CDS cds4 = new CDS(schedule4, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[3], tradeDate, pricingModel_DISCRETE, dirtyCleanPrice, useFinerDiscretization);
		CDS cds5 = new CDS(schedule5, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[4], tradeDate, pricingModel_DISCRETE, dirtyCleanPrice, useFinerDiscretization);

		// CDS with PricingModel=JPM
		CDS cds_JPM1 = new CDS(schedule1, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[0], tradeDate, pricingModel_JPM, dirtyCleanPrice,useFinerDiscretization);
		CDS cds_JPM2 = new CDS(schedule2, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[1], tradeDate, pricingModel_JPM, dirtyCleanPrice, useFinerDiscretization);
		CDS cds_JPM3 = new CDS(schedule3, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[2], tradeDate, pricingModel_JPM, dirtyCleanPrice, useFinerDiscretization);
		CDS cds_JPM4 = new CDS(schedule4, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[3], tradeDate, pricingModel_JPM, dirtyCleanPrice, useFinerDiscretization);
		CDS cds_JPM5 = new CDS(schedule5, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[4], tradeDate, pricingModel_JPM, dirtyCleanPrice, useFinerDiscretization);

		// CDS with PricingModel=JPM_NOACCFEE
		CDS cds_JPM_NOACCFEE1 = new CDS(schedule1, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[0], tradeDate, pricingModel_JPM_NOACCFEE, dirtyCleanPrice, useFinerDiscretization);
		CDS cds_JPM_NOACCFEE2 = new CDS(schedule2, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[1], tradeDate, pricingModel_JPM_NOACCFEE, dirtyCleanPrice, useFinerDiscretization);
		CDS cds_JPM_NOACCFEE3 = new CDS(schedule3, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[2], tradeDate, pricingModel_JPM_NOACCFEE, dirtyCleanPrice, useFinerDiscretization);
		CDS cds_JPM_NOACCFEE4 = new CDS(schedule4, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[3], tradeDate, pricingModel_JPM_NOACCFEE, dirtyCleanPrice, useFinerDiscretization);
		CDS cds_JPM_NOACCFEE5 = new CDS(schedule5, "discountCurve", "survivalProbabilityCurve", "recoveryRateCurve",  cdsFees[4], tradeDate, pricingModel_JPM_NOACCFEE, dirtyCleanPrice, useFinerDiscretization);


		Vector<AnalyticProduct> calibrationProductsDISCRETE = new Vector<AnalyticProduct>();
		calibrationProductsDISCRETE.add(cds1);
		calibrationProductsDISCRETE.add(cds2);
		calibrationProductsDISCRETE.add(cds3);
		calibrationProductsDISCRETE.add(cds4);
		calibrationProductsDISCRETE.add(cds5);

		Vector<AnalyticProduct> calibrationProductsJPM = new Vector<AnalyticProduct>();
		calibrationProductsJPM.add(cds_JPM1);
		calibrationProductsJPM.add(cds_JPM2);
		calibrationProductsJPM.add(cds_JPM3);
		calibrationProductsJPM.add(cds_JPM4);
		calibrationProductsJPM.add(cds_JPM5);

		Vector<AnalyticProduct> calibrationProductsJPMNOACCFEE = new Vector<AnalyticProduct>();
		calibrationProductsJPMNOACCFEE.add(cds_JPM_NOACCFEE1);
		calibrationProductsJPMNOACCFEE.add(cds_JPM_NOACCFEE2);
		calibrationProductsJPMNOACCFEE.add(cds_JPM_NOACCFEE3);
		calibrationProductsJPMNOACCFEE.add(cds_JPM_NOACCFEE4);
		calibrationProductsJPMNOACCFEE.add(cds_JPM_NOACCFEE5);



		// A model is a collection of curves (curves and products find other curves by looking up their name in the model)
		AnalyticModelFromCurvesAndVols modelDISCRETE = new AnalyticModelFromCurvesAndVols(new CurveInterpolation[] { discountCurveInterpolation, survivalProbabilityCurve, recoveryRateCurve });
		AnalyticModelFromCurvesAndVols modelJPM = new AnalyticModelFromCurvesAndVols(new CurveInterpolation[] { discountCurveInterpolation, survivalProbabilityCurve, recoveryRateCurve });
		AnalyticModelFromCurvesAndVols modelJPMNOACCFEE = new AnalyticModelFromCurvesAndVols(new CurveInterpolation[] { discountCurveInterpolation, survivalProbabilityCurve, recoveryRateCurve });

		// Create a collection of curves to calibrate
		Set<ParameterObject> curvesToCalibrate = new HashSet<ParameterObject>();
		curvesToCalibrate.add(survivalProbabilityCurve);

		// Calibrate the curve (All CDS should be valued at 0 initially ~ conventional CDS spread or with upfront)
		ArrayList<Double> targetList = new ArrayList<Double>();
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
		Solver solverDISCRETE = new Solver(modelDISCRETE, calibrationProductsDISCRETE, targetList, 0.0, errorTolerance);
		AnalyticModel calibratedModelDISCRETE = solverDISCRETE.getCalibratedModel(curvesToCalibrate);

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
			AnalyticProduct	calibrationProduct		= calibrationProductsDISCRETE.get(calibrationProductIndex);
			double						calibrationProductValue	= calibrationProduct.getValue(evaluationTime, calibratedModelDISCRETE);

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
		Solver solverJPM = new Solver(modelJPM, calibrationProductsJPM, targetList, 0.0, errorTolerance);
		AnalyticModel calibratedModelJPM = solverJPM.getCalibratedModel(curvesToCalibrate);

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
			AnalyticProduct	calibrationProduct		= calibrationProductsJPM.get(calibrationProductIndex);
			double						calibrationProductValue	= calibrationProduct.getValue(evaluationTime, calibratedModelJPM);

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
		Solver solverJPM_NOACCFEE = new Solver(modelJPMNOACCFEE, calibrationProductsJPMNOACCFEE, targetList, 0.0, errorTolerance);
		AnalyticModel calibratedModelJPM_NOACCFEE = solverJPM_NOACCFEE.getCalibratedModel(curvesToCalibrate);

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
			AnalyticProduct	calibrationProduct		= calibrationProductsJPMNOACCFEE.get(calibrationProductIndex);
			double						calibrationProductValue	= calibrationProduct.getValue(evaluationTime, calibratedModelJPM_NOACCFEE);

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

