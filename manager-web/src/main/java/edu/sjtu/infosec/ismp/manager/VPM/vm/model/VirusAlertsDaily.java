package edu.sjtu.infosec.ismp.manager.VPM.vm.model;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * 病毒告警日报
 * @author Wu Guojie
 * @date 2010-08-06
 * @version 1.0
 */
@Entity
@Table(name = "vpm_vm_vad")
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
public class VirusAlertsDaily implements Serializable {
	/**
	 * ID
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer id;
	/**
	 * 病毒所在客户端
	 */
    @ManyToOne 
    @JoinColumn(name="virus_clients_id")
	private VirusClients virusClients;
	/**
	 * 病毒信息
	 */
    @ManyToOne 
    @JoinColumn(name="virus_id")
	private Virus virus;
	/**
	 * 病毒数
	 */
    @Column(name="vcount")
	private int vcount;
	public int getVcount() {
		return vcount;
	}
	public void setVcount(int vcount) {
		this.vcount = vcount;
	}

	/**
	 * 报告时间
	 */
    @Column(name="report_date")
	private Timestamp reportDate;
	/**
	 * 入库时间
	 */
    @Column(name="insert_time")
	private Timestamp insertTime;
	/**
	 * 备注
	 */
    @Column(name="remarks")
	private String remarks;
	
	
	
	
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public VirusClients getVirusClients() {
		return virusClients;
	}
	public void setVirusClients(VirusClients virusClients) {
		this.virusClients = virusClients;
	}
	public Virus getVirus() {
		return virus;
	}
	public void setVirus(Virus virus) {
		this.virus = virus;
	}

	public Timestamp getReportDate() {
		return reportDate;
	}
	public void setReportDate(Timestamp reportDate) {
		this.reportDate = reportDate;
	}
	public Timestamp getInsertTime() {
		return insertTime;
	}
	public void setInsertTime(Timestamp insertTime) {
		this.insertTime = insertTime;
	}
	public String getRemarks() {
		return remarks;
	}
	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}
	
}
