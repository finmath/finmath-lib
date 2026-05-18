/*
 * (c) Copyright Luca Bressan. Contact: contact.lucabressan@icloud.com
 *
 * Created December 15th, 2022.
 */
package net.finmath.randomnumbers;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.junit.Test;

public class HighEntropyRandomNumberGeneratorTest {

	private static final int NUM_SAMPLES = 10000;
	private static final int NUM_THREADS = 100;
	private static final double ALPHA = 1E-9;

	@Test
	public void distributionTest() {
		final RandomNumberGenerator1D highEntropyGenerator = new HighEntropyRandomNumberGenerator();
		final double[] samples = new double[NUM_SAMPLES];
		for (int i = 0; i < NUM_SAMPLES; i++) {
			samples[i] = highEntropyGenerator.nextDouble();
		}
		final boolean ksTestResult = (new KolmogorovSmirnovTest()).kolmogorovSmirnovTest(new UniformRealDistribution(),
				samples, ALPHA);

		if (ksTestResult) {
			fail("The sampled distribution is not uniform (confidence level " + (1 - ALPHA) + ").");
		}
	}

	@Test
	public void testRNGWithConcurrency() throws InterruptedException {
		final ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);
		final RandomNumberGenerator1D highEntropyGenerator = new HighEntropyRandomNumberGenerator();

		final Callable<Double> rngCallable = () -> {
			return highEntropyGenerator.nextDouble();
		};

		final List<Callable<Double>> rngCallables = new ArrayList<>();

		for (int i = 0; i < NUM_THREADS; i++) {
			rngCallables.add(rngCallable);
		}

		final List<Future<Double>> rngNextDoubleFutures = executorService.invokeAll(rngCallables);
		final List<Double> returnValues = new ArrayList<>();
		for (final Future<Double> rngNextDoubleFuture : rngNextDoubleFutures) {
			try {
				returnValues.add(rngNextDoubleFuture.get());
			} catch (final ExecutionException e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
		System.out.println(returnValues.toArray(new Double[0]).length);
		final boolean testResult = (new KolmogorovSmirnovTest()).kolmogorovSmirnovTest(new UniformRealDistribution(),
				ArrayUtils.toPrimitive(returnValues.toArray(new Double[0])), ALPHA);

		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
				executorService.shutdownNow();
			}
		} catch (final InterruptedException e) {
			executorService.shutdownNow();
		}
		if (testResult) {
			fail("Heavy multithreading caused loss of statistical properties. The sampled distribution is not uniform (confidence level "
					+ (1 - ALPHA) + ").");
		}
	}
}
