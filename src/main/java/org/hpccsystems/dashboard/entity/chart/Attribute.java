package org.hpccsystems.dashboard.entity.chart;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
 

@XmlRootElement
public class Attribute { 
	
	public Attribute(){}

	/**
	 * @param columnName
	 * Creates and Attribute and Sets display name as column name
	 */
	public Attribute(String columnName) {
		this.columnName = columnName;
		this.displayName = columnName;
	}

	private String columnName;
	private String displayName;
	
	@XmlAttribute
	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	
	@XmlAttribute
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
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
		return "Attribute [columnName=" + columnName + ", displayXColumnName="
				+ displayName + "]";
	}
	
	
}
