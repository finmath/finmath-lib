package net.finmath.time;


import org.apache.commons.lang3.ArrayUtils;
import org.threeten.bp.LocalDate;
/**
 * 
 * Imm date logic for CDSs.
 * 
 * This is a port of the IMM date class from Strata, where we employ org.threeten.bp.LocalDate
 * for LocalDate. See https://github.com/OpenGamma/Strata/
 * 
 * @author Alessandro Gnoatto
 *
 */
public class ImmDate {
	
	/*
	 * Imm dates can only be Marth 20th, June 20th, September 20th and December 20th.
	 */
	private static final int IMM_DAY = 20;
	private static final int[] IMM_MONTHS = new int[] {3, 6, 9, 12};

	public static boolean isImmDate(LocalDate date){
		return date.getDayOfMonth() == IMM_DAY && ArrayUtils.contains( IMM_MONTHS, date.getMonthValue());
	}

	/**
	 * Given a starting Imm date, create a sequence of Imm dates.
	 * 
	 * @param baseIMMDate
	 * @param size 
	 * @return an array of Imm dates
	 */
	public static LocalDate[] getIMMDateSchedule(LocalDate baseIMMDate, int size){

		if(isImmDate(baseIMMDate) == false){
			throw new IllegalArgumentException("Not an IMM date");
		}

		LocalDate[] result = new LocalDate[size];
		result[0] = baseIMMDate;

		for(int i = 1; i < size; i++){
			int month = result[i-1].getMonthValue();
			int year  = result[i-1].getYear();

			if(month != 12){
				//If we are not in December, increase by 3 months
				result[i] = LocalDate.of(year, month + 3, IMM_DAY);
			}else{
				//If we are in December, move to the next year
				result[i] = LocalDate.of(year +1, 3, IMM_DAY);
			}
		}
		
		return result;
	}
	
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
	 * For a given date return the previous imm date.
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
