package net.finmath.modelling.descriptor;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import net.finmath.time.Period;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleFromPeriods;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.ScheduleGenerator.DaycountConvention;
import net.finmath.time.ScheduleGenerator.Frequency;
import net.finmath.time.ScheduleGenerator.ShortPeriodConvention;
import net.finmath.time.businessdaycalendar.AbstractBusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar.DateRollConvention;
import net.finmath.time.daycount.DayCountConvention;

/**
 * Descriptor for a schedule. All data to generate a schedule for any given reference date is stored. Either via a set of periods or conventions.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class ScheduleDescriptor {

	private final InternalScheduleDescriptor descriptor;

	/**
	 * Construct a schedule descriptor via a list of periods and daycount convention.
	 *
	 * @param periods A list of <code>Period</code> objects.
	 * @param daycountConvention The common daycount convention.
	 */
	public ScheduleDescriptor(final List<Period> periods,
			final DayCountConvention daycountConvention) {
		super();
		descriptor = new ScheduleDescriptorFromPeriods(periods, daycountConvention);
	}

	/**
	 * Extract a schedule descriptor from a schedule.
	 *
	 * @param schedule The schedule.
	 */
	public ScheduleDescriptor(final Schedule schedule) {
		super();
		descriptor = new ScheduleDescriptorFromPeriods(schedule.getPeriods(), schedule.getDaycountconvention());
	}

	/**
	 * Construct a schedule descriptor via a set of parameters for a factory.
	 *
	 * @param startDate The start date of the first period (unadjusted - adjustments take place during schedule generation).
	 * @param maturityDate The end date of the last period (unadjusted - adjustments take place during schedule generation).
	 * @param frequency The frequency.
	 * @param daycountConvention The daycount convention.
	 * @param shortPeriodConvention If short period exists, have it first or last.
	 * @param dateRollConvention Adjustment to be applied to the all dates.
	 * @param businessdayCalendar Businessday calendar (holiday calendar) to be used for date roll adjustment.
	 * @param fixingOffsetDays Number of business days to be added to period start to get the fixing date.
	 * @param paymentOffsetDays Number of business days to be added to period end to get the payment date.
	 * @param isUseEndOfMonth If ShortPeriodConvention is LAST and startDate is an end of month date, all period will be adjusted to EOM. If ShortPeriodConvention is FIRST and maturityDate is an end of month date, all period will be adjusted to EOM.
	 */
	public ScheduleDescriptor(final LocalDate startDate, final LocalDate maturityDate, final Frequency frequency,
			final DaycountConvention daycountConvention, final ShortPeriodConvention shortPeriodConvention,
			final DateRollConvention dateRollConvention, final BusinessdayCalendar businessdayCalendar,
			final int fixingOffsetDays, final int paymentOffsetDays, final boolean isUseEndOfMonth) {
		super();
		descriptor = new ScheduleDescriptorFromGenerator(startDate, maturityDate, frequency, daycountConvention, shortPeriodConvention, dateRollConvention, businessdayCalendar, fixingOffsetDays,
				paymentOffsetDays, isUseEndOfMonth);
	}

	/**
	 * Construct a schedule descriptor via a set of parameters for a factory.
	 *
	 * @param startDate The start date of the first period (unadjusted - adjustments take place during schedule generation).
	 * @param maturityDate The end date of the last period (unadjusted - adjustments take place during schedule generation).
	 * @param frequency The frequency.
	 * @param daycountConvention The daycount convention.
	 * @param shortPeriodConvention If short period exists, have it first or last.
	 * @param dateRollConvention Adjustment to be applied to the all dates.
	 * @param abstractBusinessdayCalendar Businessday calendar (holiday calendar) to be used for date roll adjustment.
	 * @param fixingOffsetDays Number of business days to be added to period start to get the fixing date.
	 * @param paymentOffsetDays Number of business days to be added to period end to get the payment date.
	 */
	public ScheduleDescriptor(final LocalDate startDate, final LocalDate maturityDate, final Frequency frequency,
			final DaycountConvention daycountConvention, final ShortPeriodConvention shortPeriodConvention,
			final DateRollConvention dateRollConvention, final AbstractBusinessdayCalendar abstractBusinessdayCalendar, final int fixingOffsetDays,
			final int paymentOffsetDays) {
		this(startDate, maturityDate, frequency, daycountConvention, shortPeriodConvention,
				dateRollConvention, abstractBusinessdayCalendar, fixingOffsetDays, paymentOffsetDays, false);
	}

	/**
	 * Generate a schedule relative to the given reference date.
	 *
	 * @param referenceDate The desired reference date.
	 * @return The schedule relative to the reference date.
	 */
	public Schedule getSchedule(final LocalDate referenceDate) {
		return descriptor.getSchedule(referenceDate);
	}

	/**
	 * The number of periods any schedule from this descriptor will have.
	 *
	 * @return The number of periods.
	 */
	public int getNumberOfPeriods() {
		// Note: the reference date is irrelevant for the periods.
		return descriptor.getSchedule(LocalDate.of(1970,1,1)).getNumberOfPeriods();
	}

	/**
	 * The periods of a schedule generated from this descriptor.
	 *
	 * @return The periods.
	 */
	public List<Period> getPeriods() {
		// Note: the reference date is irrelevant for the periods.
		return descriptor.getSchedule(LocalDate.of(1970,1,1)).getPeriods();
	}

	/**
	 * Private inner interface for the different kinds of schedule generation methods.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 *
	 */
	private interface InternalScheduleDescriptor {

		/**
		 * Generate a schedule relative to the given reference date.
		 *
		 * @param referenceDate The desired reference date.
		 * @return The schedule relative to the reference date.
		 */
		Schedule getSchedule(LocalDate referenceDate);
	}

	/**
	 * Private inner class to generate schedules via a list of periods and daycount convention.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 *
	 */
	private static class ScheduleDescriptorFromPeriods implements InternalScheduleDescriptor {

		private final List<Period> periods;
		private final DayCountConvention daycountConvention;

		private ScheduleDescriptorFromPeriods(final List<Period> periods,
				final DayCountConvention daycountConvention) {
			super();
			this.periods = periods;
			this.daycountConvention = daycountConvention;
		}

		@Override
		public Schedule getSchedule(final LocalDate referenceDate) {
			return new ScheduleFromPeriods(referenceDate, periods, daycountConvention);
		}

		public List<Period> getPeriods() {
			return Collections.unmodifiableList(periods);
		}

		public DayCountConvention getDaycountConvention() {
			return daycountConvention;
		}
	}

	/**
	 * Private inner class to generate schedule via conventions.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 *
	 */
	private static class ScheduleDescriptorFromGenerator implements InternalScheduleDescriptor {

		private final LocalDate startDate;
		private final LocalDate maturityDate;
		private final Frequency frequency;
		private final DaycountConvention daycountConvention;
		private final ShortPeriodConvention shortPeriodConvention;
		private final DateRollConvention dateRollConvention;
		private final BusinessdayCalendar businessdayCalendar;
		private final int fixingOffsetDays;
		private final int paymentOffsetDays;
		private final boolean isUseEndOfMonth;


		private ScheduleDescriptorFromGenerator(final LocalDate startDate, final LocalDate maturityDate, final Frequency frequency,
				final DaycountConvention daycountConvention, final ShortPeriodConvention shortPeriodConvention,
				final DateRollConvention dateRollConvention, final BusinessdayCalendar businessdayCalendar,
				final int fixingOffsetDays, final int paymentOffsetDays, final boolean isUseEndOfMonth) {
			super();
			this.startDate = startDate;
			this.maturityDate = maturityDate;
			this.frequency = frequency;
			this.daycountConvention = daycountConvention;
			this.shortPeriodConvention = shortPeriodConvention;
			this.dateRollConvention = dateRollConvention;
			this.businessdayCalendar = businessdayCalendar;
			this.fixingOffsetDays = fixingOffsetDays;
			this.paymentOffsetDays = paymentOffsetDays;
			this.isUseEndOfMonth = isUseEndOfMonth;
		}


		@Override
		public Schedule getSchedule(final LocalDate referenceDate) {
			return ScheduleGenerator.createScheduleFromConventions(referenceDate, startDate, maturityDate, frequency, daycountConvention, shortPeriodConvention, dateRollConvention,
					businessdayCalendar, fixingOffsetDays, paymentOffsetDays, isUseEndOfMonth);
		}

	}

}
