/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.marketdata.model.volatilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.threeten.bp.LocalDate;

import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.CurveInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;

/**
 * A very simple container for Caplet volatilities.
 *
 * It performs piecewise constant interpolation (discretization) in maturity dimension on iso-moneyness lines
 * and uses the default interpolation from the Curve class in strike dimension.
 *
 * It allows to convert from several quoting conventions.
 *
 * It needs a forward curve and a discount curve. The tenor length of the Caplet is inferred
 * from the forward curve.
 *
 * @TODO: Need to add forward and discount curve to support implied vol.
 * @author Christian Fries
 */
public class CapletVolatilities extends AbstractVolatilitySurface {

	private Map<Double, CurveInterface>	capletVolatilities = new HashMap<Double, CurveInterface>();

	private transient Double[]	maturities;
	private transient Object		lazyInitLock = new Object();

	/**
	 * @param name The name of this volatility surface.
	 * @param referenceDate The reference date for this volatility surface, i.e., the date which defined t=0.
	 * @param forwardCurve The underlying forward curve.
	 * @param maturities The vector of maturities of the quotes.
	 * @param strikes The vector of strikes of the quotes.
	 * @param volatilities The vector of volatilities of the quotes.
	 * @param volatilityConvention The quoting convention of the volatilities provided.
	 * @param discountCurve The associated discount curve.
	 */
	public CapletVolatilities(String name, LocalDate referenceDate, ForwardCurveInterface forwardCurve,
			double[] maturities,
			double[] strikes,
			double[] volatilities,
			QuotingConvention volatilityConvention,
			DiscountCurveInterface discountCurve)  {
		super(name, referenceDate);
		this.forwardCurve = forwardCurve;
		this.discountCurve = discountCurve;
		this.quotingConvention = volatilityConvention;

		if(maturities.length != strikes.length || maturities.length != volatilities.length) {
			throw new IllegalArgumentException("Length of vectors is not equal.");
		}

		for(int i=0; i<volatilities.length; i++) {
			double maturity		= maturities[i];
			double strike		= strikes[i];
			double volatility	= volatilities[i];
			add(maturity, strike, volatility);
		}
	}

	/**
	 * Private constructor for empty surface, to add points to it.
	 *
	 * @param name The name of this volatility surface.
	 * @param referenceDate The reference date for this volatility surface, i.e., the date which defined t=0.
	 */
	private CapletVolatilities(String name, LocalDate referenceDate) {
		super(name, referenceDate);
	}

	/**
	 * @param maturity
	 * @param strike
	 * @param volatility
	 */
	private void add(double maturity, double strike, double volatility) {
		CurveInterface curve = capletVolatilities.get(maturity);
		try {
			if(curve == null) {
				curve = (new Curve.CurveBuilder()).addPoint(strike, volatility, true).build();
			} else {
				curve = curve.getCloneBuilder().addPoint(strike, volatility, true).build();
			}
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("Unable to build curve.");
		}
		synchronized (lazyInitLock) {
			capletVolatilities.put(maturity, curve);
			maturities = null;
		}
	}

	@Override
	public double getValue(double maturity, double strike, VolatilitySurfaceInterface.QuotingConvention quotingConvention) {
		return getValue(null, maturity, strike, quotingConvention);
	}

	@Override
	public double getValue(AnalyticModelInterface model, double maturity, double strike, VolatilitySurfaceInterface.QuotingConvention quotingConvention) {
		if(maturity == 0) {
			return 0;
		}

		double value;
		if(capletVolatilities.containsKey(maturity)) {
			value			= capletVolatilities.get(maturity).getValue(strike);
		}
		else {
			synchronized (lazyInitLock) {
				if(maturities == null) {
					maturities = capletVolatilities.keySet().toArray(new Double[0]);
				}
				Arrays.sort(maturities);
			}

			int maturityGreaterEqualIndex = Arrays.binarySearch(maturities, maturity);
			if(maturityGreaterEqualIndex < 0) {
				maturityGreaterEqualIndex = -maturityGreaterEqualIndex-1;
			}
			if(maturityGreaterEqualIndex > maturities.length-1) {
				maturityGreaterEqualIndex = maturities.length-1;
			}

			double maturityGreaterOfEqual	= maturities[maturityGreaterEqualIndex];

			// @TODO: Below we should trigger an exception if no forwardCurve is supplied but needed.
			// Interpolation / extrapolation is performed on iso-moneyness lines.
			double adjustedStrike = forwardCurve.getValue(model, maturityGreaterOfEqual) + (strike - forwardCurve.getValue(model, maturity));

			value			= capletVolatilities.get(maturityGreaterOfEqual).getValue(adjustedStrike);
		}

		return convertFromTo(model, maturity, strike, value, this.quotingConvention, quotingConvention);
	}

	public static AbstractVolatilitySurface fromFile(File inputFile) throws FileNotFoundException {
		// Read data
		BufferedReader		dataStream	= new BufferedReader(new InputStreamReader(new FileInputStream(inputFile)));
		ArrayList<String>	datasets	= new ArrayList<String>();
		try {
			while(true) {
				String line = dataStream.readLine();

				// Check for end of file
				if(line == null) {
					break;
				}

				// Ignore non caplet data
				if(!line.startsWith("caplet\t")) {
					continue;
				}

				datasets.add(line);
			}
			dataStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// @TODO: Name and reference date have to be set?!
		CapletVolatilities capletVolatilities = new CapletVolatilities(null, null);

		// Parse data
		for(int datasetIndex=0; datasetIndex<datasets.size(); datasetIndex++) {
			StringTokenizer stringTokenizer = new StringTokenizer(datasets.get(datasetIndex),"\t");

			try {
				// Skip identifier
				stringTokenizer.nextToken();
				double maturity			= Double.parseDouble(stringTokenizer.nextToken());
				double strike			= Double.parseDouble(stringTokenizer.nextToken());
				double capletVolatility	= Double.parseDouble(stringTokenizer.nextToken());
				capletVolatilities.add(maturity, strike, capletVolatility);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}

		return capletVolatilities;
	}

	private void readObject(java.io.ObjectInputStream in) throws ClassNotFoundException, IOException {
		in.defaultReadObject();
		// initialization of transients
		lazyInitLock = new Object();
	}
}

