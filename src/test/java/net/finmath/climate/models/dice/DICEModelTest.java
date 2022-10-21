package net.finmath.climate.models.dice;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.finmath.climate.models.CarbonConcentration;
import net.finmath.climate.models.ClimateModel;
import net.finmath.climate.models.Temperature;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.models.FundingCapacity;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

class DICEModelTest {

	@Test
	void testTimeStep1Y() {
		final double timeStep = 1.0;
		final double timeHorizon = 500.0;
		final double savingsRate = 0.259029014481802;
		final double discountRate = 0.03;

		final double abatementInitial = 0.03;
		final double abatementMax = 1.00;
		final double abatementMaxTime = 40.0;		// years

		final double abatementIncrease = (abatementMax-abatementInitial)/abatementMaxTime;

		final RandomVariableFactory randomFactory = new RandomVariableFromArrayFactory();
		final Function<Double, RandomVariable> abatementFunction = time -> randomFactory.createRandomVariable(Math.min(abatementInitial + abatementIncrease * time, abatementMax));

		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, (int)Math.round(timeHorizon / timeStep), timeStep);

		final ClimateModel climateModel = new DICEModel(timeDiscretization, t -> abatementFunction.apply(t).doubleValue(), t -> savingsRate, discountRate);

		double value = climateModel.getValue().getAverage();
		System.out.println("Value.......... " + value);

		double temperature = Arrays.stream(climateModel.getTemperature()).map(Temperature::getExpectedTemperatureOfAtmosphere).max(Comparator.naturalOrder()).orElseThrow();
		System.out.println("Temperature.... " + temperature);

		Assertions.assertEquals(528032.6531567107, value, 1E-5, "value");
		Assertions.assertEquals(2.9951627788929107, temperature, 1E-5, "value");
	}

	@Test
	void testTimeStep5Y() {
		final double timeStep = 5.0;
		final double timeHorizon = 500.0;
		final double savingsRate = 0.259029014481802;
		final double discountRate = 0.03;

		final double abatementInitial = 0.03;
		final double abatementMax = 1.00;
		final double abatementMaxTime = 40.0;		// years

		final double abatementIncrease = (abatementMax-abatementInitial)/abatementMaxTime;

		final RandomVariableFactory randomFactory = new RandomVariableFromArrayFactory();
		final Function<Double, RandomVariable> abatementFunction = time -> randomFactory.createRandomVariable(Math.min(abatementInitial + abatementIncrease * time, abatementMax));

		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, (int)Math.round(timeHorizon / timeStep), timeStep);

		final ClimateModel climateModel = new DICEModel(timeDiscretization, t -> abatementFunction.apply(t).doubleValue(), t -> savingsRate, discountRate);

		double value = climateModel.getValue().getAverage();
		System.out.println("Value.......... " + value);

		double temperature = Arrays.stream(climateModel.getTemperature()).map(Temperature::getExpectedTemperatureOfAtmosphere).max(Comparator.naturalOrder()).orElseThrow();
		System.out.println("Temperature.... " + temperature);

		Assertions.assertEquals(563183.7077803041, value, 1E-5, "value");
		Assertions.assertEquals(3.0457430613321073, temperature, 1E-5, "value");
	}
}
