package org.hpccsystems.dashboard.controller;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hpccsystems.dashboard.services.UserCredential;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.util.Initiator;

public class LoginInit implements Initiator {
	private static final  Log LOG = LogFactory.getLog(LoginInit.class); 
	
	@Override
	public void doInit(Page arg0, Map<String, Object> arg1) throws Exception {
		
		String userName = Executions.getCurrent().getUserPrincipal().getName();
		LOG.debug("userName in LoginInit -->"+userName);
		final Session sess = Sessions.getCurrent();
		UserCredential cre = (UserCredential)sess.getAttribute("userCredential");
		
		if(userName != null){
			cre = new UserCredential(userName, userName, "A001");
			sess.setAttribute("userCredential",cre);			
		}
		if(LOG.isDebugEnabled()){
		LOG.debug("UserCredential in LoginInit -->"+cre);
		}
	}

}
