import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, Tag, Space, Row, Col, Alert, Statistic, Descriptions, Spin, Select, Drawer, message, Input, Form, Badge, Tabs, Timeline } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined, ReloadOutlined, InfoCircleOutlined, ShopOutlined, EyeOutlined, StopOutlined, PlayCircleOutlined, ExclamationCircleOutlined, HistoryOutlined } from '@ant-design/icons';
import { useDispatch, useSelector } from 'react-redux';
import { fetchAllPartners, fetchPartnerDetail, deactivatePartner, activatePartner, approveDeactivationRequest, rejectDeactivationRequest, rejectReactivationRequest, fetchPartnerHistory } from 'store/slices/adminSlice';

const { Option } = Select;
const { TextArea } = Input;

const PartnerManagement = () => {
	const dispatch = useDispatch();
	const { allPartners, selectedPartner, partnerHistory, loading, error } = useSelector((state) => state.admin);
	const { user } = useSelector((state) => state.auth);
	const [mainTab, setMainTab] = useState('all'); // 'all', 'deactivation', 'reactivation'
	const [statusFilter, setStatusFilter] = useState(null);
	const [drawerVisible, setDrawerVisible] = useState(false);
	const [rejectModalVisible, setRejectModalVisible] = useState(false);
	const [rejectType, setRejectType] = useState(null); // 'deactivation' or 'reactivation'
	const [selectedPartnerForReject, setSelectedPartnerForReject] = useState(null);
	const [rejectForm] = Form.useForm();
	const [activeTab, setActiveTab] = useState('detail');
	const [historyActionTypeFilter, setHistoryActionTypeFilter] = useState(null);

	// 관리자 권한 체크
	useEffect(() => {
		if (user && user.role !== 'ADMIN') {
			message.error('관리자만 접근할 수 있는 페이지입니다.');
			// 관리자가 아닌 경우 리다이렉트하거나 에러 표시
			return;
		}
	}, [user]);

	useEffect(() => {
		fetchPartners();
	}, [mainTab, statusFilter]);

	const fetchPartners = async () => {
		try {
			// 항상 전체 파트너 목록을 가져옴 (탭에 따라 클라이언트에서 필터링)
			await dispatch(fetchAllPartners({
				status: null, // 상태 필터는 클라이언트에서 적용
				hasDeactivationRequest: null,
				hasReactivationRequest: null
			})).unwrap();
			
			// 통계용 전체 목록도 업데이트
			const statsResult = await dispatch(fetchAllPartners({
				status: null,
				hasDeactivationRequest: null,
				hasReactivationRequest: null
			})).unwrap();
			setAllPartnersForStats(statsResult);
		} catch (err) {
			console.error('파트너 목록 조회 실패:', err);
		}
	};

	const handleViewDetail = async (partnerId) => {
		try {
			await dispatch(fetchPartnerDetail(partnerId)).unwrap();
			await dispatch(fetchPartnerHistory({ partnerId })).unwrap();
			setDrawerVisible(true);
			setActiveTab('detail');
			setHistoryActionTypeFilter(null);
		} catch (err) {
			console.error('파트너 상세 조회 실패:', err);
		}
	};

	const handleViewHistory = async (partnerId) => {
		try {
			await dispatch(fetchPartnerDetail(partnerId)).unwrap();
			await dispatch(fetchPartnerHistory({ partnerId })).unwrap();
			setDrawerVisible(true);
			setActiveTab('history');
			setHistoryActionTypeFilter(null);
		} catch (err) {
			console.error('파트너 이력 조회 실패:', err);
			message.error('파트너 이력 조회에 실패했습니다.');
		}
	};

	const handleHistoryFilterChange = async (actionType) => {
		setHistoryActionTypeFilter(actionType);
		if (selectedPartner) {
			try {
				await dispatch(fetchPartnerHistory({ 
					partnerId: selectedPartner.partnerId,
					actionType: actionType || null
				})).unwrap();
			} catch (err) {
				console.error('이력 필터링 실패:', err);
			}
		}
	};

	const handleDeactivate = (partner) => {
		Modal.confirm({
			title: '파트너 비활성화',
			content: (
				<div>
					<p>다음 파트너를 비활성화하시겠습니까?</p>
					<Descriptions column={1} size="small" bordered>
						<Descriptions.Item label="파트너명">{partner.partnerName}</Descriptions.Item>
						<Descriptions.Item label="이메일">{partner.email}</Descriptions.Item>
						<Descriptions.Item label="상태">
							<Tag color="green">{partner.partnerStatus}</Tag>
						</Descriptions.Item>
					</Descriptions>
					<p style={{ marginTop: '16px', color: '#ff4d4f' }}>
						비활성화 시 해당 파트너의 모든 상품이 고객 목록에서 사라집니다.
					</p>
				</div>
			),
			okText: '비활성화',
			okType: 'danger',
			cancelText: '취소',
			onOk: async () => {
				try {
					await dispatch(deactivatePartner(partner.partnerId)).unwrap();
					message.success('파트너가 비활성화되었습니다.');
					fetchPartners();
				} catch (err) {
					message.error(err || '파트너 비활성화에 실패했습니다.');
				}
			}
		});
	};

	const handleActivate = (partner) => {
		Modal.confirm({
			title: '파트너 재활성화',
			content: (
				<div>
					<p>다음 파트너를 재활성화하시겠습니까?</p>
					<Descriptions column={1} size="small" bordered>
						<Descriptions.Item label="파트너명">{partner.partnerName}</Descriptions.Item>
						<Descriptions.Item label="이메일">{partner.email}</Descriptions.Item>
						<Descriptions.Item label="상태">
							<Tag color="orange">{partner.partnerStatus}</Tag>
						</Descriptions.Item>
					</Descriptions>
					<p style={{ marginTop: '16px', color: '#52c41a' }}>
						재활성화 시 해당 파트너의 모든 상품이 고객 목록에 다시 표시됩니다.
					</p>
				</div>
			),
			okText: '재활성화',
			okType: 'primary',
			cancelText: '취소',
			onOk: async () => {
				try {
					await dispatch(activatePartner(partner.partnerId)).unwrap();
					message.success('파트너가 재활성화되었습니다.');
					fetchPartners();
				} catch (err) {
					message.error(err || '파트너 재활성화에 실패했습니다.');
				}
			}
		});
	};

	const handleApproveDeactivationRequest = (partner) => {
		Modal.confirm({
			title: '휴업 신청 승인',
			content: (
				<div>
					<p>다음 파트너의 휴업 신청을 승인하시겠습니까?</p>
					<Descriptions column={1} size="small" bordered>
						<Descriptions.Item label="파트너명">{partner.partnerName}</Descriptions.Item>
						<Descriptions.Item label="이메일">{partner.email}</Descriptions.Item>
						<Descriptions.Item label="휴업 신청 사유">
							{partner.deactivationRequestReason || '-'}
						</Descriptions.Item>
						<Descriptions.Item label="신청일시">
							{partner.deactivationRequestedAt ? new Date(partner.deactivationRequestedAt).toLocaleString('ko-KR') : '-'}
						</Descriptions.Item>
					</Descriptions>
					<p style={{ marginTop: '16px', color: '#ff4d4f' }}>
						승인 시 파트너가 비활성화되고 모든 상품이 고객 목록에서 사라집니다.
					</p>
				</div>
			),
			okText: '승인',
			okType: 'danger',
			cancelText: '취소',
			onOk: async () => {
				try {
					await dispatch(approveDeactivationRequest(partner.partnerId)).unwrap();
					message.success('휴업 신청이 승인되었습니다.');
					fetchPartners();
				} catch (err) {
					message.error(err || '휴업 신청 승인에 실패했습니다.');
				}
			}
		});
	};

	const handleRejectDeactivationRequest = (partner) => {
		setSelectedPartnerForReject(partner);
		setRejectType('deactivation');
		setRejectModalVisible(true);
		rejectForm.resetFields();
	};

	const handleRejectReactivationRequest = (partner) => {
		setSelectedPartnerForReject(partner);
		setRejectType('reactivation');
		setRejectModalVisible(true);
		rejectForm.resetFields();
	};

	const handleRejectSubmit = async (values) => {
		if (!values.rejectionReason || !values.rejectionReason.trim()) {
			message.error('거절 사유를 입력해주세요.');
			return;
		}

		if (!selectedPartnerForReject) {
			message.error('선택된 파트너를 찾을 수 없습니다.');
			return;
		}

		try {
			if (rejectType === 'deactivation') {
				await dispatch(rejectDeactivationRequest({
					partnerId: selectedPartnerForReject.partnerId,
					rejectionReason: values.rejectionReason.trim()
				})).unwrap();
				message.success('휴업 신청이 거절되었습니다.');
			} else {
				await dispatch(rejectReactivationRequest({
					partnerId: selectedPartnerForReject.partnerId,
					rejectionReason: values.rejectionReason.trim()
				})).unwrap();
				message.success('재활성화 신청이 거절되었습니다.');
			}
			
			setRejectModalVisible(false);
			setRejectType(null);
			setSelectedPartnerForReject(null);
			rejectForm.resetFields();
			fetchPartners();
		} catch (err) {
			message.error(err || '신청 거절에 실패했습니다.');
		}
	};

	const getStatusTag = (status) => {
		const statusMap = {
			PENDING: { color: 'orange', text: '입점 신청' },
			APPROVED: { color: 'green', text: '승인 · 운영 중' },
			REJECTED: { color: 'red', text: '거절' },
			INACTIVE: { color: 'default', text: '승인 · 비활성' }
		};
		const statusInfo = statusMap[status] || { color: 'default', text: status };
		return <Tag color={statusInfo.color}>{statusInfo.text}</Tag>;
	};

	const tableColumns = [
		{
			title: '파트너명',
			dataIndex: 'partnerName',
			key: 'partnerName',
			render: (text, record) => (
				<div>
					<ShopOutlined style={{ marginRight: 8 }} />
					<strong>{text}</strong>
					{record.deactivationRequestedAt && (
						<Badge 
							count="휴업신청" 
							style={{ backgroundColor: '#fa8c16', marginLeft: 8 }}
							title={`휴업 신청일: ${new Date(record.deactivationRequestedAt).toLocaleString('ko-KR')}`}
						/>
					)}
					{record.reactivationRequestedAt && (
						<Badge 
							count="재활성화신청" 
							style={{ backgroundColor: '#1890ff', marginLeft: 8 }}
							title={`재활성화 신청일: ${new Date(record.reactivationRequestedAt).toLocaleString('ko-KR')}`}
						/>
					)}
				</div>
			),
		},
		{
			title: '상태',
			dataIndex: 'partnerStatus',
			key: 'partnerStatus',
			width: 120,
			render: (status) => getStatusTag(status),
		},
		{
			title: '승인일시',
			dataIndex: 'partnerApprovedAt',
			key: 'partnerApprovedAt',
			render: (date) => date ? new Date(date).toLocaleString('ko-KR') : '-',
		},
		{
			title: '작업',
			key: 'action',
			width: 400,
			render: (_, record) => (
				<Space wrap>
					<Button
						icon={<EyeOutlined />}
						onClick={() => handleViewDetail(record.partnerId)}
						size="small"
					>
						상세
					</Button>
					<Button
						icon={<HistoryOutlined />}
						onClick={() => handleViewHistory(record.partnerId)}
						size="small"
					>
						이력
					</Button>
					{record.partnerStatus === 'APPROVED' && record.deactivationRequestedAt && (
						<>
							<Button
								type="primary"
								icon={<CheckCircleOutlined />}
								onClick={() => handleApproveDeactivationRequest(record)}
								size="small"
							>
								휴업승인
							</Button>
							<Button
								danger
								icon={<CloseCircleOutlined />}
								onClick={() => handleRejectDeactivationRequest(record)}
								size="small"
							>
								휴업거절
							</Button>
						</>
					)}
					{record.partnerStatus === 'APPROVED' && !record.deactivationRequestedAt && (
						<Button
							danger
							icon={<StopOutlined />}
							onClick={() => handleDeactivate(record)}
							size="small"
						>
							비활성화
						</Button>
					)}
					{record.partnerStatus === 'INACTIVE' && record.reactivationRequestedAt && (
						<>
							<Button
								type="primary"
								icon={<PlayCircleOutlined />}
								onClick={() => handleActivate(record)}
								size="small"
							>
								재활성화승인
							</Button>
							<Button
								danger
								icon={<CloseCircleOutlined />}
								onClick={() => handleRejectReactivationRequest(record)}
								size="small"
							>
								재활성화거절
							</Button>
						</>
					)}
					{record.partnerStatus === 'INACTIVE' && !record.reactivationRequestedAt && (
						<Button
							type="primary"
							icon={<PlayCircleOutlined />}
							onClick={() => handleActivate(record)}
							size="small"
						>
							재활성화
						</Button>
					)}
				</Space>
			),
		},
	];

	// 전체 파트너 목록을 별도로 관리 (통계용)
	const [allPartnersForStats, setAllPartnersForStats] = useState([]);

	// 컴포넌트 마운트 시 전체 파트너 목록 로드 (통계용)
	useEffect(() => {
		const loadStats = async () => {
			try {
				const result = await dispatch(fetchAllPartners({
					status: null,
					hasDeactivationRequest: null,
					hasReactivationRequest: null
				})).unwrap();
				setAllPartnersForStats(result);
			} catch (err) {
				console.error('통계용 파트너 목록 조회 실패:', err);
			}
		};
		loadStats();
	}, []);

	// 통계 계산 (전체 파트너 기준)
	const statusCounts = {
		PENDING: allPartnersForStats.filter(p => p.partnerStatus === 'PENDING').length,
		APPROVED: allPartnersForStats.filter(p => p.partnerStatus === 'APPROVED').length,
		REJECTED: allPartnersForStats.filter(p => p.partnerStatus === 'REJECTED').length,
		INACTIVE: allPartnersForStats.filter(p => p.partnerStatus === 'INACTIVE').length,
	};
	
	const deactivationRequestCount = allPartnersForStats.filter(p => p.deactivationRequestedAt != null).length;
	const reactivationRequestCount = allPartnersForStats.filter(p => p.reactivationRequestedAt != null).length;

	// 현재 탭에 맞는 파트너 목록 필터링
	const getFilteredPartners = () => {
		let filtered = allPartners;
		
		// 탭에 따른 필터링
		if (mainTab === 'deactivation') {
			filtered = filtered.filter(p => p.deactivationRequestedAt != null);
		} else if (mainTab === 'reactivation') {
			filtered = filtered.filter(p => p.reactivationRequestedAt != null);
		}
		
		// 상태 필터 적용
		if (statusFilter) {
			filtered = filtered.filter(p => p.partnerStatus === statusFilter);
		}
		
		return filtered;
	};

	const filteredPartners = getFilteredPartners();

	// 관리자가 아닌 경우 접근 차단
	if (user && user.role !== 'ADMIN') {
		return (
			<Card>
				<Alert
					message="접근 권한 없음"
					description="이 페이지는 관리자만 접근할 수 있습니다."
					type="error"
					showIcon
				/>
			</Card>
		);
	}

	return (
		<>
			<Card title="파트너 관리">
				<Row gutter={16} style={{ marginBottom: 24 }}>
					<Col span={4}>
						<Statistic
							title="입점 신청"
							value={statusCounts.PENDING}
							prefix={<InfoCircleOutlined />}
							valueStyle={{ color: '#fa8c16' }}
						/>
					</Col>
					<Col span={4}>
						<Statistic
							title="운영 중"
							value={statusCounts.APPROVED}
							prefix={<CheckCircleOutlined />}
							valueStyle={{ color: '#52c41a' }}
						/>
					</Col>
					<Col span={4}>
						<Statistic
							title="거절"
							value={statusCounts.REJECTED}
							prefix={<CloseCircleOutlined />}
							valueStyle={{ color: '#ff4d4f' }}
						/>
					</Col>
					<Col span={4}>
						<Statistic
							title="비활성"
							value={statusCounts.INACTIVE}
							prefix={<StopOutlined />}
							valueStyle={{ color: '#8c8c8c' }}
						/>
					</Col>
					<Col span={4}>
						<Statistic
							title="휴업 신청"
							value={deactivationRequestCount}
							prefix={<ExclamationCircleOutlined />}
							valueStyle={{ color: '#fa8c16' }}
						/>
					</Col>
					<Col span={4}>
						<Statistic
							title="재활성화 신청"
							value={reactivationRequestCount}
							prefix={<InfoCircleOutlined />}
							valueStyle={{ color: '#1890ff' }}
						/>
					</Col>
				</Row>

				<Tabs 
					activeKey={mainTab} 
					onChange={setMainTab}
					style={{ marginBottom: 16 }}
				>
					<Tabs.TabPane 
						tab={
							<span>
								<ShopOutlined />
								전체 파트너
							</span>
						} 
						key="all"
					/>
					<Tabs.TabPane 
						tab={
							<span>
								<ExclamationCircleOutlined />
								휴업 신청 ({deactivationRequestCount})
							</span>
						} 
						key="deactivation"
					/>
					<Tabs.TabPane 
						tab={
							<span>
								<InfoCircleOutlined />
								재활성화 신청 ({reactivationRequestCount})
							</span>
						} 
						key="reactivation"
					/>
				</Tabs>

				<Space style={{ marginBottom: 16 }} wrap>
					<Select
						placeholder="상태 필터"
						allowClear
						style={{ width: 200 }}
						onChange={(value) => setStatusFilter(value)}
						value={statusFilter}
					>
						<Option value="PENDING">입점 신청</Option>
						<Option value="APPROVED">승인 · 운영 중</Option>
						<Option value="REJECTED">거절</Option>
						<Option value="INACTIVE">승인 · 비활성</Option>
					</Select>
					<Button
						icon={<ReloadOutlined />}
						onClick={fetchPartners}
						loading={loading}
					>
						새로고침
					</Button>
				</Space>

				{error && (
					<Alert
						message="오류"
						description={error}
						type="error"
						showIcon
						className="mb-3"
						closable
					/>
				)}

				<Spin spinning={loading}>
					<Table
						columns={tableColumns}
						dataSource={filteredPartners}
						rowKey="partnerId"
						pagination={{
							pageSize: 10,
							showSizeChanger: true,
							showTotal: (total) => {
								if (mainTab === 'deactivation') {
									return `총 ${total}개 휴업 신청`;
								} else if (mainTab === 'reactivation') {
									return `총 ${total}개 재활성화 신청`;
								}
								return `총 ${total}개 파트너`;
							},
						}}
						locale={{
							emptyText: mainTab === 'deactivation' 
								? '휴업 신청이 없습니다.' 
								: mainTab === 'reactivation'
								? '재활성화 신청이 없습니다.'
								: '파트너가 없습니다.',
						}}
					/>
				</Spin>
			</Card>

			{/* 파트너 상세 정보 Drawer */}
			<Drawer
				title="파트너 상세 정보"
				placement="right"
				width={700}
				onClose={() => {
					setDrawerVisible(false);
					setActiveTab('detail');
					setHistoryActionTypeFilter(null);
				}}
				open={drawerVisible}
			>
				{selectedPartner && (
					<Tabs activeKey={activeTab} onChange={setActiveTab}>
						<Tabs.TabPane tab="상세 정보" key="detail">
							<Descriptions title="파트너 입점 신청 상세 정보" column={1} bordered size="small">
								<Descriptions.Item label="파트너 ID">{selectedPartner.partnerId}</Descriptions.Item>
								<Descriptions.Item label="상태">
									{getStatusTag(selectedPartner.partnerStatus)}
								</Descriptions.Item>
								<Descriptions.Item label="파트너명">{selectedPartner.partnerName}</Descriptions.Item>
								<Descriptions.Item label="이메일">{selectedPartner.email}</Descriptions.Item>
								<Descriptions.Item label="연락처">{selectedPartner.partnerContact}</Descriptions.Item>
								<Descriptions.Item label="사업자등록번호">{selectedPartner.businessRegistrationNumber}</Descriptions.Item>
								<Descriptions.Item label="정산계좌">{selectedPartner.partnerBankAccount}</Descriptions.Item>
								<Descriptions.Item label="승인일시">
									{selectedPartner.partnerApprovedAt 
										? new Date(selectedPartner.partnerApprovedAt).toLocaleString('ko-KR')
										: '-'}
								</Descriptions.Item>
								{selectedPartner.deactivationRequestedAt && (
									<>
										<Descriptions.Item label="휴업 신청 사유">
											<Alert
												message={selectedPartner.deactivationRequestReason || '사유 없음'}
												type="warning"
												showIcon
												icon={<ExclamationCircleOutlined />}
											/>
										</Descriptions.Item>
										<Descriptions.Item label="휴업 신청일시">
											{new Date(selectedPartner.deactivationRequestedAt).toLocaleString('ko-KR')}
										</Descriptions.Item>
									</>
								)}
								{selectedPartner.reactivationRequestedAt && (
									<>
										<Descriptions.Item label="재활성화 신청 사유">
											<Alert
												message={selectedPartner.reactivationRequestReason || '사유 없음'}
												type="info"
												showIcon
												icon={<InfoCircleOutlined />}
											/>
										</Descriptions.Item>
										<Descriptions.Item label="재활성화 신청일시">
											{new Date(selectedPartner.reactivationRequestedAt).toLocaleString('ko-KR')}
										</Descriptions.Item>
									</>
								)}
							</Descriptions>
						</Tabs.TabPane>
						<Tabs.TabPane 
							tab={
								<span>
									<HistoryOutlined />
									이력 ({partnerHistory.length})
								</span>
							} 
							key="history"
						>
							<Space style={{ marginBottom: 16 }} wrap>
								<Select
									placeholder="이력 유형 선택"
									allowClear
									style={{ width: 200 }}
									onChange={handleHistoryFilterChange}
									value={historyActionTypeFilter}
								>
									<Option value="APPLICATION">입점 신청</Option>
									<Option value="APPROVAL">입점 승인</Option>
									<Option value="REJECTION">입점 거절</Option>
									<Option value="DEACTIVATION_REQUEST">휴업 신청</Option>
									<Option value="DEACTIVATION_APPROVED">휴업 승인</Option>
									<Option value="DEACTIVATION_REJECTION">휴업 신청 거절</Option>
									<Option value="REACTIVATION_REQUEST">재활성화 신청</Option>
									<Option value="REACTIVATION_APPROVED">재활성화 승인</Option>
									<Option value="REACTIVATION_REJECTION">재활성화 신청 거절</Option>
									<Option value="DEACTIVATED">비활성화</Option>
									<Option value="ACTIVATED">재활성화</Option>
								</Select>
								<Button
									icon={<ReloadOutlined />}
									onClick={() => {
										if (selectedPartner) {
											handleHistoryFilterChange(null);
										}
									}}
									size="small"
								>
									전체 보기
								</Button>
							</Space>
							<Spin spinning={loading}>
								{partnerHistory.length === 0 ? (
									<Alert
										message="이력이 없습니다"
										description={
											historyActionTypeFilter 
												? "선택한 이력 유형의 기록이 없습니다."
												: "파트너의 상태 변경 이력이 없습니다."
										}
										type="info"
										showIcon
									/>
								) : (
									<Timeline>
										{partnerHistory.map((history) => {
											// 파트너가 만든 기록인지 확인
											const isPartnerAction = [
												'APPLICATION',
												'DEACTIVATION_REQUEST',
												'REACTIVATION_REQUEST'
											].includes(history.actionType);

											const getActionColor = (actionType, isPartner) => {
												// 파트너 기록은 파란색 계열, 관리자 기록은 다른 색상
												if (isPartner) {
													return 'blue'; // 파트너 기록
												}
												
												// 관리자 기록은 액션 타입에 따라 색상 구분
												switch (actionType) {
													case 'APPROVAL':
													case 'REACTIVATION_APPROVED':
													case 'ACTIVATED':
														return 'green'; // 승인/활성화
													case 'REJECTION':
													case 'DEACTIVATION_REJECTION':
													case 'REACTIVATION_REJECTION':
														return 'red'; // 거절
													case 'DEACTIVATION_APPROVED':
													case 'DEACTIVATED':
														return 'gray'; // 비활성화
													default:
														return 'purple'; // 기타 관리자 액션
												}
											};

											const getActionLabel = (actionType) => {
												const labels = {
													APPLICATION: '입점 신청',
													APPROVAL: '입점 승인',
													REJECTION: '입점 거절',
													DEACTIVATION_REQUEST: '휴업 신청',
													DEACTIVATION_APPROVED: '휴업 승인',
													DEACTIVATION_REJECTION: '휴업 신청 거절',
													REACTIVATION_REQUEST: '재활성화 신청',
													REACTIVATION_APPROVED: '재활성화 승인',
													REACTIVATION_REJECTION: '재활성화 신청 거절',
													DEACTIVATED: '비활성화',
													ACTIVATED: '재활성화'
												};
												return labels[actionType] || actionType;
											};

											return (
												<Timeline.Item 
													key={history.historyId}
													color={getActionColor(history.actionType, isPartnerAction)}
												>
													<div style={{ marginBottom: 8 }}>
														<strong>{getActionLabel(history.actionType)}</strong>
														{isPartnerAction && (
															<Tag color="blue" style={{ marginLeft: 8 }}>파트너</Tag>
														)}
														{!isPartnerAction && (
															<Tag color="purple" style={{ marginLeft: 8 }}>관리자</Tag>
														)}
													</div>
													{history.reason && (
														<div style={{ marginBottom: 4, color: '#595959' }}>
															사유: {history.reason}
														</div>
													)}
													{history.adminName && (
														<div style={{ marginBottom: 4, color: '#8c8c8c', fontSize: '12px' }}>
															처리자: {history.adminName}
														</div>
													)}
													<div style={{ color: '#8c8c8c', fontSize: '12px' }}>
														{new Date(history.createdAt).toLocaleString('ko-KR')}
													</div>
												</Timeline.Item>
											);
										})}
									</Timeline>
								)}
							</Spin>
						</Tabs.TabPane>
					</Tabs>
				)}
			</Drawer>

			{/* 거절 모달 */}
			<Modal
				title={rejectType === 'deactivation' ? '휴업 신청 거절' : '재활성화 신청 거절'}
				open={rejectModalVisible}
				onCancel={() => {
					setRejectModalVisible(false);
					setRejectType(null);
					setSelectedPartnerForReject(null);
					rejectForm.resetFields();
				}}
				onOk={() => rejectForm.submit()}
				okText="거절"
				okButtonProps={{ danger: true }}
				cancelText="취소"
			>
				{selectedPartnerForReject && (
					<div style={{ marginBottom: 16 }}>
						<p>다음 파트너의 {rejectType === 'deactivation' ? '휴업' : '재활성화'} 신청을 거절하시겠습니까?</p>
						<Descriptions column={1} size="small" bordered>
							<Descriptions.Item label="파트너명">{selectedPartnerForReject.partnerName}</Descriptions.Item>
							<Descriptions.Item label="이메일">{selectedPartnerForReject.email}</Descriptions.Item>
							{rejectType === 'deactivation' && (
								<Descriptions.Item label="휴업 신청 사유">
									{selectedPartnerForReject.deactivationRequestReason || '-'}
								</Descriptions.Item>
							)}
							{rejectType === 'reactivation' && (
								<Descriptions.Item label="재활성화 신청 사유">
									{selectedPartnerForReject.reactivationRequestReason || '-'}
								</Descriptions.Item>
							)}
						</Descriptions>
					</div>
				)}
				<Form
					form={rejectForm}
					onFinish={handleRejectSubmit}
					layout="vertical"
				>
					<Form.Item
						name="rejectionReason"
						label="거절 사유"
						rules={[
							{ required: true, message: '거절 사유를 입력해주세요.' },
							{ min: 10, message: '거절 사유는 최소 10자 이상 입력해주세요.' }
						]}
					>
						<TextArea
							rows={4}
							placeholder="거절 사유를 입력해주세요. (최소 10자 이상)"
							maxLength={500}
							showCount
						/>
					</Form.Item>
				</Form>
			</Modal>
		</>
	);
};

export default PartnerManagement;
