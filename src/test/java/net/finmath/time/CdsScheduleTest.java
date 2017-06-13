package net.finmath.time;

import java.time.LocalDate;
import java.util.List;

/**
 * This console application tests the date logic for credit default swaps.
 * 
 * We deviate from the example in that the first accrual start date is the trade date and not the previous Imm date.
 * 
 * @author Alessandro Gnoatto
 *
 */
public class CdsScheduleTest {
	
	public static void main(String[] args){
				
		LocalDate tradeDate = LocalDate.of(2009, 2, 9);
		CdsPeriods myCdsPeriods = new CdsPeriods(tradeDate, "1Y");
		
		System.out.println("Accrual Start"+ "\t"+"Accrual End" + "\t"+ "Pay Date");
		
		List<Period> myCdsPeriodTable = myCdsPeriods.getCdsCoupons();
		for(int i = 0; i < myCdsPeriodTable.size();i++){
			Period ithCoupon = myCdsPeriodTable.get(i);
			
			System.out.println(ithCoupon.getPeriodStart() +"\t"+ ithCoupon.getPeriodEnd() +"\t"+ithCoupon.getPayment());
		}
	}

}
