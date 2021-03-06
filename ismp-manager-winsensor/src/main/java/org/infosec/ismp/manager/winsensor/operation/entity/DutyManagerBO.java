package org.infosec.ismp.manager.winsensor.operation.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;


/**
 * @author Rocky
 * @version create time: Jan 5, 2011 7:29:58 PM
 *
 */
@Entity
@Table(name="duty_manager")
public class DutyManagerBO implements Serializable {

	private static final long serialVersionUID = -8674888997796726496L;

	@Id
	private long id;

	@ManyToMany(cascade={CascadeType.ALL})
	@JoinTable(name="duty_manager_duty", joinColumns={@JoinColumn(name="duty_manager_id")}, 
			inverseJoinColumns={@JoinColumn(name="duty_id")})
	private List<DutyBO> duties = new ArrayList<DutyBO>();
	
	@Column(name="domain_id", length=100)
	private String domainId;		//Domain id.
	
	@Column(name="begin_date", length=50)
	private String beginDate;		//值班开始时间
	
	@Column(name="end_date", length=50)
	private String endDate;		//值班结束时间
	
	@Column(name="complaint_number", length=50)
	private String complaintNumber;		//值班电话
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="create_time")
	private Date createTime;
	
	@Column(name="expired")
	private Boolean expired;		//Current DutyManager whether expired.

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<DutyBO> getDuties() {
		return duties;
	}

	public void setDuties(List<DutyBO> duties) {
		this.duties = duties;
	}

	public String getDomainId() {
		return domainId;
	}

	public void setDomainId(String domainId) {
		this.domainId = domainId;
	}

	public String getBeginDate() {
		return beginDate;
	}

	public void setBeginDate(String beginDate) {
		this.beginDate = beginDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public String getComplaintNumber() {
		return complaintNumber;
	}

	public void setComplaintNumber(String complaintNumber) {
		this.complaintNumber = complaintNumber;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Boolean getExpired() {
		return expired;
	}

	public void setExpired(Boolean expired) {
		this.expired = expired;
	}
}
