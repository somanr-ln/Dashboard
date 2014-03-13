package org.hpccsystems.dashboard.entity.chart;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.hpccsystems.dashboard.api.entity.Field;

@XmlRootElement
public class XYChartData {
	
	private HpccConnection hpccConnection;
	
	private String fileName;
	
	private List<Attribute> xColumnNames;
	private List<Measure> yColumns;
	private List<String> tableColumns;
	
	
	private Boolean isFiltered = false;
	private Set<Filter> filterSet;
	
	private Boolean isGrouped = false;
	private Group group;
	
	private List<Field> fields;
	
	@XmlElement
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@XmlElement
	public List<Measure> getYColumns() {
		if(yColumns == null) {
			yColumns = new ArrayList<Measure>();
		} 
		return yColumns;
	}

	public void setYColumns(List<Measure> yColumnNames) {
		this.yColumns = yColumnNames;
	}

	@XmlElement
	public Boolean getIsFiltered() {
		return isFiltered;
	}

	public void setIsFiltered(Boolean isFiltered) {
		this.isFiltered = isFiltered;
	}

	@XmlElement
	public final List<String> getTableColumns() {
		if(tableColumns == null) {
			tableColumns = new ArrayList<String>();
		}
		return tableColumns;
	}

	public final void setTableColumns(List<String> tableColumnName) {
		this.tableColumns = tableColumnName;
	}

	@XmlElement
	public HpccConnection getHpccConnection() {
		if(this.hpccConnection == null)
			this.hpccConnection = new HpccConnection();
		return hpccConnection;
	}

	public void setHpccConnection(HpccConnection hpccConnection) {
		this.hpccConnection = hpccConnection;
	}

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	@XmlElement
	public Boolean getIsGrouped() {
		return isGrouped;
	}

	public void setIsGrouped(Boolean isGrouped) {
		this.isGrouped = isGrouped;
	}

	@XmlElement
	public Set<Filter> getFilterSet() {
		if(filterSet == null){
			filterSet = new LinkedHashSet<Filter>();
		}
		return filterSet;
	}

	public void setFilterSet(Set<Filter> filterSet) {
		this.filterSet = filterSet;
	}
	
	@XmlElement
	public List<Field> getFields() {
		return fields;
	}
	
	public void setFields(List<Field> fields) {
		this.fields = fields;
	}

	@Override
	public String toString() {
		return "XYChartData [hpccConnection=" + hpccConnection + ", fileName="
				+ fileName + ", xColumnNames=" + xColumnNames + ", yColumns="
				+ yColumns + ", tableColumns=" + tableColumns + ", isFiltered="
				+ isFiltered + ", filterSet=" + filterSet + ", isGrouped="
				+ isGrouped + ", group=" + group + ", fields=" + fields + "]";
	}
	
	@XmlElement
	public List<Attribute> getxColumnNames() {
		
		if(xColumnNames == null) {
			xColumnNames = new ArrayList<Attribute>();
		} 
		return xColumnNames;
	}

	public void setxColumnNames(List<Attribute> xColumnNames) {
		this.xColumnNames = xColumnNames;
	}

	
}
