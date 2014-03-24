package org.hpccsystems.dashboard.entity.chart.utils;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hpccsystems.dashboard.entity.Portlet;
import org.hpccsystems.dashboard.entity.chart.HpccConnection;
import org.hpccsystems.dashboard.entity.chart.XYChartData;
import org.hpccsystems.dashboard.services.HPCCService;
import org.hpccsystems.dashboard.util.DashboardUtil;
import org.zkoss.zkplus.spring.SpringUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Class defines functionality of Chord Diagram
 *
 */
public class ChordRenderer {
	
private static final  Log LOG = LogFactory.getLog(ChordRenderer.class);

/**
 * Constructs JSON to draw Chord Diagram
 */
public Portlet constructChordJSON(Portlet portlet){
	final JsonObject jsonObj = new JsonObject();
	 JsonArray rows = new JsonArray();
		try {
			portlet.setChartData(new XYChartData());
			portlet.getChartData().setHpccConnection(new DashboardUtil().constructHpccObj());
			List<Map<String, Integer>> dataList = getChartData(portlet.getChartData().getHpccConnection());
			Integer[][] dataArray = formMatrix(dataList);
			int rowCount = dataArray[0].length;
			int columnCount = dataArray.length;

			JsonArray row = null;
			for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
				row = new JsonArray();
				for (int colIndex = 0; colIndex < columnCount; colIndex++) {
					row.add(new JsonPrimitive(dataArray[rowIndex][colIndex]));
				}
				rows.add(row);
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("Chord JSON -->" + rows);
			}
			jsonObj.add("matrix", rows);
			portlet.setChartDataJSON(jsonObj.toString());
		}catch(Exception ex){
		LOG.error("Exception in drawChord()"+ex);
	}
		return portlet;	
}

/**
 * form 2D array to construct matrix
 * @param dataList
 * @return Integer[][]
 */
private Integer[][] formMatrix(List<Map<String, Integer>> dataList) {
	Map<String, Integer> indiaMap = dataList.get(0);
	Map<String, Integer> usMap = dataList.get(1);
	Map<String, Integer> ukMap = dataList.get(2);
	Integer[][] matrixArray = new Integer[3][3];
	
	//Considering matrixArray[0] --> living India
	//matrixArray[1] --> living Us
	//matrixArray[2] --> living UK 
	
	//People Living in India
		for(Entry<String,Integer> entry :indiaMap.entrySet()){
			if("India".equalsIgnoreCase(entry.getKey())){ //Prefers India
				matrixArray[0][0] = entry.getValue();
			}else if("Us".equalsIgnoreCase(entry.getKey())){ //Prefers Us
				matrixArray[0][1] = entry.getValue();
			}else{                                            //Prefers Uk
				matrixArray[0][2] = entry.getValue();
			}				
		}
		//People Living in Us
				for(Entry<String,Integer> entry :usMap.entrySet()){
					if("India".equalsIgnoreCase(entry.getKey())){
						matrixArray[1][0] = entry.getValue();
					}else if("Us".equalsIgnoreCase(entry.getKey())){
						matrixArray[1][1] = entry.getValue();
					}else{
						matrixArray[1][2] = entry.getValue();
					}				
				}
				
		//People Living in Uk
				for(Entry<String,Integer> entry :ukMap.entrySet()){
					if("India".equalsIgnoreCase(entry.getKey())){
						matrixArray[2][0] = entry.getValue();
					}else if("Us".equalsIgnoreCase(entry.getKey())){
						matrixArray[2][1] = entry.getValue();
					}else{
						matrixArray[2][2] = entry.getValue();
					}				
				}
		return matrixArray;
}

/**
 * gets Chord data from Hpcc
 * @return List<Map<String, Integer>>
 * @throws Exception
 */
private  List<Map<String, Integer>> getChartData(HpccConnection hpccConnection) throws Exception{
	HPCCService hpccService = (HPCCService)SpringUtil.getBean("hpccService");
	return hpccService.getChordData(hpccConnection);
}
}
