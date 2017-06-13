package net.finmath.time;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface.DateRollConvention;

/**
 * This class stores a list containing all coupon dates involved in a Cds contract according to the post Big bank convention.
 * 
 * @author Alessandro Gnoatto
 *
 */
public class CdsPeriods {
	
	private final List<Period> cdsCoupons;
	/*
	 * Imm dates can only be March 20th, June 20th, September 20th and December 20th.
	 */
	private static final int IMM_DAY = 20;
	private static final int[] IMM_MONTHS = new int[] {3, 6, 9, 12};
	
	
	private static final BusinessdayCalendarExcludingTARGETHolidays calendar = new BusinessdayCalendarExcludingTARGETHolidays();
	private static final DateRollConvention dateRollConvention = DateRollConvention.FOLLOWING;

	public CdsPeriods(LocalDate tradeDate, String maturityString){
		this.cdsCoupons = generateCdsPeriods(tradeDate, maturityString);
	}

	/**
	 * The tenor is assumed to be a String of the form xY or xM.
	 * 
	 * @param tradeDate
	 * @param tenor
	 * @return the list containing all coupon dates
	 */
	private List<Period> generateCdsPeriods(LocalDate tradeDate, String maturityString){
		/*
		 * We look for the first Imm date before and after the trade Date.
		 */
		LocalDate nextImmDate = getNextIMMDate(tradeDate);
		LocalDate previousImmDate = getPreviousIMMDate(tradeDate);
				
		/*
		 * Identify the maturity, an unadjusted Imm date.
		 */
		LocalDate maturityDate = calendar.createDateFromDateAndOffsetCode(nextImmDate, maturityString);
		
		//Initialize all dates		
		LocalDate fixing = previousImmDate;
		LocalDate payment = calendar.getAdjustedDate(nextImmDate,dateRollConvention);
		LocalDate periodStart = tradeDate; 
		LocalDate periodEnd = calendar.getAdjustedDate(nextImmDate,dateRollConvention);
		
		//Initialize the list and put the first date
		List<Period> result = new ArrayList<Period>();
		result.add(new Period(fixing,payment,periodStart,periodEnd));
		
		while(nextImmDate.isBefore(maturityDate)){
			
			//unadjusted period start
			fixing = nextImmDate;
			//Payment dates: adjusted Imm Dates
			payment = calendar.getAdjustedDate(nextImmDate.plusMonths(3),dateRollConvention);
			
			//accrual start: an adjusted Imm Date
			periodStart = calendar.getAdjustedDate(fixing,dateRollConvention);  
			//accrual end: an adjusted Imm Date. EXCEPTION: for the final date which is unadjusted!
			if(getNextIMMDate(nextImmDate).equals(maturityDate)){
				periodEnd = payment;
			}else{
				periodEnd = calendar.getAdjustedDate(payment,dateRollConvention);
			}
			result.add(new Period(fixing,payment,periodStart,periodEnd));
			
			nextImmDate = getNextIMMDate(nextImmDate);
			
		}
		
		return result;
	}
	
	
	public List<Period> getCdsCoupons() {
		return cdsCoupons;
	}

	@Override
	public String toString() {
		return "CdsPeriods [cdsCoupons=" + cdsCoupons + "]";
	}
	
	
	
	/*
	 * Convenience methods for the creation of Imm dates.
	 */
	
	/**
	 * For a given date return the next Imm Date
	 * 
	 * @param date
	 * @return the next Imm Date
	 */
	public static LocalDate getNextIMMDate(LocalDate date){
		int day = date.getDayOfMonth();
		int month = date.getMonthValue();
		int year = date.getYear();

		if(ArrayUtils.contains( IMM_MONTHS, date.getMonthValue())){
			if(day < IMM_DAY){
				return LocalDate.of(year,month,IMM_DAY); 
			}else{
				if(month != 12){
					return LocalDate.of(year,month + 3, IMM_DAY);
				}else{
					return LocalDate.of(year + 1, IMM_MONTHS[0], IMM_DAY);
				}				 
			}
		}else{
			return LocalDate.of(year, IMM_MONTHS[month / 3], IMM_DAY);
		}
	}
	
	/**
	 * For a given date return the previous Imm date.
	 * 
	 * @param date
	 * @return the previous Imm date.
	 */
	public static LocalDate getPreviousIMMDate(LocalDate date){
		int day = date.getDayOfMonth();
		int month = date.getMonthValue();
		int year = date.getYear();

		if(ArrayUtils.contains( IMM_MONTHS, date.getMonthValue())){
			if(day > IMM_DAY){
				return LocalDate.of(year,month,IMM_DAY);
			}else{
				if(month != 3){
					return LocalDate.of(year,month - 3,IMM_DAY);
				}else{
					return LocalDate.of(year - 1, IMM_MONTHS[3],IMM_DAY);
				}
			}
		}else{
			int i = month / 3;
			if(i == 0){
				return LocalDate.of(year - 1, IMM_MONTHS[3],IMM_DAY);
			}else{
				return LocalDate.of(year, IMM_MONTHS[i - 1], IMM_DAY);
			}
		}

	}

}
