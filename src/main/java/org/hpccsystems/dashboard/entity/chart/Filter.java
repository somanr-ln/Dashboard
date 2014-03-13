package org.hpccsystems.dashboard.entity.chart;

import java.math.BigDecimal;
import java.util.List; 

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Filter {
	private Integer type;
	private String column;
	/**
	 *  Present only got String Filter
	 */
	private List<String> values;
	
	/**
	 * Present only for Numeric Filter
	 */
	private BigDecimal StartValue;
	private BigDecimal EndValue;
	
	private boolean isCommonFilter = false;
	
	
	@XmlAttribute
	public Integer getType() {
		return type;
	}
	
	public void setType(Integer filterType) {
		this.type = filterType;
	}
	
	@XmlElement
	public List<String> getValues() {
		return values;
	}
	
	public void setValues(List<String> list) {
		this.values = list;
	}
	
	@XmlElement
	public BigDecimal getStartValue() {
		return StartValue;
	}
	
	public void setStartValue(BigDecimal startValue) {
		StartValue = startValue;
	}
	
	@XmlElement
	public BigDecimal getEndValue() {
		return EndValue;
	}
	
	public void setEndValue(BigDecimal endValue) {
		EndValue = endValue;
	}
	
	@XmlElement
	public String getColumn() {
		return column;
	}
	
	public void setColumn(String column) {
		this.column = column;
	}
	
	@XmlElement
	public boolean getIsCommonFilter() {
		return isCommonFilter;
	}
	
	public void setIsCommonFilter(boolean isGlobalFilter) {
		this.isCommonFilter = isGlobalFilter;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Filter){
			Filter filter= (Filter) obj;
			if(this.column.equals(filter.getColumn())){
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return this.column.hashCode();
	}

	@Override
	public String toString() {
		return "Filter [type=" + type + ", column=" + column + ", values="
				+ values + ", StartValue=" + StartValue + ", EndValue="
				+ EndValue + ", isGlobalFilter=" + isCommonFilter + "]";
	}
	
	
}
