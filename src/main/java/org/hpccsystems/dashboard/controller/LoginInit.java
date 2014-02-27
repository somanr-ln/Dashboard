package org.hpccsystems.dashboard.controller;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hpccsystems.dashboard.services.AuthenticationService;
import org.hpccsystems.dashboard.services.UserCredential;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.util.Initiator;
import org.zkoss.zkplus.spring.SpringUtil;

public class LoginInit implements Initiator {
	private static final  Log LOG = LogFactory.getLog(LoginInit.class); 
	
	@Override
	public void doInit(Page arg0, Map<String, Object> arg1) throws Exception {
		
		String userName = Executions.getCurrent().getUserPrincipal().getName();
		LOG.debug("userName in LoginInit -->"+userName);
		
		AuthenticationService authenticationService = (AuthenticationService)SpringUtil.getBean("authenticationService");
		UserCredential cre = authenticationService.getUserCredential();
		
		if(userName != null){
			cre.setUserName(userName);
			cre.setUserId(userName);
		}
		if(LOG.isDebugEnabled()){
		LOG.debug("UserCredential in LoginInit -->"+cre);
		}
	}

}
