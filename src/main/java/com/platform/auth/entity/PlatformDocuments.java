package com.platform.auth.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "platform_documents")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class PlatformDocuments {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private String id;
	
	@Column(name = "member_id")
	private String memberId;
	
	@Column(name = "member_type")
	private String memberType;
	
	@Column(name = "document_id")
	private String documentId;
	
	@Column(name = "service_center_id")
	private String serviceCenterId;
	
	@Column(name = "file_name")
	private String fileName;
	
	@Column(name = "insert_tms")
	private Date insertTms;
	
}
