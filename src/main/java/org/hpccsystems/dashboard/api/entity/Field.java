package org.hpccsystems.dashboard.api.entity;

public class Field {
	
	String columnName;
	String dataType;
	String displayColumnName;
	
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
	
	public String getDisplayColumnName() {
		return displayColumnName;
	}
	public void setDisplayColumnName(String displayColumnName) {
		this.displayColumnName = displayColumnName;
	}
	
	@Override
	public int hashCode() {		
		return (columnName == null) ? 0 : columnName.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		
		if(obj instanceof Field) {
			Field other = (Field) obj;
			if (columnName == null) {
				if (other.columnName != null)
					return false;
			} else if (!columnName.equals(other.columnName)){
				return false;
			}
			return true;
		} else {
			return obj.equals(this.columnName);
		}
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Field [columnName=").append(columnName)
				.append(", dataType=").append(dataType).append("]");
		return builder.toString() ;
	}
}
