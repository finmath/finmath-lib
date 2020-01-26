package net.finmath.fouriermethod.products.smile;

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
 * The transform is taken for -1 &lt; \alpha &lt; 0, hence we have a correction term since we are applying the residue theorem as reported in Lee (2004).
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
 * <ul>
 *  <li> Carr. P. and Madan, D. (1999) Option Valuation Using the Fast Fourier Transform. Journal of Computational Finance.</li>
 *  <li> Lee, R. (2004) Option pricing by transform methods: extensions, unification and error control. Journal of Computational Finance.</li>
 *  <li> Lewis, A. (2002) A simple option formula for general jump diffusion and other exponential Levy processes.</li>
 *  <li> Lukacks, E. (1970) Characteristic Functions. 2nd edition.
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public class EuropeanOptionSmileByCarrMadan extends EuropeanOptionSmile{
	//Fields
	private final int numberOfPoints;
	private final double gridSpacing;
	private final InterpolationMethod intMethod;
	private final ExtrapolationMethod extMethod;

	//Constructors
	public EuropeanOptionSmileByCarrMadan(final double maturity, final double[] strikes) {
		super(maturity, strikes);
		numberOfPoints = 4096;
		gridSpacing = 0.1;
		intMethod =InterpolationMethod.HARMONIC_SPLINE;
		extMethod = ExtrapolationMethod.CONSTANT;
	}

	public EuropeanOptionSmileByCarrMadan(final String underlyingName, final double maturity, final double[] strikes) {
		super(underlyingName, maturity, strikes);
		numberOfPoints = 4096;
		gridSpacing = 0.1;
		intMethod =InterpolationMethod.HARMONIC_SPLINE;
		extMethod = ExtrapolationMethod.CONSTANT;
	}


	public EuropeanOptionSmileByCarrMadan(final String underlyingName, final double maturity, final double[] strikes, final int numberOfPoints,
			final double gridSpacing, final InterpolationMethod intMethod, final ExtrapolationMethod extMethod) {
		super(underlyingName, maturity, strikes);
		this.numberOfPoints = numberOfPoints;
		this.gridSpacing = gridSpacing;
		this.intMethod = intMethod;
		this.extMethod = extMethod;
	}

	@Override
	public Map<String, Function<Double, Double>> getValue(final double evaluationTime, final CharacteristicFunctionModel model) throws CalculationException {

		final CharacteristicFunction modelCF = model.apply(getMaturity());

		final double lineOfIntegration = 0.5 * (getIntegrationDomainImagUpperBound()+getIntegrationDomainImagLowerBound());

		final double lambda = 2*Math.PI/(numberOfPoints*gridSpacing); //Equation 23 Carr and Madan
		final double upperBound = (numberOfPoints * lambda)/2.0; //Equation 20 Carr and Madan

		final Complex[] integrandEvaluations = new Complex[numberOfPoints];

		for(int i = 0; i<numberOfPoints; i++) {

			final double u = gridSpacing * i;

			//Integration over a line parallel to the real axis
			final Complex z = new Complex(u,-lineOfIntegration);

			//The characteristic function is already discounted
			final Complex numerator = modelCF.apply(z.subtract(Complex.I));

			final Complex denominator = apply(z);
			Complex ratio = numerator.divide(denominator);
			ratio = (ratio.multiply(((Complex.I).multiply(upperBound*u)).exp())).multiply(gridSpacing);

			double delta;
			if (i==0){
				delta=1.0;
			}else{
				delta = 0.0;
			}
			final double simpsonWeight = (3+Math.pow(-1,i+1)-delta)/3;

			integrandEvaluations[i] = ratio.multiply(simpsonWeight);
		}

		//Compute the FFT
		Complex[] transformedVector = new Complex[numberOfPoints];
		final FastFourierTransformer fft=new FastFourierTransformer(DftNormalization.STANDARD);
		transformedVector=	fft.transform(integrandEvaluations,TransformType.FORWARD);

		//Find relevant prices via interpolation
		final double[] logStrikeVector = new double[numberOfPoints];
		final double[] strikeVector = new double[numberOfPoints];
		final double[] optionPriceVector = new double[numberOfPoints];

		for(int j = 0; j<numberOfPoints; j++) {
			logStrikeVector[j] = -upperBound+lambda*j;
			strikeVector[j] = Math.exp(logStrikeVector[j]);
			optionPriceVector[j] = (transformedVector[j].multiply(Math.exp(-lineOfIntegration * logStrikeVector[j]))).getReal()/Math.PI;
		}

		final RationalFunctionInterpolation interpolation = new RationalFunctionInterpolation(strikeVector, optionPriceVector,intMethod, extMethod);

		final Complex minusI = new Complex(0,-1);
		final double residueTerm = (modelCF.apply(minusI)).getReal();

		final Function<Double, Double> strikeToPrice = new Function<Double, Double>(){

			@Override
			public Double apply(final Double t) {
				return residueTerm + interpolation.getValue(t);
			}

		};

		final HashMap<String, Function<Double, Double>> results = new HashMap<>();
		results.put("valuePerStrike", strikeToPrice);
		return results;
	}

	@Override
	public EuropeanOptionSmile getCloneWithModifiedParameters(final double maturity, final double[] strikes) {
		return new EuropeanOptionSmileByCarrMadan(maturity, strikes);
	}

}
