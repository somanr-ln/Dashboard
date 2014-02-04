package org.hpccsystems.dashboard.api.richlet;

import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hpccsystems.dashboard.entity.ApiConfiguration;
import org.hpccsystems.dashboard.entity.User;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.GenericRichlet;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.util.Clients;

public class ChartSettings extends GenericRichlet{

	private static final Log LOG = LogFactory.getLog(ChartSettings.class);
	
	@Override
	public void service(Page page) throws Exception {		
		String source;
		String sourceId;
		String format;
		String config;
		
		try {
			try{
			Map<String, String[]> args = Executions.getCurrent().getParameterMap();
			source = args.get("source")[0];
			sourceId = args.get("source_id")[0];
			format = args.get("format")[0];
			config = args.get("config")[0];
			}catch(Exception ex){
				Clients.showNotification("Malformated URL string", false);
				LOG.error("Exception while parsing Request Parameter in ChartSettings.service()", ex);
				return;			
			}
			ApiConfiguration apiConfig = new ApiConfiguration();
			apiConfig.setApiChartSetting(true);
			Sessions.getCurrent().setAttribute("apiConfiguration", apiConfig);	
			//TODO: have to set user details into session
			User user =  new User();
			user.setFullName("admin");
			user.setUserId("2");
			user.setValidUser(true);
			user.setActiveFlag("Y"); 
			Sessions.getCurrent().setAttribute("user", user);	
			if(LOG.isDebugEnabled()) {
				LOG.debug("Creating API edit portlet screen...");
			}
			StringBuilder url = new StringBuilder("/demo/layout/edit_chart.zul?");
			url.append("source").append("=").append(source).append("&")
					.append("source_id").append("=").append(sourceId).append("&")
					.append("format").append("=").append(format).append("&")
					.append("config").append("=").append(config);
			Executions.sendRedirect(url.toString());		
		} catch (Exception e) {
			Clients.showNotification("Unable to open Configure Window.Please input correct data", false);
			LOG.error("Exception while parsing Request Parameter in ChartSettings.service()", e);
			//Executions.sendRedirect("/demo/index.zul");
			return;
		}
		
	}

}
