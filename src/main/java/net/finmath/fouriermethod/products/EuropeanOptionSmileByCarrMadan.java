package net.finmath.fouriermethod.products;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.CharacteristicFunction;
import net.finmath.fouriermethod.models.CharacteristicFunctionModel;
import net.finmath.interpolation.RationalFunctionInterpolation;
import net.finmath.interpolation.RationalFunctionInterpolation.ExtrapolationMethod;
import net.finmath.interpolation.RationalFunctionInterpolation.InterpolationMethod;

/**
 * This class computes the prices of a collection of call options for a fixed maturity and a family of strikes.
 *
 * The pricing method is the FFT methodology as introduced in Carr and Madan (1999).
 * The transform is taken for -1 <\alpha<0, hence we have a correction term since we are applying the residue theorem as reported in Lee (2004).
 * In this strip the transform of any meaningful financial model is well defined because we expect the following conditions to be satisfied:
 *
 * 1) When computed in the point z=-i, the discounted characteristic function gives us the initial asset price due to the martingale property.
 * 2) By definition of characteristic function also z = 0 is a good point.
 *
 * Analytic extension over the strip is then guaranteed by Lukacs (1970), Theorem 7.1.1.
 *
 * From a financial point of view the choice of this strip corresponds to transforming a covered call position.
 *
 * References:
 *<p><ul>
 * <li> Carr. P. and Madan, D. (1999) Option Valuation Using the Fast Fourier Transform. Journal of Computational Finance.
 * <li> Lee, R. (2004) Option pricing by transform methods: extensions, unification and error control. Journal of Computational Finance.
 * <li> Lewis, A. (2002) A simple option formula for general jump diffusion and other exponential Levy processes.
 * <li> Lukacks, E. (1970) Characteristic Functions. 2nd edition.
 *</p></ul>
 * @author Alessandro Gnoatto
 *
 */
public class EuropeanOptionSmileByCarrMadan extends EuropeanOptionSmile{
	//Fields
	private final int numberOfPoints;
	private final double gridSpacing;
	private final InterpolationMethod intMethod;
	private final ExtrapolationMethod extMethod;

	//Constructors
	public EuropeanOptionSmileByCarrMadan(double maturity, double[] strikes) {
		super(maturity, strikes);
		this.numberOfPoints = 4096;
		this.gridSpacing = 0.1;
		this.intMethod =InterpolationMethod.HARMONIC_SPLINE;
		this.extMethod = ExtrapolationMethod.CONSTANT;
	}

	public EuropeanOptionSmileByCarrMadan(String underlyingName, double maturity, double[] strikes) {
		super(underlyingName, maturity, strikes);
		this.numberOfPoints = 4096;
		this.gridSpacing = 0.1;
		this.intMethod =InterpolationMethod.HARMONIC_SPLINE;
		this.extMethod = ExtrapolationMethod.CONSTANT;
	}


	public EuropeanOptionSmileByCarrMadan(String underlyingName, double maturity, double[] strikes, int numberOfPoints,
			double gridSpacing, InterpolationMethod intMethod, ExtrapolationMethod extMethod) {
		super(underlyingName, maturity, strikes);
		this.numberOfPoints = numberOfPoints;
		this.gridSpacing = gridSpacing;
		this.intMethod = intMethod;
		this.extMethod = extMethod;
	}

	public Map<String, Function<Double, Double>> getValue(double evaluationTime, CharacteristicFunctionModel model) throws CalculationException {

		CharacteristicFunction modelCF = model.apply(getMaturity());

		final double lineOfIntegration = 0.5 * (getIntegrationDomainImagUpperBound()+getIntegrationDomainImagLowerBound());

		double lambda = 2*Math.PI/(numberOfPoints*gridSpacing); //Equation 23 Carr and Madan
		double upperBound = (numberOfPoints * lambda)/2.0; //Equation 20 Carr and Madan

		Complex[] integrandEvaluations = new Complex[numberOfPoints];

		for(int i = 0; i<numberOfPoints; i++) {

			double u = gridSpacing * i;

			//Integration over a line parallel to the real axis
			Complex z = new Complex(u,-lineOfIntegration);

			//The characteristic function is already discounted
			Complex numerator = modelCF.apply(z.subtract(Complex.I));

			Complex denominator = apply(z);
			Complex ratio = numerator.divide(denominator);
			ratio = (ratio.multiply(((Complex.I).multiply(upperBound*u)).exp())).multiply(gridSpacing);

			double delta;
			if (i==0){
				delta=1.0;
			}else{
				delta = 0.0;
			}
			double simpsonWeight = (3+Math.pow(-1,i+1)-delta)/3;

			integrandEvaluations[i] = ratio.multiply(simpsonWeight);
		}

		//Compute the FFT
		Complex[] transformedVector = new Complex[numberOfPoints];
		FastFourierTransformer fft=new FastFourierTransformer(DftNormalization.STANDARD);
		transformedVector=	fft.transform(integrandEvaluations,TransformType.FORWARD);

		//Find relevant prices via interpolation
		double[] logStrikeVector = new double[numberOfPoints];
		double[] strikeVector = new double[numberOfPoints];
		double[] optionPriceVector = new double[numberOfPoints];

		for(int j = 0; j<numberOfPoints; j++) {
			logStrikeVector[j] = -upperBound+lambda*j;
			strikeVector[j] = Math.exp(logStrikeVector[j]);
			optionPriceVector[j] = (transformedVector[j].multiply(Math.exp(-lineOfIntegration * logStrikeVector[j]))).getReal()/Math.PI;
		}

		RationalFunctionInterpolation interpolation = new RationalFunctionInterpolation(strikeVector, optionPriceVector,intMethod, extMethod);

		Complex minusI = new Complex(0,-1);
		double residueTerm = (modelCF.apply(minusI)).getReal();

		Function<Double, Double> strikeToPrice = new Function<Double, Double>(){

			@Override
			public Double apply(Double t) {
				return residueTerm + interpolation.getValue(t);
			}

		};

		HashMap<String, Function<Double, Double>> results = new HashMap<String, Function<Double, Double>>();
		results.put("valuePerStrike", strikeToPrice);
		return results;
	}

	@Override
	public EuropeanOptionSmile getCloneWithModifiedParameters(double maturity, double[] strikes) {
		return new EuropeanOptionSmileByCarrMadan(maturity, strikes);
	}

}
