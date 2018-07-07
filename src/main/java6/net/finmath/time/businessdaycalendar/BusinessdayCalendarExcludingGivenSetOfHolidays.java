package net.finmath.time.businessdaycalendar;

import java.util.Set;

import org.threeten.bp.LocalDate;

/**
 * A class for a business day calendar, where every day is a business day, except
 * weekends days provided by a <code>Set</code>.
 *
 * @author Christian Fries
 */
public abstract class BusinessdayCalendarExcludingGivenSetOfHolidays extends BusinessdayCalendarExcludingGivenHolidays {

	/**
	 * 
	 */
	private static final long serialVersionUID = -485496533316101770L;
	private final Set<LocalDate> holidays;

	public BusinessdayCalendarExcludingGivenSetOfHolidays(String name, BusinessdayCalendarInterface baseCalendar, boolean isExcludeWeekends, Set<LocalDate> holidays) {
		super(name, baseCalendar, isExcludeWeekends);
		this.holidays = holidays;
	}

	public BusinessdayCalendarExcludingGivenSetOfHolidays(String name, boolean isExcludeWeekends, Set<LocalDate> holidays) {
		this(name, null, isExcludeWeekends, holidays);
	}

	/**
	 * @return A set of (additional) holidays.
	 */
	public Set<LocalDate> getHolidays() { return holidays; };
}
