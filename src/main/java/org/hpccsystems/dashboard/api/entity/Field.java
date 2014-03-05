package org.hpccsystems.dashboard.api.entity;

public class Field {
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Field [columnName=").append(columnName)
				.append(", dataType=").append(dataType).append("]");
		return builder.toString() ;
	}
	String columnName;
	String dataType;
	
	public String getColumnName() {
		return columnName;
	}
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	public String getDataType() {
		return dataType;
	}
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}
}
