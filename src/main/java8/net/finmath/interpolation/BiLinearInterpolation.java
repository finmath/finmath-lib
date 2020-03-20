/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.01.2018
 */
package net.finmath.interpolation;

import java.util.Arrays;
import java.util.function.BiFunction;

/**
 * Simple bi-linear interpolation of data points \( z_{i,j} \) over a Cartesian grid \( (x_{i},y_{j}) \).
 *
 * The interpolation function is
 * \[
 *	z = f(x,y) =
 *		\alpha_{x} \alpha_{y} z_{k_{x},k_{y}} +
 *		\alpha_{x} (1-\alpha_{y}) z_{k_{x},k_{y}+1} +
 *		(1-\alpha_{x}) \alpha_{y} z_{k_{x}+1,k_{y}} +
 *		(1-\alpha_{x}) (1-\alpha_{y}) z_{k_{x}+1,k_{y}+1}
 * \]
 * where \( x_{k_{x}} \leq x \leq x_{k_{x}+1} \) and \( y_{k_{y}} \leq y \leq y_{k_{x}+1} \) and \( \alpha_{x} = (x_{k_{x}+1}-x)/(x_{k_{x}+1}-x_{k_{x}}) \) and \( \alpha_{y} = (x_{k_{y}+1}-x)/(x_{k_{y}+1}-x_{k_{y}}) \).
 *
 * @author Christian Fries
 * @version 1.0
 */
public class BiLinearInterpolation implements BiFunction<Double, Double, Double> {

	private final double[] x;
	private final double[] y;
	private final double[][] z;

	public BiLinearInterpolation(final double[] x, final double[] y, final double[][] z) {
		super();
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public Double apply(final Double x, final Double y) {
		return getValue(x, y);
	}

	double getValue(final double x, final double y) {
		int indexGreaterOrEqualX = Arrays.binarySearch(this.x, x);
		if(indexGreaterOrEqualX < 0) {
			indexGreaterOrEqualX = -indexGreaterOrEqualX-1;
		}
		int upperIndexX = Math.min(Math.max(indexGreaterOrEqualX, 0), this.x.length-1);
		final int lowerIndexX = Math.min(Math.max(upperIndexX-1, 0), this.x.length-1);

		int indexGreaterOrEqualY = Arrays.binarySearch(this.y, y);
		if(indexGreaterOrEqualY < 0) {
			indexGreaterOrEqualY = -indexGreaterOrEqualY-1;
		}
		int upperIndexY = Math.min(Math.max(indexGreaterOrEqualY, 0), this.y.length-1);
		final int lowerIndexY = Math.min(Math.max(upperIndexY-1, 0), this.y.length-1);

		if(upperIndexX == lowerIndexX) {
			upperIndexX++;
		}
		if(upperIndexY == lowerIndexY) {
			upperIndexY++;
		}

		final double alphaX = (this.x[upperIndexX]-x)/(this.x[upperIndexX]-this.x[lowerIndexX]);
		final double alphaY = (this.y[upperIndexY]-y)/(this.y[upperIndexY]-this.y[lowerIndexY]);

		final double interpolatedValue =
				alphaX * alphaY * z[lowerIndexX][lowerIndexY] +
				alphaX * (1.0-alphaY) * z[lowerIndexX][upperIndexY] +
				(1-alphaX) * alphaY * z[upperIndexX][lowerIndexY] +
				(1-alphaX) * (1-alphaY) * z[upperIndexX][upperIndexY];

		return interpolatedValue;
	}
}
