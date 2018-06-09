/*
 * Created on 10.02.2004
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORMarketModelInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModel;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModelFourParameterExponentialForm;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.time.TimeDiscretization;



/**
 * This class tests the some LIBOR market model and products. 
 *
 * @author Christian Fries
 */
public class InterestRateProductTest {

	static DecimalFormat formatterMaturity	= new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.ENGLISH));
	static DecimalFormat formatterPrice		= new DecimalFormat(" ##0.000%;-##0.000%", new DecimalFormatSymbols(Locale.ENGLISH));
	static DecimalFormat formatterDeviation	= new DecimalFormat(" 0.00000E00;-0.00000E00", new DecimalFormatSymbols(Locale.ENGLISH));
	
	private final LIBORModelMonteCarloSimulationInterface liborMarketModel;

	public static void main(String[] args) throws CalculationException {

        long start = System.currentTimeMillis();


        InterestRateProductTest interestRateProductTest = new InterestRateProductTest();
        
		// Run bond price testing on this model
        interestRateProductTest.testBond();
		
		// Run swaption price testing on this model
        interestRateProductTest.testSwap();

		// Run swaption price testing on this model
        interestRateProductTest.testSwaption();

        //		flexiCapTest(liborMarketModel);
        //		tarnPriceTest(liborMarketModel);

        long end = System.currentTimeMillis();
        System.out.println("Calculation Time: " + (double)(end-start)/1000.0 + "sec.");
	}

	public InterestRateProductTest() throws CalculationException {
		super();
		this.liborMarketModel = createLIBORMarketModel(10000 /* numberOfPaths */, 8, 0.02);
	}

	public static LIBORModelMonteCarloSimulationInterface createLIBORMarketModel(
			int numberOfPaths, int numberOfFactors, double correlationDecayParam) throws CalculationException {

		/*
		 * Create the libor tenor structure and the initial values
		 */
		double liborPeriodLength	= 0.5;
		double liborRateTimeHorzion	= 20.0;
		TimeDiscretization liborPeriodDiscretization = new TimeDiscretization(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		// Create the forward curve (initial value of the LIBOR market model)
		ForwardCurve forwardCurve = ForwardCurve.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {0.05, 0.05, 0.05, 0.05, 0.05}	/* forwards */,
				liborPeriodLength							/* tenor / period length */
				);

		/*
		 * Create a simulation time discretization
		 */
		double lastTime	= 20.0;
		double dt		= 0.5;

		TimeDiscretization timeDiscretization = new TimeDiscretization(0.0, (int) (lastTime / dt), dt);

		/*
		 * Create a volatility structure v[i][j] = sigma_j(t_i)
		 */
		double a = 0.2, b = 0.0, c = 0.25, d = 0.3;
		LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelFourParameterExponentialForm(timeDiscretization, liborPeriodDiscretization, a, b, c, d, false);		

		/*
		 * Create a correlation model rho_{i,j} = exp(-a * abs(T_i-T_j))
		 */
		LIBORCorrelationModelExponentialDecay correlationModel = new LIBORCorrelationModelExponentialDecay(
				timeDiscretization, liborPeriodDiscretization, numberOfFactors,
				correlationDecayParam);


		/*
		 * Combine volatility model and correlation model to a covariance model
		 */
		LIBORCovarianceModelFromVolatilityAndCorrelation covarianceModel =
				new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretization,
						liborPeriodDiscretization, volatilityModel, correlationModel);

		// BlendedLocalVolatlityModel (future extension)
		//		AbstractLIBORCovarianceModel covarianceModel2 = new BlendedLocalVolatlityModel(covarianceModel, 0.00, false);

		// Set model properties
		Map<String, String> properties = new HashMap<String, String>();

		// Choose the simulation measure
		properties.put("measure", LIBORMarketModel.Measure.SPOT.name());

		// Choose log normal model
		properties.put("stateSpace", LIBORMarketModel.StateSpace.LOGNORMAL.name());

		// Empty array of calibration items - hence, model will use given covariance
		LIBORMarketModel.CalibrationItem[] calibrationItems = new LIBORMarketModel.CalibrationItem[0];

		/*
		 * Create corresponding LIBOR Market Model
		 */
		LIBORMarketModelInterface liborMarketModel = new LIBORMarketModel(liborPeriodDiscretization, forwardCurve, new DiscountCurveFromForwardCurve(forwardCurve), covarianceModel, calibrationItems, properties);

		BrownianMotionInterface brownianMotion = new net.finmath.montecarlo.BrownianMotion(timeDiscretization, numberOfFactors, numberOfPaths, 3141 /* seed */);

		ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion, ProcessEulerScheme.Scheme.PREDICTOR_CORRECTOR);

		return new LIBORModelMonteCarloSimulation(liborMarketModel, process);
	}

	@Test
    public void testBond() throws CalculationException {
		/*
		 * Price a bond
		 */
        
		System.out.println("Bond prices:");
        System.out.println("Maturity      Simulation       Analytic        Deviation");
						
		for(int maturityIndex = 0; maturityIndex<=liborMarketModel.getNumberOfLibors(); maturityIndex++) {
			double maturity = liborMarketModel.getLiborPeriod(maturityIndex);
			System.out.print(formatterMaturity.format(maturity) + "          ");
			
			// Create a bond
			Bond	bond	= new Bond(maturity);
			
			// Bond price with Monte Carlo
			double priceOfBond		= bond.getValue(liborMarketModel);
			System.out.print(formatterPrice.format(priceOfBond) + "          ");

			// Bond price analytic
			double priceOfBondAnalytic = 1.0;
			
			double lastPeriodIndex = liborMarketModel.getLiborPeriodIndex(bond.getMaturity()) - 1;
			for(int periodIndex=0; periodIndex<=lastPeriodIndex; periodIndex++)
				priceOfBondAnalytic /= 1.0 + liborMarketModel.getLIBOR(0, periodIndex).doubleValue() * (liborMarketModel.getLiborPeriod(periodIndex+1) - liborMarketModel.getLiborPeriod(periodIndex));

			System.out.print(formatterPrice.format(priceOfBondAnalytic) + "          ");

			// Relative deviation
			double deviation = (priceOfBond - priceOfBondAnalytic);
			System.out.println(formatterDeviation.format(deviation));
		}

		System.out.println("");
	}
	
	@Test
    public void testSwaption() throws CalculationException {
        /*
         * Price a bond
         */
        System.out.println("Swaption prices:");
        System.out.println("Maturity      Simulation 1     Simulation 2     Analytic        Deviation");

        
               
        for(int maturityIndex = 1; maturityIndex<=liborMarketModel.getNumberOfLibors()-10; maturityIndex++) {
            
            double exerciseDate = liborMarketModel.getLiborPeriod(maturityIndex);
            System.out.print(formatterMaturity.format(exerciseDate) + "          ");

            int numberOfPeriods = 5;

            // Create a swaption
            
            double[] fixingDates      = new double[numberOfPeriods];
            double[] paymentDates     = new double[numberOfPeriods];
            double[] swapTenor        = new double[numberOfPeriods+1];
            double swapPeriodLength     = 1.0;
            
            for(int periodStartIndex=0; periodStartIndex<numberOfPeriods; periodStartIndex++) {
                fixingDates[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
                paymentDates[periodStartIndex] = exerciseDate + (periodStartIndex+1) * swapPeriodLength;
                swapTenor[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
            }
            swapTenor[numberOfPeriods] = exerciseDate + numberOfPeriods * swapPeriodLength;

            // Swaptions swap rate
            double swaprate = 0.05;//getParSwaprate(liborMarketModel, swapTenor);
            
            // Set swap rates for each period
            double[] swaprates        = new double[numberOfPeriods];
            for(int periodStartIndex=0; periodStartIndex<numberOfPeriods; periodStartIndex++) {
            	swaprates[periodStartIndex] = swaprate;
            }

            Swaption swaption1 = new Swaption(exerciseDate, fixingDates, paymentDates, swaprates);
            SwaptionAnalyticApproximation swaptionAnalyitc = new SwaptionAnalyticApproximation(swaprate, swapTenor, SwaptionAnalyticApproximation.ValueUnit.VALUE);
            SwaptionWithComponents swaption2 = new SwaptionWithComponents(exerciseDate, fixingDates, paymentDates, swaprates);
            
            // Price with Monte Carlo
            double priceSimulation1      = swaption1.getValue(liborMarketModel);
            System.out.print(formatterPrice.format(priceSimulation1) + "          ");

            // Price with Monte Carlo
            double priceSimulation2      = swaption2.getValue(liborMarketModel);
            System.out.print(formatterPrice.format(priceSimulation2) + "          ");

            // Price analytic
            double priceAnalytic      = swaptionAnalyitc.getValue(liborMarketModel);
            System.out.print(formatterPrice.format(priceAnalytic) + "          ");

            // Relative deviation
            double deviation1 = (priceSimulation1 - priceAnalytic);
            System.out.print(formatterDeviation.format(deviation1));

            // Relative deviation
            double deviation2 = (priceSimulation2 - priceAnalytic);
            System.out.println(formatterDeviation.format(deviation2));
       }
        System.out.println("");     
    }

	@Test
    public void testSwap() throws CalculationException {
        /*
         * Price a bond
         */
        System.out.println("Swap prices:");
        System.out.println("Maturity      Simulation 1     Simulation 2     Deviation");

        long start,end;
        
        long constructor1 = 0;
        long constructor2 = 0;
        long valuation1 = 0;
        long valuation2 = 0;
               
        for(int maturityIndex = 1; maturityIndex<=liborMarketModel.getNumberOfLibors()-10; maturityIndex++) {
            
            double exerciseDate = liborMarketModel.getLiborPeriod(maturityIndex);
            System.out.print(formatterMaturity.format(exerciseDate) + "          ");

            int numberOfPeriods = 5;

            // Create a swaption
            
            double[] fixingDates      = new double[numberOfPeriods];
            double[] paymentDates     = new double[numberOfPeriods];
            double[] swapTenor        = new double[numberOfPeriods+1];
            double swapPeriodLength     = 1.0;
            
            for(int periodStartIndex=0; periodStartIndex<numberOfPeriods; periodStartIndex++) {
                fixingDates[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
                paymentDates[periodStartIndex] = exerciseDate + (periodStartIndex+1) * swapPeriodLength;
                swapTenor[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
            }
            swapTenor[numberOfPeriods] = exerciseDate + numberOfPeriods * swapPeriodLength;

            // Swaptions swap rate
            double swaprate = 0.05;//getParSwaprate(liborMarketModel, swapTenor);
            
            // Set swap rates for each period
            double[] swaprates        = new double[numberOfPeriods];
            for(int periodStartIndex=0; periodStartIndex<numberOfPeriods; periodStartIndex++) {
            	swaprates[periodStartIndex] = swaprate;
            }

            start = System.currentTimeMillis();
            Swap swap1 = new Swap(fixingDates, paymentDates, swaprates);
            end = System.currentTimeMillis();
            constructor1 += end-start;
            
            start = System.currentTimeMillis();
            SwapWithComponents swap2 = new SwapWithComponents(fixingDates, paymentDates, swaprates);
            end = System.currentTimeMillis();
            constructor2 += end-start;
            
            // Price with Monte Carlo
            start = System.currentTimeMillis();
            double priceSimulation1      = swap1.getValue(liborMarketModel);
            end = System.currentTimeMillis();
            valuation1 += end-start;
            System.out.print(formatterPrice.format(priceSimulation1) + "          ");

            // Price analytic
            start = System.currentTimeMillis();
            double priceSimulation2      = swap2.getValue(liborMarketModel);
            end = System.currentTimeMillis();
            valuation2 += end-start;
            System.out.print(formatterPrice.format(priceSimulation2) + "          ");

            // Relative deviation
            double deviation = (priceSimulation1 - priceSimulation2);
            System.out.println(formatterDeviation.format(deviation));

       }
        System.out.println("1: Constructor:"+constructor1/1000.0+"sec. Valuation:"+valuation1/1000.0);
        System.out.println("2: Constructor:"+constructor2/1000.0+"sec. Valuation:"+valuation2/1000.0);
        System.out.println("");     
    }

    @SuppressWarnings("unused")
    private static double getParSwaprate(LIBORModelMonteCarloSimulationInterface liborMarketModel, double[] swapTenor) throws CalculationException {
    	double swapStart	= swapTenor[0];
    	double swapEnd		= swapTenor[swapTenor.length-1];
    	
    	int swapStartIndex	= liborMarketModel.getLiborPeriodIndex(swapStart);
    	int swapEndIndex	= liborMarketModel.getLiborPeriodIndex(swapEnd);
    	
    	// Calculate discount factors from model
    	double[] discountFactors = new double[swapEndIndex+1];
    	discountFactors[0] = 1.0;
    	for(int periodIndex=0; periodIndex<swapEndIndex; periodIndex++) {
            double libor = liborMarketModel.getLIBOR(0, periodIndex).doubleValue();
            double periodLength = liborMarketModel.getLiborPeriod(periodIndex+1) - liborMarketModel.getLiborPeriod(periodIndex);
    		discountFactors[periodIndex+1] = discountFactors[periodIndex] / (1.0 + libor * periodLength); 
    		
    	}
    	
    	// Calculate swap annuity from discount factors
    	double swapAnnuity = 0.0;
    	for(int swapPeriodIndex=0; swapPeriodIndex<swapTenor.length-1; swapPeriodIndex++) {
    		int periodEndIndex = liborMarketModel.getLiborPeriodIndex(swapTenor[swapPeriodIndex+1]);
    		swapAnnuity += discountFactors[periodEndIndex] * (swapTenor[swapPeriodIndex+1] - swapTenor[swapPeriodIndex]); 
    	}

    	// Calculate swaprate
    	double swaprate = (discountFactors[swapStartIndex] - discountFactors[swapEndIndex]) / swapAnnuity;;
    	
    	return swaprate;
	}

	public void flexiCapTest(LIBORModelMonteCarloSimulationInterface liborMarketModel) throws CalculationException {
		/*
		 * Price an flexi cap
		 */
		double	capFixingDates[]	= {0.5 , 1.0 , 1.5 };
		double	capPaymentDates[]	= {1.0 , 1.5 , 2.0 };
		double	capStrikes[]		= {0.05, 0.05, 0.02};
		int		maximumNumberOfExercises = 2;

		double priceOfFlexiCap	= (new FlexiCap(
				capFixingDates,
				capPaymentDates,
				capStrikes,
				maximumNumberOfExercises)).getValue(liborMarketModel);

		System.out.println("Price of flexi cap:" + formatterPrice.format(priceOfFlexiCap));

		/*
		 * Test the flexi cap
		 */
		
		// Open corresponding data file
		try {
			FileWriter out = new FileWriter(new File("FexiCap.data"));
			for(double strike=0; strike<0.2; strike+=0.0005)
			{
				capStrikes[1] = strike;
				FlexiCap flexiCap	= new FlexiCap(
						capFixingDates,
						capPaymentDates,
						capStrikes,
						maximumNumberOfExercises);
				double price	= flexiCap.getValue(liborMarketModel);

				out.write(strike + " " + price + "\n");
			}
			out.close();		
		} catch (IOException e) {
			System.out.println("Could not write FexiCap.data file.");
		}
	}
/*
	@Test
	public void testTarnPrice(LIBORModelMonteCarloSimulationInterface liborMarketModel) throws CalculationException {
        double[] periodLengths = { 0.5, 0.5, 0.5, 0.5, 0.5 };
        
		double[] fixingDates = { 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5 };
		double[] paymentDates = { 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0 };
		
		double targetCoupon = 0.05;
        TargetRedemptionNote product1 = new TargetRedemptionNote(fixingDates, paymentDates, 0.0, targetCoupon, 0.0, null, true, false);
        TargetRedemptionNote product2 = new TargetRedemptionNote(fixingDates, paymentDates, 0.0, targetCoupon, 0.0, new ConstantMaturitySwaprate(periodLengths), true, false);

        System.out.println("TARN:("+targetCoupon+"):"
					+ "\t" + product1.getValue(liborMarketModel)
					+ "\t" + product2.getValue(liborMarketModel)
        );
	}
*/
}