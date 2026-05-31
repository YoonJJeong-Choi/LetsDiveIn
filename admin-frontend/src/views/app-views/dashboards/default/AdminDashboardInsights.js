import React, { useEffect, useMemo, useState } from 'react';
import { useSelector } from 'react-redux';
import { Link } from 'react-router-dom';
import {
	Row,
	Col,
	Card,
	Spin,
	message,
	Segmented,
	Table,
	Statistic,
	Tag,
} from 'antd';
import ApexChart from 'react-apexcharts';
import AdminService from 'services/AdminService';
import { APP_PREFIX_PATH } from 'configs/AppConfig';
import { COLORS, COLOR_TEXT } from 'constants/ChartConstant';

const formatWon = (n) => {
	const v = Number(n);
	if (Number.isNaN(v)) return '-';
	return `${v.toLocaleString('ko-KR')}원`;
};

const formatDateTime = (iso) => {
	if (!iso) return '-';
	const d = new Date(iso);
	if (Number.isNaN(d.getTime())) return String(iso);
	return d.toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
};

const orderStatusKo = (s) => {
	switch (s) {
		case 'PENDING_PAYMENT':
			return '결제 대기';
		case 'PAID':
			return '결제 완료';
		case 'ACTIVE':
			return '진행 중';
		case 'PAYMENT_FAILED':
			return '결제 실패';
		case 'CANCELLED':
			return '취소';
		default:
			return s || '-';
	}
};

const returnStatusKo = (s) => {
	switch (s) {
		case 'REQUESTED':
			return '신청';
		case 'APPROVED':
			return '승인';
		case 'REJECTED':
			return '거절';
		case 'PICKUP_COMPLETED':
			return '수거 완료';
		case 'REFUNDED':
			return '환불 완료';
		default:
			return s || '-';
	}
};

const pieOptionsBase = {
	chart: { type: 'donut', toolbar: { show: false } },
	labels: [],
	legend: { position: 'bottom', fontSize: '12px' },
	dataLabels: { enabled: true, formatter: (val) => `${Number(val).toFixed(0)}%` },
	plotOptions: {
		pie: {
			donut: {
				labels: {
					show: true,
					total: {
						show: true,
						label: '합계',
						formatter: () => '',
					},
				},
			},
		},
	},
	stroke: { show: false },
	colors: COLORS,
};

const AdminDashboardInsights = () => {
	const role = useSelector((state) => state.auth.user?.role);
	const [trendDays, setTrendDays] = useState(30);
	const [data, setData] = useState(null);
	const [loading, setLoading] = useState(false);

	useEffect(() => {
		if (role !== 'ADMIN') {
			return undefined;
		}
		let cancelled = false;
		setLoading(true);
		AdminService.getDashboardInsights({ days: trendDays })
			.then((res) => {
				const dto = res?.data ?? res;
				if (!cancelled) setData(dto);
			})
			.catch(() => {
				if (!cancelled) message.error('추이·운영 요약을 불러오지 못했습니다.');
			})
			.finally(() => {
				if (!cancelled) setLoading(false);
			});
		return () => {
			cancelled = true;
		};
	}, [role, trendDays]);

	const lineChart = useMemo(() => {
		const trend = data?.dailyPaidTrend || [];
		const categories = trend.map((p) => String(p.date || '').slice(5));
		const counts = trend.map((p) => Number(p.paidOrderCount) || 0);
		const rev = trend.map((p) => Number(p.paidRevenueKrw) || 0);
		return {
			series: [
				{ name: '결제 완료 주문(건)', type: 'column', data: counts, yAxisIndex: 0 },
				{ name: '매출(원)', type: 'line', data: rev, yAxisIndex: 1 },
			],
			options: {
				chart: {
					type: 'line',
					height: 320,
					toolbar: { show: false },
					zoom: { enabled: false },
					fontFamily: 'inherit',
					stacked: false,
				},
				colors: [COLORS[0], COLORS[1]],
				stroke: { width: [0, 3], curve: 'smooth' },
				dataLabels: { enabled: false },
				xaxis: {
					categories,
					labels: { style: { colors: COLOR_TEXT } },
				},
				yaxis: [
					{
						title: { text: '주문(건)' },
						labels: { style: { colors: COLOR_TEXT } },
					},
					{
						opposite: true,
						title: { text: '매출(원)' },
						labels: {
							style: { colors: COLOR_TEXT },
							formatter: (v) => `${Math.round(Number(v) / 1000)}k`,
						},
					},
				],
				tooltip: {
					shared: true,
					intersect: false,
					y: [
						{ formatter: (val) => `${val}건` },
						{ formatter: (val) => formatWon(val) },
					],
				},
				legend: { position: 'top' },
			},
		};
	}, [data]);

	const categoryPie = useMemo(() => {
		const slices = data?.categoryRevenueShare || [];
		const series = slices.map((s) => Number(s.amountKrw) || 0);
		const labels = slices.map((s) => `${s.label} (${((Number(s.ratio) || 0) * 100).toFixed(1)}%)`);
		return {
			series,
			options: {
				...pieOptionsBase,
				labels: labels.length ? labels : ['데이터 없음'],
				plotOptions: {
					pie: {
						donut: {
							labels: {
								show: true,
								total: {
									show: true,
									label: '매출',
									formatter: () => formatWon(series.reduce((a, b) => a + b, 0)),
								},
							},
						},
					},
				},
			},
		};
	}, [data]);

	const brandPie = useMemo(() => {
		const slices = data?.brandRevenueShare || [];
		const series = slices.map((s) => Number(s.amountKrw) || 0);
		const labels = slices.map((s) => `${s.label} (${((Number(s.ratio) || 0) * 100).toFixed(1)}%)`);
		return {
			series,
			options: {
				...pieOptionsBase,
				labels: labels.length ? labels : ['데이터 없음'],
				plotOptions: {
					pie: {
						donut: {
							labels: {
								show: true,
								total: {
									show: true,
									label: '매출',
									formatter: () => formatWon(series.reduce((a, b) => a + b, 0)),
								},
							},
						},
					},
				},
			},
		};
	}, [data]);

	const topProductColumns = [
		{ title: '순위', dataIndex: 'rank', key: 'rank', width: 56 },
		{ title: '상품', dataIndex: 'productName', key: 'productName', ellipsis: true },
		{
			title: '매출',
			dataIndex: 'revenueKrw',
			key: 'revenueKrw',
			width: 120,
			render: (v) => formatWon(v),
		},
		{
			title: '판매 수량',
			dataIndex: 'quantitySold',
			key: 'quantitySold',
			width: 96,
		},
	];

	const recentOrderColumns = [
		{
			title: '주문',
			key: 'orderNo',
			render: (_, r) => (
				<Link to={`${APP_PREFIX_PATH}/apps/order?orderNo=${r.orderNo}`}>#{r.orderNo}</Link>
			),
		},
		{ title: '일시', dataIndex: 'orderCreatedAt', key: 'orderCreatedAt', render: formatDateTime },
		{
			title: '상태',
			dataIndex: 'orderStatus',
			key: 'orderStatus',
			width: 100,
			render: (s) => <Tag>{orderStatusKo(s)}</Tag>,
		},
		{ title: '수령인', dataIndex: 'recipientName', key: 'recipientName', ellipsis: true },
		{
			title: '합계',
			dataIndex: 'orderTotalPriceKrw',
			key: 'orderTotalPriceKrw',
			width: 110,
			render: (v) => formatWon(v),
		},
	];

	const recentReturnColumns = [
		{
			title: '반품',
			key: 'returnNo',
			render: (_, r) => (
				<Link to={`${APP_PREFIX_PATH}/apps/return?returnNo=${r.returnNo}`}>#{r.returnNo}</Link>
			),
		},
		{
			title: '주문',
			key: 'orderNo',
			width: 88,
			render: (_, r) =>
				r.orderNo != null ? (
					<Link to={`${APP_PREFIX_PATH}/apps/order?orderNo=${r.orderNo}`}>#{r.orderNo}</Link>
				) : (
					'-'
				),
		},
		{ title: '신청일', dataIndex: 'returnRequestedAt', key: 'returnRequestedAt', render: formatDateTime },
		{
			title: '상태',
			dataIndex: 'returnStatus',
			key: 'returnStatus',
			width: 96,
			render: (s) => <Tag>{returnStatusKo(s)}</Tag>,
		},
		{
			title: '금액',
			dataIndex: 'returnAmountKrw',
			key: 'returnAmountKrw',
			width: 110,
			render: (v) => formatWon(v),
		},
	];

	if (role !== 'ADMIN') {
		return null;
	}

	const sb = data?.settlementBrief;

	return (
		<div className="mb-4">
			<Row justify="space-between" align="middle" className="mb-3" gutter={[12, 12]}>
				<Col>
					<h4 className="mb-0">추이·랭킹·운영 보조</h4>
					<p className="text-gray-light font-size-sm mb-0 mt-1">
						일별 추이·비중·인기 상품은 <strong>결제 완료 시각</strong>이 구간에 포함된 주문·라인 매출 기준입니다.
					</p>
				</Col>
				<Col>
					<Segmented
						options={[
							{ label: '14일', value: 14 },
							{ label: '30일', value: 30 },
						]}
						value={trendDays}
						onChange={(v) => setTrendDays(v)}
					/>
				</Col>
			</Row>

			{loading && !data ? (
				<div className="text-center py-5">
					<Spin />
				</div>
			) : (
				<>
					<Card size="small" title={`일별 주문·매출 (최근 ${data?.trendDays ?? trendDays}일)`} className="mb-3">
						<ApexChart options={lineChart.options} series={lineChart.series} height={320} type="line" />
					</Card>

					<Row gutter={[16, 16]} className="mb-3">
						<Col xs={24} lg={12}>
							<Card size="small" title="카테고리별 매출 비중">
								{categoryPie.series.some((x) => x > 0) ? (
									<ApexChart options={categoryPie.options} series={categoryPie.series} type="donut" height={280} />
								) : (
									<div className="text-gray-light py-5 text-center">해당 기간 매출 데이터가 없습니다.</div>
								)}
							</Card>
						</Col>
						<Col xs={24} lg={12}>
							<Card size="small" title="브랜드별 매출 비중">
								{brandPie.series.some((x) => x > 0) ? (
									<ApexChart options={brandPie.options} series={brandPie.series} type="donut" height={280} />
								) : (
									<div className="text-gray-light py-5 text-center">해당 기간 매출 데이터가 없습니다.</div>
								)}
							</Card>
						</Col>
					</Row>

					<Card size="small" title="인기 상품 TOP (매출 기준)" className="mb-3">
						<Table
							size="small"
							rowKey={(r) => String(r.productNo)}
							pagination={false}
							columns={topProductColumns}
							dataSource={data?.topProductsByRevenue || []}
						/>
					</Card>

					<Row gutter={[16, 16]} className="mb-3">
						<Col xs={24} lg={12}>
							<Card
								size="small"
								title="최근 주문"
								extra={
									<Link to={`${APP_PREFIX_PATH}/apps/order`}>전체 목록</Link>
								}
							>
								<Table
									size="small"
									rowKey="orderNo"
									pagination={false}
									columns={recentOrderColumns}
									dataSource={data?.recentOrders || []}
								/>
							</Card>
						</Col>
						<Col xs={24} lg={12}>
							<Card
								size="small"
								title="최근 반품"
								extra={
									<Link to={`${APP_PREFIX_PATH}/apps/return`}>전체 목록</Link>
								}
							>
								<Table
									size="small"
									rowKey="returnNo"
									pagination={false}
									columns={recentReturnColumns}
									dataSource={data?.recentReturns || []}
								/>
							</Card>
						</Col>
					</Row>

					<Card
						size="small"
						title="정산 요약 (미지급·이번 달)"
						extra={
							<Link to={`${APP_PREFIX_PATH}/apps/admin/settlement`}>정산 화면</Link>
						}
					>
						<Row gutter={[16, 16]}>
							<Col xs={24} sm={12} md={6}>
								<Statistic title="미지급 건수 (PENDING)" value={sb?.pendingSettlementCount ?? 0} />
							</Col>
							<Col xs={24} sm={12} md={6}>
								<div className="text-gray-light font-size-sm mb-1">미지급 정산금 합계</div>
								<div className="h3 mb-0">{formatWon(sb?.pendingSettlementAmountKrw)}</div>
							</Col>
							<Col xs={24} sm={12} md={6}>
								<Statistic
									title="이번 달 생성·미지급"
									value={sb?.pendingCreatedThisMonthCount ?? 0}
									suffix="건"
								/>
								<div className="text-gray-light font-size-sm mt-1">
									금액 {formatWon(sb?.pendingCreatedThisMonthAmountKrw)}
								</div>
							</Col>
							<Col xs={24} sm={12} md={6}>
								<Statistic
									title="정산 기간 종료일이 이번 달·미지급"
									value={sb?.pendingPeriodEndsThisMonthCount ?? 0}
									suffix="건"
								/>
								<div className="text-gray-light font-size-sm mt-1">
									금액 {formatWon(sb?.pendingPeriodEndsThisMonthAmountKrw)}
								</div>
							</Col>
						</Row>
						<p className="text-gray-light font-size-sm mb-0 mt-3">
							집계는 정산 엔티티 상태·생성일·기간 종료일과 동일한 기준입니다. 상세는 정산 메뉴에서 확인할 수 있습니다.
						</p>
					</Card>
				</>
			)}
		</div>
	);
};

export default AdminDashboardInsights;
