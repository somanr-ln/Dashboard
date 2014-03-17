package org.hpccsystems.dashboard.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hpccsystems.dashboard.api.entity.Field;
import org.hpccsystems.dashboard.common.Constants;
import org.hpccsystems.dashboard.entity.FileMeta;
import org.hpccsystems.dashboard.entity.chart.Attribute;
import org.hpccsystems.dashboard.entity.chart.Filter;
import org.hpccsystems.dashboard.entity.chart.HpccConnection;
import org.hpccsystems.dashboard.entity.chart.Measure;
import org.hpccsystems.dashboard.entity.chart.XYChartData;
import org.hpccsystems.dashboard.entity.chart.XYModel;
import org.hpccsystems.dashboard.services.HPCCService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import ws_sql.ws.hpccsystems.ExecuteSQLRequest;
import ws_sql.ws.hpccsystems.ExecuteSQLResponse;
import ws_sql.ws.hpccsystems.Ws_sqlLocator;
import ws_sql.ws.hpccsystems.Ws_sqlServiceSoap;
import wsdfu.ws.hpccsystems.DFUInfoRequest;
import wsdfu.ws.hpccsystems.DFUInfoResponse;
import wsdfu.ws.hpccsystems.WsDfuLocator;
import wsdfu.ws.hpccsystems.WsDfuServiceSoap;

public class HPCCServiceImpl implements HPCCService{
	
	private static final  Log LOG = LogFactory.getLog(HPCCServiceImpl.class); 
		
	final static String WS_SQL_ENDPOINT = "8009/ws_sql?ver_=1";
	final static String DFU_ENDPOINT = "8010/WsDfu?ver_=1.2";
	
	public Set<Field> getColumnSchema(final String Sql, final HpccConnection hpccConnection) throws Exception
	{
		final Set<Field> columnSet = new HashSet<Field>();
		String[] rowObj=null,columnObj=null;Field fieldObj=null;
		try 
		{
			final WsDfuLocator locator = new WsDfuLocator();
			locator.setWsDfuServiceSoap_userName(hpccConnection.getUsername());
			locator.setWsDfuServiceSoap_password(hpccConnection.getPassword());
			if(hpccConnection.getIsSSL()) {
				locator.setWsDfuServiceSoapAddress("https://" + hpccConnection.getHostIp() + ":1" + DFU_ENDPOINT);
			} else {
				locator.setWsDfuServiceSoapAddress("http://" + hpccConnection.getHostIp() + ":" + DFU_ENDPOINT);
			}
			
			final WsDfuServiceSoap soap = locator.getWsDfuServiceSoap();
			final DFUInfoRequest req = new DFUInfoRequest();
			req.setName(Sql);
			req.setCluster("mythor");
			
			if(LOG.isDebugEnabled()){
				LOG.debug("Fetching Column Schema --> FileName: " + Sql );
				LOG.debug("Hpcc Connection : " + hpccConnection);
			}
			
			final DFUInfoResponse result = soap.DFUInfo(req);	
			
			//Two types of column schema results been parsed here to 
			// get column name and datatype
			if(result.getFileDetail()!=null)
			{
				final StringBuilder resultString =new StringBuilder(result.getFileDetail().getEcl());
				if(resultString.indexOf("{")!=-1)
				{
					resultString.replace(resultString.length()-3, resultString.length(), "").replace(0, 1, "");
					rowObj=resultString.toString().trim().split(",");
				}
				else
				{
					resultString.replace(resultString.indexOf("RECORD"), resultString.indexOf("RECORD")+6,"");
					resultString.replace(resultString.indexOf("END"), resultString.indexOf("END")+4,"");
					rowObj=resultString.toString().split(";");
				}
				for(String rowString:rowObj)
				{
					rowString=rowString.trim();
					if(rowString!=null && rowString.length()>0)
					{
						fieldObj = new Field();
						columnObj=rowString.split(" ");
						if(columnObj!=null && columnObj.length>1){
						fieldObj.setColumnName(columnObj[1]);
						fieldObj.setDataType(columnObj[0]);
						columnSet.add(fieldObj);
						}
					}
				}
			}
			else{
				throw new Exception(Constants.ERROR_RETRIEVE_COLUMNS);
			}
		} catch (ServiceException e) {
			LOG.error("ServiceException in getColumnSchema()", e);
			throw e;
		} catch (RemoteException e) {
			LOG.error("RemoteException in getColumnSchema()", e);
			throw e;
		}		
		if(LOG.isDebugEnabled()){
			LOG.debug("columnSet -->"+columnSet);
		}	
		return columnSet;
	}
	

	 /**
	  * getChartData() is used the retrieve the ChartData details and render the D3 charts.
	 * @param chartParamsMap
	 * @return List<BarChart>
	 * 
	 */
	public List<XYModel> getChartData(XYChartData chartData) throws Exception {
		final List<XYModel> dataList=new ArrayList<XYModel>();
		XYModel dataObj=null;
		try {
			final Ws_sqlLocator locator = new Ws_sqlLocator();
			locator.setWs_sqlServiceSoap_userName(chartData.getHpccConnection().getUsername());
			locator.setWs_sqlServiceSoap_password(chartData.getHpccConnection().getPassword());
			
			if(chartData.getHpccConnection().getIsSSL()) {
				locator.setWs_sqlServiceSoapAddress("https://" + chartData.getHpccConnection().getHostIp()+ ":1" + WS_SQL_ENDPOINT);
			} else {
				locator.setWs_sqlServiceSoapAddress("http://" + chartData.getHpccConnection().getHostIp()+ ":" + WS_SQL_ENDPOINT);
			}
			
			if(LOG.isDebugEnabled()){
				LOG.debug("Inside getChartData");
				if(chartData.getxColumnNames()!= null && chartData.getxColumnNames().size() > 0)				{
				LOG.debug("Column names --> " + chartData.getxColumnNames().get(0) + chartData.getYColumns().get(0));
				}
			}
			
			final Ws_sqlServiceSoap soap = locator.getws_sqlServiceSoap();
			final ExecuteSQLRequest req = new ExecuteSQLRequest();
			final String queryTxt = constructQuery(chartData);
			
			if(LOG.isDebugEnabled()) {
				LOG.debug("WS_SQL Query ->" + queryTxt);
			}
			
			req.setSqlText(queryTxt.toString());
			req.setTargetCluster("thor");
			final ExecuteSQLResponse result = soap.executeSQL(req);
			final String resultString = result.getResult();
			if(LOG.isDebugEnabled())
				LOG.debug("Result String: " + resultString);
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final InputSource inStream = new InputSource();
			inStream.setCharacterStream(new StringReader(resultString));
			final Document doc = db.parse(inStream);
			Node fstNode=null;Element fstElmnt=null,lstNmElmnt=null;NodeList lstNmElmntLst=null,lstNm=null;
			String nodeValue=null;
			
			List<Object> valueList = null;
			
			final NodeList nodeList = doc.getElementsByTagName("Row");
				for (int s = 0; s < nodeList.getLength(); s++) {
					 fstNode = nodeList.item(s);
					  if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
						dataObj=new XYModel();
					    
						fstElmnt = (Element) fstNode;
					    valueList = new ArrayList<Object>();
					    for(Attribute xColumnName : chartData.getxColumnNames()){
					    	lstNmElmntLst = fstElmnt.getElementsByTagName(xColumnName.getColumnName());
					    	lstNmElmnt = (Element) lstNmElmntLst.item(0);
					    	lstNm = lstNmElmnt.getChildNodes();
					    	if(lstNm.item(0) == null){
					    		nodeValue = "";
					    	}else{
					    		nodeValue = ((Node) lstNm.item(0)).getNodeValue();
					    	}
					    	valueList.add(nodeValue);
					    }
					    dataObj.setxAxisValues(valueList);
					    
					    valueList = new ArrayList<Object>();
					    int outCount = chartData.getxColumnNames().size() + 1;
					    for (Measure measure : chartData.getYColumns()) {
					    	lstNmElmntLst = fstElmnt.getElementsByTagName( measure.getAggregateFunction() + "out" + outCount);
					    	lstNmElmnt = (Element) lstNmElmntLst.item(0);
					    	lstNm = lstNmElmnt.getChildNodes();
					    	nodeValue = ((Node) lstNm.item(0)).getNodeValue();
					    	valueList.add(new BigDecimal(nodeValue));
					    	outCount ++;
						}
					    
					    dataObj.setyAxisValues(valueList);
					    dataList.add(dataObj);
					  }
				}
		} catch (ServiceException e) {
			LOG.error("ServiceException in getChartData()", e);
			throw e;
		} catch (RemoteException e) {
			LOG.error("RemoteException in getChartData()", e);
			throw e;
		}
		return dataList;
	}
	
	@Override
	public List<String> getDistinctValues(String fieldName, XYChartData chartData, Boolean applyFilter) throws Exception {
		List<String> filterDataList = new ArrayList<String>();
		
		final Ws_sqlLocator locator = new Ws_sqlLocator();
		locator.setWs_sqlServiceSoap_userName(chartData.getHpccConnection().getUsername());
		locator.setWs_sqlServiceSoap_password(chartData.getHpccConnection().getPassword());
		if(chartData.getHpccConnection().getIsSSL()) {
			locator.setWs_sqlServiceSoapAddress("https://" + chartData.getHpccConnection().getHostIp()+ ":1" + WS_SQL_ENDPOINT);
		} else {
			locator.setWs_sqlServiceSoapAddress("http://" + chartData.getHpccConnection().getHostIp()+ ":" + WS_SQL_ENDPOINT);
		}
		
		Ws_sqlServiceSoap soap;
		try {
			soap = locator.getws_sqlServiceSoap();
		final ExecuteSQLRequest req = new ExecuteSQLRequest();
		
		final StringBuilder queryTxt=new StringBuilder("select ");
		queryTxt.append(fieldName);
		queryTxt.append(" from ");
		queryTxt.append(chartData.getFileName());
		
		if(applyFilter){
			queryTxt.append(constructWhereClause(chartData));
		}

		queryTxt.append(" group by ");
		queryTxt.append(fieldName);
		queryTxt.append(" order by ");
		queryTxt.append(fieldName);
		
		if(LOG.isDebugEnabled()){
			LOG.debug("Query for Distinct values -> " + queryTxt.toString());
		}
		
		req.setSqlText(queryTxt.toString());
		req.setTargetCluster("thor");
		final ExecuteSQLResponse result = soap.executeSQL(req);
		final String resultString = result.getResult();
		
		if(LOG.isDebugEnabled()){
			LOG.debug("Hitting URL for filter - " + locator.getws_sqlServiceSoapAddress());
			LOG.debug("Result String: " + resultString);
		}
		
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final InputSource inStream = new InputSource();
		inStream.setCharacterStream(new StringReader(resultString));
		final Document doc = db.parse(inStream);
		
		doc.getDocumentElement().normalize();
		
		final NodeList nodeList = doc.getElementsByTagName("Row");
		
		Node nodeItem;
		Element element;
		for (int count = 0; count < nodeList.getLength(); count++) {
			nodeItem = nodeList.item(count);
			if (nodeItem.getNodeType() == Node.ELEMENT_NODE) {
				element = (Element) nodeItem;
				filterDataList.add(
						element.getElementsByTagName(fieldName).item(0).getTextContent()
						);
			}
			
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug(("filterDataList -->" + filterDataList));
		}
		}
		catch (ServiceException | ParserConfigurationException | SAXException | IOException ex) {
			LOG.error("Exception occurred while fetching String filter data in fetchFilterData()", ex);
			throw ex;
		} 
		return filterDataList;
	}

	@Override
	public Map<Integer, BigDecimal> getMinMax(String fieldName, XYChartData chartData, Boolean applyFilter) throws Exception {
		Map<Integer, BigDecimal> resultMap = new HashMap<Integer, BigDecimal>();	
		
		final Ws_sqlLocator locator = new Ws_sqlLocator();
		locator.setWs_sqlServiceSoap_userName(chartData.getHpccConnection().getUsername());
		locator.setWs_sqlServiceSoap_password(chartData.getHpccConnection().getPassword());
		if(chartData.getHpccConnection().getIsSSL()) {
			locator.setWs_sqlServiceSoapAddress("https://" + chartData.getHpccConnection().getHostIp()+ ":1" + WS_SQL_ENDPOINT);
		} else {
			locator.setWs_sqlServiceSoapAddress("http://" + chartData.getHpccConnection().getHostIp()+ ":" + WS_SQL_ENDPOINT);
		}
		
		Ws_sqlServiceSoap soap = null;
		try
		{
			soap = locator.getws_sqlServiceSoap();
		final ExecuteSQLRequest req = new ExecuteSQLRequest();
		
		//It is required to specify minimum value first in the query as result XML element names are dependent on the order
		final StringBuilder queryTxt=new StringBuilder("select min(")
			.append(fieldName)
			.append("), max(")
			.append(fieldName)
			.append(") from ")
			.append(chartData.getFileName());
		
		if(applyFilter){
			queryTxt.append(constructWhereClause(chartData));
		}
		
		req.setSqlText(queryTxt.toString());
		req.setTargetCluster("thor");
		final ExecuteSQLResponse result = soap.executeSQL(req);
		final String resultString = result.getResult();
		
		if(LOG.isDebugEnabled()){
			LOG.debug("queryTxt in fetchFilterMinMax() -->"+queryTxt) ;
			LOG.debug("Hitting URL for filter - " + locator.getws_sqlServiceSoapAddress());
			LOG.debug("Result String: " + resultString);
		}
		
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final InputSource inStream = new InputSource();
		inStream.setCharacterStream(new StringReader(resultString));
		final Document doc = db.parse(inStream);
		
		doc.getDocumentElement().normalize();
		
		final NodeList nodeList = doc.getElementsByTagName("Row");
		
		resultMap.put(Constants.FILTER_MINIMUM, 
				new BigDecimal(nodeList.item(0).getChildNodes().item(0).getTextContent()) );
		
		resultMap.put(Constants.FILTER_MAXIMUM, 
				new BigDecimal(nodeList.item(0).getChildNodes().item(1).getTextContent()) );
		}catch (ServiceException | ParserConfigurationException | SAXException | IOException ex) {
			LOG.error("Exception occurred while fetching Numeric filter data in fetchFilterMinMax()", ex);
			throw ex;
		}
		return resultMap;
	}
	
	
	/**
	 * Constructs a where clause only when ChartData is Filtered
	 * 
	 * @param chartData
	 * @return
	 */
	private String constructWhereClause(XYChartData chartData) {
		StringBuilder queryTxt = new StringBuilder();
		
		if(chartData.getIsFiltered()){
			queryTxt.append(" where ");
			
			Iterator<Filter> iterator = chartData.getFilterSet().iterator();
			while (iterator.hasNext()) {
				Filter filter = iterator.next();
				
				if(LOG.isDebugEnabled()){
					LOG.debug("Contructing where clause " + filter.toString());
				}
				
				queryTxt.append("(");
				
				if( Constants.STRING_DATA.equals(filter.getType())) {
					queryTxt.append(filter.getColumn());
					queryTxt.append(" in ");
					queryTxt.append(" (");
					
					for(int i=1;i<= filter.getValues().size(); i++){
						
						queryTxt.append(" '").append( filter.getValues().get(i-1)).append("'");
						
						if(i<filter.getValues().size()){
							queryTxt.append(",");
						}	
					}
					
					queryTxt.append(" )");
				} else if(Constants.NUMERIC_DATA.equals(filter.getType())) {
					queryTxt.append(filter.getColumn());
					queryTxt.append(" >= ");
					queryTxt.append(filter.getStartValue().setScale(0, RoundingMode.DOWN));
					queryTxt.append(" and ");
					queryTxt.append(filter.getColumn());
					queryTxt.append(" <= ");
					queryTxt.append(filter.getEndValue().setScale(0, RoundingMode.CEILING));
				}
				
				queryTxt.append(")");
				
				if(iterator.hasNext()) {
					queryTxt.append(" AND ");
				}
			}
		}
		return queryTxt.toString();
	}
	
	/**
	 * Method to generate query for Hpcc
	 * @param chartParamsMap
	 * @return StringBuilder
	 * 
	 */
	private String constructQuery(XYChartData chartData) {
		StringBuilder queryTxt = new StringBuilder("select ");
		try {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Building Query");
				LOG.debug("isFiltered -> " + chartData.getIsFiltered());
			}

			for (Attribute columnName : chartData.getxColumnNames()) {
				queryTxt.append(columnName.getColumnName());
				queryTxt.append(", ");
			}

			for (Measure measure : chartData.getYColumns()) {
				queryTxt.append(measure.getAggregateFunction());
				queryTxt.append("(");
				queryTxt.append(measure.getColumn());
				queryTxt.append("),");
			}
			// Deleting last comma
			queryTxt.deleteCharAt(queryTxt.length() - 1);
			queryTxt.append(" from ");
			queryTxt.append(chartData.getFileName());
			queryTxt.append(constructWhereClause(chartData));
			queryTxt.append(" group by ");
			
			for (Attribute columnName : chartData.getxColumnNames()) {
				queryTxt.append(columnName.getColumnName());
				queryTxt.append(",");
			}
			// Deleting last comma
			queryTxt.deleteCharAt(queryTxt.length() - 1);

			queryTxt.append(" order by ");
			
			for (Attribute columnName : chartData.getxColumnNames()) {
				queryTxt.append(columnName.getColumnName());
				queryTxt.append(",");
			}
			// Deleting last comma
			queryTxt.deleteCharAt(queryTxt.length() - 1);
			
		} catch (Exception e) {
			LOG.error("Exception while constructing query in constructQuery()",	e);
		}
		return queryTxt.toString();
	}
	
	/**
	 * fetchTableData() is used to retrieve the Column values from HPCC systems 
	 * to construct Table Widget.
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public LinkedHashMap<String, List<Attribute>> fetchTableData(XYChartData tableData)
			throws Exception {
		final StringBuilder queryTxt = new StringBuilder("select ");
		final Ws_sqlLocator locator = new Ws_sqlLocator();
		locator.setWs_sqlServiceSoap_userName(tableData.getHpccConnection().getUsername());
		locator.setWs_sqlServiceSoap_password(tableData.getHpccConnection().getPassword());
		if(tableData.getHpccConnection().getIsSSL()) {
			locator.setWs_sqlServiceSoapAddress("https://" + tableData.getHpccConnection().getHostIp()+ ":1" + WS_SQL_ENDPOINT);
		} else {
			locator.setWs_sqlServiceSoapAddress("http://" + tableData.getHpccConnection().getHostIp()+ ":" + WS_SQL_ENDPOINT);
		}
		LinkedHashMap<String, List<Attribute>> tableDataMap = new LinkedHashMap<String, List<Attribute>>();
		try
		{
		final Ws_sqlServiceSoap soap = locator.getws_sqlServiceSoap();
		final ExecuteSQLRequest req = new ExecuteSQLRequest();

		List<Attribute> listData = tableData.getTableColumns();
		int index = 0;
		for (Attribute data : listData) {
			if (index != listData.size() - 1) {
				queryTxt.append(data.getColumnName()).append(",");
			} else if (index == listData.size() - 1) {
				queryTxt.append(data.getColumnName());
			}
			index++;
		}
		queryTxt.append(" from ");
		queryTxt.append(tableData.getFileName());
		if(tableData.getIsFiltered() && tableData.getFilterSet().size() > 0){
			queryTxt.append(constructWhereClause(tableData));
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("queryTxt --> " + queryTxt);
		}
		req.setSqlText(queryTxt.toString());
		req.setTargetCluster("thor");
		final ExecuteSQLResponse result = soap.executeSQL(req);
		final String resultString = result.getResult();
		if (LOG.isDebugEnabled()) {
			LOG.debug("Hitting URL for filter - "+ locator.getws_sqlServiceSoapAddress());
			LOG.debug("Result String: " + resultString);
		}

		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final InputSource inStream = new InputSource();
		inStream.setCharacterStream(new StringReader(resultString));
		final Document doc = db.parse(inStream);
		Node fstNode = null;
		Element fstElmnt = null, lstNmElmnt = null;
		NodeList lstNmElmntLst = null;
		List<Attribute> columnListvalue =null;
		for (Attribute columnName : tableData.getTableColumns()) {
			columnListvalue = new ArrayList<Attribute>();
			tableDataMap.put(columnName.getColumnName(), columnListvalue);
		}
		final NodeList nodeList = doc.getElementsByTagName("Row");
		if (nodeList != null) {
			for (int count = 0; count < nodeList.getLength(); count++) {
				fstNode = nodeList.item(count);
				if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
					fstElmnt = (Element) fstNode;
					for (Attribute data : tableData.getTableColumns()) {
						lstNmElmntLst = fstElmnt.getElementsByTagName(data.getColumnName());
						lstNmElmnt = (Element) lstNmElmntLst.item(0);
						String str = lstNmElmnt.getTextContent();
						columnListvalue = tableDataMap.get(lstNmElmnt.getNodeName());
						columnListvalue.add(new Attribute(str));
					}
				}
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug(("tableDataMap -->" + tableDataMap));
		}
		}catch (ServiceException | ParserConfigurationException | SAXException | IOException ex) {
			LOG.error("Exception occurred while fetching TAble Data data in fetchTableData()", ex);
			throw ex;
		}		
		return tableDataMap;
	}


	public List<FileMeta> getFileList(String scope, HpccConnection hpccConnection) throws Exception{
		ECLSoap soap = new ECLSoap();
		soap.setHostname(hpccConnection.getHostIp());
		soap.setUser(hpccConnection.getUsername());
		soap.setPass(hpccConnection.getPassword());
		soap.setSSL(hpccConnection.getIsSSL());
		
		
		soap.setUser(hpccConnection.getUsername());
		soap.setPass(hpccConnection.getPassword());		
		StringBuffer xmlInitial = new StringBuffer();
		xmlInitial.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
		.append("<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">")
		.append("<soap:Body>") 
		.append( "<DFUFileView xmlns=\"urn:hpccsystems:ws:wsdfu\">")
		.append("<Scope>")
		.append(scope)
		.append("</Scope>")
		.append("</DFUFileView>")
		.append("</soap:Body>")
		.append("</soap:Envelope>");		

		String path = "/WsDfu/DFUFileView?ver_=1.2";

		InputStream is = soap.doSoap(xmlInitial.toString(), path);
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("Response ->" + is.toString());
		}
		
		ArrayList<FileMeta> results = new ArrayList<FileMeta>();
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();

			Document dom = db.parse(is);

			Element docElement = dom.getDocumentElement();
			NodeList dfuResponse = docElement
					.getElementsByTagName(Constants.DFU_FILE_RESPONSE);
			if (dfuResponse != null && dfuResponse.getLength() > 0) {
				FileMeta fileMeta;
				for (int i = 0; i < dfuResponse.getLength(); i++) {
					Element ds = (Element) dfuResponse.item(i);
					NodeList rowList = ds.getElementsByTagName(Constants.DFU_LOGICAL_FILE);
					if (rowList != null && rowList.getLength() > 0) {
						
						for (int j = 0; j < rowList.getLength(); j++) {

							Element row = (Element) rowList.item(j);
							String val = "";
							fileMeta = new FileMeta();
							String isDir = row.getElementsByTagName(Constants.TREE_IS_DIRECTORY)
									.item(0).getTextContent();
							if (isDir.equals(Constants.ONE)) {
								val = row.getElementsByTagName(Constants.TREE_DIRECTORY).item(0).getTextContent();
								LOG.debug("Getting for scope - " + scope);
								if(scope.length() > 0 ){
									fileMeta.setScope(scope + "::" + val);
									LOG.debug("Set scope as - " + scope + "::" + val);
								} else {
									fileMeta.setScope(val);
									LOG.debug("Set scope as - " + val);
								}
								
								fileMeta.setFileName(val);
								fileMeta.setIsDirectory(true);
							} else {
								val = row.getElementsByTagName(Constants.NAME).item(0)
										.getTextContent();
								fileMeta.setFileName(val);
								fileMeta.setIsDirectory(false);
							}
							results.add(fileMeta);						
						}
					}

				}
			}
		  }catch (ParserConfigurationException | SAXException | IOException ex) {
				LOG.error("Exception occurred while fetching files for selected scope in getFileList()", ex);
				throw ex;
			} 
		return results;
	}


	@Override
	public List<List<String>> getFirstLevel(String fName, String lName, HpccConnection hpccConnection) throws Exception {

		List<List<String>> list = new ArrayList<List<String>>();
		
		final Ws_sqlLocator locator = new Ws_sqlLocator();
		locator.setWs_sqlServiceSoap_userName(hpccConnection.getUsername());
		locator.setWs_sqlServiceSoap_password(hpccConnection.getPassword());
		if(hpccConnection.getIsSSL()) {
			locator.setWs_sqlServiceSoapAddress("https://" + hpccConnection.getHostIp()+ ":1" + WS_SQL_ENDPOINT);
		} else {
			locator.setWs_sqlServiceSoapAddress("http://" + hpccConnection.getHostIp()+ ":" + WS_SQL_ENDPOINT);
		}
		
		Ws_sqlServiceSoap soap;
		try {
			soap = locator.getws_sqlServiceSoap();
			final ExecuteSQLRequest req = new ExecuteSQLRequest();
			
			final StringBuilder queryTxt=new StringBuilder("select prim_range, prim_name, addr_suffix, v_city_name, st  from test::providers where lname = '");
			queryTxt.append(lName);
			queryTxt.append("' and fname = '");
			queryTxt.append(fName);
			queryTxt.append("'");
			
			if(LOG.isDebugEnabled()){
				LOG.debug("Query for First level -> " + queryTxt.toString());
			}
			
			req.setSqlText(queryTxt.toString());
			req.setTargetCluster("thor");
			final ExecuteSQLResponse result = soap.executeSQL(req);
			final String resultString = result.getResult();
			
			if(LOG.isDebugEnabled()){
				LOG.debug("Result String: " + resultString);
			}
			
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final InputSource inStream = new InputSource();
			inStream.setCharacterStream(new StringReader(resultString));
			final Document doc = db.parse(inStream);
			
			NodeList nList = doc.getElementsByTagName("Row");
			
			List<String> row;
			for (int i = 0; i < nList.getLength(); i++) {
				Node nNode = nList.item(i);
				
				row = new ArrayList<String>();
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					
					row.add(eElement.getElementsByTagName("prim_range").item(0).getTextContent());
					row.add(eElement.getElementsByTagName("prim_name").item(0).getTextContent());
					row.add(eElement.getElementsByTagName("addr_suffix").item(0).getTextContent());
				}
				
				list.add(row);
			}
		
		}
		catch (ServiceException | ParserConfigurationException | SAXException | IOException ex) {
			LOG.error("Exception occurred while fetching String filter data in fetchFilterData()", ex);
			throw ex;
		} 
		return list;
	}
	
	@Override
	public List<List<String>> getSecondLevel(String primRange, String primName, HpccConnection hpccConnection) throws Exception {
		
		List<List<String>> list = new ArrayList<List<String>>();
		
		final Ws_sqlLocator locator = new Ws_sqlLocator();
		locator.setWs_sqlServiceSoap_userName(hpccConnection.getUsername());
		locator.setWs_sqlServiceSoap_password(hpccConnection.getPassword());
		if(hpccConnection.getIsSSL()) {
			locator.setWs_sqlServiceSoapAddress("https://" + hpccConnection.getHostIp()+ ":1" + WS_SQL_ENDPOINT);
		} else {
			locator.setWs_sqlServiceSoapAddress("http://" + hpccConnection.getHostIp()+ ":" + WS_SQL_ENDPOINT);
		}
		
		Ws_sqlServiceSoap soap;
		try {
			soap = locator.getws_sqlServiceSoap();
			final ExecuteSQLRequest req = new ExecuteSQLRequest();
			
			final StringBuilder queryTxt=new StringBuilder("select t1.lname, t1.fname from test::providers as t1 inner join test::facility as t2 on (t2.lnpid = t1.lnpid AND t2.prim_range = t1.prim_range and t2.prim_name = t1.prim_name AND (t2.prim_range = '");
			queryTxt.append(primRange);
			queryTxt.append("' AND t2.prim_name = '");
			queryTxt.append(primName);
			queryTxt.append("'))");
			
			if(LOG.isDebugEnabled()){
				LOG.debug("Query for First level -> " + queryTxt.toString());
			}
			
			req.setSqlText(queryTxt.toString());
			req.setTargetCluster("thor");
			final ExecuteSQLResponse result = soap.executeSQL(req);
			final String resultString = result.getResult();
			
			if(LOG.isDebugEnabled()){
				LOG.debug("Result String: " + resultString);
			}
			
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final InputSource inStream = new InputSource();
			inStream.setCharacterStream(new StringReader(resultString));
			final Document doc = db.parse(inStream);
			
			NodeList nList = doc.getElementsByTagName("Row");
			
			List<String> row;
			for (int i = 0; i < nList.getLength(); i++) {
				Node nNode = nList.item(i);
				
				row = new ArrayList<String>();
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					
					row.add(eElement.getElementsByTagName("fname").item(0).getTextContent());
					row.add(eElement.getElementsByTagName("lname").item(0).getTextContent());
				}
				
				list.add(row);
			}
			
		} catch (ServiceException | ParserConfigurationException | SAXException | IOException ex) {
			LOG.error("Exception occurred while fetching String filter data in fetchFilterData()", ex);
			throw ex;
		} 
		return list;
	}


	@Override 
	public String[] getRootKeyList(HpccConnection hpccConnection) throws Exception {
		
		List<String> fnameLnameList = new ArrayList<String>();
		String[] fnameLnameArray = null;
		final Ws_sqlLocator locator = new Ws_sqlLocator();
		locator.setWs_sqlServiceSoap_userName(hpccConnection.getUsername());
		locator.setWs_sqlServiceSoap_password(hpccConnection.getPassword());
		if(hpccConnection.getIsSSL()) {
			locator.setWs_sqlServiceSoapAddress("https://" + hpccConnection.getHostIp()+ ":1" + WS_SQL_ENDPOINT);
		} else {
			locator.setWs_sqlServiceSoapAddress("http://" + hpccConnection.getHostIp()+ ":" + WS_SQL_ENDPOINT);
		}
		
		Ws_sqlServiceSoap soap;
		try {
			soap = locator.getws_sqlServiceSoap();
			final ExecuteSQLRequest req = new ExecuteSQLRequest();
			
			final StringBuilder queryTxt=new StringBuilder("select fname,lname from test::providers group by fname,lname");
			
			if(LOG.isDebugEnabled()){
				LOG.debug("queryTxt in getRootKeyList() -> " + queryTxt.toString());
			}
			
			req.setSqlText(queryTxt.toString());
			req.setTargetCluster("thor");
			final ExecuteSQLResponse result = soap.executeSQL(req);
			final String resultString = result.getResult();
			
			if(LOG.isDebugEnabled()){
				LOG.debug("Result String in getRootKeyList() --> " + resultString);
			}
			
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final InputSource inStream = new InputSource();
			inStream.setCharacterStream(new StringReader(resultString));
			final Document doc = db.parse(inStream);
			
			NodeList nList = doc.getElementsByTagName("Row");
			StringBuilder fnameLname=null;
			for (int i = 0; i < nList.getLength(); i++) {
				Node nNode = nList.item(i);				
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					fnameLname = new StringBuilder();
					fnameLname.append(eElement.getElementsByTagName("fname").item(0).getTextContent());
					fnameLname.append(" ");
					fnameLname.append(eElement.getElementsByTagName("lname").item(0).getTextContent());
				}
				fnameLnameList.add(fnameLname.toString());
			}
			fnameLnameArray = fnameLnameList.toArray((new String[fnameLnameList.size()]));
			if(LOG.isDebugEnabled()){
				LOG.debug("fnameLnameList in getRootKeyList() --> " + fnameLnameList);
			}
		}catch(ServiceException | ParserConfigurationException | SAXException | IOException ex){
			LOG.error("Exception occurred while fetching Root key List in getRootKeyList()", ex);
			throw ex;
		}
		return fnameLnameArray;
	}


	
}
