package net.finmath.climate.models;

import java.util.function.Function;

import net.finmath.stochastic.RandomVariable;

public interface SavingsRateModel extends Function<Double, RandomVariable> {

}