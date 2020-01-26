package net.finmath.time.businessdaycalendar;

import java.time.LocalDate;
import java.util.Set;

/**
 * A class for a business day calendar, where every day is a business day, except
 * weekends days provided by a <code>Set</code>.
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class BusinessdayCalendarExcludingGivenSetOfHolidays extends BusinessdayCalendarExcludingGivenHolidays {

	/**
	 *
	 */
	private static final long serialVersionUID = -485496533316101770L;
	private final Set<LocalDate> holidays;

	public BusinessdayCalendarExcludingGivenSetOfHolidays(final String name, final BusinessdayCalendar baseCalendar, final boolean isExcludeWeekends, final Set<LocalDate> holidays) {
		super(name, baseCalendar, isExcludeWeekends);
		this.holidays = holidays;
	}

	public BusinessdayCalendarExcludingGivenSetOfHolidays(final String name, final boolean isExcludeWeekends, final Set<LocalDate> holidays) {
		this(name, null, isExcludeWeekends, holidays);
	}

	/**
	 * @return A set of (additional) holidays.
	 */
	@Override
	public Set<LocalDate> getHolidays() { return holidays; }
}
