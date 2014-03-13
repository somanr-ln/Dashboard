package org.hpccsystems.dashboard.entity.chart;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
 

@XmlRootElement
public class Attribute { 
	
	public Attribute(){}

	/**
	 * Creates aggregated Attribute
	 * @param columnName
	 * @param displayXColumnName
	 */
	public Attribute(String columnName) {
		this.columnName = columnName;
	}

	private String columnName;
	private String displayXColumnName;
	
	@XmlAttribute
	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	
	@XmlAttribute
	public String getDisplayXColumnName() {
		return displayXColumnName;
	}

	public void setDisplayXColumnName(String displayXColumnName) {
		this.displayXColumnName = displayXColumnName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columnName == null) ? 0 : columnName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Attribute) {
			Attribute arg = (Attribute) obj;
			return this.columnName.equals(arg.getColumnName());
		} else if (obj instanceof Filter) {
			Filter arg = (Filter) obj;
			return this.columnName.equals(arg.getColumn());
		} else {
			return this.columnName.equals(obj);
		}
	}
	
	@Override
	public String toString() {
		return "Attribute [columnName=" + columnName +"]";
	}

}
