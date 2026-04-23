import React, { useEffect, useState, useCallback } from 'react';
import { Card, Table, Button, Badge, Tag, Spin, Alert, message, Select, DatePicker, Row, Col, Statistic, Tabs, Modal, Descriptions, Tooltip } from 'antd';
import { ReloadOutlined, DollarOutlined, FilePdfOutlined, FileExcelOutlined } from '@ant-design/icons';
import Flex from 'components/shared-components/Flex';
import PartnerService from 'services/PartnerService';
import dayjs from 'dayjs';

const { Option } = Select;
const { RangePicker } = DatePicker;
const { TabPane } = Tabs;

const PartnerSettlement = () => {
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState(null);
	const [settlementData, setSettlementData] = useState(null);
	const [dateRange, setDateRange] = useState(null);
	const [statusFilter, setStatusFilter] = useState('ALL');
	const [activeTab, setActiveTab] = useState('items');
	
	// 생성된 정산 목록 관련 상태
	const [settlementListLoading, setSettlementListLoading] = useState(false);
	const [settlementList, setSettlementList] = useState(null);
	const [settlementListStatus, setSettlementListStatus] = useState(null);
	
	// 정산 대상 상세 모달 관련 상태
	const [detailModalVisible, setDetailModalVisible] = useState(false);
	const [selectedItem, setSelectedItem] = useState(null);
	
	// 정산 상세 모달 관련 상태
	const [settlementDetailModalVisible, setSettlementDetailModalVisible] = useState(false);
	const [settlementDetail, setSettlementDetail] = useState(null);
	const [settlementDetailLoading, setSettlementDetailLoading] = useState(false);

	// 정산 목록 조회
	const fetchSettlementItems = useCallback(async () => {
		try {
			setLoading(true);
			setError(null);

			const params = {};
			if (dateRange && dateRange.length === 2) {
				params.startDate = dateRange[0].format('YYYY-MM-DD');
				params.endDate = dateRange[1].format('YYYY-MM-DD');
			}
			if (statusFilter !== 'ALL') {
				params.status = statusFilter;
			}

			const response = await PartnerService.getSettlementItems(params);
			// API 응답 구조: { success: true, data: { items: [], summary: {} } }
			const data = response?.data || response;
			setSettlementData(data);
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '정산 목록을 불러오는데 실패했습니다.';
			setError(errorMessage);
			message.error(errorMessage);
		} finally {
			setLoading(false);
		}
	}, [dateRange, statusFilter]);

	// 생성된 정산 목록 조회
	const fetchSettlementList = useCallback(async () => {
		try {
			setSettlementListLoading(true);
			const params = {};
			if (settlementListStatus) {
				params.status = settlementListStatus;
			}
			const response = await PartnerService.getSettlementList(params);
			const data = response?.data || response;
			setSettlementList(data);
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '정산 목록을 불러오는데 실패했습니다.';
			message.error(errorMessage);
		} finally {
			setSettlementListLoading(false);
		}
	}, [settlementListStatus]);
	
	// 컴포넌트 마운트 시 및 필터 변경 시 정산 목록 조회
	useEffect(() => {
		if (activeTab === 'items') {
			fetchSettlementItems();
		} else if (activeTab === 'list') {
			fetchSettlementList();
		}
	}, [fetchSettlementItems, fetchSettlementList, activeTab]);
	
	// 탭 변경 핸들러
	const handleTabChange = (key) => {
		setActiveTab(key);
	};

	// 기간 필터 변경
	const handleDateRangeChange = (dates) => {
		setDateRange(dates);
	};

	// 상태 필터 변경
	const handleStatusFilterChange = (value) => {
		setStatusFilter(value);
	};

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

	// 상세 모달 열기
	const handleShowDetail = (record) => {
		setSelectedItem(record);
		setDetailModalVisible(true);
	};
	
	// 테이블 컬럼 정의 (간소화된 목록)
	const tableColumns = [
		{
			title: '주문번호',
			dataIndex: 'orderId',
			key: 'orderId',
			width: 100,
			sorter: (a, b) => a.orderId - b.orderId,
		},
		{
			title: '상품명',
			dataIndex: 'productName',
			key: 'productName',
			ellipsis: true,
			render: (text) => <span className="font-weight-semibold">{text}</span>
		},
		{
			title: '주문일',
			dataIndex: 'orderDate',
			key: 'orderDate',
			width: 150,
			sorter: (a, b) => {
				if (!a.orderDate && !b.orderDate) return 0;
				if (!a.orderDate) return 1;
				if (!b.orderDate) return -1;
				return dayjs(a.orderDate).valueOf() - dayjs(b.orderDate).valueOf();
			},
			render: (date) => date ? dayjs(date).format('YYYY-MM-DD') : '-'
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

	// 정산 상세 조회
	const handleShowSettlementDetail = async (settlementId) => {
		try {
			setSettlementDetailLoading(true);
			const response = await PartnerService.getSettlementDetail(settlementId);
			const data = response?.data || response;
			setSettlementDetail(data);
			setSettlementDetailModalVisible(true);
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '정산 상세 정보를 불러오는데 실패했습니다.';
			message.error(errorMessage);
		} finally {
			setSettlementDetailLoading(false);
		}
	};
	
	// PDF 다운로드
	const handleDownloadPdf = async (settlementId) => {
		try {
			await PartnerService.downloadSettlementPdf(settlementId);
			message.success('PDF 파일이 다운로드되었습니다.');
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || 'PDF 다운로드에 실패했습니다.';
			message.error(errorMessage);
		}
	};
	
	// Excel 다운로드
	const handleDownloadExcel = async () => {
		try {
			const params = {};
			if (settlementListStatus) {
				params.status = settlementListStatus;
			}
			await PartnerService.downloadSettlementExcel(params);
			message.success('엑셀 파일이 다운로드되었습니다.');
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '엑셀 다운로드에 실패했습니다.';
			message.error(errorMessage);
		}
	};
	
	// 생성된 정산 목록 테이블 컬럼 정의
	const settlementListColumns = [
		{
			title: '정산 ID',
			dataIndex: 'settlementId',
			key: 'settlementId',
			width: 100,
			sorter: (a, b) => a.settlementId - b.settlementId,
		},
		{
			title: '정산 기간',
			key: 'settlementPeriod',
			width: 200,
			render: (_, record) => {
				if (record.settlementPeriodStart && record.settlementPeriodEnd) {
					return `${dayjs(record.settlementPeriodStart).format('YYYY-MM-DD')} ~ ${dayjs(record.settlementPeriodEnd).format('YYYY-MM-DD')}`;
				}
				return '-';
			},
		},
		{
			title: '생성일',
			dataIndex: 'settlementCreatedAt',
			key: 'settlementCreatedAt',
			width: 120,
			sorter: (a, b) => dayjs(a.settlementCreatedAt).valueOf() - dayjs(b.settlementCreatedAt).valueOf(),
			render: (date) => date ? dayjs(date).format('YYYY-MM-DD') : '-'
		},
		{
			title: '지급일',
			dataIndex: 'settlementPaidDate',
			key: 'settlementPaidDate',
			width: 120,
			render: (date) => date ? dayjs(date).format('YYYY-MM-DD') : '-'
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
				const statusMap = {
					'PENDING': { color: 'orange', text: '대기' },
					'COMPLETED': { color: 'green', text: '완료' },
					'CANCELLED': { color: 'red', text: '취소' }
				};
				const statusInfo = statusMap[status] || { color: 'default', text: status };
				return <Tag color={statusInfo.color}>{statusInfo.text}</Tag>;
			}
		},
		{
			title: '총 판매금액',
			dataIndex: 'totalSalesAmount',
			key: 'totalSalesAmount',
			width: 120,
			align: 'right',
			sorter: (a, b) => a.totalSalesAmount - b.totalSalesAmount,
			render: (amount) => `₩${(amount || 0).toLocaleString()}`
		},
		{
			title: '총 수수료',
			dataIndex: 'commissionAmount',
			key: 'commissionAmount',
			width: 120,
			align: 'right',
			sorter: (a, b) => a.commissionAmount - b.commissionAmount,
			render: (amount) => (
				<span className="text-danger">₩{(amount || 0).toLocaleString()}</span>
			)
		},
		{
			title: '총 정산금액',
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
			title: '주문 내역',
			key: 'itemCount',
			width: 120,
			render: (_, record) => (
				<Button 
					type="link" 
					size="small"
					onClick={() => handleShowSettlementDetail(record.settlementId)}
				>
					{record.itemCount || 0}개 상세보기
				</Button>
			)
		},
		{
			title: '정산서',
			key: 'pdf',
			width: 100,
			render: (_, record) => (
				<Button
					type="link"
					icon={<FilePdfOutlined />}
					onClick={() => handleDownloadPdf(record.settlementId)}
					title="PDF 다운로드"
				>
					PDF
				</Button>
			)
		}
	];

	return (
		<>
			<Tabs activeKey={activeTab} onChange={handleTabChange}>
				<TabPane tab="정산 대상 조회" key="items">
					<Card>
				<Flex alignItems="center" justifyContent="between" mobileFlex={false}>
					<Flex className="mb-1" mobileFlex={false}>
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
								onChange={handleDateRangeChange}
								style={{ width: 250 }}
							/>
						</div>
						<div className="mr-md-3 mb-3">
							<Select
								placeholder="상태 필터"
								style={{ width: 150 }}
								value={statusFilter}
								onChange={handleStatusFilterChange}
							>
								<Option value="ALL">전체</Option>
								<Option value="SETTLEMENT_READY">정산 가능만</Option>
							</Select>
						</div>
					</Flex>
					<div>
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
							dateRange || statusFilter !== 'ALL' 
								? "선택한 조건에 맞는 정산 대상 주문이 없습니다. 다른 기간이나 필터를 선택해보세요."
								: "아직 정산 대상이 되는 주문이 없습니다. 배송 완료 후 구매 확정된 주문이 정산 대상이 됩니다."
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
				
				<TabPane tab="생성된 정산 목록" key="list">
					<Card>
						<Flex alignItems="center" justifyContent="between" mobileFlex={false}>
							<Flex className="mb-1" mobileFlex={false}>
								<div className="mr-md-3 mb-3">
									<Select
										placeholder="상태 필터"
										style={{ width: 150 }}
										value={settlementListStatus}
										onChange={(value) => {
											setSettlementListStatus(value);
										}}
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
									onClick={handleDownloadExcel}
									className="mr-2"
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
			
			{/* 정산 대상 상세 모달 */}
			<Modal
				title="정산 대상 상세 정보"
				open={detailModalVisible}
				onCancel={() => {
					setDetailModalVisible(false);
					setSelectedItem(null);
				}}
				footer={[
					<Button key="close" onClick={() => {
						setDetailModalVisible(false);
						setSelectedItem(null);
					}}>
						닫기
					</Button>
				]}
				width={800}
			>
				{selectedItem && (
					<Descriptions bordered column={2}>
						<Descriptions.Item label="주문번호">{selectedItem.orderId}</Descriptions.Item>
						<Descriptions.Item label="주문아이템번호">{selectedItem.orderItemId}</Descriptions.Item>
						<Descriptions.Item label="상품명" span={2}>
							<span className="font-weight-semibold">{selectedItem.productName}</span>
						</Descriptions.Item>
						<Descriptions.Item label="수량">{selectedItem.quantity}개</Descriptions.Item>
						<Descriptions.Item label="정산 가능">
							<Badge
								status={selectedItem.isSettlementReady ? 'success' : 'default'}
								text={selectedItem.isSettlementReady ? '가능' : '불가'}
							/>
						</Descriptions.Item>
						<Descriptions.Item label="판매금액">
							₩{(selectedItem.salesAmount || 0).toLocaleString()}
						</Descriptions.Item>
						<Descriptions.Item label="수수료">
							<span className="text-danger">₩{(selectedItem.commissionAmount || 0).toLocaleString()}</span>
						</Descriptions.Item>
						<Descriptions.Item label="정산금액" span={2}>
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
					</Descriptions>
				)}
			</Modal>
			
			{/* 정산 상세 모달 */}
			<Modal
				title="정산 상세 정보"
				open={settlementDetailModalVisible}
				onCancel={() => {
					setSettlementDetailModalVisible(false);
					setSettlementDetail(null);
				}}
				footer={[
					<Button key="close" onClick={() => {
						setSettlementDetailModalVisible(false);
						setSettlementDetail(null);
					}}>
						닫기
					</Button>
				]}
				width={800}
			>
				<Spin spinning={settlementDetailLoading}>
					{settlementDetail && (
						<>
							<Descriptions bordered column={2}>
								<Descriptions.Item label="정산 ID">{settlementDetail.settlementId}</Descriptions.Item>
								<Descriptions.Item label="상태">
									<Tag color={settlementDetail.settlementStatus === 'COMPLETED' ? 'green' : settlementDetail.settlementStatus === 'PENDING' ? 'orange' : 'red'}>
										{settlementDetail.settlementStatus === 'COMPLETED' ? '완료' : settlementDetail.settlementStatus === 'PENDING' ? '대기' : '취소'}
									</Tag>
								</Descriptions.Item>
								<Descriptions.Item label="정산 기간">
									{settlementDetail.settlementPeriodStart && settlementDetail.settlementPeriodEnd
										? `${dayjs(settlementDetail.settlementPeriodStart).format('YYYY-MM-DD')} ~ ${dayjs(settlementDetail.settlementPeriodEnd).format('YYYY-MM-DD')}`
										: '-'}
								</Descriptions.Item>
								<Descriptions.Item label="생성일">
									{dayjs(settlementDetail.settlementCreatedAt).format('YYYY-MM-DD')}
								</Descriptions.Item>
								<Descriptions.Item label="지급일">
									{settlementDetail.settlementPaidDate ? dayjs(settlementDetail.settlementPaidDate).format('YYYY-MM-DD') : '-'}
								</Descriptions.Item>
								<Descriptions.Item label="포함된 주문 아이템 수">
									{settlementDetail.itemCount || 0}개
								</Descriptions.Item>
								<Descriptions.Item label="총 판매금액">
									₩{(settlementDetail.totalSalesAmount || 0).toLocaleString()}
								</Descriptions.Item>
								<Descriptions.Item label="총 수수료">
									₩{(settlementDetail.commissionAmount || 0).toLocaleString()}
								</Descriptions.Item>
								<Descriptions.Item label="총 정산금액" span={2}>
									<span className="font-weight-semibold text-success">
										₩{(settlementDetail.settlementAmount || 0).toLocaleString()}
									</span>
								</Descriptions.Item>
							</Descriptions>
							
							{settlementDetail.items && settlementDetail.items.length > 0 && (
								<div style={{ marginTop: 24 }}>
									<h4>포함된 주문 아이템 목록</h4>
									<Table
										columns={[
											{ title: '주문 ID', dataIndex: 'orderId', key: 'orderId', width: 100 },
											{ title: '상품명', dataIndex: 'productName', key: 'productName' },
											{ title: '항목명', dataIndex: 'optionInfo', key: 'optionInfo', render: (text) => text || '-' },
											{ title: '수량', dataIndex: 'quantity', key: 'quantity', width: 80, align: 'right' },
											{ title: '판매금액', dataIndex: 'salesAmount', key: 'salesAmount', width: 120, align: 'right', render: (amount) => `₩${(amount || 0).toLocaleString()}` },
											{ title: '수수료', dataIndex: 'commissionAmount', key: 'commissionAmount', width: 120, align: 'right', render: (amount) => `₩${(amount || 0).toLocaleString()}` },
											{ title: '정산금액', dataIndex: 'settlementAmount', key: 'settlementAmount', width: 120, align: 'right', render: (amount) => `₩${(amount || 0).toLocaleString()}` }
										]}
										dataSource={settlementDetail.items}
										rowKey={(record, index) => `${record.orderId}-${index}`}
										pagination={false}
										size="small"
									/>
								</div>
							)}
						</>
					)}
				</Spin>
			</Modal>
		</>
	);
};

export default PartnerSettlement;
