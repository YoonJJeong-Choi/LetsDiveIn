import React, { useEffect, useState, useCallback } from 'react';
import { Card, Table, Button, Badge, Spin, Alert, message, Select, DatePicker, Row, Col, Statistic, Input, Modal, Checkbox, Tabs, Tag, Popconfirm, Descriptions, Tooltip } from 'antd';
import { ReloadOutlined, DollarOutlined, SearchOutlined, CheckOutlined, FilePdfOutlined, FileExcelOutlined, HistoryOutlined } from '@ant-design/icons';
import Flex from 'components/shared-components/Flex';
import AdminService from 'services/AdminService';
import PartnerService from 'services/PartnerService';
import dayjs from 'dayjs';

const { Option } = Select;
const { RangePicker } = DatePicker;
const { TabPane } = Tabs;

const AdminSettlement = () => {
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState(null);
	const [settlementData, setSettlementData] = useState(null);
	const [partners, setPartners] = useState([]);
	const [selectedPartnerId, setSelectedPartnerId] = useState(null);
	const [dateRange, setDateRange] = useState(null);
	const [statusFilter, setStatusFilter] = useState('ALL');
	const [selectedRowKeys, setSelectedRowKeys] = useState([]);
	const [createModalVisible, setCreateModalVisible] = useState(false);
	const [creating, setCreating] = useState(false);
	const [autoComplete, setAutoComplete] = useState(false); // 바로 완료 처리 여부
	
	// 생성된 정산 목록 관련 상태
	const [settlementListLoading, setSettlementListLoading] = useState(false);
	const [settlementList, setSettlementList] = useState(null);
	const [settlementListPartnerId, setSettlementListPartnerId] = useState(null);
	const [settlementListStatus, setSettlementListStatus] = useState(null);
	const [activeTab, setActiveTab] = useState('dashboard');
	
	// 대시보드 관련 상태
	const [dashboardLoading, setDashboardLoading] = useState(false);
	const [dashboardData, setDashboardData] = useState(null);
	
	// 상세 모달 관련 상태
	const [detailModalVisible, setDetailModalVisible] = useState(false);
	const [selectedItem, setSelectedItem] = useState(null);
	
	// 정산 상세 모달 관련 상태
	const [settlementDetailModalVisible, setSettlementDetailModalVisible] = useState(false);
	const [settlementDetail, setSettlementDetail] = useState(null);
	const [settlementDetailLoading, setSettlementDetailLoading] = useState(false);
	const [settlementHistoryTab, setSettlementHistoryTab] = useState('detail');
	const [settlementHistory, setSettlementHistory] = useState([]);
	const [settlementHistoryLoading, setSettlementHistoryLoading] = useState(false);
	
	// 정산 상태 변경 모달 관련 상태
	const [statusModalVisible, setStatusModalVisible] = useState(false);
	const [statusModalSettlementId, setStatusModalSettlementId] = useState(null);
	const [statusModalNewStatus, setStatusModalNewStatus] = useState(null);
	const [paidDate, setPaidDate] = useState(null);
	
	// 정산 생성 후 바로 완료 처리용 상태
	const [pendingCompleteSettlementId, setPendingCompleteSettlementId] = useState(null);
	
	// 지급일 수정 모달 관련 상태
	const [paidDateModalVisible, setPaidDateModalVisible] = useState(false);
	const [paidDateModalSettlementId, setPaidDateModalSettlementId] = useState(null);
	const [paidDateModalPaidDate, setPaidDateModalPaidDate] = useState(null);
	
	// 엑셀 다운로드 모달 관련 상태
	const [excelDownloadModalVisible, setExcelDownloadModalVisible] = useState(false);
	const [excelDownloadPartnerId, setExcelDownloadPartnerId] = useState(null);
	const [excelDownloadStatus, setExcelDownloadStatus] = useState(null);

	// 파트너 목록 조회
	useEffect(() => {
		fetchPartners();
	}, []);

	const fetchPartners = async () => {
		try {
			const response = await PartnerService.getAllPartners({ status: 'APPROVED' });
			const partnerList = Array.isArray(response?.data) ? response.data : [];
			setPartners(partnerList);
		} catch (err) {
			console.error('파트너 목록 조회 실패:', err);
		}
	};

	// 정산 목록 조회
	const fetchSettlementItems = useCallback(async () => {
		try {
			setLoading(true);
			setError(null);

			const params = {};
			if (selectedPartnerId) {
				params.partnerId = selectedPartnerId;
			}
			if (dateRange && dateRange.length === 2) {
				params.startDate = dateRange[0].format('YYYY-MM-DD');
				params.endDate = dateRange[1].format('YYYY-MM-DD');
			}
			if (statusFilter !== 'ALL') {
				params.status = statusFilter;
			}

			const response = await AdminService.getSettlementItems(params);
			const data = response?.data || response;
			setSettlementData(data);
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '정산 목록을 불러오는데 실패했습니다.';
			setError(errorMessage);
			message.error(errorMessage);
		} finally {
			setLoading(false);
		}
	}, [selectedPartnerId, dateRange, statusFilter]);

	// 컴포넌트 마운트 시 및 필터 변경 시 정산 목록 조회
	useEffect(() => {
		fetchSettlementItems();
	}, [fetchSettlementItems]);

	// 빠른 기간 선택
	const handleQuickDateSelect = (type) => {
		if (type === 'all') {
			setDateRange(null);
			return;
		}

		const today = dayjs();
		let start, end;

		switch (type) {
			case 'thisMonth':
				start = today.startOf('month');
				end = today.endOf('month');
				break;
			case 'lastMonth':
				start = today.subtract(1, 'month').startOf('month');
				end = today.subtract(1, 'month').endOf('month');
				break;
			case 'thisYear':
				start = today.startOf('year');
				end = today.endOf('year');
				break;
			default:
				return;
		}

		setDateRange([start, end]);
	};

	// 정산 생성 처리
	const handleCreateSettlement = async () => {
		// 선택된 항목이 없으면 경고
		if (selectedRowKeys.length === 0) {
			message.warning('정산할 항목을 선택해주세요.');
			return;
		}

		// 선택된 항목들 확인
		const selectedItems = settlementData?.items?.filter(item => 
			selectedRowKeys.includes(item.orderItemId) && item.isSettlementReady
		) || [];

		if (selectedItems.length === 0) {
			message.warning('정산 가능한 항목을 선택해주세요.');
			return;
		}

		// 파트너가 선택되지 않았으면, 선택된 항목들의 파트너 확인
		let targetPartnerId = selectedPartnerId;
		if (!targetPartnerId) {
			// 선택된 항목들의 파트너 ID 추출
			const partnerIds = [...new Set(selectedItems.map(item => item.partnerId).filter(id => id != null))];
			
			if (partnerIds.length === 0) {
				message.error('선택한 항목에 파트너 정보가 없습니다.');
				return;
			}
			
			if (partnerIds.length > 1) {
				message.warning('다른 파트너의 항목이 섞여 있습니다. 한 번에 하나의 파트너만 정산할 수 있습니다.');
				return;
			}
			
			// 단일 파트너로 자동 설정
			targetPartnerId = partnerIds[0];
			setSelectedPartnerId(targetPartnerId);
			message.info(`파트너가 자동으로 선택되었습니다: ${selectedItems[0].partnerName || '파트너 ID ' + targetPartnerId}`);
		}

		setCreateModalVisible(true);
	};

	// 정산 생성 확인
	const handleConfirmCreateSettlement = async () => {
		try {
			setCreating(true);

			const selectedItems = settlementData?.items?.filter(item => 
				selectedRowKeys.includes(item.orderItemId) && item.isSettlementReady
			) || [];

			// 파트너 ID 확인 (선택된 항목에서 자동으로 가져올 수도 있음)
			const finalPartnerId = selectedPartnerId || selectedItems[0]?.partnerId;
			if (!finalPartnerId) {
				message.error('파트너 정보를 찾을 수 없습니다.');
				return;
			}

			// 정산 기간은 백엔드에서 선택한 주문 아이템들의 실제 주문일 범위로 자동 계산됨
			// 프론트엔드에서는 전달하지 않음 (null로 전달하면 백엔드에서 자동 계산)
			const requestData = {
				partnerId: finalPartnerId,
				orderItemIds: selectedItems.map(item => item.orderItemId),
				settlementPeriodStart: null, // 백엔드에서 자동 계산
				settlementPeriodEnd: null    // 백엔드에서 자동 계산
			};

			const response = await AdminService.createSettlement(requestData);
			const settlementId = response?.data?.settlementId || response?.settlementId;
			
			// 바로 완료 처리 옵션이 선택된 경우 - 지급일 입력 모달 표시
			if (autoComplete && settlementId) {
				setPendingCompleteSettlementId(settlementId);
				setCreateModalVisible(false);
				setAutoComplete(false);
				setSelectedRowKeys([]);
				// 지급일 입력 모달 표시
				handleOpenStatusModal(settlementId, 'COMPLETED');
			} else {
				message.success('정산이 생성되었습니다.');
				setCreateModalVisible(false);
				setAutoComplete(false);
				setSelectedRowKeys([]);
				fetchSettlementItems();
				// 생성된 정산 목록도 새로고침
				if (activeTab === 'list') {
					fetchSettlementList();
				}
			}
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '정산 생성에 실패했습니다.';
			message.error(errorMessage);
		} finally {
			setCreating(false);
		}
	};
	
	// 생성된 정산 목록 조회
	const fetchSettlementList = useCallback(async () => {
		try {
			setSettlementListLoading(true);
			setError(null);
			
			const params = {};
			if (settlementListPartnerId) {
				params.partnerId = settlementListPartnerId;
			}
			if (settlementListStatus) {
				params.status = settlementListStatus;
			}
			
			const response = await AdminService.getSettlementList(params);
			const data = response?.data || response;
			console.log('생성된 정산 목록:', data);
			setSettlementList(data);
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '정산 목록을 불러오는데 실패했습니다.';
			setError(errorMessage);
			message.error(errorMessage);
		} finally {
			setSettlementListLoading(false);
		}
	}, [settlementListPartnerId, settlementListStatus]);
	
	// 정산 상태 변경 모달 열기
	const handleOpenStatusModal = (settlementId, newStatus) => {
		setStatusModalSettlementId(settlementId);
		setStatusModalNewStatus(newStatus);
		// 완료 처리 시 기본값을 오늘 날짜로 설정
		if (newStatus === 'COMPLETED') {
			setPaidDate(dayjs());
		} else {
			setPaidDate(null);
		}
		setStatusModalVisible(true);
	};
	
	// 지급일 수정 모달 열기
	const handleOpenPaidDateModal = (settlementId, currentPaidDate) => {
		setPaidDateModalSettlementId(settlementId);
		setPaidDateModalPaidDate(currentPaidDate ? dayjs(currentPaidDate) : dayjs());
		setPaidDateModalVisible(true);
	};
	
	// 정산서 PDF 다운로드
	const handleDownloadPdf = async (settlementId) => {
		try {
			await AdminService.downloadSettlementPdf(settlementId);
			message.success('정산서가 다운로드되었습니다.');
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '정산서 다운로드에 실패했습니다.';
			message.error(errorMessage);
		}
	};
	
	// 엑셀 다운로드 모달 열기
	const handleOpenExcelDownloadModal = () => {
		// 현재 목록 필터 값을 기본값으로 설정
		setExcelDownloadPartnerId(settlementListPartnerId);
		setExcelDownloadStatus(settlementListStatus);
		setExcelDownloadModalVisible(true);
	};
	
	// 정산 목록 Excel 다운로드
	const handleDownloadExcel = async () => {
		try {
			const params = {};
			if (excelDownloadPartnerId) {
				params.partnerId = excelDownloadPartnerId;
			}
			if (excelDownloadStatus) {
				params.status = excelDownloadStatus;
			}
			await AdminService.downloadSettlementExcel(params);
			message.success('엑셀 파일이 다운로드되었습니다.');
			setExcelDownloadModalVisible(false);
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '엑셀 다운로드에 실패했습니다.';
			message.error(errorMessage);
		}
	};
	
	// 지급일 수정 확인
	const handleConfirmPaidDateChange = async () => {
		if (!paidDateModalPaidDate) {
			message.warning('지급일을 입력해주세요.');
			return;
		}

		try {
			const paidDateStr = paidDateModalPaidDate.format('YYYY-MM-DD');
			await AdminService.updateSettlementStatus(paidDateModalSettlementId, 'COMPLETED', paidDateStr);
			message.success('지급일이 수정되었습니다.');
			setPaidDateModalVisible(false);
			setPaidDateModalPaidDate(null);
			fetchSettlementList();
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '지급일 수정에 실패했습니다.';
			message.error(errorMessage);
		}
	};

	// 정산 상태 변경 확인
	const handleConfirmStatusChange = async () => {
		// 완료 처리 시 지급일 필수 검증
		if (statusModalNewStatus === 'COMPLETED' && !paidDate) {
			message.warning('정산 완료 처리 시 지급일을 입력해주세요.');
			return;
		}

		try {
			const paidDateStr = paidDate ? paidDate.format('YYYY-MM-DD') : null;
			await AdminService.updateSettlementStatus(statusModalSettlementId, statusModalNewStatus, paidDateStr);
			
			// 정산 생성 후 바로 완료 처리한 경우
			if (pendingCompleteSettlementId === statusModalSettlementId) {
				message.success('정산이 생성되고 완료 처리되었습니다.');
				setPendingCompleteSettlementId(null);
				// 생성된 정산 목록 탭으로 이동
				setActiveTab('list');
			} else {
				message.success('정산 상태가 변경되었습니다.');
			}
			
			setStatusModalVisible(false);
			setPaidDate(null);
			fetchSettlementItems();
			fetchSettlementList();
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '정산 상태 변경에 실패했습니다.';
			message.error(errorMessage);
		}
	};
	
	// 대시보드 조회
	const fetchDashboard = useCallback(async () => {
		try {
			setDashboardLoading(true);
			const response = await AdminService.getSettlementDashboard();
			const data = response?.data || response;
			setDashboardData(data);
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '대시보드 데이터를 불러오는데 실패했습니다.';
			message.error(errorMessage);
		} finally {
			setDashboardLoading(false);
		}
	}, []);
	
	// 탭 변경 시 생성된 정산 목록 조회
	useEffect(() => {
		if (activeTab === 'list') {
			fetchSettlementList();
		} else if (activeTab === 'dashboard') {
			fetchDashboard();
		}
	}, [activeTab, fetchSettlementList, fetchDashboard]);

	// 테이블 행 선택
	const rowSelection = {
		selectedRowKeys,
		onChange: setSelectedRowKeys,
		getCheckboxProps: (record) => ({
			disabled: !record.isSettlementReady, // 정산 불가능한 항목은 선택 불가
		}),
	};

	// 상세 모달 열기
	const handleShowDetail = (record) => {
		setSelectedItem(record);
		setDetailModalVisible(true);
	};
	
	// 정산 상세 모달 열기
	const handleShowSettlementDetail = async (settlementId) => {
		try {
			setSettlementDetailLoading(true);
			setSettlementHistoryTab('detail');
			const response = await AdminService.getSettlementDetail(settlementId);
			const data = response?.data || response;
			setSettlementDetail(data);
			setSettlementDetailModalVisible(true);
			// 기본 정보 탭이므로 이력은 나중에 로드
			setSettlementHistory([]);
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '정산 상세 정보를 불러오는데 실패했습니다.';
			message.error(errorMessage);
		} finally {
			setSettlementDetailLoading(false);
		}
	};
	
	// 정산 이력 조회
	const fetchSettlementHistory = useCallback(async (settlementId) => {
		try {
			setSettlementHistoryLoading(true);
			const response = await AdminService.getSettlementHistory(settlementId);
			const data = response?.data || response;
			setSettlementHistory(Array.isArray(data) ? data : []);
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '정산 이력을 불러오는데 실패했습니다.';
			message.error(errorMessage);
			setSettlementHistory([]);
		} finally {
			setSettlementHistoryLoading(false);
		}
	}, []);
	
	// 정산 상세 모달 탭 변경
	const handleSettlementDetailTabChange = (key) => {
		setSettlementHistoryTab(key);
		if (key === 'history' && settlementDetail && !settlementHistory.length) {
			// 이력 탭으로 변경 시 이력 조회
			fetchSettlementHistory(settlementDetail.settlementId);
		}
	};

	// 테이블 컬럼 정의 (간소화된 목록)
	const tableColumns = [
		{
			title: '파트너',
			dataIndex: 'partnerName',
			key: 'partnerName',
			width: 150,
			render: (text) => <span className="font-weight-semibold">{text || '-'}</span>
		},
		{
			title: '상품명',
			dataIndex: 'productName',
			key: 'productName',
			ellipsis: true,
			render: (text) => <span className="font-weight-semibold">{text}</span>
		},
		{
			title: '판매금액',
			dataIndex: 'salesAmount',
			key: 'salesAmount',
			width: 120,
			align: 'right',
			sorter: (a, b) => a.salesAmount - b.salesAmount,
			render: (amount) => `₩${(amount || 0).toLocaleString()}`
		},
		{
			title: '정산금액',
			dataIndex: 'settlementAmount',
			key: 'settlementAmount',
			width: 120,
			align: 'right',
			sorter: (a, b) => a.settlementAmount - b.settlementAmount,
			render: (amount) => (
				<span className="font-weight-semibold text-success">
					₩{(amount || 0).toLocaleString()}
				</span>
			)
		},
		{
			title: '정산 가능',
			dataIndex: 'isSettlementReady',
			key: 'isSettlementReady',
			width: 100,
			filters: [
				{ text: '가능', value: true },
				{ text: '불가', value: false }
			],
			onFilter: (value, record) => record.isSettlementReady === value,
			render: (isReady) => (
				<Badge
					status={isReady ? 'success' : 'default'}
					text={isReady ? '가능' : '불가'}
				/>
			)
		},
		{
			title: '작업',
			key: 'action',
			width: 100,
			render: (_, record) => (
				<Button 
					type="link" 
					size="small"
					onClick={() => handleShowDetail(record)}
				>
					상세보기
				</Button>
			)
		}
	];

	// 생성된 정산 목록 테이블 컬럼 정의 (간소화)
	const settlementListColumns = [
		{
			title: '정산 ID',
			dataIndex: 'settlementId',
			key: 'settlementId',
			width: 100,
			sorter: (a, b) => a.settlementId - b.settlementId,
		},
		{
			title: '파트너',
			dataIndex: 'partnerName',
			key: 'partnerName',
			width: 150,
			render: (text, record) => (
				<div>
					<div className="font-weight-semibold">{text || '-'}</div>
					<div className="text-muted" style={{ fontSize: '12px' }}>ID: {record.partnerId}</div>
				</div>
			)
		},
		{
			title: '주문 내역',
			key: 'itemSummary',
			width: 120,
			render: (_, record) => {
				const itemCount = record.itemCount || 0;
				return (
					<span className="font-weight-semibold">{itemCount}개</span>
				);
			}
		},
		{
			title: '총 정산금액',
			dataIndex: 'settlementAmount',
			key: 'settlementAmount',
			width: 130,
			align: 'right',
			sorter: (a, b) => a.settlementAmount - b.settlementAmount,
			render: (amount) => (
				<span className="font-weight-semibold text-success" style={{ fontSize: '14px' }}>
					₩{(amount || 0).toLocaleString()}
				</span>
			)
		},
		{
			title: '상태',
			dataIndex: 'settlementStatus',
			key: 'settlementStatus',
			width: 100,
			filters: [
				{ text: '대기', value: 'PENDING' },
				{ text: '완료', value: 'COMPLETED' },
				{ text: '취소', value: 'CANCELLED' }
			],
			onFilter: (value, record) => record.settlementStatus === value,
			render: (status) => {
				const statusConfig = {
					'PENDING': { color: 'orange', text: '대기' },
					'COMPLETED': { color: 'green', text: '완료' },
					'CANCELLED': { color: 'red', text: '취소' }
				};
				const config = statusConfig[status] || { color: 'default', text: status };
				return <Tag color={config.color}>{config.text}</Tag>;
			}
		},
		{
			title: '생성일',
			dataIndex: 'settlementCreatedAt',
			key: 'settlementCreatedAt',
			width: 110,
			sorter: (a, b) => {
				if (!a.settlementCreatedAt && !b.settlementCreatedAt) return 0;
				if (!a.settlementCreatedAt) return 1;
				if (!b.settlementCreatedAt) return -1;
				return dayjs(a.settlementCreatedAt).valueOf() - dayjs(b.settlementCreatedAt).valueOf();
			},
			render: (date) => date ? dayjs(date).format('YYYY-MM-DD') : '-'
		},
		{
			title: '작업',
			key: 'action',
			width: 300,
					render: (_, record) => {
						const status = record.settlementStatus;
						return (
							<div>
								<Button 
									size="small" 
									type="link"
									style={{ marginRight: 8 }}
									onClick={() => handleShowSettlementDetail(record.settlementId)}
								>
									상세보기
								</Button>
								<Button 
									size="small" 
									type="link"
									icon={<FilePdfOutlined />}
									style={{ marginRight: 8 }}
									onClick={() => handleDownloadPdf(record.settlementId)}
								>
									정산서
								</Button>
								{status === 'PENDING' && (
									<Button 
										size="small" 
										type="primary"
										onClick={() => handleOpenStatusModal(record.settlementId, 'COMPLETED')}
									>
										완료 처리
									</Button>
								)}
								{status === 'COMPLETED' && (
									<Button 
										size="small" 
										type="link"
										onClick={() => handleOpenPaidDateModal(record.settlementId, record.settlementPaidDate)}
									>
										지급일 수정
									</Button>
								)}
								{(status === 'PENDING' || status === 'COMPLETED') && (
									<Button 
										size="small" 
										type="link"
										danger
										onClick={() => handleOpenStatusModal(record.settlementId, 'CANCELLED')}
									>
										취소
									</Button>
								)}
							</div>
						);
					}
		}
	];

	// 탭 변경 핸들러
	const handleTabChange = (key) => {
		setActiveTab(key);
		// 탭 변경 시 정산 대상 조회의 선택 항목 초기화
		if (key === 'list') {
			setSelectedRowKeys([]);
		}
	};

	return (
		<>
			<Tabs activeKey={activeTab} onChange={handleTabChange}>
				<TabPane tab="대시보드" key="dashboard">
					<Card>
						<Spin spinning={dashboardLoading}>
							{dashboardData && (
								<>
									{/* 전체 정산 현황 통계 */}
									<h3 style={{ marginBottom: 16 }}>전체 정산 현황</h3>
									<Row gutter={16} className="mb-4">
										<Col xs={24} sm={12} md={6}>
											<Card>
												<Statistic
													title="전체 정산 건수"
													value={dashboardData.totalSettlementCount || 0}
													suffix="건"
													valueStyle={{ color: '#1890ff' }}
													formatter={(value) => value.toLocaleString()}
												/>
											</Card>
										</Col>
										<Col xs={24} sm={12} md={6}>
											<Card>
												<Statistic
													title="대기 중"
													value={dashboardData.pendingSettlementCount || 0}
													suffix="건"
													valueStyle={{ color: '#faad14' }}
													formatter={(value) => value.toLocaleString()}
												/>
											</Card>
										</Col>
										<Col xs={24} sm={12} md={6}>
											<Card>
												<Statistic
													title="완료"
													value={dashboardData.completedSettlementCount || 0}
													suffix="건"
													valueStyle={{ color: '#3f8600' }}
													formatter={(value) => value.toLocaleString()}
												/>
											</Card>
										</Col>
										<Col xs={24} sm={12} md={6}>
											<Card>
												<Statistic
													title="전체 정산 금액"
													value={dashboardData.totalSettlementAmount || 0}
													prefix="₩"
													valueStyle={{ color: '#3f8600' }}
													formatter={(value) => value.toLocaleString()}
												/>
											</Card>
										</Col>
									</Row>
									<Row gutter={16} className="mb-4">
										<Col xs={24} sm={12} md={8}>
											<Card>
												<Statistic
													title="전체 판매금액"
													value={dashboardData.totalSalesAmount || 0}
													prefix="₩"
													valueStyle={{ color: '#1890ff' }}
													formatter={(value) => value.toLocaleString()}
												/>
											</Card>
										</Col>
										<Col xs={24} sm={12} md={8}>
											<Card>
												<Statistic
													title="전체 수수료"
													value={dashboardData.totalCommissionAmount || 0}
													prefix="₩"
													valueStyle={{ color: '#cf1322' }}
													formatter={(value) => value.toLocaleString()}
												/>
											</Card>
										</Col>
										<Col xs={24} sm={12} md={8}>
											<Card>
												<Statistic
													title="전체 정산금액"
													value={dashboardData.totalSettlementAmount || 0}
													prefix="₩"
													valueStyle={{ color: '#3f8600' }}
													formatter={(value) => value.toLocaleString()}
												/>
											</Card>
										</Col>
									</Row>
									
									{/* 파트너별 정산 금액 순위 */}
									<h3 style={{ marginBottom: 16, marginTop: 24 }}>파트너별 정산 금액 순위</h3>
									<Card>
										<Table
											columns={[
												{
													title: '순위',
													dataIndex: 'rank',
													key: 'rank',
													width: 80,
													render: (rank) => (
														<span style={{ fontWeight: 'bold', fontSize: '16px' }}>
															{rank === 1 ? '🥇' : rank === 2 ? '🥈' : rank === 3 ? '🥉' : rank}
														</span>
													)
												},
												{
													title: '파트너명',
													dataIndex: 'partnerName',
													key: 'partnerName',
													render: (text) => <span className="font-weight-semibold">{text}</span>
												},
												{
													title: '정산 건수',
													dataIndex: 'settlementCount',
													key: 'settlementCount',
													width: 120,
													align: 'right',
													render: (count) => `${count}건`
												},
												{
													title: '총 판매금액',
													dataIndex: 'totalSalesAmount',
													key: 'totalSalesAmount',
													width: 150,
													align: 'right',
													render: (amount) => `₩${(amount || 0).toLocaleString()}`
												},
												{
													title: '총 수수료',
													dataIndex: 'totalCommissionAmount',
													key: 'totalCommissionAmount',
													width: 150,
													align: 'right',
													render: (amount) => (
														<span className="text-danger">₩{(amount || 0).toLocaleString()}</span>
													)
												},
												{
													title: '총 정산금액',
													dataIndex: 'totalSettlementAmount',
													key: 'totalSettlementAmount',
													width: 150,
													align: 'right',
													render: (amount) => (
														<span className="font-weight-semibold text-success">
															₩{(amount || 0).toLocaleString()}
														</span>
													)
												}
											]}
											dataSource={dashboardData.partnerRankings || []}
											rowKey="partnerId"
											pagination={false}
											size="small"
										/>
									</Card>
									
									{/* 월별 정산 현황 */}
									<h3 style={{ marginBottom: 16, marginTop: 24 }}>월별 정산 현황</h3>
									<Card>
										<Table
											columns={[
												{
													title: '년월',
													dataIndex: 'yearMonth',
													key: 'yearMonth',
													width: 120,
													render: (text) => <span className="font-weight-semibold">{text}</span>
												},
												{
													title: '정산 건수',
													dataIndex: 'settlementCount',
													key: 'settlementCount',
													width: 120,
													align: 'right',
													render: (count) => `${count}건`
												},
												{
													title: '총 판매금액',
													dataIndex: 'totalSalesAmount',
													key: 'totalSalesAmount',
													width: 150,
													align: 'right',
													render: (amount) => `₩${(amount || 0).toLocaleString()}`
												},
												{
													title: '총 수수료',
													dataIndex: 'totalCommissionAmount',
													key: 'totalCommissionAmount',
													width: 150,
													align: 'right',
													render: (amount) => (
														<span className="text-danger">₩{(amount || 0).toLocaleString()}</span>
													)
												},
												{
													title: '총 정산금액',
													dataIndex: 'totalSettlementAmount',
													key: 'totalSettlementAmount',
													width: 150,
													align: 'right',
													render: (amount) => (
														<span className="font-weight-semibold text-success">
															₩{(amount || 0).toLocaleString()}
														</span>
													)
												}
											]}
											dataSource={dashboardData.monthlySettlements || []}
											rowKey="yearMonth"
											pagination={false}
											size="small"
										/>
									</Card>
								</>
							)}
							{!dashboardLoading && !dashboardData && (
								<Alert
									message="대시보드 데이터가 없습니다"
									description="정산 데이터가 없어 대시보드를 표시할 수 없습니다."
									type="info"
									showIcon
								/>
							)}
						</Spin>
					</Card>
				</TabPane>
				<TabPane tab="정산 대상 조회" key="items">
					<Card>
				<Flex alignItems="center" justifyContent="between" mobileFlex={false}>
					<Flex className="mb-1" mobileFlex={false}>
						<div className="mr-md-3 mb-3">
							<Select
								placeholder="파트너 선택"
								style={{ width: 200 }}
								value={selectedPartnerId}
								onChange={setSelectedPartnerId}
								allowClear
								showSearch
								filterOption={(input, option) =>
									option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
								}
							>
								<Option value={null}>전체 파트너</Option>
								{partners.map(partner => (
									<Option key={partner.partnerId} value={partner.partnerId}>
										{partner.partnerName}
									</Option>
								))}
							</Select>
						</div>
						<div className="mr-md-3 mb-3">
							<Select
								placeholder="빠른 선택"
								style={{ width: 150 }}
								onChange={handleQuickDateSelect}
								allowClear
							>
								<Option value="thisMonth">이번 달</Option>
								<Option value="lastMonth">지난 달</Option>
								<Option value="thisYear">올해</Option>
								<Option value="all">전체</Option>
							</Select>
						</div>
						<div className="mr-md-3 mb-3">
							<RangePicker
								format="YYYY-MM-DD"
								value={dateRange}
								onChange={setDateRange}
								style={{ width: 250 }}
							/>
						</div>
						<div className="mr-md-3 mb-3">
							<Select
								placeholder="상태 필터"
								style={{ width: 150 }}
								value={statusFilter}
								onChange={setStatusFilter}
							>
								<Option value="ALL">전체</Option>
								<Option value="SETTLEMENT_READY">정산 가능만</Option>
							</Select>
						</div>
					</Flex>
					<div>
						<Button
							type="primary"
							icon={<CheckOutlined />}
							onClick={handleCreateSettlement}
							disabled={selectedRowKeys.length === 0}
							style={{ marginRight: 8 }}
						>
							정산 생성 ({selectedRowKeys.length})
						</Button>
						<Button
							type="default"
							icon={<ReloadOutlined />}
							onClick={fetchSettlementItems}
							loading={loading}
						>
							새로고침
						</Button>
					</div>
				</Flex>
			</Card>

			{/* 합계 카드 */}
			{settlementData?.summary && (
				<Row gutter={16} className="mb-3">
					{settlementData.totalPartnerCount !== null && (
						<Col xs={24} sm={12} md={6}>
							<Card>
								<Statistic
									title="파트너 수"
									value={settlementData.totalPartnerCount || 0}
									suffix="명"
									valueStyle={{ color: '#722ed1' }}
									formatter={(value) => value.toLocaleString()}
								/>
							</Card>
						</Col>
					)}
					<Col xs={24} sm={12} md={6}>
						<Card>
							<Statistic
								title="총 판매금액"
								value={settlementData.summary.totalSalesAmount || 0}
								prefix="₩"
								valueStyle={{ color: '#1890ff' }}
								formatter={(value) => value.toLocaleString()}
							/>
						</Card>
					</Col>
					<Col xs={24} sm={12} md={6}>
						<Card>
							<Statistic
								title="총 수수료"
								value={settlementData.summary.totalCommissionAmount || 0}
								prefix="₩"
								valueStyle={{ color: '#cf1322' }}
								formatter={(value) => value.toLocaleString()}
							/>
						</Card>
					</Col>
					<Col xs={24} sm={12} md={6}>
						<Card>
							<Statistic
								title="총 정산금액"
								value={settlementData.summary.totalSettlementAmount || 0}
								prefix="₩"
								valueStyle={{ color: '#3f8600' }}
								formatter={(value) => value.toLocaleString()}
							/>
						</Card>
					</Col>
					<Col xs={24} sm={12} md={6}>
						<Card>
							<Statistic
								title="정산 대상 건수"
								value={settlementData.summary.settlementReadyCount || 0}
								suffix="건"
								valueStyle={{ color: '#722ed1' }}
								formatter={(value) => value.toLocaleString()}
							/>
						</Card>
					</Col>
				</Row>
			)}

			<Card>
				{error && (
					<Alert
						message="오류"
						description={error}
						type="error"
						showIcon
						className="mb-3"
						closable
						onClose={() => setError(null)}
					/>
				)}

				{!loading && settlementData && (!settlementData.items || settlementData.items.length === 0) && !error && (
					<Alert
						message="정산 대상이 없습니다"
						description={
							selectedPartnerId || dateRange || statusFilter !== 'ALL' 
								? "선택한 조건에 맞는 정산 대상 주문이 없습니다. 다른 조건을 선택해보세요."
								: "아직 정산 대상이 되는 주문이 없습니다."
						}
						type="info"
						showIcon
						className="mb-3"
					/>
				)}

				<Spin spinning={loading}>
					<Table
						columns={tableColumns}
						dataSource={settlementData?.items || []}
						rowKey="orderItemId"
						rowSelection={rowSelection}
						pagination={{
							pageSize: 10,
							showSizeChanger: true,
							pageSizeOptions: ['10', '20', '50', '100'],
							showTotal: (total, range) => `${range[0]}-${range[1]} / 총 ${total}개`
						}}
						scroll={{ x: 'max-content' }}
					/>
				</Spin>
			</Card>

			{/* 정산 생성 확인 모달 */}
			<Modal
				title="정산 생성 확인"
				open={createModalVisible}
				onOk={handleConfirmCreateSettlement}
				onCancel={() => {
					setCreateModalVisible(false);
					setAutoComplete(false);
				}}
				confirmLoading={creating}
				okText="생성"
				cancelText="취소"
				width={500}
			>
				<div style={{ marginBottom: 16 }}>
					<p>선택한 <strong>{selectedRowKeys.length}개</strong>의 항목으로 정산을 생성하시겠습니까?</p>
					{(selectedPartnerId || (settlementData?.items?.find(item => selectedRowKeys.includes(item.orderItemId))?.partnerId)) && (
						<p>
							<strong>파트너:</strong> {
								partners.find(p => p.partnerId === (selectedPartnerId || settlementData?.items?.find(item => selectedRowKeys.includes(item.orderItemId))?.partnerId))?.partnerName || 
								settlementData?.items?.find(item => selectedRowKeys.includes(item.orderItemId))?.partnerName || 
								'-'
							}
						</p>
					)}
					{dateRange && dateRange.length === 2 && (
						<p>
							<strong>정산 기간:</strong> {dateRange[0].format('YYYY-MM-DD')} ~ {dateRange[1].format('YYYY-MM-DD')}
						</p>
					)}
				</div>
				<Checkbox
					checked={autoComplete}
					onChange={(e) => setAutoComplete(e.target.checked)}
				>
					정산 생성 후 바로 완료 처리하기
				</Checkbox>
				<div style={{ marginTop: 12, padding: 12, backgroundColor: '#f5f5f5', borderRadius: 4 }}>
					<p style={{ margin: 0, fontSize: '12px', color: '#666' }}>
						<strong>참고:</strong> 정산 생성 후 "생성된 정산 목록" 탭에서 상태를 변경할 수 있습니다.
					</p>
				</div>
			</Modal>
				</TabPane>
			
			<TabPane tab="생성된 정산 목록" key="list">
				<Card>
					<Flex alignItems="center" justifyContent="between" mobileFlex={false}>
						<Flex className="mb-1" mobileFlex={false}>
							<div className="mr-md-3 mb-3">
								<Select
									placeholder="파트너 선택"
									style={{ width: 200 }}
									value={settlementListPartnerId}
									onChange={setSettlementListPartnerId}
									allowClear
									showSearch
									filterOption={(input, option) =>
										option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
									}
								>
									<Option value={null}>전체 파트너</Option>
									{partners.map(partner => (
										<Option key={partner.partnerId} value={partner.partnerId}>
											{partner.partnerName}
										</Option>
									))}
								</Select>
							</div>
							<div className="mr-md-3 mb-3">
								<Select
									placeholder="상태 필터"
									style={{ width: 150 }}
									value={settlementListStatus}
									onChange={setSettlementListStatus}
									allowClear
								>
									<Option value="PENDING">대기</Option>
									<Option value="COMPLETED">완료</Option>
									<Option value="CANCELLED">취소</Option>
								</Select>
							</div>
						</Flex>
						<div>
							<Button
								type="default"
								icon={<FileExcelOutlined />}
								onClick={handleOpenExcelDownloadModal}
								style={{ marginRight: 8 }}
							>
								엑셀 다운로드
							</Button>
							<Button
								type="default"
								icon={<ReloadOutlined />}
								onClick={fetchSettlementList}
								loading={settlementListLoading}
							>
								새로고침
							</Button>
						</div>
					</Flex>
				</Card>
				
				<Card>
					{error && (
						<Alert
							message="오류"
							description={error}
							type="error"
							showIcon
							className="mb-3"
							closable
							onClose={() => setError(null)}
						/>
					)}
					
					{!settlementListLoading && settlementList && (!settlementList.settlements || settlementList.settlements.length === 0) && !error && (
						<Alert
							message="생성된 정산이 없습니다"
							description="아직 생성된 정산이 없습니다."
							type="info"
							showIcon
							className="mb-3"
						/>
					)}
					
					<Spin spinning={settlementListLoading}>
						<Table
							columns={settlementListColumns}
							dataSource={settlementList?.settlements || []}
							rowKey="settlementId"
							pagination={{
								pageSize: 10,
								showSizeChanger: true,
								pageSizeOptions: ['10', '20', '50', '100'],
								showTotal: (total, range) => `${range[0]}-${range[1]} / 총 ${total}개`
							}}
							scroll={{ x: 'max-content' }}
						/>
					</Spin>
				</Card>
			</TabPane>
		</Tabs>
		
		{/* 정산 상세 모달 */}
		<Modal
			title="정산 상세 정보"
			open={detailModalVisible}
			onCancel={() => setDetailModalVisible(false)}
			footer={[
				<Button key="close" onClick={() => setDetailModalVisible(false)}>
					닫기
				</Button>
			]}
			width={800}
		>
			{selectedItem && (
				<Descriptions bordered column={2}>
					<Descriptions.Item label="파트너">
						{selectedItem.partnerName || '-'} (ID: {selectedItem.partnerId})
					</Descriptions.Item>
					<Descriptions.Item label="주문번호">
						{selectedItem.orderId}
					</Descriptions.Item>
					<Descriptions.Item label="주문아이템번호">
						{selectedItem.orderItemId}
					</Descriptions.Item>
					<Descriptions.Item label="상품명">
						{selectedItem.productName}
					</Descriptions.Item>
					<Descriptions.Item label="수량">
						{selectedItem.quantity}개
					</Descriptions.Item>
					<Descriptions.Item label="판매금액">
						₩{(selectedItem.salesAmount || 0).toLocaleString()}
					</Descriptions.Item>
					<Descriptions.Item label="수수료">
						<span className="text-danger">₩{(selectedItem.commissionAmount || 0).toLocaleString()}</span>
					</Descriptions.Item>
					<Descriptions.Item label="정산금액">
						<span className="font-weight-semibold text-success">
							₩{(selectedItem.settlementAmount || 0).toLocaleString()}
						</span>
					</Descriptions.Item>
					<Descriptions.Item label="주문일">
						{selectedItem.orderDate ? dayjs(selectedItem.orderDate).format('YYYY-MM-DD HH:mm') : '-'}
					</Descriptions.Item>
					<Descriptions.Item label="배송완료일">
						{selectedItem.deliveryCompletedDate ? dayjs(selectedItem.deliveryCompletedDate).format('YYYY-MM-DD HH:mm') : '-'}
					</Descriptions.Item>
					<Descriptions.Item label="정산 가능">
						<Badge
							status={selectedItem.isSettlementReady ? 'success' : 'default'}
							text={selectedItem.isSettlementReady ? '가능' : '불가'}
						/>
					</Descriptions.Item>
				</Descriptions>
			)}
		</Modal>
		
		{/* 정산 상태 변경 모달 */}
		<Modal
			title="정산 상태 변경"
			open={statusModalVisible}
			onOk={handleConfirmStatusChange}
			onCancel={() => {
				setStatusModalVisible(false);
				setPaidDate(null);
			}}
			okText="변경"
			cancelText="취소"
			width={500}
		>
			<div style={{ marginBottom: 16 }}>
				<p>
					정산 상태를 <strong>
						{statusModalNewStatus === 'COMPLETED' ? '완료' : 
						 statusModalNewStatus === 'CANCELLED' ? '취소' : 
						 statusModalNewStatus}
					</strong>로 변경하시겠습니까?
				</p>
				{statusModalNewStatus === 'CANCELLED' && (
					<Alert
						message="주의"
						description="정산을 취소하면 해당 정산에 포함된 주문 아이템이 다시 정산 대상으로 복구됩니다."
						type="warning"
						showIcon
						style={{ marginTop: 16 }}
					/>
				)}
				{statusModalNewStatus === 'COMPLETED' && (
					<div style={{ marginTop: 16 }}>
						<p style={{ marginBottom: 8 }}>
							<strong>정산 지급일 <span style={{ color: 'red' }}>*</span>:</strong>
						</p>
						<DatePicker
							style={{ width: '100%' }}
							format="YYYY-MM-DD"
							value={paidDate}
							onChange={setPaidDate}
							placeholder="지급일 선택"
							required
						/>
						<p style={{ marginTop: 8, fontSize: '12px', color: '#666' }}>
							실제 입금이 완료된 날짜를 입력해주세요. (기본값: 오늘)
						</p>
					</div>
				)}
			</div>
		</Modal>
		
		{/* 지급일 수정 모달 */}
		<Modal
			title="정산 지급일 수정"
			open={paidDateModalVisible}
			onOk={handleConfirmPaidDateChange}
			onCancel={() => {
				setPaidDateModalVisible(false);
				setPaidDateModalPaidDate(null);
			}}
			okText="수정"
			cancelText="취소"
			width={500}
		>
			<div style={{ marginBottom: 16 }}>
				<p>
					정산 지급일을 수정하시겠습니까?
				</p>
				<div style={{ marginTop: 16 }}>
					<p style={{ marginBottom: 8 }}>
						<strong>정산 지급일 <span style={{ color: 'red' }}>*</span>:</strong>
					</p>
					<DatePicker
						style={{ width: '100%' }}
						format="YYYY-MM-DD"
						value={paidDateModalPaidDate}
						onChange={setPaidDateModalPaidDate}
						placeholder="지급일 선택"
						required
					/>
					<p style={{ marginTop: 8, fontSize: '12px', color: '#666' }}>
						실제 입금이 완료된 날짜를 입력해주세요.
					</p>
				</div>
			</div>
		</Modal>
		
		{/* 엑셀 다운로드 필터 모달 */}
		<Modal
			title="엑셀 다운로드 필터 설정"
			open={excelDownloadModalVisible}
			onOk={handleDownloadExcel}
			onCancel={() => {
				setExcelDownloadModalVisible(false);
				setExcelDownloadPartnerId(null);
				setExcelDownloadStatus(null);
			}}
			okText="다운로드"
			cancelText="취소"
			width={500}
		>
			<div style={{ marginBottom: 16 }}>
				<p style={{ marginBottom: 16, color: '#666' }}>
					다운로드할 정산 목록의 필터를 설정하세요. 필터를 설정하지 않으면 전체 정산 목록이 다운로드됩니다.
				</p>
				<div style={{ marginBottom: 16 }}>
					<p style={{ marginBottom: 8 }}>
						<strong>파트너:</strong>
					</p>
					<Select
						placeholder="파트너 선택 (선택사항)"
						style={{ width: '100%' }}
						value={excelDownloadPartnerId}
						onChange={setExcelDownloadPartnerId}
						allowClear
						showSearch
						filterOption={(input, option) =>
							option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
						}
					>
						<Option value={null}>전체 파트너</Option>
						{partners.map(partner => (
							<Option key={partner.partnerId} value={partner.partnerId}>
								{partner.partnerName}
							</Option>
						))}
					</Select>
				</div>
				<div style={{ marginBottom: 16 }}>
					<p style={{ marginBottom: 8 }}>
						<strong>정산 상태:</strong>
					</p>
					<Select
						placeholder="상태 선택 (선택사항)"
						style={{ width: '100%' }}
						value={excelDownloadStatus}
						onChange={setExcelDownloadStatus}
						allowClear
					>
						<Option value="PENDING">대기</Option>
						<Option value="COMPLETED">완료</Option>
						<Option value="CANCELLED">취소</Option>
					</Select>
				</div>
				<div style={{ padding: 12, backgroundColor: '#f5f5f5', borderRadius: 4 }}>
					<p style={{ margin: 0, fontSize: '12px', color: '#666' }}>
						<strong>💡 팁:</strong> 현재 목록 화면의 필터 값이 기본값으로 설정되어 있습니다. 필요에 따라 변경하세요.
					</p>
				</div>
			</div>
		</Modal>
		
		{/* 정산 상세 모달 */}
		<Modal
			title="정산 상세 정보"
			open={settlementDetailModalVisible}
			onCancel={() => {
				setSettlementDetailModalVisible(false);
				setSettlementDetail(null);
				setSettlementHistory([]);
				setSettlementHistoryTab('detail');
			}}
			footer={[
				<Button key="close" onClick={() => {
					setSettlementDetailModalVisible(false);
					setSettlementDetail(null);
					setSettlementHistory([]);
					setSettlementHistoryTab('detail');
				}}>
					닫기
				</Button>
			]}
			width={1000}
		>
			<Spin spinning={settlementDetailLoading}>
				{settlementDetail && (
					<Tabs activeKey={settlementHistoryTab} onChange={handleSettlementDetailTabChange}>
						<TabPane tab="기본 정보" key="detail">
							<Descriptions bordered column={2} style={{ marginBottom: 24 }}>
							<Descriptions.Item label="정산 ID">
								{settlementDetail.settlementId}
							</Descriptions.Item>
							<Descriptions.Item label="파트너">
								{settlementDetail.partnerName || '-'} (ID: {settlementDetail.partnerId})
							</Descriptions.Item>
							<Descriptions.Item label="총 판매금액">
								₩{(settlementDetail.totalSalesAmount || 0).toLocaleString()}
							</Descriptions.Item>
							<Descriptions.Item label="총 수수료">
								<span className="text-danger">₩{(settlementDetail.commissionAmount || 0).toLocaleString()}</span>
							</Descriptions.Item>
							<Descriptions.Item label="총 정산금액">
								<span className="font-weight-semibold text-success">
									₩{(settlementDetail.settlementAmount || 0).toLocaleString()}
								</span>
							</Descriptions.Item>
							<Descriptions.Item label="정산 생성일">
								{settlementDetail.settlementCreatedAt ? dayjs(settlementDetail.settlementCreatedAt).format('YYYY-MM-DD') : '-'}
							</Descriptions.Item>
							<Descriptions.Item label="정산 기간">
								{settlementDetail.settlementPeriodStart && settlementDetail.settlementPeriodEnd ? (
									<Tooltip title="이 정산에 포함된 주문들이 실제로 발생한 기간입니다 (주문일 기준).">
										<span style={{ cursor: 'help' }}>
											{dayjs(settlementDetail.settlementPeriodStart).format('YYYY-MM-DD')} ~ {dayjs(settlementDetail.settlementPeriodEnd).format('YYYY-MM-DD')}
										</span>
									</Tooltip>
								) : (
									<span style={{ color: '#999' }}>미설정</span>
								)}
							</Descriptions.Item>
							<Descriptions.Item label="정산 지급일">
								{settlementDetail.settlementPaidDate ? dayjs(settlementDetail.settlementPaidDate).format('YYYY-MM-DD') : (
									<span style={{ color: '#ff4d4f' }}>미입력</span>
								)}
							</Descriptions.Item>
							<Descriptions.Item label="상태">
								<Tag color={
									settlementDetail.settlementStatus === 'COMPLETED' ? 'success' :
									settlementDetail.settlementStatus === 'CANCELLED' ? 'error' : 'default'
								}>
									{settlementDetail.settlementStatus === 'COMPLETED' ? '완료' :
									 settlementDetail.settlementStatus === 'CANCELLED' ? '취소' : '대기'}
								</Tag>
							</Descriptions.Item>
							<Descriptions.Item label="포함된 주문 아이템 수">
								{settlementDetail.itemCount || 0}개
							</Descriptions.Item>
						</Descriptions>
						
						{settlementDetail.items && settlementDetail.items.length > 0 && (
							<div>
								<h4 style={{ marginBottom: 16 }}>포함된 주문 아이템 목록</h4>
								<Table
									columns={[
										{
											title: '주문 ID',
											dataIndex: 'orderId',
											key: 'orderId',
											width: 100,
										},
										{
											title: '주문 아이템 ID',
											dataIndex: 'orderItemId',
											key: 'orderItemId',
											width: 120,
										},
										{
											title: '상품명',
											dataIndex: 'productName',
											key: 'productName',
											ellipsis: true,
										},
										{
											title: '수량',
											dataIndex: 'quantity',
											key: 'quantity',
											width: 80,
											align: 'right',
											render: (qty) => `${qty}개`
										},
										{
											title: '판매금액',
											dataIndex: 'salesAmount',
											key: 'salesAmount',
											width: 120,
											align: 'right',
											render: (amount) => `₩${(amount || 0).toLocaleString()}`
										},
										{
											title: '수수료',
											dataIndex: 'commissionAmount',
											key: 'commissionAmount',
											width: 120,
											align: 'right',
											render: (amount) => (
												<span className="text-danger">₩{(amount || 0).toLocaleString()}</span>
											)
										},
										{
											title: '정산금액',
											dataIndex: 'settlementAmount',
											key: 'settlementAmount',
											width: 120,
											align: 'right',
											render: (amount) => (
												<span className="font-weight-semibold text-success">
													₩{(amount || 0).toLocaleString()}
												</span>
											)
										},
										{
											title: '주문일',
											dataIndex: 'orderDate',
											key: 'orderDate',
											width: 150,
											render: (date) => date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
										},
										{
											title: '배송완료일',
											dataIndex: 'deliveryCompletedDate',
											key: 'deliveryCompletedDate',
											width: 150,
											render: (date) => date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
										}
									]}
									dataSource={settlementDetail.items}
									rowKey="orderItemId"
									pagination={{
										pageSize: 10,
										showSizeChanger: true,
										pageSizeOptions: ['10', '20', '50'],
										showTotal: (total, range) => `${range[0]}-${range[1]} / 총 ${total}개`
									}}
									scroll={{ x: 'max-content' }}
									size="small"
								/>
							</div>
						)}
						</TabPane>
						
						<TabPane tab={<span><HistoryOutlined /> 변경 이력</span>} key="history">
							<Spin spinning={settlementHistoryLoading}>
								<Table
									columns={[
										{
											title: '변경 일시',
											dataIndex: 'changedAt',
											key: 'changedAt',
											width: 180,
											sorter: (a, b) => dayjs(a.changedAt).valueOf() - dayjs(b.changedAt).valueOf(),
											render: (date) => date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'
										},
										{
											title: '변경 타입',
											dataIndex: 'actionType',
											key: 'actionType',
											width: 120,
											render: (type) => {
												const typeMap = {
													'CREATE': { color: 'blue', text: '정산 생성' },
													'STATUS_CHANGE': { color: 'orange', text: '상태 변경' },
													'PAID_DATE_UPDATE': { color: 'green', text: '지급일 수정' }
												};
												const typeInfo = typeMap[type] || { color: 'default', text: type };
												return <Tag color={typeInfo.color}>{typeInfo.text}</Tag>;
											}
										},
										{
											title: '변경한 관리자',
											dataIndex: 'adminName',
											key: 'adminName',
											width: 150,
											render: (name, record) => `${name || '-'} (ID: ${record.adminId})`
										},
										{
											title: '변경 전',
											dataIndex: 'oldValue',
											key: 'oldValue',
											width: 200,
											render: (value) => {
												if (!value) return <span style={{ color: '#999' }}>-</span>;
												try {
													const parsed = JSON.parse(value);
													return (
														<div style={{ fontSize: '12px' }}>
															{Object.entries(parsed).map(([key, val]) => (
																<div key={key}>
																	<strong>{key}:</strong> {val}
																</div>
															))}
														</div>
													);
												} catch {
													return <span style={{ fontSize: '12px' }}>{value}</span>;
												}
											}
										},
										{
											title: '변경 후',
											dataIndex: 'newValue',
											key: 'newValue',
											width: 200,
											render: (value) => {
												if (!value) return <span style={{ color: '#999' }}>-</span>;
												try {
													const parsed = JSON.parse(value);
													return (
														<div style={{ fontSize: '12px' }}>
															{Object.entries(parsed).map(([key, val]) => (
																<div key={key}>
																	<strong>{key}:</strong> {val}
																</div>
															))}
														</div>
													);
												} catch {
													return <span style={{ fontSize: '12px' }}>{value}</span>;
												}
											}
										},
										{
											title: '변경 사유',
											dataIndex: 'reason',
											key: 'reason',
											render: (reason) => reason || <span style={{ color: '#999' }}>-</span>
										}
									]}
									dataSource={settlementHistory}
									rowKey="historyId"
									pagination={{
										pageSize: 10,
										showSizeChanger: true,
										pageSizeOptions: ['10', '20', '50'],
										showTotal: (total, range) => `${range[0]}-${range[1]} / 총 ${total}개`
									}}
									locale={{
										emptyText: '변경 이력이 없습니다.'
									}}
								/>
							</Spin>
						</TabPane>
					</Tabs>
				)}
			</Spin>
		</Modal>
		</>
	);
};

export default AdminSettlement;
