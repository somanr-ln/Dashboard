package org.hpccsystems.dashboard.util;

import org.hpccsystems.dashboard.entity.chart.HpccConnection;


public class DashboardUtil {	
	/**
	 * Checks whether a column is numeric
	 * @param column
	 * @param dataType
	 * @return
	 */
	public static boolean checkNumeric(final String dataType)
	{
		boolean numericColumn = false;
			if(dataType.contains("integer")	|| 
					dataType.contains("real") || 
					dataType.contains("decimal") ||  
					dataType.contains("unsigned"))	{
				numericColumn = true;
			}
		return numericColumn;
	}
	/**Method constructs Hpcc Object
	 * @return HpccConnection
	 */
	public HpccConnection constructHpccObj(){
		HpccConnection hpccConnection = new HpccConnection(
				"216.19.105.2", 18010, "", "generic_dashboard",
				"Lexis123!", true, false);	
		return hpccConnection;
			
	}

}
