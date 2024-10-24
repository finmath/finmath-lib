package net.finmath.climate.models.dice;

import java.util.Arrays;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;

import net.finmath.climate.models.AbatementModel;
import net.finmath.climate.models.ClimateModel;
import net.finmath.climate.models.SavingsRateModel;
import net.finmath.climate.models.dice.submodels.AbatementCostFunction;
import net.finmath.climate.models.dice.submodels.CarbonConcentration3DScalar;
import net.finmath.climate.models.dice.submodels.DamageFromTemperature;
import net.finmath.climate.models.dice.submodels.EmissionExternalFunction;
import net.finmath.climate.models.dice.submodels.EvolutionOfCapital;
import net.finmath.climate.models.dice.submodels.EvolutionOfCarbonConcentration;
import net.finmath.climate.models.dice.submodels.EvolutionOfEmissionIndustrialIntensity;
import net.finmath.climate.models.dice.submodels.EvolutionOfPopulation;
import net.finmath.climate.models.dice.submodels.EvolutionOfProductivity;
import net.finmath.climate.models.dice.submodels.EvolutionOfTemperature;
import net.finmath.climate.models.dice.submodels.ForcingExternalFunction;
import net.finmath.climate.models.dice.submodels.ForcingFunction;
import net.finmath.climate.models.dice.submodels.Temperature2DScalar;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;

/**
 * A simulation of a simplified DICE model (see <code>net.finmath.climate.models.dice.DICEModelTest</code> in src/test/java) for an example usage.
 *
 * The model just composes the sub-models (evolution equations and functions) from the package {@link net.finmath.climate.models.dice.submodels}.
 *
 * Note: The code uses exponential discounting.
 */
public class DICEModel implements ClimateModel {

	private static Logger logger = Logger.getLogger("net.finmath.climate");

	/*
	 * Input to this class
	 */
	private final TimeDiscretization timeDiscretization;
	private final UnaryOperator<Double> abatementFunction;
	private final UnaryOperator<Double> savingsRateFunction;
	private final double discountRate;

	/*
	 * Simulated values - stored for plotting ande analysis
	 */
	private Temperature2DScalar[] temperature;
	private CarbonConcentration3DScalar[] carbonConcentration;
	private double[] gdp;
	private double[] emission;
	private double[] abatement;
	private double[] abatementCosts;
	private double[] damage;
	private double[] damageCosts;
	private double[] capital;
	private double[] population;
	private double[] productivity;
	private double[] consumptions;
	private double[] welfare;
	private double[] value;

	/**
	 * Create the model.
	 * 
	 * @param timeDiscretization The time discretization to be used.
	 * @param abatementFunction Abatement function \( t \mapsto \mu(t) \)
	 * @param savingsRateFunction Savings rate function \( t \mapsto s(t) \)
	 * @param discountRate Constant exponential disocunt rate r
	 * @param modelProperties A key value map of optional model properties or parameters.
	 */
	public DICEModel(TimeDiscretization timeDiscretization, UnaryOperator<Double> abatementFunction, UnaryOperator<Double> savingsRateFunction, double discountRate, Map<String, Object> modelProperties) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.abatementFunction = abatementFunction;
		this.savingsRateFunction = savingsRateFunction;
		this.discountRate = discountRate;

		int numberOfTimes = this.timeDiscretization.getNumberOfTimes();

		temperature = new Temperature2DScalar[numberOfTimes];
		carbonConcentration = new CarbonConcentration3DScalar[numberOfTimes];
		gdp = new double[numberOfTimes];
		emission = new double[numberOfTimes];
		abatement = new double[numberOfTimes];
		abatementCosts = new double[numberOfTimes];
		damage = new double[numberOfTimes];
		damageCosts = new double[numberOfTimes];
		capital = new double[numberOfTimes];
		population = new double[numberOfTimes];
		productivity = new double[numberOfTimes];
		consumptions = new double[numberOfTimes];
		welfare = new double[numberOfTimes];
		value = new double[numberOfTimes];

		this.init(modelProperties);
	}

	public DICEModel(TimeDiscretization timeDiscretization, UnaryOperator<Double> abatementFunction, UnaryOperator<Double> savingsRateFunction, double discountRate) {
		this(timeDiscretization, abatementFunction, savingsRateFunction, discountRate, Map.of());
	}

	public DICEModel(TimeDiscretization timeDiscretization, UnaryOperator<Double> abatementFunction) {
		this(timeDiscretization, abatementFunction, t -> 0.259029014481802, 0.03);
	}

	private void init(Map<String, Object> modelProperties) {

		Predicate<Integer> isTimeIndexToShift = (Predicate<Integer>) modelProperties.getOrDefault("isTimeIndexToShift", (Predicate<Integer>) i -> true);
		double initialEmissionShift = (double) modelProperties.getOrDefault("initialEmissionShift", 0.0);
		double initialConsumptionShift = (double) modelProperties.getOrDefault("initialConsumptionShift", 0.0);

		final double timeStep = timeDiscretization.getTimeStep(0);

		/*
		 * Building the model by composing the different functions
		 */

		/*
		 * Note: Calling constructors without additional arguments will use default arguments consistent with the 2016 original model.
		 */

		/*
		 * State vectors initial values
		 */
		final Temperature2DScalar temperatureInitial = new Temperature2DScalar(0.85, 0.0068);
		final CarbonConcentration3DScalar carbonConcentrationInitial = new CarbonConcentration3DScalar(851, 460, 1740);	// Level of Carbon (GtC)

		/*
		 * Sub-Modules: functional dependencies and evolution
		 */

		// Model that describes the damage on the GBP as a function of the temperature-above-normal
		final DoubleUnaryOperator damageFunction = new DamageFromTemperature();

		final EvolutionOfEmissionIndustrialIntensity emissionIndustrialIntensityFunction = new EvolutionOfEmissionIndustrialIntensity(timeDiscretization);

		final Function<Double, Double> emissionExternalFunction = new EmissionExternalFunction();

		final EvolutionOfCarbonConcentration evolutionOfCarbonConcentration = new EvolutionOfCarbonConcentration(timeDiscretization);

		final ForcingFunction forcingFunction = new ForcingFunction();
		final ForcingExternalFunction forcingExternalFunction = new ForcingExternalFunction();

		final EvolutionOfTemperature evolutionOfTemperature = new EvolutionOfTemperature(timeDiscretization);

		// Abatement
		final AbatementCostFunction abatementCostFunction = new AbatementCostFunction();

		/*
		 * GDP - Economics
		 */
		final double K0 = 223;		// Initial Capital
		final double L0 = 7403;		// Initial Population (world in million)
		final double A0 = 5.115;		// Initial Total Factor of Productivity
		final double gamma = 0.3;		// Capital Elasticity in Production Function
		final double gdpInitial = A0*Math.pow(K0,gamma)*Math.pow(L0/1000,1-gamma);

		// Capital
		final EvolutionOfCapital evolutionOfCapital = new EvolutionOfCapital(timeDiscretization);

		// Population
		final EvolutionOfPopulation evolutionOfPopulation = new EvolutionOfPopulation(timeDiscretization);

		// Productivity
		final EvolutionOfProductivity evolutionOfProductivity = new EvolutionOfProductivity(timeDiscretization);

		/*
		 * Set initial values
		 */
		temperature[0] = temperatureInitial;
		carbonConcentration[0] = carbonConcentrationInitial;
		gdp[0] = gdpInitial;
		capital[0] = K0;
		population[0] = L0;
		productivity[0] = A0;
		double utilityDiscountedSum = 0;
		//Emission intensity initial value, sigma(0) = e0/q0
		double emissionIntensity = 35.85/105.5;

		/*
		 * Evolve
		 */
		for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			double time = timeDiscretization.getTime(timeIndex);

			/*
			 * Evolve geo-physical quantities i -> i+1 (as a function of gdp[i])
			 */

			// Abatement
			abatement[timeIndex] = abatementFunction.apply(timeDiscretization.getTime(timeIndex));

			// Carbon
			double emissionIndustrial = emissionIntensity/(1-abatement[0]) * gdp[timeIndex];
			double emissionExternal = emissionExternalFunction.apply(time);
			emission[timeIndex] = (1 - abatement[timeIndex]) * emissionIndustrial + emissionExternal;

			// Allow for an external shift to the emissions (e.g. to calculate SCC).
			emission[timeIndex] += isTimeIndexToShift.test(timeIndex) ? initialEmissionShift : 0.0;

			carbonConcentration[timeIndex+1] = evolutionOfCarbonConcentration.apply(timeIndex, carbonConcentration[timeIndex], emission[timeIndex]);

			// Temperature
			double forcingExternal = forcingExternalFunction.apply(time+timeStep);
			final double forcing = forcingFunction.apply(carbonConcentration[timeIndex+1], forcingExternal);
			temperature[timeIndex+1] = evolutionOfTemperature.apply(timeIndex, temperature[timeIndex], forcing);

			/*
			 * Cost
			 */

			damage[timeIndex] = damageFunction.applyAsDouble(temperature[timeIndex].getExpectedTemperatureOfAtmosphere());

			double damageCostAbsolute = damage[timeIndex] * gdp[timeIndex];
			damageCosts[timeIndex] = damageCostAbsolute;
			
			double abatementCostAbsolute = abatementCostFunction.apply(time, abatement[timeIndex]) * emissionIndustrial;
			abatementCosts[timeIndex] = abatementCostAbsolute;

			/*
			 * Evolve economy i -> i+1 (as a function of temperature[i])
			 */

			// Remaining gdp
			double gdpNet = gdp[timeIndex] - damageCostAbsolute - abatementCostAbsolute;

			/*
			 * Equivalent (alternative way) to calculate the abatement
			 */
			double abatementCost = abatementCostFunction.apply(time, abatement[timeIndex]) * emissionIntensity/(1-abatement[0]);
			double gdpNet2 = gdp[timeIndex] * (1-damage[timeIndex] - abatementCost);
			if(Math.abs(gdpNet2-gdpNet)/(1+Math.abs(gdpNet)) > 1E-10) logger.warning("Calculation of relative and absolute net GDP does not match.");

			// Evolve emission intensity
			emissionIntensity = emissionIndustrialIntensityFunction.apply(timeIndex, emissionIntensity);


			// Constant from the original model - in the original model this is a time varying control variable.
			double savingsRate = savingsRateFunction.apply(time);	//0.259029014481802;

			double consumption = (1-savingsRate) * gdpNet;
			double investment = savingsRate * gdpNet;
			
			// Allow for an external shift to the emissions (e.g. to calculate SCC).
			consumption += isTimeIndexToShift.test(timeIndex) ? initialConsumptionShift : 0.0;
			consumptions[timeIndex] = consumption;
			
			capital[timeIndex+1] = evolutionOfCapital.apply(timeIndex).apply(capital[timeIndex], investment);

			/*
			 * Evolve population and productivity for next GDP
			 */
			population[timeIndex+1] = evolutionOfPopulation.apply(timeIndex).apply(population[timeIndex]);
			productivity[timeIndex+1] = evolutionOfProductivity.apply(timeIndex).apply(productivity[timeIndex]);

			double L = population[timeIndex+1];
			double A = productivity[timeIndex+1];
			gdp[timeIndex+1] = A*Math.pow(capital[timeIndex+1],gamma)*Math.pow(L/1000,1-gamma);

			/*
			 * Calculate utility
			 */
			double alpha = 1.45;           // Elasticity of marginal utility of consumption (GAMS elasmu)
			double C = consumption;
			// U = L * [ ( (1000*C/L)^(1-alpha) - 1)/(1-alpha) - 1 ]
			double utility = population[timeIndex]*( (Math.pow(1000*C/(population[timeIndex]),1-alpha)-1) /(1-alpha)-1 );

			/*
			 * Discounted utility
			 */
			double discountFactor = Math.exp(- discountRate * time);
			welfare[timeIndex] = utility * discountFactor;

			utilityDiscountedSum = utilityDiscountedSum + utility*discountFactor*timeStep;
			value[timeIndex+1] = utilityDiscountedSum;
		}
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
		return timeDiscretization;
	}

	@Override
	public RandomVariable getTemperature(double time) {
		return Scalar.of(temperature[timeDiscretization.getTimeIndex(time)].getExpectedTemperatureOfAtmosphere());
	}

	@Override
	public RandomVariable getValue() {
		return Scalar.of(value[value.length-1]);
	}

	@Override
	public RandomVariable[] getValues() {
		return Arrays.stream(value).mapToObj(Scalar::of).toArray(RandomVariable[]::new);
	}

	@Override
	public RandomVariable[] getAbatement() {
		return Arrays.stream(abatement).mapToObj(Scalar::of).toArray(RandomVariable[]::new);
	}

	@Override
	public RandomVariable[] getEmission() {
		return Arrays.stream(emission).mapToObj(Scalar::of).toArray(RandomVariable[]::new);
	}

	@Override
	public net.finmath.climate.models.CarbonConcentration[] getCarbonConcentration() {
		return carbonConcentration;
	}

	@Override
	public net.finmath.climate.models.Temperature[] getTemperature() {
		return temperature;
	}

	@Override
	public RandomVariable[] getDamage() {
		return Arrays.stream(damage).mapToObj(Scalar::of).toArray(RandomVariable[]::new);
	}

	@Override
	public RandomVariable[] getGDP() {
		return Arrays.stream(gdp).mapToObj(Scalar::of).toArray(RandomVariable[]::new);
	}

	@Override
	public RandomVariable[] getConsumptions() {
		return Arrays.stream(consumptions).mapToObj(Scalar::of).toArray(RandomVariable[]::new);
	}

	@Override
	public RandomVariable[] getAbatementCosts() {
		return Arrays.stream(abatementCosts).mapToObj(Scalar::of).toArray(RandomVariable[]::new);
	}

	@Override
	public RandomVariable getAbatementCost() {
		double abatementCost = 0.0;
		for(int timeIndex = 0; timeIndex < timeDiscretization.getNumberOfTimes(); timeIndex++) {
			abatementCost += abatementCosts[timeIndex] * Math.exp(- discountRate * timeDiscretization.getTime(timeIndex));
		}		
		return Scalar.of(abatementCost);
	}

	@Override
	public RandomVariable[] getDamageCosts() {
		return Arrays.stream(damageCosts).mapToObj(Scalar::of).toArray(RandomVariable[]::new);
	}

	@Override
	public RandomVariable getDamageCost() {
		double damageCost = 0.0;
		for(int timeIndex = 0; timeIndex < timeDiscretization.getNumberOfTimes(); timeIndex++) {
			damageCost += damageCosts[timeIndex] * Math.exp(- discountRate * timeDiscretization.getTime(timeIndex));
		}		
		return Scalar.of(damageCost);
	}

	@Override
	public RandomVariable getNumeraire(double time) {
		return Scalar.of(Math.exp(- discountRate * time));
	}

	@Override
	public AbatementModel getAbatementModel() {
		return (AbatementModel)abatementFunction.andThen(Scalar::new).andThen(RandomVariable.class::cast);
	}

	@Override
	public SavingsRateModel getSavingsRateModel() {
		return (SavingsRateModel)savingsRateFunction.andThen(Scalar::new).andThen(RandomVariable.class::cast);
	}
}
