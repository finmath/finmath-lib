/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.marketdata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.CurveInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;

/**
 * A very simple container for caplet volatilities.
 * 
 * It allows to convert from several quoting conventions.
 * 
 * It needs a forward curve and a discount curve. The tenor length of the caplet is inferred
 * from the forward curve.
 * 
 * @author Christian Fries
 */
public class CapletVolatilities {

	public enum QuotingConvention {
		VOLATILITYLOGNORMAL,
		VOLATILITYNORMAL,
		PRICE
	}
		
	private ForwardCurveInterface		forwardCurve;
	private DiscountCurveInterface		discountCurve;
	private Map<Double, CurveInterface>	capletVolatilities = new HashMap<Double, CurveInterface>();
	private QuotingConvention			quotingConvention;

	public CapletVolatilities(ForwardCurveInterface forwardCurve,
			double[] maturities,
			double[] strikes,
			double[] volatilities,
			QuotingConvention volatilityConvention,
			DiscountCurveInterface discountCurve)  {
		this.forwardCurve = forwardCurve;
		this.discountCurve = discountCurve;
		this.quotingConvention = volatilityConvention;
		
		for(int i=0; i<volatilities.length; i++) {
			double maturity		= maturities[i];
			double strike		= strikes[i];
			double volatility	= volatilities[i];
			this.add(maturity, strike, volatility);
		}
	}

	/**
	 * @param maturity
	 * @param strike
	 * @param volatility
	 */
	private void add(double maturity, double strike, double volatility) {
		CurveInterface curve = capletVolatilities.get(maturity);
		try {
			if(curve == null) curve = (new Curve.CurveBuilder()).addPoint(strike, volatility, true).build();
			else curve = curve.getCloneBuilder().addPoint(strike, volatility, true).build();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("Unable to buid curve.");
		}
		capletVolatilities.put(maturity, curve);
	}

	/**
	 * 
	 */
	public CapletVolatilities() {
		// TODO Auto-generated constructor stub
	}

	public double getValue(double maturity, double strike, QuotingConvention quotingConvention) {
		CurveInterface capletVolatilityCurve = capletVolatilities.get(maturity);
		double value = capletVolatilityCurve.getValue(strike);

		return convertFromTo(maturity, strike, value, this.quotingConvention, quotingConvention);
	}

	/**
	 * @param value
	 * @param fromQuotingConvention
	 * @param toQuotingConvention
	 * @return
	 */
	private double convertFromTo(double optionMaturity, double optionStrike, double value, QuotingConvention fromQuotingConvention, QuotingConvention toQuotingConvention) {

		if(fromQuotingConvention.equals(toQuotingConvention)) return value;
		
		double forward = forwardCurve.getForward(null, optionMaturity);
		double payoffUnit = discountCurve.getDiscountFactor(optionMaturity+forwardCurve.getPaymentOffset(optionMaturity)) * forwardCurve.getPaymentOffset(optionMaturity);
		
		if(toQuotingConvention.equals(QuotingConvention.PRICE) && fromQuotingConvention.equals(QuotingConvention.VOLATILITYLOGNORMAL)) {
			return AnalyticFormulas.blackScholesGeneralizedOptionValue(forward, value, optionMaturity, optionStrike, payoffUnit);
		}
		else if(toQuotingConvention.equals(QuotingConvention.PRICE) && fromQuotingConvention.equals(QuotingConvention.VOLATILITYNORMAL)) {
			return AnalyticFormulas.bachelierOptionValue(forward, value, optionMaturity, optionStrike, payoffUnit);
		}
		else if(toQuotingConvention.equals(QuotingConvention.VOLATILITYLOGNORMAL) && fromQuotingConvention.equals(QuotingConvention.PRICE)) {
			return AnalyticFormulas.blackScholesOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, value);
		}
		else if(toQuotingConvention.equals(QuotingConvention.VOLATILITYNORMAL) && fromQuotingConvention.equals(QuotingConvention.PRICE)) {
			return AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, value);
		}
		else {
			return convertFromTo(optionMaturity, optionStrike, convertFromTo(optionMaturity, optionStrike, value, fromQuotingConvention, QuotingConvention.PRICE), QuotingConvention.PRICE, toQuotingConvention);
		}
	}

	public static CapletVolatilities fromFile(File inputFile) throws FileNotFoundException {
		// Read data
        BufferedReader		dataStream	= new BufferedReader(new InputStreamReader(new FileInputStream(inputFile)));		
		ArrayList<String>	datasets	= new ArrayList<String>();
		try {
			while(true) {
				String line = dataStream.readLine();
	
				// Check for end of file
				if(line == null)	break;
	
				// Ignore non caplet data
				if(!line.startsWith("caplet\t")) continue;

				datasets.add(line);
			}
			dataStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		CapletVolatilities capletVolatilities = new CapletVolatilities();
		
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
}
