package net.finmath.climate.models.dice;

import java.util.function.DoubleUnaryOperator;
import java.util.function.UnaryOperator;

import net.finmath.climate.models.dice.submodels.AbatementCostFunction;
import net.finmath.climate.models.dice.submodels.CarbonConcentration;
import net.finmath.climate.models.dice.submodels.DamageFromTemperature;
import net.finmath.climate.models.dice.submodels.EmissionFunction;
import net.finmath.climate.models.dice.submodels.EmissionIntensityFunction;
import net.finmath.climate.models.dice.submodels.EvolutionOfCapital;
import net.finmath.climate.models.dice.submodels.EvolutionOfCarbonConcentration;
import net.finmath.climate.models.dice.submodels.EvolutionOfPopulation;
import net.finmath.climate.models.dice.submodels.EvolutionOfProductivity;
import net.finmath.climate.models.dice.submodels.EvolutionOfTemperature;
import net.finmath.climate.models.dice.submodels.ForcingFunction;
import net.finmath.climate.models.dice.submodels.Temperature;
import net.finmath.time.TimeDiscretization;

/**
 * A simulation of a simplified DICE model.
 * 
 * Note: The code makes some small simplification: it uses a constant savings rate and a constant external forcings.
 * It may still be useful for illustration.
 */
public class DICEModel {

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
	private Temperature[] temperature;
	private CarbonConcentration[] carbonConcentration;
	private double[] gdp;
	private double[] emission;
	private double[] abatement;
	private double[] damage;
	private double[] capital;
	private double[] population;
	private double[] productivity;
	private double[] welfare;
	private double[] value;

	public DICEModel(TimeDiscretization timeDiscretization, UnaryOperator<Double> abatementFunction, UnaryOperator<Double> savingsRateFunction, double discountRate) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.abatementFunction = abatementFunction;
		this.savingsRateFunction = savingsRateFunction;
		this.discountRate = discountRate;

		int numberOfTimes = this.timeDiscretization.getNumberOfTimes();

		temperature = new Temperature[numberOfTimes];
		carbonConcentration = new CarbonConcentration[numberOfTimes];
		gdp = new double[numberOfTimes];
		emission = new double[numberOfTimes];
		abatement = new double[numberOfTimes];
		damage = new double[numberOfTimes];
		capital = new double[numberOfTimes];
		population = new double[numberOfTimes];
		productivity = new double[numberOfTimes];
		welfare = new double[numberOfTimes];
		value = new double[numberOfTimes];

		this.init();
	}

	public DICEModel(TimeDiscretization timeDiscretization, UnaryOperator<Double> abatementFunction) {
		this(timeDiscretization, abatementFunction, t -> 0.259029014481802, 0.03);
	}

	public double[] getValues() {
		synchronized (welfare) {
			this.init();
			return welfare;
		}
	}

	public double getValue() {
		synchronized (value) {
			this.init();
			return value[value.length-1];
		}
	}

	private void init() {
		// TODO Assuming that the time steps are constant.
		final double timeStep = timeDiscretization.getTimeStep(0);

		/*
		 * Building the model by composing the different functions
		 */

		/*
		 * Note: Calling default constructors for the sub-models will initialise the default parameters.
		 */

		/*
		 * State vectors initial values
		 */
		final Temperature temperatureInitial = new Temperature(0.85, 0.0068);
		final CarbonConcentration carbonConcentrationInitial = new CarbonConcentration(851, 460, 1740);	// Level of Carbon (GtC)

		/*
		 * Sub-Modules: functional dependencies and evolution
		 */

		// Model that describes the damage on the GBP as a function of the temperature-above-normal
		final DoubleUnaryOperator damageFunction = new DamageFromTemperature();

		final EmissionIntensityFunction emissionIntensityFunction = new EmissionIntensityFunction();
		final EmissionFunction emissionFunction = new EmissionFunction(timeStep, emissionIntensityFunction);

		final EvolutionOfCarbonConcentration evolutionOfCarbonConcentration = new EvolutionOfCarbonConcentration(timeStep);

		final ForcingFunction forcingFunction = new ForcingFunction();
		final double forcingExternal = 1.0/5.0;

		final EvolutionOfTemperature evolutionOfTemperature = new EvolutionOfTemperature(timeStep);

		// Abatement
		final AbatementCostFunction abatementCostFunction = new AbatementCostFunction();

		/*
		 * GDP
		 */
		final double K0 = 223;		// Initial Capital
		final double L0 = 7403;		// Initial Population
		final double A0 = 5.115;		// Initial Total Factor of Productivity
		final double gamma = 0.3;		// Capital Elasticity in Production Function
		final double gdpInitial = A0*Math.pow(K0,gamma)*Math.pow(L0/1000,1-gamma);

		// Capital
		final EvolutionOfCapital evolutionOfCapital = new EvolutionOfCapital(timeStep);

		// Population
		final EvolutionOfPopulation evolutionOfPopulation = new EvolutionOfPopulation(timeStep);

		// Productivity
		final EvolutionOfProductivity evolutionOfProductivity = new EvolutionOfProductivity(timeStep);

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

		/*
		 * Evolve
		 */
		for(int i=0; i<timeDiscretization.getNumberOfTimeSteps(); i++) {

			final double time = timeDiscretization.getTime(i);

			/*
			 * Evolve geo-physical quantities i -> i+1 (as a function of gdp[i])
			 */

			final double forcing = forcingFunction.apply(carbonConcentration[i], forcingExternal);
			temperature[i+1] = evolutionOfTemperature.apply(temperature[i], forcing);

			abatement[i] = abatementFunction.apply(time);

			// Note: In the original model the 1/(1-\mu(0)) is part of the emission function. Here we add the factor on the outside
			emission[i] = (1 - abatement[i])/(1-abatement[0]) * emissionFunction.apply(time, gdp[i]);

			carbonConcentration[i+1] = evolutionOfCarbonConcentration.apply(carbonConcentration[i], emission[i]);


			/*
			 * Evolve economy i -> i+1 (as a function of temperature[i])
			 */

			// Apply damage to economy
			damage[i] = damageFunction.applyAsDouble(temperature[i].getTemperatureOfAtmosphere());

			/*
			 * Abatement cost
			 */
			double abatementCost = abatementCostFunction.apply(time, abatement[i]);

			/*
			 * Evolve economy to i+1
			 */
			double gdpNet = gdp[i] * (1-damage[i]) * (1 - abatementCost);

			// Constant from the original model - in the original model this is a time varying control variable.
			double savingsRate = savingsRateFunction.apply(time);	//0.259029014481802;

			double consumption = (1-savingsRate) * gdpNet;
			double investment = savingsRate * gdpNet;

			capital[i+1] = evolutionOfCapital.apply(time).apply(capital[i], investment);

			/*
			 * Evolve population and productivity for next GDP
			 */
			population[i+1] = evolutionOfPopulation.apply(time).apply(population[i]);
			productivity[i+1] = evolutionOfProductivity.apply(time).apply(productivity[i]);

			double L = population[i+1];
			double A = productivity[i+1];
			gdp[i+1] = A*Math.pow(capital[i+1],gamma)*Math.pow(L/1000,1-gamma);

			/*
			 * Calculate utility
			 */
			double alpha = 1.45;           // Elasticity of marginal utility of consumption (GAMS elasmu)
			double C = consumption;
			double utility = L*Math.pow(C / (L/1000),1-alpha)/(1-alpha);

			/*
			 * Discounted utility
			 */
			double discountFactor = Math.exp(- discountRate * time);
			welfare[i] = utility*discountFactor;

			utilityDiscountedSum = utilityDiscountedSum + utility*discountFactor;
			value[i+1] = utilityDiscountedSum;
		}
	}

	/**
	 * @return the damage
	 */
	public double[] getDamage() {
		return damage;
	}

	/**
	 * @return the carbonConcentration
	 */
	public CarbonConcentration[] getCarbonConcentration() {
		return carbonConcentration;
	}

	/**
	 * @return the temperature
	 */
	public Temperature[] getTemperature() {
		return temperature;
	}

	/**
	 * @return the abatement
	 */
	public double[] getAbatement() {
		return abatement;
	}
}
