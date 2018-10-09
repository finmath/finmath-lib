package net.finmath.modelling.descriptor;

import java.time.LocalDate;
import java.util.List;

import net.finmath.time.Period;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.ScheduleGenerator.DaycountConvention;
import net.finmath.time.ScheduleGenerator.Frequency;
import net.finmath.time.ScheduleGenerator.ShortPeriodConvention;
import net.finmath.time.ScheduleInterface;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface.DateRollConvention;
import net.finmath.time.daycount.DayCountConventionInterface;

/**
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class ScheduleDescriptor {

	private final InternalScheduleDescriptor descriptor;
	
	public ScheduleDescriptor(List<Period> periods,
			DayCountConventionInterface daycountConvention) {
		super();
		descriptor = new ScheduleDescriptorFromPeriods(periods, daycountConvention);
	}
	
	public ScheduleDescriptor(ScheduleInterface schedule) {
		super();
		descriptor = new ScheduleDescriptorFromPeriods(schedule.getPeriods(), schedule.getDaycountconvention());
	}
	
	public ScheduleDescriptor(LocalDate startDate, LocalDate maturityDate, Frequency frequency,
			DaycountConvention daycountConvention, ShortPeriodConvention shortPeriodConvention,
			DateRollConvention dateRollConvention, BusinessdayCalendarInterface businessdayCalendar,
			int fixingOffsetDays, int paymentOffsetDays, boolean isUseEndOfMonth) {
		super();
		descriptor = new ScheduleDescriptorFromGenerator(startDate, maturityDate, frequency, daycountConvention, shortPeriodConvention, dateRollConvention, businessdayCalendar, fixingOffsetDays,
				paymentOffsetDays, isUseEndOfMonth);
	}
	
	public ScheduleDescriptor(LocalDate startDate, LocalDate maturityDate, Frequency frequency,
			DaycountConvention daycountConvention, ShortPeriodConvention shortPeriodConvention,
			DateRollConvention dateRollConvention, BusinessdayCalendar businessdayCalendar, int fixingOffsetDays,
			int paymentOffsetDays) {
		this(startDate, maturityDate, frequency, daycountConvention, shortPeriodConvention,
				dateRollConvention, businessdayCalendar, fixingOffsetDays, paymentOffsetDays, false);
	}

	public ScheduleInterface getSchedule(LocalDate referenceDate) {
		return descriptor.getSchedule(referenceDate);
	}
	
	
	
	
	
	
	
	
	private static interface InternalScheduleDescriptor {
		
		ScheduleInterface getSchedule(LocalDate referenceDate);
		
	}
	
	private static class ScheduleDescriptorFromPeriods implements InternalScheduleDescriptor {
		
		private final List<Period> periods;
		private final DayCountConventionInterface daycountConvention;
		
		private ScheduleDescriptorFromPeriods(List<Period> periods,
				DayCountConventionInterface daycountConvention) {
			super();
			this.periods = periods;
			this.daycountConvention = daycountConvention;
		}
		
		@Override
		public ScheduleInterface getSchedule(LocalDate referenceDate) {
			return new Schedule(referenceDate, periods, daycountConvention);
		}
		
	}
	
	private static class ScheduleDescriptorFromGenerator implements InternalScheduleDescriptor {
		
		private final LocalDate startDate;
		private final LocalDate maturityDate;
		private final Frequency frequency;
		private final DaycountConvention daycountConvention;
		private final ShortPeriodConvention shortPeriodConvention;
		private final DateRollConvention dateRollConvention;
		private final BusinessdayCalendarInterface businessdayCalendar;
		private final int fixingOffsetDays;
		private final int paymentOffsetDays;
		private final boolean isUseEndOfMonth;
		

		private ScheduleDescriptorFromGenerator(LocalDate startDate, LocalDate maturityDate, Frequency frequency,
				DaycountConvention daycountConvention, ShortPeriodConvention shortPeriodConvention,
				DateRollConvention dateRollConvention, BusinessdayCalendarInterface businessdayCalendar,
				int fixingOffsetDays, int paymentOffsetDays, boolean isUseEndOfMonth) {
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
		public ScheduleInterface getSchedule(LocalDate referenceDate) {
			return ScheduleGenerator.createScheduleFromConventions(referenceDate, startDate, maturityDate, frequency, daycountConvention, shortPeriodConvention, dateRollConvention, 
					businessdayCalendar, fixingOffsetDays, paymentOffsetDays, isUseEndOfMonth);
		}
		
	}

	public int getNumberOfPeriods() {
		return descriptor.getSchedule(LocalDate.parse("1970-01-01")).getNumberOfPeriods();
	}
}
