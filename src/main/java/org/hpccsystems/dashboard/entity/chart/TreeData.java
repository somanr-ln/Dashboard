package org.hpccsystems.dashboard.entity.chart;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class TreeData {
	
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("TreeData [hpccConnection=").append(hpccConnection)
				.append("]");
		return  buffer.toString();
	}

	private HpccConnection hpccConnection;

	@XmlElement
	public HpccConnection getHpccConnection() {
		return hpccConnection;
	}

	public void setHpccConnection(HpccConnection hpccConnection) {
		this.hpccConnection = hpccConnection;
	}
}
