import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, Input, Form, message, Tag, Space, Row, Col, Select, Descriptions, Statistic, Tabs, Spin, Image, Tooltip } from 'antd';
import { UndoOutlined, CheckCircleOutlined, DollarOutlined, EyeOutlined, BellOutlined, CarOutlined, HistoryOutlined, InfoCircleOutlined } from '@ant-design/icons';
import ReturnService from 'services/ReturnService';

const { Option } = Select;
const { TextArea } = Input;

const getAiRiskColor = (level) => {
	switch (level) {
		case 'HIGH':
			return 'red';
		case 'MEDIUM':
			return 'orange';
		case 'LOW':
			return 'green';
		default:
			return 'default';
	}
};

/** AI 보조: 위험 등급 코드 → 한글 (짧은 표기) */
const getAiRiskLevelLabelShort = (level) => {
	switch (level) {
		case 'LOW':
			return '낮음';
		case 'MEDIUM':
			return '보통';
		case 'HIGH':
			return '높음';
		default:
			return level || '알 수 없음';
	}
};

const AI_RISK_FACTOR_KO = {
	TARGET_RETURN_MISSING: '대상 반품 정보 없음',
	RECENT_RETURN_FREQUENCY_HIGH: '최근 90일 반품 빈도 높음',
	RECENT_RETURN_FREQUENCY: '최근 90일 반품 이력 있음',
	SAME_ADDRESS_REPEAT_HIGH: '동일 배송지 반복(다수)',
	SAME_ADDRESS_REPEAT: '동일 배송지 반복',
	REJECTED_HISTORY_HIGH: '과거 반려 이력 다수',
	REJECTED_HISTORY: '과거 반려 이력',
	DISPUTE_LIKE_HISTORY: '분쟁·이의 관련 문구가 있는 반려 이력',
	HIGH_RETURN_AMOUNT: '고액 반품(10만원 이상)',
	NO_STRONG_FRAUD_SIGNAL: '특이 위험 신호 없음',
	AI_FEATURE_DISABLED: 'AI 기능 비활성',
};

const AI_EVIDENCE_TAG_KO = {
	IMAGE_ATTACHED: '증빙 이미지 첨부됨',
	MULTI_ANGLE_IMAGE: '다각도 이미지(2장 이상)',
	SUFFICIENT_IMAGE_VOLUME: '이미지 수량 충분(4장 이상)',
	HAS_REASON_TEXT: '상세 사유 텍스트 있음',
	DAMAGE_CLAIM_MENTIONED: '파손·하자 언급',
	WRONG_ITEM_CLAIM_MENTIONED: '오배송·옵션 불일치 언급',
	EVIDENCE_REFERENCE_IN_TEXT: '사진·증빙 언급',
	EVIDENCE_SIGNAL_WEAK: '증빙 신호 약함',
};

const AI_EVIDENCE_GAP_KO = {
	NO_IMAGE_EVIDENCE: '증빙 이미지 없음',
	REASON_TEXT_TOO_SHORT: '상세 사유 짧음',
	REQUIRED_IMAGE_MISSING: '필수 증빙 이미지 없음',
	ADDITIONAL_IMAGE_RECOMMENDED: '추가 이미지 권장',
};

const translateAiCode = (code, map) => {
	if (!code) return '-';
	return map[code] || code;
};

const getReturnStatusColor = (status) => {
	switch (status) {
		case 'REQUESTED':
			return 'orange';
		case 'APPROVED':
			return 'cyan';
		case 'REJECTED':
			return 'red';
		case 'PICKUP_COMPLETED':
			return 'blue';
		case 'REFUNDED':
			return 'green';
		default:
			return 'default';
	}
};

const getReturnReasonTypeLabel = (t) => {
	switch (t) {
		case 'CHANGE_OF_MIND':
			return '단순 변심';
		case 'ORDER_MISTAKE':
			return '주문 실수';
		case 'DEFECT':
			return '상품 불량·하자';
		case 'WRONG_ITEM':
			return '쇼핑몰 측 오배송';
		case 'OTHER':
			return '기타';
		default:
			return t || '-';
	}
};

const getReturnRiskTierLabel = (tier) => {
	switch (tier) {
		case 'LOW':
			return '낮음';
		case 'MEDIUM':
			return '보통';
		case 'HIGH':
			return '높음';
		default:
			return tier || '-';
	}
};

const getReturnStatusLabel = (status) => {
	switch (status) {
		case 'REQUESTED':
			return '반품 신청';
		case 'APPROVED':
			return '반품 승인';
		case 'REJECTED':
			return '반품 거절';
		case 'PICKUP_COMPLETED':
			return '수거 완료';
		case 'REFUNDED':
			return '환불 완료';
		default:
			return status;
	}
};

const { TabPane } = Tabs;

const ReturnManagement = () => {
	const [returns, setReturns] = useState([]);
	const [loading, setLoading] = useState(false);
	const [detailModalVisible, setDetailModalVisible] = useState(false);
	const [updateModalVisible, setUpdateModalVisible] = useState(false);
	const [rejectModalVisible, setRejectModalVisible] = useState(false);
	const [selectedReturn, setSelectedReturn] = useState(null);
	const [statusFilter, setStatusFilter] = useState('ALL'); // 기본값을 ALL로 설정
	const [form] = Form.useForm();
	const [returnHistoryTab, setReturnHistoryTab] = useState('detail');
	const [returnHistory, setReturnHistory] = useState([]);
	const [returnHistoryLoading, setReturnHistoryLoading] = useState(false);
	const [aiAssist, setAiAssist] = useState(null);
	const [aiAssistLoading, setAiAssistLoading] = useState(false);

	useEffect(() => {
		fetchAllReturns();
	}, []);

	const fetchAllReturns = async () => {
		try {
			setLoading(true);
			const response = await ReturnService.getAllReturns();
			const returnsData = response.data || response || [];
			setReturns(Array.isArray(returnsData) ? returnsData : []);
		} catch (err) {
			console.error('반품 목록 조회 실패:', err);
			message.error(err.response?.data?.message || '반품 목록을 불러오는데 실패했습니다.');
			setReturns([]);
		} finally {
			setLoading(false);
		}
	};

	const handleViewDetail = async (returnNo) => {
		try {
			const response = await ReturnService.getReturn(returnNo);
			const returnData = response.data || response;
			setSelectedReturn(returnData);
			setDetailModalVisible(true);
			setReturnHistoryTab('detail');
			setReturnHistory([]);
			setAiAssist(null);
			fetchReturnAiAssist(returnNo);
		} catch (err) {
			message.error(err.response?.data?.message || '반품 상세 정보를 불러오는데 실패했습니다.');
		}
	};

	const fetchReturnAiAssist = async (returnNo) => {
		try {
			setAiAssistLoading(true);
			const response = await ReturnService.getReturnAiAssist(returnNo);
			const aiData = response.data || response;
			setAiAssist(aiData || null);
		} catch (err) {
			setAiAssist(null);
		} finally {
			setAiAssistLoading(false);
		}
	};
	
	const fetchReturnHistory = async (returnNo) => {
		try {
			setReturnHistoryLoading(true);
			const response = await ReturnService.getReturnHistory(returnNo);
			const historyData = response.data || response || [];
			setReturnHistory(Array.isArray(historyData) ? historyData : []);
		} catch (err) {
			console.error('반품 이력 조회 실패:', err);
			message.error(err.response?.data?.message || '반품 이력을 불러오는데 실패했습니다.');
			setReturnHistory([]);
		} finally {
			setReturnHistoryLoading(false);
		}
	};
	
	const handleReturnDetailTabChange = (key) => {
		setReturnHistoryTab(key);
		if (key === 'history' && selectedReturn && !returnHistory.length) {
			// 이력 탭으로 변경 시 이력 조회
			fetchReturnHistory(selectedReturn.returnNo);
		}
	};

	const handleApproveReturn = async (returnNo) => {
		try {
			await ReturnService.approveReturn(returnNo);
			message.success('반품이 승인되었습니다.');
			fetchAllReturns();
			if (detailModalVisible && selectedReturn?.returnNo === returnNo) {
				await handleViewDetail(returnNo);
			}
		} catch (err) {
			message.error(err.response?.data?.message || '반품 승인에 실패했습니다.');
		}
	};

	const handleRejectReturn = async (returnNo, rejectionReason) => {
		try {
			await ReturnService.rejectReturn(returnNo, rejectionReason);
			message.success('반품이 거절되었습니다.');
			setRejectModalVisible(false);
			form.resetFields();
			fetchAllReturns();
			if (detailModalVisible && selectedReturn?.returnNo === returnNo) {
				await handleViewDetail(returnNo);
			}
		} catch (err) {
			message.error(err.response?.data?.message || '반품 거절에 실패했습니다.');
		}
	};

	const handleCompleteRefund = async (returnNo) => {
		Modal.confirm({
			title: '환불 완료 처리',
			content: '환불 완료 처리하시겠습니까?',
			okText: '확인',
			cancelText: '닫기',
			onOk: async () => {
				try {
					await ReturnService.updateReturnStatus(returnNo, {
						returnStatus: 'REFUNDED',
						returnTrackingNumber: null,
						returnCourier: null,
					});
					message.success('환불 완료 처리되었습니다.');
					fetchAllReturns();
					if (detailModalVisible && selectedReturn?.returnNo === returnNo) {
						await handleViewDetail(returnNo);
					}
				} catch (err) {
					message.error(err.response?.data?.message || '환불 완료 처리에 실패했습니다.');
				}
			},
		});
	};

	const handleUpdateStatus = async (values) => {
		if (!selectedReturn) return;
		
		try {
			await ReturnService.updateReturnStatus(selectedReturn.returnNo, {
				returnStatus: values.returnStatus,
				returnTrackingNumber: values.returnTrackingNumber || null,
				returnCourier: values.returnCourier || null,
			});
			message.success('반품 상태가 변경되었습니다.');
			setUpdateModalVisible(false);
			form.resetFields();
			fetchAllReturns();
			if (detailModalVisible) {
				await handleViewDetail(selectedReturn.returnNo);
			}
		} catch (err) {
			message.error(err.response?.data?.message || '반품 상태 변경에 실패했습니다.');
		}
	};

	const openUpdateModal = (returnItem) => {
		setSelectedReturn(returnItem);
		// 다음 상태 자동 설정
		let nextStatus = 'PICKUP_COMPLETED';
		if (returnItem.returnStatus === 'APPROVED') {
			nextStatus = 'PICKUP_COMPLETED';
		} else if (returnItem.returnStatus === 'PICKUP_COMPLETED') {
			nextStatus = 'REFUNDED';
		}
		
		form.setFieldsValue({
			returnStatus: nextStatus,
			returnTrackingNumber: returnItem.returnTrackingNumber || '',
			returnCourier: returnItem.returnCourier || '',
		});
		setUpdateModalVisible(true);
	};

	const openRejectModal = (returnItem) => {
		setSelectedReturn(returnItem);
		form.setFieldsValue({
			rejectionReason: '',
		});
		setRejectModalVisible(true);
	};

	const formatDate = (dateString) => {
		if (!dateString) return '-';
		const date = new Date(dateString);
		return date.toLocaleString('ko-KR', {
			year: 'numeric',
			month: '2-digit',
			day: '2-digit',
			hour: '2-digit',
			minute: '2-digit',
		});
	};

	// 필터링된 반품 목록
	const getFilteredReturns = () => {
		if (statusFilter === 'ALL') {
			return returns;
		}
		return returns.filter((returnItem) => returnItem.returnStatus === statusFilter);
	};

	// 반품 상태별 통계 계산
	const getReturnStats = () => {
		const stats = {
			total: returns.length,
			requested: 0,
			rejected: 0,
			refunded: 0,
		};
		returns.forEach((returnItem) => {
			switch (returnItem.returnStatus) {
				case 'REQUESTED':
					stats.requested++;
					break;
				case 'REJECTED':
					stats.rejected++;
					break;
				case 'REFUNDED':
					stats.refunded++;
					break;
			}
		});
		return stats;
	};

	const filteredReturns = getFilteredReturns();
	const stats = getReturnStats();

	const tableColumns = [
		{
			title: '반품 번호',
			dataIndex: 'returnNo',
			key: 'returnNo',
		},
		{
			title: '주문 번호',
			dataIndex: 'orderNo',
			key: 'orderNo',
		},
		{
			title: '상품 정보',
			key: 'productInfo',
			render: (_, record) => (
				<div>
					<div style={{ fontWeight: 'bold' }}>{record.productName || '-'}</div>
					{record.color && record.size && (
						<div style={{ fontSize: '12px', color: '#666' }}>
							{record.color} / {record.size}
						</div>
					)}
				</div>
			),
		},
		{
			title: '반품 금액',
			dataIndex: 'returnAmount',
			key: 'returnAmount',
			render: (text) => `₩${text?.toLocaleString() || 0}`,
		},
		{
			title: '상태',
			dataIndex: 'returnStatus',
			key: 'returnStatus',
			render: (status) => (
				<Tag color={getReturnStatusColor(status)}>
					{getReturnStatusLabel(status)}
				</Tag>
			),
		},
		{
			title: '신청일시',
			dataIndex: 'returnRequestedAt',
			key: 'returnRequestedAt',
			render: (date) => formatDate(date),
		},
		{
			title: '작업',
			key: 'actions',
			render: (_, record) => (
				<Space>
					<Button
						type="link"
						size="small"
						icon={<EyeOutlined />}
						onClick={() => handleViewDetail(record.returnNo)}
					>
						상세
					</Button>
					{record.returnStatus === 'REQUESTED' && (
						<>
							<Button
								type="primary"
								size="small"
								icon={<CheckCircleOutlined />}
								onClick={() => handleApproveReturn(record.returnNo)}
								style={{ backgroundColor: '#52c41a', borderColor: '#52c41a' }}
							>
								반품 승인
							</Button>
							<Button
								type="primary"
								danger
								size="small"
								icon={<CheckCircleOutlined />}
								onClick={() => openRejectModal(record)}
							>
								반품 거절
							</Button>
						</>
					)}
					{record.returnStatus === 'APPROVED' && (
						<Button
							type="primary"
							size="small"
							icon={<CarOutlined />}
							onClick={() => openUpdateModal(record)}
						>
							수거 완료
						</Button>
					)}
					{record.returnStatus === 'PICKUP_COMPLETED' && (
						<>
							<Button
								type="primary"
								size="small"
								icon={<DollarOutlined />}
								onClick={() => handleCompleteRefund(record.returnNo)}
								style={{ backgroundColor: '#52c41a', borderColor: '#52c41a' }}
							>
								환불 완료
							</Button>
							<Button
								type="primary"
								danger
								size="small"
								icon={<CheckCircleOutlined />}
								onClick={() => openRejectModal(record)}
							>
								반품 거절
							</Button>
						</>
					)}
				</Space>
			),
		},
	];

	return (
		<div>
			{/* 반품 신청 알림 카드 */}
			{stats.requested > 0 && (
				<Card 
					style={{ 
						marginBottom: 16, 
						background: 'linear-gradient(135deg, #ff9800 0%, #f57c00 100%)',
						border: 'none',
						color: 'white'
					}}
				>
					<Row align="middle" justify="space-between">
						<Col>
							<Space size="large">
								<BellOutlined style={{ fontSize: 24 }} />
								<div>
									<div style={{ fontSize: 18, fontWeight: 'bold', marginBottom: 4 }}>
										반품 신청이 {stats.requested}건 있습니다
									</div>
									<div style={{ fontSize: 14, opacity: 0.9 }}>
										반품 신청 건을 확인하고 승인/거절해주세요
									</div>
								</div>
							</Space>
						</Col>
						<Col>
							<Button
								type="primary"
								ghost
								onClick={() => setStatusFilter('REQUESTED')}
								style={{ borderColor: 'white', color: 'white' }}
							>
								반품 신청 보기
							</Button>
						</Col>
					</Row>
				</Card>
			)}

			{/* 통계 카드 */}
			<Row gutter={16} style={{ marginBottom: 16 }}>
				<Col span={6}>
					<Card>
						<Statistic
							title="전체 반품"
							value={stats.total}
							prefix={<UndoOutlined />}
						/>
					</Card>
				</Col>
				<Col span={6}>
					<Card>
						<Statistic
							title="반품 신청"
							value={stats.requested}
							valueStyle={{ color: '#ff9800' }}
							prefix={<BellOutlined />}
						/>
					</Card>
				</Col>
				<Col span={6}>
					<Card>
						<Statistic
							title="반품 거절"
							value={stats.rejected}
							valueStyle={{ color: '#dc3545' }}
							prefix={<CheckCircleOutlined />}
						/>
					</Card>
				</Col>
				<Col span={6}>
					<Card>
						<Statistic
							title="환불 완료"
							value={stats.refunded}
							valueStyle={{ color: '#52c41a' }}
							prefix={<DollarOutlined />}
						/>
					</Card>
				</Col>
			</Row>

			<Card>
				<div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
					<h4 style={{ margin: 0 }}>반품 목록</h4>
					<Select
						value={statusFilter}
						onChange={setStatusFilter}
						style={{ width: 150 }}
					>
						<Option value="ALL">전체</Option>
						<Option value="REQUESTED">반품 신청</Option>
						<Option value="REJECTED">반품 거절</Option>
						<Option value="REFUNDED">환불 완료</Option>
					</Select>
				</div>
				{filteredReturns.length === 0 ? (
					<div className="text-center p-4">
						{returns.length === 0 ? '반품 정보가 없습니다.' : '선택한 조건에 맞는 반품이 없습니다.'}
					</div>
				) : (
					<Table
						columns={tableColumns}
						dataSource={filteredReturns}
						rowKey="returnNo"
						loading={loading}
						pagination={{
							pageSize: 10,
							showSizeChanger: true,
							showTotal: (total) => `총 ${total}건`,
						}}
					/>
				)}
			</Card>

			{/* 반품 상세 모달 */}
			<Modal
				title="반품 상세 정보"
				open={detailModalVisible}
				onCancel={() => {
					setDetailModalVisible(false);
					setSelectedReturn(null);
					setAiAssist(null);
				}}
				footer={[
					<Button key="close" onClick={() => {
						setDetailModalVisible(false);
						setSelectedReturn(null);
						setAiAssist(null);
					}}>
						닫기
					</Button>,
					selectedReturn && selectedReturn.returnStatus === 'REQUESTED' && (
						<>
							<Button
								key="approve"
								type="primary"
								style={{ backgroundColor: '#52c41a', borderColor: '#52c41a' }}
								onClick={() => {
									handleApproveReturn(selectedReturn.returnNo);
								}}
							>
								반품 승인
							</Button>
							<Button
								key="reject"
								type="primary"
								danger
								onClick={() => {
									setDetailModalVisible(false);
									openRejectModal(selectedReturn);
								}}
							>
								반품 거절
							</Button>
						</>
					),
					selectedReturn && selectedReturn.returnStatus === 'APPROVED' && (
						<Button
							key="pickup"
							type="primary"
							onClick={() => {
								setDetailModalVisible(false);
								openUpdateModal(selectedReturn);
							}}
						>
							수거 완료 처리
						</Button>
					),
					selectedReturn && selectedReturn.returnStatus === 'PICKUP_COMPLETED' && (
						<>
							<Button
								key="refund"
								type="primary"
								style={{ backgroundColor: '#52c41a', borderColor: '#52c41a' }}
								onClick={() => {
									setDetailModalVisible(false);
									handleCompleteRefund(selectedReturn.returnNo);
								}}
							>
								환불 완료 처리
							</Button>
							<Button
								key="reject-after-pickup"
								type="primary"
								danger
								onClick={() => {
									setDetailModalVisible(false);
									openRejectModal(selectedReturn);
								}}
							>
								반품 거절
							</Button>
						</>
					),
				].filter(Boolean)}
				width={800}
			>
				{selectedReturn && (
					<Tabs activeKey={returnHistoryTab} onChange={handleReturnDetailTabChange}>
						<TabPane tab="기본 정보" key="detail">
							<Descriptions bordered column={2} style={{ marginBottom: 24 }}>
								<Descriptions.Item label="반품 번호">{selectedReturn.returnNo}</Descriptions.Item>
								<Descriptions.Item label="주문 번호">{selectedReturn.orderNo}</Descriptions.Item>
								<Descriptions.Item label="주문 아이템 번호">{selectedReturn.orderItemNo}</Descriptions.Item>
								<Descriptions.Item label="상품명">{selectedReturn.productName}</Descriptions.Item>
								{selectedReturn.color && (
									<Descriptions.Item label="색상">{selectedReturn.color}</Descriptions.Item>
								)}
								{selectedReturn.size && (
									<Descriptions.Item label="사이즈">{selectedReturn.size}</Descriptions.Item>
								)}
								<Descriptions.Item label="수량">{selectedReturn.quantity}개</Descriptions.Item>
								<Descriptions.Item label="반품 금액">₩{selectedReturn.returnAmount?.toLocaleString() || 0}</Descriptions.Item>
								<Descriptions.Item label="반품 상태">
									<Tag color={getReturnStatusColor(selectedReturn.returnStatus)}>
										{getReturnStatusLabel(selectedReturn.returnStatus)}
									</Tag>
								</Descriptions.Item>
								<Descriptions.Item label="신청일시">{formatDate(selectedReturn.returnRequestedAt)}</Descriptions.Item>
								<Descriptions.Item label="반품 사유 유형">
									{getReturnReasonTypeLabel(selectedReturn.returnReasonType)}
								</Descriptions.Item>
								<Descriptions.Item label={(
									<Space size={4}>
										<span>신청 시점 위험도</span>
										<Tooltip title="반품 신청 당시 위험도입니다. 현재 사기 위험등급과 다를 수 있습니다.">
											<InfoCircleOutlined style={{ color: '#8c8c8c' }} />
										</Tooltip>
									</Space>
								)}>
									{selectedReturn.returnRiskTier != null ? (
										<span style={{ fontSize: 12, color: '#666' }}>
											{getReturnRiskTierLabel(selectedReturn.returnRiskTier)}
											{selectedReturn.returnRiskScore != null ? ` (점수: ${selectedReturn.returnRiskScore})` : ''}
										</span>
									) : '-'}
								</Descriptions.Item>
								<Descriptions.Item label="반품 사유(상세)" span={2}>
									{selectedReturn.returnReason || '-'}
								</Descriptions.Item>
								<Descriptions.Item label="반품 증빙 이미지" span={2}>
									{Array.isArray(selectedReturn.imageUrls) && selectedReturn.imageUrls.length > 0 ? (
										<Image.PreviewGroup>
											<Space size={[8, 8]} wrap>
												{selectedReturn.imageUrls.map((url, index) => (
													<Image
														key={`${selectedReturn.returnNo}-image-${index}`}
														width={84}
														height={84}
														src={url}
														alt={`return-evidence-${index + 1}`}
														style={{ objectFit: 'cover', borderRadius: 6, border: '1px solid #f0f0f0' }}
													/>
												))}
											</Space>
										</Image.PreviewGroup>
									) : (
										'-'
									)}
								</Descriptions.Item>
								{selectedReturn.returnTrackingNumber && (
									<Descriptions.Item label="반품 송장번호">{selectedReturn.returnTrackingNumber}</Descriptions.Item>
								)}
								{selectedReturn.returnCourier && (
									<Descriptions.Item label="반품 택배사">{selectedReturn.returnCourier}</Descriptions.Item>
								)}
							</Descriptions>
							<Card size="small" title="AI 보조 분석 (관리자 참고용)">
								<Spin spinning={aiAssistLoading}>
									{!aiAssist ? (
										<div style={{ color: '#666' }}>
											AI 보조 결과가 없습니다. 기존 관리자 검토 절차를 진행하세요.
										</div>
									) : aiAssist.featureEnabled === false ? (
										<div style={{ color: '#666' }}>
											AI 기능이 비활성화되어 기존 관리자 검토 절차를 진행하세요.
										</div>
									) : (
										<Descriptions bordered size="small" column={1}>
											<Descriptions.Item label="위험 등급(현재 평가)">
												<Space wrap>
													<Tag color={getAiRiskColor(aiAssist.riskLevel)}>
														{getAiRiskLevelLabelShort(aiAssist.riskLevel)}
													</Tag>
													<span>점수: {aiAssist.fraudScore ?? 0}</span>
												</Space>
											</Descriptions.Item>
											<Descriptions.Item label="리스크 요인">
												{Array.isArray(aiAssist.riskFactors) && aiAssist.riskFactors.length > 0 ? (
													<Space wrap size={[8, 8]}>
														{aiAssist.riskFactors.map((f) => (
															<Tag key={f}>{translateAiCode(f, AI_RISK_FACTOR_KO)}</Tag>
														))}
													</Space>
												) : (
													'-'
												)}
											</Descriptions.Item>
											<Descriptions.Item label="증빙 태그">
												{Array.isArray(aiAssist.evidenceTags) && aiAssist.evidenceTags.length > 0 ? (
													<Space wrap size={[8, 8]}>
														{aiAssist.evidenceTags.map((f) => (
															<Tag key={f} color="blue">{translateAiCode(f, AI_EVIDENCE_TAG_KO)}</Tag>
														))}
													</Space>
												) : (
													'-'
												)}
											</Descriptions.Item>
											<Descriptions.Item label="증빙 누락">
												{Array.isArray(aiAssist.evidenceGaps) && aiAssist.evidenceGaps.length > 0 ? (
													<Space wrap size={[8, 8]}>
														{aiAssist.evidenceGaps.map((f) => (
															<Tag key={f} color="red">{translateAiCode(f, AI_EVIDENCE_GAP_KO)}</Tag>
														))}
													</Space>
												) : (
													'-'
												)}
											</Descriptions.Item>
											<Descriptions.Item label="권장 조치">
												{Array.isArray(aiAssist.recommendedActions) && aiAssist.recommendedActions.length > 0 ? (
													<ul style={{ margin: 0, paddingLeft: 18 }}>
														{aiAssist.recommendedActions.map((item, idx) => (
															<li key={`${idx}-${item}`}>{item}</li>
														))}
													</ul>
												) : (
													'-'
												)}
											</Descriptions.Item>
										</Descriptions>
									)}
								</Spin>
							</Card>
						</TabPane>
						<TabPane tab={<span><HistoryOutlined /> 변경 이력</span>} key="history">
							<Spin spinning={returnHistoryLoading}>
								<Table
									columns={[
										{
											title: '변경 일시',
											dataIndex: 'changedAt',
											key: 'changedAt',
											width: 180,
											render: (date) => formatDate(date)
										},
										{
											title: '변경 유형',
											dataIndex: 'actionType',
											key: 'actionType',
											width: 120,
											render: (actionType, record) => record.getActionTypeLabel ? record.getActionTypeLabel() : (
												actionType === 'CREATE' ? '반품 신청' :
												actionType === 'APPROVE' ? '반품 승인' :
												actionType === 'REJECT' ? '반품 거절' :
												actionType === 'STATUS_CHANGE' ? '상태 변경' :
												actionType === 'TRACKING_UPDATE' ? '송장번호 수정' :
												actionType
											)
										},
										{
											title: '변경한 사용자',
											key: 'changedBy',
											width: 120,
											render: (_, record) => record.getChangedByName ? record.getChangedByName() : (
												record.adminName || record.partnerName || '시스템'
											)
										},
										{
											title: '변경 전',
											dataIndex: 'oldValue',
											key: 'oldValue',
											render: (value) => {
												if (!value) return '-';
												try {
													const parsed = JSON.parse(value);
													return Object.entries(parsed).map(([key, val]) => {
														const label = key === 'status' ? '상태' : key === 'trackingNumber' ? '송장번호' : key === 'courier' ? '택배사' : key;
														return `${label}: ${val || '-'}`;
													}).join(', ');
												} catch {
													return value;
												}
											}
										},
										{
											title: '변경 후',
											dataIndex: 'newValue',
											key: 'newValue',
											render: (value) => {
												if (!value) return '-';
												try {
													const parsed = JSON.parse(value);
													return Object.entries(parsed).map(([key, val]) => {
														const label = key === 'status' ? '상태' : key === 'trackingNumber' ? '송장번호' : key === 'courier' ? '택배사' : key;
														return `${label}: ${val || '-'}`;
													}).join(', ');
												} catch {
													return value;
												}
											}
										},
										{
											title: '변경 사유',
											dataIndex: 'reason',
											key: 'reason',
											render: (reason) => reason || '-'
										}
									]}
									dataSource={returnHistory}
									rowKey="historyId"
									pagination={{
										pageSize: 10,
										showSizeChanger: true,
										pageSizeOptions: ['10', '20', '50'],
										showTotal: (total) => `총 ${total}건`
									}}
									locale={{
										emptyText: '변경 이력이 없습니다.'
									}}
								/>
							</Spin>
						</TabPane>
					</Tabs>
				)}
			</Modal>

			{/* 반품 상태 변경 모달 (수거 완료 / 환불 완료) */}
			<Modal
				title={
					selectedReturn?.returnStatus === 'APPROVED'
						? '수거 완료 처리'
						: selectedReturn?.returnStatus === 'PICKUP_COMPLETED'
						? '환불 완료 처리'
						: '반품 상태 변경'
				}
				open={updateModalVisible && (selectedReturn?.returnStatus === 'APPROVED' || selectedReturn?.returnStatus === 'PICKUP_COMPLETED')}
				onCancel={() => {
					setUpdateModalVisible(false);
					setSelectedReturn(null);
					form.resetFields();
				}}
				onOk={() => form.submit()}
				okText="확인"
				cancelText="닫기"
				zIndex={1001}
			>
				<Form
					form={form}
					layout="vertical"
					onFinish={handleUpdateStatus}
				>
					<Form.Item
						label="반품 상태"
						name="returnStatus"
						rules={[{ required: true, message: '반품 상태를 선택해주세요.' }]}
					>
						<Select>
							{selectedReturn?.returnStatus === 'APPROVED' && (
								<Option value="PICKUP_COMPLETED">수거 완료</Option>
							)}
							{selectedReturn?.returnStatus === 'PICKUP_COMPLETED' && (
								<Option value="REFUNDED">환불 완료</Option>
							)}
						</Select>
					</Form.Item>
					{selectedReturn?.returnStatus === 'APPROVED' && (
						<>
							<Form.Item
								label="반품 송장번호 (선택)"
								name="returnTrackingNumber"
								help="고객이 반품 상품을 보낼 때 사용한 송장번호입니다. 없으면 비워두셔도 됩니다."
							>
								<Input placeholder="반품 송장번호를 입력하세요 (선택)" />
							</Form.Item>
							<Form.Item
								label="반품 택배사 (선택)"
								name="returnCourier"
								help="고객이 반품 상품을 보낼 때 사용한 택배사입니다. 없으면 비워두셔도 됩니다."
							>
								<Input placeholder="반품 택배사를 입력하세요 (선택)" />
							</Form.Item>
						</>
					)}
				</Form>
			</Modal>

			{/* 반품 거절 모달 */}
			<Modal
				title={
					selectedReturn?.returnStatus === 'PICKUP_COMPLETED'
						? '반품 거절 (상품 확인 후)'
						: '반품 거절'
				}
				open={rejectModalVisible}
				onCancel={() => {
					setRejectModalVisible(false);
					setSelectedReturn(null);
					form.resetFields();
				}}
				onOk={() => {
					const rejectionReason = form.getFieldValue('rejectionReason');
					if (selectedReturn) {
						handleRejectReturn(selectedReturn.returnNo, rejectionReason || '반품 거절');
					}
				}}
				okText="확인"
				cancelText="닫기"
				zIndex={1001}
			>
				<Form
					form={form}
					layout="vertical"
				>
					<Form.Item
						label="거절 사유"
						name="rejectionReason"
						rules={[{ required: true, message: '거절 사유를 입력해주세요.' }]}
					>
						<TextArea 
							rows={4} 
							placeholder={
								selectedReturn?.returnStatus === 'PICKUP_COMPLETED'
									? '수거한 상품 확인 결과 거절 사유를 입력해주세요'
									: '반품 거절 사유를 입력해주세요'
							}
						/>
					</Form.Item>
				</Form>
			</Modal>
		</div>
	);
};

export default ReturnManagement;
