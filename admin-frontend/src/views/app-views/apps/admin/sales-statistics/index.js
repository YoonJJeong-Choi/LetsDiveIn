import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Statistic, DatePicker, Space, Button, Table, Tag, Spin, message, Select } from 'antd';
import { DollarOutlined, ShoppingOutlined, ShoppingCartOutlined, ReloadOutlined, BarChartOutlined } from '@ant-design/icons';
import AdminService from 'services/AdminService';
import dayjs from 'dayjs';

const { RangePicker } = DatePicker;

const SalesStatistics = () => {
	const [loading, setLoading] = useState(false);
	const [statistics, setStatistics] = useState(null);
	const [dateRange, setDateRange] = useState([dayjs().subtract(7, 'day'), dayjs()]);
	const [dailySalesPageSize, setDailySalesPageSize] = useState(10);

	useEffect(() => {
		fetchStatistics();
	}, []);

	const fetchStatistics = async () => {
		try {
			setLoading(true);
			const startDate = dateRange[0] ? dateRange[0].format('YYYY-MM-DD') : null;
			const endDate = dateRange[1] ? dateRange[1].format('YYYY-MM-DD') : null;
			
			const response = await AdminService.getSalesStatistics(startDate, endDate);
			const data = response.data || response;
			setStatistics(data);
		} catch (err) {
			console.error('매출 통계 조회 실패:', err);
			message.error(err.response?.data?.message || '매출 통계를 불러오는데 실패했습니다.');
		} finally {
			setLoading(false);
		}
	};

	const handleDateRangeChange = (dates) => {
		setDateRange(dates);
		// 날짜 범위가 변경되면 자동으로 조회하지 않음 (조회 버튼 클릭 필요)
	};

	const handleRefresh = () => {
		fetchStatistics();
	};

	// 날짜 프리셋 핸들러
	const handlePresetClick = (preset) => {
		const today = dayjs();
		let startDate, endDate;
		
		switch (preset) {
			case 'today':
				startDate = today;
				endDate = today;
				break;
			case '7days':
				startDate = today.subtract(6, 'day');
				endDate = today;
				break;
			case '30days':
				startDate = today.subtract(29, 'day');
				endDate = today;
				break;
			case 'thisMonth':
				startDate = today.startOf('month');
				endDate = today;
				break;
			case 'lastMonth':
				startDate = today.subtract(1, 'month').startOf('month');
				endDate = today.subtract(1, 'month').endOf('month');
				break;
			default:
				return;
		}
		
		setDateRange([startDate, endDate]);
	};

	const formatCurrency = (amount) => {
		return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(amount || 0);
	};

	const formatNumber = (num) => {
		return new Intl.NumberFormat('ko-KR').format(num || 0);
	};

	// 일별 매출 테이블 컬럼
	const dailySalesColumns = [
		{
			title: '날짜',
			dataIndex: 'date',
			key: 'date',
			render: (date) => dayjs(date).format('YYYY-MM-DD (ddd)'),
			sorter: (a, b) => dayjs(a.date).unix() - dayjs(b.date).unix(),
		},
		{
			title: '매출액',
			dataIndex: 'totalSales',
			key: 'totalSales',
			align: 'right',
			render: (amount) => formatCurrency(amount),
			sorter: (a, b) => a.totalSales - b.totalSales,
		},
		{
			title: '주문 수',
			dataIndex: 'totalOrders',
			key: 'totalOrders',
			align: 'right',
			render: (count) => formatNumber(count),
			sorter: (a, b) => a.totalOrders - b.totalOrders,
		},
		{
			title: '판매 수량',
			dataIndex: 'totalQuantity',
			key: 'totalQuantity',
			align: 'right',
			render: (quantity) => formatNumber(quantity),
			sorter: (a, b) => a.totalQuantity - b.totalQuantity,
		},
		{
			title: '평균 주문 금액',
			dataIndex: 'averageOrderAmount',
			key: 'averageOrderAmount',
			align: 'right',
			render: (amount) => formatCurrency(amount),
			sorter: (a, b) => a.averageOrderAmount - b.averageOrderAmount,
		},
	];

	// 주별 매출 테이블 컬럼
	const weeklySalesColumns = [
		{
			title: '주 (월요일)',
			dataIndex: 'date',
			key: 'date',
			render: (date) => `${dayjs(date).format('YYYY-MM-DD')} ~ ${dayjs(date).add(6, 'day').format('YYYY-MM-DD')}`,
			sorter: (a, b) => dayjs(a.date).unix() - dayjs(b.date).unix(),
		},
		{
			title: '매출액',
			dataIndex: 'totalSales',
			key: 'totalSales',
			align: 'right',
			render: (amount) => formatCurrency(amount),
			sorter: (a, b) => a.totalSales - b.totalSales,
		},
		{
			title: '주문 수',
			dataIndex: 'totalOrders',
			key: 'totalOrders',
			align: 'right',
			render: (count) => formatNumber(count),
			sorter: (a, b) => a.totalOrders - b.totalOrders,
		},
		{
			title: '판매 수량',
			dataIndex: 'totalQuantity',
			key: 'totalQuantity',
			align: 'right',
			render: (quantity) => formatNumber(quantity),
			sorter: (a, b) => a.totalQuantity - b.totalQuantity,
		},
		{
			title: '평균 주문 금액',
			dataIndex: 'averageOrderAmount',
			key: 'averageOrderAmount',
			align: 'right',
			render: (amount) => formatCurrency(amount),
			sorter: (a, b) => a.averageOrderAmount - b.averageOrderAmount,
		},
	];

	// 월별 매출 테이블 컬럼
	const monthlySalesColumns = [
		{
			title: '월',
			dataIndex: 'date',
			key: 'date',
			render: (date) => dayjs(date).format('YYYY년 MM월'),
			sorter: (a, b) => dayjs(a.date).unix() - dayjs(b.date).unix(),
		},
		{
			title: '매출액',
			dataIndex: 'totalSales',
			key: 'totalSales',
			align: 'right',
			render: (amount) => formatCurrency(amount),
			sorter: (a, b) => a.totalSales - b.totalSales,
		},
		{
			title: '주문 수',
			dataIndex: 'totalOrders',
			key: 'totalOrders',
			align: 'right',
			render: (count) => formatNumber(count),
			sorter: (a, b) => a.totalOrders - b.totalOrders,
		},
		{
			title: '판매 수량',
			dataIndex: 'totalQuantity',
			key: 'totalQuantity',
			align: 'right',
			render: (quantity) => formatNumber(quantity),
			sorter: (a, b) => a.totalQuantity - b.totalQuantity,
		},
		{
			title: '평균 주문 금액',
			dataIndex: 'averageOrderAmount',
			key: 'averageOrderAmount',
			align: 'right',
			render: (amount) => formatCurrency(amount),
			sorter: (a, b) => a.averageOrderAmount - b.averageOrderAmount,
		},
	];

	// 상품별 매출 테이블 컬럼
	const topProductsColumns = [
		{
			title: '순위',
			key: 'rank',
			width: 80,
			render: (_, __, index) => index + 1,
		},
		{
			title: '상품명',
			dataIndex: 'productName',
			key: 'productName',
		},
		{
			title: '매출액',
			dataIndex: 'totalSales',
			key: 'totalSales',
			align: 'right',
			render: (amount) => formatCurrency(amount),
			sorter: (a, b) => a.totalSales - b.totalSales,
		},
		{
			title: '판매 수량',
			dataIndex: 'totalQuantity',
			key: 'totalQuantity',
			align: 'right',
			render: (quantity) => formatNumber(quantity),
			sorter: (a, b) => a.totalQuantity - b.totalQuantity,
		},
		{
			title: '주문 건수',
			dataIndex: 'orderCount',
			key: 'orderCount',
			align: 'right',
			render: (count) => formatNumber(count),
			sorter: (a, b) => a.orderCount - b.orderCount,
		},
	];

	// 고객별 매출 테이블 컬럼
	const topCustomersColumns = [
		{
			title: '순위',
			key: 'rank',
			width: 80,
			render: (_, __, index) => index + 1,
		},
		{
			title: '고객명',
			dataIndex: 'customerName',
			key: 'customerName',
		},
		{
			title: '이메일',
			dataIndex: 'customerEmail',
			key: 'customerEmail',
		},
		{
			title: '총 구매 금액',
			dataIndex: 'totalSales',
			key: 'totalSales',
			align: 'right',
			render: (amount) => formatCurrency(amount),
			sorter: (a, b) => a.totalSales - b.totalSales,
		},
		{
			title: '주문 건수',
			dataIndex: 'orderCount',
			key: 'orderCount',
			align: 'right',
			render: (count) => formatNumber(count),
			sorter: (a, b) => a.orderCount - b.orderCount,
		},
		{
			title: '평균 주문 금액',
			dataIndex: 'averageOrderAmount',
			key: 'averageOrderAmount',
			align: 'right',
			render: (amount) => formatCurrency(amount),
			sorter: (a, b) => a.averageOrderAmount - b.averageOrderAmount,
		},
	];

	// 파트너별 매출 테이블 컬럼
	const partnerSalesColumns = [
		{
			title: '순위',
			key: 'rank',
			width: 80,
			render: (_, __, index) => index + 1,
		},
		{
			title: '파트너명',
			dataIndex: 'partnerName',
			key: 'partnerName',
		},
		{
			title: '이메일',
			dataIndex: 'partnerEmail',
			key: 'partnerEmail',
		},
		{
			title: '총 매출액',
			dataIndex: 'totalSales',
			key: 'totalSales',
			align: 'right',
			render: (amount) => formatCurrency(amount),
			sorter: (a, b) => a.totalSales - b.totalSales,
		},
		{
			title: '판매 수량',
			dataIndex: 'totalQuantity',
			key: 'totalQuantity',
			align: 'right',
			render: (quantity) => formatNumber(quantity),
			sorter: (a, b) => a.totalQuantity - b.totalQuantity,
		},
		{
			title: '주문 건수',
			dataIndex: 'orderCount',
			key: 'orderCount',
			align: 'right',
			render: (count) => formatNumber(count),
			sorter: (a, b) => a.orderCount - b.orderCount,
		},
		{
			title: '평균 주문 금액',
			dataIndex: 'averageOrderAmount',
			key: 'averageOrderAmount',
			align: 'right',
			render: (amount) => formatCurrency(amount),
			sorter: (a, b) => a.averageOrderAmount - b.averageOrderAmount,
		},
	];

	return (
		<div>
			<Card>
				<Space direction="vertical" style={{ width: '100%' }} size="large">
					{/* 필터 영역 */}
					<Row justify="space-between" align="middle">
						<Col>
							<Space direction="vertical" size="small" style={{ width: '100%' }}>
								<Space wrap>
									<Button size="small" onClick={() => handlePresetClick('today')}>오늘</Button>
									<Button size="small" onClick={() => handlePresetClick('7days')}>최근 7일</Button>
									<Button size="small" onClick={() => handlePresetClick('30days')}>최근 30일</Button>
									<Button size="small" onClick={() => handlePresetClick('thisMonth')}>이번 달</Button>
									<Button size="small" onClick={() => handlePresetClick('lastMonth')}>지난 달</Button>
								</Space>
								<Space>
									<RangePicker
										value={dateRange}
										onChange={handleDateRangeChange}
										format="YYYY-MM-DD"
										style={{ width: 300 }}
									/>
									<Button type="primary" onClick={handleRefresh} loading={loading}>
										조회
									</Button>
									<Button icon={<ReloadOutlined />} onClick={handleRefresh} loading={loading}>
										새로고침
									</Button>
								</Space>
							</Space>
						</Col>
					</Row>

					{/* 전체 통계 */}
					{loading && !statistics ? (
						<Spin size="large" style={{ display: 'block', textAlign: 'center', padding: '50px' }} />
					) : statistics ? (
						<>
							<Row gutter={16}>
								<Col xs={24} sm={12} lg={6}>
									<Card>
										<Statistic
											title="총 매출액"
											value={statistics.totalSales || 0}
											prefix={<DollarOutlined />}
											formatter={(value) => formatCurrency(value)}
										/>
									</Card>
								</Col>
								<Col xs={24} sm={12} lg={6}>
									<Card>
										<Statistic
											title="총 주문 수"
											value={statistics.totalOrders || 0}
											prefix={<ShoppingOutlined />}
											formatter={(value) => formatNumber(value)}
										/>
									</Card>
								</Col>
								<Col xs={24} sm={12} lg={6}>
									<Card>
										<Statistic
											title="총 판매 수량"
											value={statistics.totalQuantity || 0}
											prefix={<ShoppingCartOutlined />}
											formatter={(value) => formatNumber(value)}
										/>
									</Card>
								</Col>
								<Col xs={24} sm={12} lg={6}>
									<Card>
										<Statistic
											title="평균 주문 금액"
											value={statistics.averageOrderAmount || 0}
											prefix={<BarChartOutlined />}
											formatter={(value) => formatCurrency(value)}
										/>
									</Card>
								</Col>
							</Row>

							{/* 기간 비교 (전 기간 대비) */}
							{statistics.periodComparison && (
								<Card title="기간 비교 (전 기간 대비)" style={{ marginTop: 16 }}>
									<Row gutter={16}>
										<Col xs={24} sm={12} lg={6}>
											<Statistic
												title="현재 기간 매출"
												value={statistics.periodComparison.currentPeriodSales || 0}
												formatter={(value) => formatCurrency(value)}
											/>
										</Col>
										<Col xs={24} sm={12} lg={6}>
											<Statistic
												title="이전 기간 매출"
												value={statistics.periodComparison.previousPeriodSales || 0}
												formatter={(value) => formatCurrency(value)}
											/>
										</Col>
										<Col xs={24} sm={12} lg={6}>
											<Statistic
												title="매출 변화율 (전 기간 대비)"
												value={statistics.periodComparison.salesChangeRate || 0}
												precision={1}
												suffix="%"
												valueStyle={{
													color: (statistics.periodComparison.salesChangeRate || 0) >= 0 ? '#3f8600' : '#cf1322',
												}}
											/>
										</Col>
										<Col xs={24} sm={12} lg={6}>
											<Statistic
												title="주문 수 변화율 (전 기간 대비)"
												value={statistics.periodComparison.orderChangeRate || 0}
												precision={1}
												suffix="%"
												valueStyle={{
													color: (statistics.periodComparison.orderChangeRate || 0) >= 0 ? '#3f8600' : '#cf1322',
												}}
											/>
										</Col>
									</Row>
								</Card>
							)}

							{/* 일별 매출 */}
							<Card 
								title={
									<span>
										일별 매출
										{dateRange[0] && dateRange[1] && (
											<Tag color="blue" style={{ marginLeft: 8 }}>
												{dayjs(dateRange[1]).diff(dayjs(dateRange[0]), 'day') + 1}일간
											</Tag>
										)}
										{dateRange[0] && dateRange[1] && dayjs(dateRange[1]).diff(dayjs(dateRange[0]), 'day') > 30 && (
											<Tag color="orange" style={{ marginLeft: 8 }}>
												기간이 길어 주별/월별 매출을 권장합니다
											</Tag>
										)}
									</span>
								}
								style={{ marginTop: 16 }}
							>
								<Table
									columns={dailySalesColumns}
									dataSource={statistics.dailySales || []}
									rowKey="date"
									pagination={{
										pageSize: dailySalesPageSize,
										showSizeChanger: true,
										showTotal: (total) => `총 ${total}일`,
										pageSizeOptions: ['10', '20', '30', '50', '100'],
										onShowSizeChange: (current, size) => setDailySalesPageSize(size),
									}}
									size="small"
									loading={loading}
									scroll={{ x: 'max-content' }}
								/>
							</Card>

							{/* 주별 매출 */}
							<Card title="주별 매출" style={{ marginTop: 16 }}>
								<Table
									columns={weeklySalesColumns}
									dataSource={statistics.weeklySales || []}
									rowKey="date"
									pagination={false}
									size="small"
									loading={loading}
								/>
							</Card>

							{/* 월별 매출 */}
							<Card title="월별 매출" style={{ marginTop: 16 }}>
								<Table
									columns={monthlySalesColumns}
									dataSource={statistics.monthlySales || []}
									rowKey="date"
									pagination={false}
									size="small"
									loading={loading}
								/>
							</Card>

							{/* 상품별 매출 TOP 10 */}
							<Card title="상품별 매출 TOP 10" style={{ marginTop: 16 }}>
								<Table
									columns={topProductsColumns}
									dataSource={statistics.topProducts || []}
									rowKey="productNo"
									pagination={false}
									size="small"
									loading={loading}
								/>
							</Card>

							{/* 카테고리별 매출 */}
							{statistics.categorySales && statistics.categorySales.length > 0 && (
								<Card title="카테고리별 매출" style={{ marginTop: 16 }}>
									<Table
										columns={[
											{
												title: '카테고리명',
												dataIndex: 'categoryName',
												key: 'categoryName',
											},
											{
												title: '매출액',
												dataIndex: 'totalSales',
												key: 'totalSales',
												align: 'right',
												render: (amount) => formatCurrency(amount),
												sorter: (a, b) => a.totalSales - b.totalSales,
											},
											{
												title: '판매 수량',
												dataIndex: 'totalQuantity',
												key: 'totalQuantity',
												align: 'right',
												render: (quantity) => formatNumber(quantity),
												sorter: (a, b) => a.totalQuantity - b.totalQuantity,
											},
											{
												title: '주문 건수',
												dataIndex: 'orderCount',
												key: 'orderCount',
												align: 'right',
												render: (count) => formatNumber(count),
												sorter: (a, b) => a.orderCount - b.orderCount,
											},
										]}
										dataSource={statistics.categorySales}
										rowKey="categoryName"
										pagination={false}
										size="small"
										loading={loading}
									/>
								</Card>
							)}

							{/* 고객별 매출 TOP 10 */}
							<Card title="고객별 매출 TOP 10" style={{ marginTop: 16 }}>
								<Table
									columns={topCustomersColumns}
									dataSource={statistics.topCustomers || []}
									rowKey="customerId"
									pagination={false}
									size="small"
									loading={loading}
								/>
							</Card>

							{/* 파트너별 매출 */}
							<Card title="파트너별 매출" style={{ marginTop: 16 }}>
								<Table
									columns={partnerSalesColumns}
									dataSource={statistics.partnerSales || []}
									rowKey="partnerId"
									pagination={false}
									size="small"
									loading={loading}
								/>
							</Card>
						</>
					) : null}
				</Space>
			</Card>
		</div>
	);
};

export default SalesStatistics;
