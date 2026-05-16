package net.finmath.finitedifference.interestrate.products;

import net.finmath.finitedifference.interestrate.models.FiniteDifferenceInterestRateModel;
import net.finmath.time.Schedule;

/**
 * Finite-difference valuation of a reduced-scope swap.
 *
 * <p>
 * The swap value is
 * </p>
 *
 * <p>
 * <i>
 * V_{\mathrm{swap}} = V_{\mathrm{receiver\ leg}} - V_{\mathrm{payer\ leg}}.
 * </i>
 * </p>
 *
 * <p>
 * In line with {@link SwapLeg}, this class is interpreted as a remaining
 * forward-looking swap in the current Markovian PDE setting. It is valued
 * directly from discount bonds and forward rates and therefore does not require
 * a PDE solver.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class Swap implements FiniteDifferenceInterestRateProduct {

	/**
	 * The leg receiver.
	 */
	private final SwapLeg legReceiver;
	/**
	 * The leg payer.
	 */
	private final SwapLeg legPayer;

	/**
	 * Creates a swap with value
	 * {@code legReceiver - legPayer}.
	 *
	 * @param legReceiver The receiver leg.
	 * @param legPayer The payer leg.
	 */
	public Swap(final SwapLeg legReceiver, final SwapLeg legPayer) {
		if (legReceiver == null) {
			throw new IllegalArgumentException("legReceiver must not be null.");
		}
		if (legPayer == null) {
			throw new IllegalArgumentException("legPayer must not be null.");
		}

		this.legReceiver = legReceiver;
		this.legPayer = legPayer;
	}

	/**
	 * Creates a swap from leg specifications.
	 *
	 * @param scheduleReceiveLeg The receiver-leg schedule.
	 * @param forwardCurveReceiveName The receiver-leg forwarding-curve name.
	 * @param notionalsReceive The receiver-leg notionals.
	 * @param spreadsReceive The receiver-leg spreads.
	 * @param schedulePayLeg The payer-leg schedule.
	 * @param forwardCurvePayName The payer-leg forwarding-curve name.
	 * @param notionalsPay The payer-leg notionals.
	 * @param spreadsPay The payer-leg spreads.
	 * @param isNotionalExchanged True if notional exchange is included on both
	 *     legs.
	 */
	public Swap(
			final Schedule scheduleReceiveLeg,
			final String forwardCurveReceiveName,
			final double[] notionalsReceive,
			final double[] spreadsReceive,
			final Schedule schedulePayLeg,
			final String forwardCurvePayName,
			final double[] notionalsPay,
			final double[] spreadsPay,
			final boolean isNotionalExchanged) {
		this(
				new SwapLeg(
						scheduleReceiveLeg,
						forwardCurveReceiveName,
						notionalsReceive,
						spreadsReceive,
						isNotionalExchanged
				),
				new SwapLeg(
						schedulePayLeg,
						forwardCurvePayName,
						notionalsPay,
						spreadsPay,
						isNotionalExchanged
				)
		);
	}

	@Override
	public double[] getValue(
			final double evaluationTime,
			final FiniteDifferenceInterestRateModel model) {

		final double[] receiverValues = legReceiver.getValue(evaluationTime, model);
		final double[] payerValues = legPayer.getValue(evaluationTime, model);

		if (receiverValues.length != payerValues.length) {
			throw new IllegalArgumentException("Receiver and payer leg value vectors have different lengths.");
		}

		final double[] values = new double[receiverValues.length];
		for (int i = 0; i < values.length; i++) {
			values[i] = receiverValues[i] - payerValues[i];
		}

		return values;
	}

	@Override
	public double[][] getValues(final FiniteDifferenceInterestRateModel model) {

		final double[][] receiverValues = legReceiver.getValues(model);
		final double[][] payerValues = legPayer.getValues(model);

		if (receiverValues.length != payerValues.length) {
			throw new IllegalArgumentException("Receiver and payer leg grids have different spatial lengths.");
		}

		final double[][] values = new double[receiverValues.length][receiverValues[0].length];
		for (int i = 0; i < values.length; i++) {
			for (int j = 0; j < values[i].length; j++) {
				values[i][j] = receiverValues[i][j] - payerValues[i][j];
			}
		}

		return values;
	}

	/**
	 * Returns the swap value at the given evaluation time and state.
	 *
	 * @param evaluationTime The evaluation time.
	 * @param stateVariable The state variable.
	 * @param model The finite-difference interest-rate model.
	 * @return The swap value.
	 */
	public double getValueAt(
			final double evaluationTime,
			final double stateVariable,
			final FiniteDifferenceInterestRateModel model) {
		return legReceiver.getValueAt(evaluationTime, stateVariable, model)
				- legPayer.getValueAt(evaluationTime, stateVariable, model);
	}

	/**
	 * Returns the receiver leg.
	 *
	 * @return The receiver leg.
	 */
	public SwapLeg getLegReceiver() {
		return legReceiver;
	}

	/**
	 * Returns the payer leg.
	 *
	 * @return The payer leg.
	 */
	public SwapLeg getLegPayer() {
		return legPayer;
	}

	/**
	 * Returns the forward swap rate corresponding to the given fixed-leg and
	 * floating-leg schedules under the specified model state.
	 *
	 * <p>
	 * The returned quantity is
	 * </p>
	 *
	 * <p>
	 * <i>
	 * S(t,x) = \frac{V_{\mathrm{float}}(t,x)}{A(t,x)},
	 * </i>
	 * </p>
	 *
	 * <p>
	 * where {@code A(t,x)} is the swap annuity of the fixed-leg schedule.
	 * </p>
	 *
	 * @param evaluationTime The evaluation time.
	 * @param fixSchedule The fixed-leg schedule.
	 * @param floatSchedule The floating-leg schedule.
	 * @param forwardCurveName The forwarding-curve name of the floating leg.
	 * @param model The finite-difference interest-rate model.
	 * @param stateVariables The current state variables.
	 * @return The forward swap rate.
	 */
	public static double getForwardSwapRate(
			final double evaluationTime,
			final Schedule fixSchedule,
			final Schedule floatSchedule,
			final String forwardCurveName,
			final FiniteDifferenceInterestRateModel model,
			final double... stateVariables) {

		if (stateVariables == null || stateVariables.length != 1) {
			throw new IllegalArgumentException("Exactly one state variable is required.");
		}

		final SwapLeg floatingLeg = new SwapLeg(
				floatSchedule,
				forwardCurveName,
				0.0,
				false
		);

		final double valueFloatLeg = floatingLeg.getValueAt(
				evaluationTime,
				stateVariables[0],
				model
		);

		final double swapAnnuity = SwapAnnuity.getSwapAnnuity(
				evaluationTime,
				fixSchedule,
				model,
				stateVariables[0]
		);

		if (Math.abs(swapAnnuity) < 1E-15) {
			throw new IllegalArgumentException("Swap annuity is numerically zero.");
		}

		return valueFloatLeg / swapAnnuity;
	}

	@Override
	public String toString() {
		return "Swap [legReceiver=" + legReceiver + ", legPayer=" + legPayer + "]";
	}
}
