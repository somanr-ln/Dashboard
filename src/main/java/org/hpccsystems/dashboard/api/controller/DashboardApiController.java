package org.hpccsystems.dashboard.api.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hpccsystems.dashboard.api.entity.ChartConfiguration;
import org.hpccsystems.dashboard.api.entity.Field;
import org.hpccsystems.dashboard.common.Constants;
import org.hpccsystems.dashboard.entity.ChartDetails;
import org.hpccsystems.dashboard.entity.Dashboard;
import org.hpccsystems.dashboard.entity.Portlet;
import org.hpccsystems.dashboard.entity.chart.Filter;
import org.hpccsystems.dashboard.entity.chart.Measure;
import org.hpccsystems.dashboard.entity.chart.XYChartData;
import org.hpccsystems.dashboard.entity.chart.utils.ChartRenderer;
import org.hpccsystems.dashboard.services.AuthenticationService;
import org.hpccsystems.dashboard.services.DashboardService;
import org.hpccsystems.dashboard.services.WidgetService;
import org.hpccsystems.dashboard.util.DashboardUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.zkoss.json.JSONArray;
import org.zkoss.json.JSONObject;
import org.zkoss.util.resource.Labels;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Controller to interact with Circuit application
 *
 */
@Controller
@RequestMapping("*.do")
public class DashboardApiController {	
	private static final  Log LOG = LogFactory.getLog(DashboardApiController.class); 
	
	DashboardService dashboardService;
	WidgetService widgetService;
	AuthenticationService authenticationService;
	
	@Autowired
	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}
	@Autowired
	public void setDashboardService(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}
	@Autowired
	public void setWidgetService(WidgetService widgetService) {
		this.widgetService = widgetService;
	}
		
/**
 * Method to process delete dashboard request from circuit
 * 
 * No authentication set for the process
 * 
 * @param request
 * @param response
 */
@RequestMapping(value = Constants.CIRCUIT_DELETE_REQ)
public void deleteDashboard(HttpServletRequest request, HttpServletResponse response)throws Exception
{	
	JSONObject jsObj = new JSONObject();
	int rowsDeleted = 0;
	try { 		
			String dashboardId = null;
			dashboardId = request.getParameter(Constants.DB_DASHBOARD_ID);	
			rowsDeleted = dashboardService.deleteDashboard(Integer.valueOf(dashboardId),null);	
			if(rowsDeleted > 0)	{
				jsObj.put(Constants.STATUS, Constants.STATUS_SUCCESS);
			}else{
				jsObj.put(Constants.STATUS, Constants.STATUS_FAIL);
				jsObj.put(Constants.STATUS_MESSAGE, Constants.DASHBOARD_NOT_EXIST);
			}
		
			response.setContentType(Constants.RES_TEXT_TYPE_JSON);
			response.setCharacterEncoding(Constants.CHAR_CODE);
			response.getWriter().write(jsObj.toJSONString());
	}catch(DataAccessException ex)
	{
		LOG.error(Labels.getLabel("exceptionOnDeleteDashboardfromCircuit"),ex);
		jsObj.put(Constants.STATUS, Constants.STATUS_FAIL);
		jsObj.put(Constants.STATUS_MESSAGE, ex.getMessage());
	}
	catch(Exception ex) {
		LOG.error(Labels.getLabel("exceptionOnDeletingDashboardFromCircuit"),ex);
		throw new Exception(Labels.getLabel("unabletoDeleteDashboard"));
	}
	
}

@RequestMapping(value = Constants.CIRCUIT_CHARTLIST_REQ)
public void getChartList(HttpServletRequest request, HttpServletResponse response)throws Exception
 {
		try {
			
			String paramValue = request.getParameter(Constants.CHARTLIST_FORMAT);			
			if (paramValue != null &&Constants.JSON .equals(paramValue)) {
				JsonObject jsonObject;
				JsonArray jsonArray = new JsonArray();
				Map<Integer, ChartDetails> chartdetailsMap = Constants.CHART_MAP;
				for (Map.Entry<Integer, ChartDetails> entry : chartdetailsMap.entrySet()) {
					
					jsonObject = new JsonObject();
					jsonObject.addProperty(Constants.VALUE, entry.getValue().getChartId());
					jsonObject.addProperty(Constants.LABEL, entry.getValue().getChartName());
					jsonObject.addProperty(Constants.DESCRIPTION, entry.getValue().getChartDesc());
					jsonArray.add(jsonObject);
				}
				JsonObject result = new JsonObject();
				result.add(Constants.CHART_LIST, jsonArray);
				response.setContentType(Constants.RES_TEXT_TYPE_JSON);
				response.getWriter().write(result.toString());
			} 
		} catch (Exception e) {
			LOG.error(Labels.getLabel("exceptionOnChartListFromCircuit"), e);
			throw new Exception("Unable to process ChartLsit Request");
		}

	}

@RequestMapping(value = Constants.CIRCUIT_SEARCH_REQ)
public void searchDashboard(HttpServletRequest request, HttpServletResponse response)throws Exception
 {
	JSONObject jsonResposeObj = new JSONObject();
		try {			
			List<Dashboard> dashboardList = null;
			Dashboard dashBoard = null;
			JSONObject jsonObject = null;			
			JSONArray jsonArray = new JSONArray();
			dashboardList = dashboardService.retrieveDashboardMenuPages(
						request.getParameter(Constants.SOURCE),
						"2",
						null,
						request.getParameter(Constants.SOURCE_ID)
						);
			if (dashboardList != null) {
				for (final Iterator<Dashboard> iter = dashboardList.iterator(); iter.hasNext();) {
					dashBoard = (Dashboard) iter.next();
					jsonObject = new JSONObject();
					jsonObject.put(Constants.NAME_SMALL, dashBoard.getName());
					jsonObject.put(Constants.DB_DASHBOARD_ID, dashBoard.getDashboardId());
					jsonObject.put(Constants.LAST_UPDATED_DATE, dashBoard.getLastupdatedDate().toString());
					jsonArray.add(jsonObject);
				}
				if(jsonArray.size() > 0){
					jsonResposeObj.put(Constants.DASHBOARDS, jsonArray);
				}else{
					jsonResposeObj.put(Constants.DASHBOARDS,"No Dashboard Exists for the given Source" );
				}
			}			
			response.setContentType(Constants.RES_TEXT_TYPE_JSON);
			response.getWriter().write(jsonResposeObj.toString());
		}catch(DataAccessException ex){
			LOG.error(Labels.getLabel("exceptiononretrievingDashbaord"),ex);
			jsonResposeObj.put(Constants.STATUS_FAIL,ex.getMessage());
		}
		catch (Exception e) {
			LOG.error(Labels.getLabel("exceptionOnSearchDashboardFromCircuit"), e);
			throw new Exception("Unable to process Search Request");
		}
		
	}

/**
 * validateDashboard() is responsible for validate that the field list sent in contains all fields needed to render a dashboard. 
 * @param request
 * @param response
 * @throws IOException
 */
@RequestMapping(value = Constants.CIRCUIT_VALIDATE_REQ, method = RequestMethod.POST)
	public void validateDashboard(HttpServletRequest request, HttpServletResponse response) throws Exception {
	JsonObject jsonObject = new JsonObject();
		try {
			
			XYChartData chartData = null;
			List<String> xColumnList = null;
			List<Measure> yColumnList = null;
			String filterColumn = null;
			Integer filterDataType = 0;
			ChartConfiguration chartConfiguration = null;			
			List<String> failedValColumnList = new ArrayList<String>();
			String circuitFields = request.getParameter(Constants.CIRCUIT_CONFIG);
			String dashboard_id = request.getParameter(Constants.CIRCUIT_DASHBOARD_ID);
			chartConfiguration = new GsonBuilder().create().fromJson(circuitFields, ChartConfiguration.class);
			List<Portlet> portletList = widgetService.retriveWidgetDetails(Integer.valueOf(dashboard_id));
			
			if (portletList != null && portletList.size() > 0) {
				ChartRenderer chartRenderer = new ChartRenderer();
				for (Portlet portlet : portletList) {
					chartData = chartRenderer.parseXML(portlet.getChartDataXML());
					if (chartData != null) {
						xColumnList = chartData.getXColumnNames();
						yColumnList = chartData.getYColumns();
						// For XAxis & YAxis Validation
						xColumnValidation(failedValColumnList, xColumnList,	chartConfiguration);
						yColumnValidation(failedValColumnList, yColumnList,	chartConfiguration);
						// Filter Column Validation
						if (chartData.getIsFiltered()) {
							for (Filter filter : chartData.getFilterList()) {
								filterColumn = filter.getColumn();
								filterDataType = filter.getType();
								filterColumnValidation(failedValColumnList,	filterColumn, filterDataType, chartConfiguration);
							}
						}
					}
				}
				if (failedValColumnList.isEmpty()) {
					jsonObject.addProperty(Constants.STATUS, Constants.STATUS_SUCCESS);
				} else {
					jsonObject.addProperty(Constants.STATUS, Constants.STATUS_FAIL);
					StringBuilder failedStr = new StringBuilder();
					int index = 0;
					for (String failedColumn : failedValColumnList) {
						if (index != failedValColumnList.size() - 1) {
							failedStr.append(failedColumn).append(",");
						} else if (index == failedValColumnList.size() - 1) {
							failedStr.append(failedColumn);
						}
						index++;
					}
					if(failedValColumnList.size() == 1){
						failedStr.append(Constants.FIELD_NOT_EXIST);
					}else if(failedValColumnList.size() > 1){
						failedStr.append(Constants.FIELDS_NOT_EXIST);
					}
					jsonObject.addProperty(Constants.STATUS_MESSAGE, failedStr.toString());
				}

			} else {
				jsonObject.addProperty(Constants.STATUS, Constants.STATUS_FAIL);
				jsonObject.addProperty(Constants.STATUS_MESSAGE, Constants.DASHBOARD_NOT_EXISTS);
			}
			response.setContentType(Constants.RES_TEXT_TYPE_JSON);
			response.setCharacterEncoding(Constants.CHAR_CODE);
			response.getWriter().write(jsonObject.toString());
		} catch (DataAccessException ex) {
				LOG.error(Labels.getLabel("widgetException"), ex);
				jsonObject.addProperty(Constants.STATUS, Constants.STATUS_FAIL);
				jsonObject.addProperty(Constants.STATUS_MESSAGE, ex.getMessage());
		}catch (Exception e) {
			LOG.error(Labels.getLabel("exceptionOnValidateDashboardFromCircuit"), e);
			throw new Exception("Unable to process Validate Request");
		}
	}

	/**
	 * xColumnValidation() is responsible for Validate the xColumn.
	 * @param failedColumnList
	 * @param xColumnList
	 * @param configuration
	 * @return
	 */
	private void xColumnValidation(List<String> failedColumnList,
			List<String> xColumnList, ChartConfiguration configuration) {
		Boolean xAxisValStatus = false;
		if(xColumnList != null){
		for (String fieldValue : xColumnList) {
			for (Field entry : configuration.getFields()) {
				if (fieldValue.equals(entry.getColumnName().trim())) {
					xAxisValStatus = true;
					break;
				}
			}
			if (!xAxisValStatus && !failedColumnList.contains(fieldValue.trim())) {
				failedColumnList.add(fieldValue.trim());
			}
			xAxisValStatus = false;
		  }
		}
	}

	/**
	 * yColumnValidation() is responsible for Validate the yColumn.
	 * @param failedColumnList
	 * @param yColumnList
	 * @param configuration
	 * @return
	 */
	private void yColumnValidation(List<String> failedColumnList, 
			List<Measure> yColumnList, ChartConfiguration configuration) {
		Boolean yAxisValStatus = false;
		if(yColumnList != null){
		for (Measure measure : yColumnList) {
			for (Field entry : configuration.getFields()) {
				if (measure.equals(entry.getColumnName().trim())
						&& DashboardUtil.checkNumeric(entry.getDataType().trim()) ){
					yAxisValStatus = true;
					break;
				}
			}
			if (!yAxisValStatus && !failedColumnList.contains(measure.getColumn().trim())) {
				failedColumnList.add(measure.getColumn().trim());
			}
			yAxisValStatus = false;
		  }
		}
	}

	/**
	 * filterColumnValidation() is responsible for Validate the filterColumn.
	 * @param failedColumnList
	 * @param filterColumn
	 * @param filterDataType
	 * @param configuration
	 * @return
	 */
	private void filterColumnValidation(List<String> failedColumnList,
			String filterColumn, Integer filterDataType, ChartConfiguration configuration) {
		Boolean filterColumnValStatus = false;
		for (Field entry : configuration.getFields()) {
			if (filterColumn.equals(entry.getColumnName().trim())
					&& DashboardUtil.checkNumeric(entry.getDataType().trim())) {
				filterColumnValStatus = true;
				break;
			}
		}
		if (!filterColumnValStatus
				&& !failedColumnList.contains(filterColumn.trim())) {
			failedColumnList.add(filterColumn.trim());
		}
	}

	
	
}
