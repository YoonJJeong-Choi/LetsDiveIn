import React, { useEffect, useMemo, useState } from 'react';
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
	Alert,
	Collapse,
	Typography,
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
	return d.toLocaleString('ko-KR', {
		month: '2-digit',
		day: '2-digit',
		hour: '2-digit',
		minute: '2-digit',
	});
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

const pctVsPrior = (curr, prev) => {
	if (prev == null || prev <= 0) return null;
	return ((Number(curr) - Number(prev)) / Number(prev)) * 100;
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

const { Paragraph, Text } = Typography;

const AdminAnalyticDashboard = () => {
	const [trendDays, setTrendDays] = useState(30);
	const [analytics, setAnalytics] = useState(null);
	const [loading, setLoading] = useState(false);

	useEffect(() => {
		let cancelled = false;
		setLoading(true);
		AdminService.getDashboardAnalytics({ trendDays })
			.then((aRes) => {
				if (cancelled) return;
				setAnalytics(aRes?.data ?? aRes);
			})
			.catch(() => {
				if (!cancelled) message.error('매출·운영 상세를 불러오지 못했습니다.');
			})
			.finally(() => {
				if (!cancelled) setLoading(false);
			});
		return () => {
			cancelled = true;
		};
	}, [trendDays]);

	const insights = analytics?.insights;
	const q = analytics?.fulfillmentQueue;
	const cmp = analytics?.sevenDayVsPriorSeven;

	const revDeltaPct = pctVsPrior(cmp?.last7PaidRevenueKrw, cmp?.prior7PaidRevenueKrw);
	const ordDeltaPct = pctVsPrior(cmp?.last7PaidOrderCount, cmp?.prior7PaidOrderCount);

	const lineChart = useMemo(() => {
		const trend = insights?.dailyPaidTrend || [];
		const n = trend.length;
		const categories = trend.map((p) => String(p.date || '').slice(5));
		const counts = trend.map((p) => Number(p.paidOrderCount) || 0);
		const rev = trend.map((p) => Number(p.paidRevenueKrw) || 0);
		const chartHeight = n >= 30 ? 500 : n >= 21 ? 440 : n >= 14 ? 400 : 360;
		const labelSize = n > 28 ? '9px' : n > 18 ? '10px' : n > 10 ? '11px' : '12px';
		const longRange = n > 18;
		return {
			chartHeight,
			series: [
				{ name: '결제 완료 주문(건)', type: 'column', data: counts, yAxisIndex: 0 },
				{ name: '매출(원)', type: 'line', data: rev, yAxisIndex: 1 },
			],
			options: {
				chart: {
					type: 'line',
					height: chartHeight,
					toolbar: {
						show: longRange,
						tools: {
							zoom: longRange,
							zoomin: longRange,
							zoomout: longRange,
							pan: longRange,
							reset: longRange,
						},
					},
					zoom: longRange ? { enabled: true, type: 'x', autoScaleYaxis: true } : { enabled: false },
					fontFamily: 'inherit',
					stacked: false,
				},
				colors: [COLORS[0], COLORS[1]],
				stroke: { width: [0, 3], curve: 'smooth' },
				dataLabels: { enabled: false },
				grid: { padding: { bottom: longRange ? 48 : n > 12 ? 28 : 10, top: 8 } },
				xaxis: {
					categories,
					labels: {
						rotate: longRange ? -55 : -40,
						rotateAlways: n > 8,
						hideOverlappingLabels: true,
						trim: true,
						maxHeight: longRange ? 160 : 120,
						style: { colors: COLOR_TEXT, fontSize: labelSize },
					},
					tickPlacement: 'on',
				},
				yaxis: [
					{ title: { text: '주문(건)' }, labels: { style: { colors: COLOR_TEXT } } },
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
					y: [{ formatter: (val) => `${val}건` }, { formatter: (val) => formatWon(val) }],
				},
				legend: { position: 'top' },
			},
		};
	}, [insights, trendDays]);

	const categoryPie = useMemo(() => {
		const slices = insights?.categoryRevenueShare || [];
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
	}, [insights]);

	const brandPie = useMemo(() => {
		const slices = insights?.brandRevenueShare || [];
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
	}, [insights]);

	const hourChart = useMemo(() => {
		const rows = analytics?.paidPaymentCountByHour || [];
		const categories = rows.map((r) => `${r.hourOfDay}시`);
		const data = rows.map((r) => Number(r.paymentCount) || 0);
		return {
			series: [{ name: '결제 건수(행)', data }],
			options: {
				chart: { toolbar: { show: false }, fontFamily: 'inherit' },
				colors: [COLORS[2]],
				dataLabels: { enabled: false },
				xaxis: { categories, labels: { style: { colors: COLOR_TEXT }, rotate: -45 } },
				yaxis: { labels: { style: { colors: COLOR_TEXT } } },
				tooltip: { y: { formatter: (v) => `${v}건` } },
			},
		};
	}, [analytics]);

	const weekdayChart = useMemo(() => {
		const rows = analytics?.paidRevenueByWeekday || [];
		const categories = rows.map((r) => r.weekdayLabel);
		const data = rows.map((r) => Number(r.paidRevenueKrw) || 0);
		return {
			series: [{ name: '매출(원)', data }],
			options: {
				chart: { toolbar: { show: false }, fontFamily: 'inherit' },
				colors: [COLORS[3]],
				dataLabels: { enabled: false },
				xaxis: { categories, labels: { style: { colors: COLOR_TEXT } } },
				yaxis: {
					labels: {
						style: { colors: COLOR_TEXT },
						formatter: (v) => `${Math.round(Number(v) / 1000)}k`,
					},
				},
				tooltip: { y: { formatter: (v) => formatWon(v) } },
			},
		};
	}, [analytics]);

	const topProductColumns = [
		{ title: '순위', dataIndex: 'rank', key: 'rank', width: 56 },
		{ title: '상품', dataIndex: 'productName', key: 'productName', ellipsis: true },
		{ title: '매출', dataIndex: 'revenueKrw', key: 'revenueKrw', width: 120, render: (v) => formatWon(v) },
		{ title: '판매 수량', dataIndex: 'quantitySold', key: 'quantitySold', width: 96 },
	];

	const partnerColumns = [
		{ title: '순위', dataIndex: 'rank', key: 'rank', width: 56 },
		{ title: '파트너', dataIndex: 'partnerName', key: 'partnerName', ellipsis: true },
		{
			title: '매출(기간)',
			dataIndex: 'lineRevenueKrw',
			key: 'lineRevenueKrw',
			width: 140,
			render: (v) => formatWon(v),
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
		{ title: '합계', dataIndex: 'orderTotalPriceKrw', key: 'orderTotalPriceKrw', width: 110, render: (v) => formatWon(v) },
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
		{ title: '금액', dataIndex: 'returnAmountKrw', key: 'returnAmountKrw', width: 110, render: (v) => formatWon(v) },
	];

	const sb = insights?.settlementBrief;

	const fixedWeekBullets = [];
	if (cmp?.last7PaidRevenueKrw != null) {
		fixedWeekBullets.push(
			`최근 일주일 매출 ${formatWon(cmp.last7PaidRevenueKrw)}${
				revDeltaPct != null ? ` (직전 일주일 대비 ${revDeltaPct >= 0 ? '+' : ''}${revDeltaPct.toFixed(1)}%)` : ''
			}`
		);
	}
	if (cmp?.last7PaidOrderCount != null) {
		fixedWeekBullets.push(
			`최근 일주일 결제 완료 주문 ${cmp.last7PaidOrderCount}건${
				ordDeltaPct != null ? ` (직전 일주일 대비 ${ordDeltaPct >= 0 ? '+' : ''}${ordDeltaPct.toFixed(1)}%)` : ''
			}`
		);
	}
	const rangeBullets = [];
	if (analytics?.paidDistinctOrdersInTrendPeriod != null) {
		rangeBullets.push(
			`결제 완료 주문 ${analytics.paidDistinctOrdersInTrendPeriod}건 · 반품 신청 ${analytics.returnsRequestedInTrendPeriod ?? 0}건`
		);
	}

	return (
		<div className="mb-4">
			<Row className="mb-3">
				<Col span={24}>
					<h3 className="mb-1">매출·운영 상세</h3>
					<Paragraph type="secondary" className="mb-0 font-size-sm">
						총괄·기획·운영 점검용입니다. <Text strong>기본 대시보드</Text>는 처리 대기와 오늘 요약만 보여 주고, 여기서는 기간·비중·파트너·시간대
						등을 나누어 봅니다.
					</Paragraph>
				</Col>
			</Row>

			{loading && !analytics ? (
				<div className="text-center py-5">
					<Spin />
				</div>
			) : (
				<>
					<Alert
						type="info"
						showIcon
						className="mb-3"
						message="요약"
						description={
							fixedWeekBullets.length === 0 && rangeBullets.length === 0 ? (
								'집계할 데이터가 없습니다.'
							) : (
								<div>
									{fixedWeekBullets.length > 0 && (
										<div className="mb-2">
											<strong>일주일</strong>
											<ul className="mb-0 pl-3 mt-1">
												{fixedWeekBullets.map((t, i) => (
													<li key={`fix-${i}`}>{t}</li>
												))}
											</ul>
										</div>
									)}
									{rangeBullets.length > 0 && (
										<div>
											<strong>
												선택 기간 {insights?.trendDays ?? trendDays}일
											</strong>
											<ul className="mb-0 pl-3 mt-1">
												{rangeBullets.map((t, i) => (
													<li key={`rng-${i}`}>{t}</li>
												))}
											</ul>
										</div>
									)}
								</div>
							)
						}
					/>

					<div className="mb-2">
						<Typography.Title level={4} className="mb-1">
							매출·주문
						</Typography.Title>
						<Text type="secondary">최근 일주일과 그 직전 일주일을 비교합니다.</Text>
					</div>
					<Row gutter={[16, 16]} className="mb-3">
						<Col xs={24} sm={8}>
							<Card size="small" title="일주일 매출">
								<Statistic value={formatWon(cmp?.last7PaidRevenueKrw)} />
								{revDeltaPct != null && (
									<div className="mt-2">
										<Tag color={revDeltaPct >= 0 ? 'green' : 'red'}>
											직전 일주일 대비 {revDeltaPct >= 0 ? '+' : ''}
											{revDeltaPct.toFixed(1)}%
										</Tag>
									</div>
								)}
							</Card>
						</Col>
						<Col xs={24} sm={8}>
							<Card size="small" title="전 일주일 매출">
								<Statistic value={formatWon(cmp?.prior7PaidRevenueKrw)} />
							</Card>
						</Col>
						<Col xs={24} sm={8}>
							<Card
								size="small"
								title="최근 일주일 결제 완료 주문"
								styles={{ body: { paddingBottom: 8 } }}
							>
								<Statistic value={cmp?.last7PaidOrderCount ?? 0} suffix="건" />
								{ordDeltaPct != null && (
									<div className="mt-2">
										<Tag color={ordDeltaPct >= 0 ? 'green' : 'red'}>
											직전 일주일 대비 {ordDeltaPct >= 0 ? '+' : ''}
											{ordDeltaPct.toFixed(1)}%
										</Tag>
									</div>
								)}
							</Card>
						</Col>
					</Row>

					<Row className="mb-3" align="middle" gutter={[12, 12]}>
						<Col xs={24}>
							<Segmented
								options={[
									{ label: '일주일', value: 7 },
									{ label: '2주', value: 14 },
									{ label: '30일', value: 30 },
									{ label: '60일', value: 60 },
									{ label: '90일', value: 90 },
								]}
								value={trendDays}
								onChange={(v) => setTrendDays(v)}
							/>
						</Col>
					</Row>

					<Collapse
						className="mb-4"
						items={[
							{
								key: 'help',
								label: '지표가 의미하는 것 (클릭하여 펼치기)',
								children: (
									<ul className="mb-0 pl-3 text-gray-light font-size-sm">
										<li>
											<strong>전주 대비</strong>: &quot;최근 일주일&quot;과 &quot;그 직전 일주일&quot;만 비교하며, 기간 탭과는
											무관합니다.
										</li>
										<li>
											<strong>카테고리·브랜드·일별 추이·파트너·상품·시간대·요일</strong>: 기간 탭에서 고른 일수에 맞춰
											집계가 바뀝니다.
										</li>
										<li>
											<strong>시간대·요일</strong>: 구간 안 모든 결제 행을 시간·요일 버킷에 쌓은 분포입니다(주문 중복 가능).
										</li>
										<li>
											<strong>반품 비율</strong>: 위에서 고른 기간 안에서, 반품 <strong>신청</strong> 건수를 결제가 완료된{' '}
											<strong>주문 번호 기준(한 주문은 한 번만)</strong> 건수로 나눈 값입니다.
										</li>
										<li>
											<strong>이행·병목</strong>: 현재 시점 스냅샷으로, 기본 대시보드 &quot;처리 대기&quot;와 동일 출처입니다. 기간
											탭과 무관합니다.
										</li>
										<li>
											<strong>미지급 요약</strong>: 상단 요약과 집계 방식이 같으며 기간 탭과 무관합니다. 전체 정산 건수·합계는
											정산 메뉴에서 확인합니다.
										</li>
										<li>
											<strong>최근 주문·반품 표</strong>: 전체 중 최신 몇 건만 보여 주며, 탭 일수로 잘리지 않습니다.
										</li>
									</ul>
								),
							},
						]}
					/>

					<Row gutter={[16, 16]} className="mb-4">
						<Col xs={24} md={12} lg={10}>
							<Card
								size="small"
								title="반품 신청 비율"
								styles={{ body: { paddingBottom: 8 } }}
							>
								<div className="mb-1">
									반품 신청 <strong>{analytics?.returnsRequestedInTrendPeriod ?? 0}</strong>건 / 결제 주문{' '}
									<strong>{analytics?.paidDistinctOrdersInTrendPeriod ?? 0}</strong>건
								</div>
							</Card>
						</Col>
					</Row>

					<div className="mb-2 mt-4">
						<Typography.Title level={4} className="mb-1">
							일별 추이
						</Typography.Title>
						<Text type="secondary">
							기간 탭에서 고른 <strong>{insights?.trendDays ?? trendDays}일</strong> 구간입니다.</Text>
					</div>
					<Card size="small" title={`최근 ${insights?.trendDays ?? trendDays}일`} className="mb-4">
						{(insights?.dailyPaidTrend || []).length > 18 ? (
							<p className="text-gray-light font-size-sm mb-2">
								도구줄에서 <strong>확대·이동</strong> 후 <strong>초기화</strong>로 돌아올 수 있습니다.
							</p>
						) : null}
						{loading && analytics ? (
							<div className="text-center py-4">
								<Spin />
							</div>
						) : (
							<ApexChart
								options={lineChart.options}
								series={lineChart.series}
								height={lineChart.chartHeight}
								type="line"
							/>
						)}
					</Card>

					<div className="mb-2 mt-1">
						<Typography.Title level={4} className="mb-1">
							상위
						</Typography.Title>
					</div>
					<Row gutter={[16, 16]} className="mb-4">
						<Col xs={24} lg={12}>
							<Card size="small" title="카테고리별 매출 비중">
								{categoryPie.series.some((x) => x > 0) ? (
									<ApexChart options={categoryPie.options} series={categoryPie.series} type="donut" height={280} />
								) : (
									<div className="text-gray-light py-5 text-center">데이터 없음</div>
								)}
							</Card>
						</Col>
						<Col xs={24} lg={12}>
							<Card size="small" title="브랜드별 매출 비중">
								{brandPie.series.some((x) => x > 0) ? (
									<ApexChart options={brandPie.options} series={brandPie.series} type="donut" height={280} />
								) : (
									<div className="text-gray-light py-5 text-center">데이터 없음</div>
								)}
							</Card>
						</Col>
					</Row>

					<Row gutter={[16, 16]} className="mb-4">
						<Col xs={24} lg={12}>
							<Card
								size="small"
								title="파트너"
								extra={<Link to={`${APP_PREFIX_PATH}/apps/admin/partner-approval`}>입점·파트너</Link>}
							>
								<Table
									size="small"
									rowKey={(r) => String(r.partnerId)}
									pagination={false}
									columns={partnerColumns}
									dataSource={analytics?.topPartnersByLineRevenue || []}
									locale={{ emptyText: '데이터 없음' }}
								/>
							</Card>
						</Col>
						<Col xs={24} lg={12}>
							<Card
								size="small"
								title="상품"
								extra={<Link to={`${APP_PREFIX_PATH}/apps/admin/product-approval`}>상품 승인</Link>}
							>
								<Table
									size="small"
									rowKey={(r) => String(r.productNo)}
									pagination={false}
									columns={topProductColumns}
									dataSource={insights?.topProductsByRevenue || []}
								/>
							</Card>
						</Col>
					</Row>

					<div className="mb-2 mt-1">
						<Typography.Title level={4} className="mb-1">
							시간대·요일
						</Typography.Title>
					</div>
					<Row gutter={[16, 16]} className="mb-4">
						<Col xs={24} lg={14}>
							<Card size="small" title="결제 시간대">
								<ApexChart options={hourChart.options} series={hourChart.series} height={280} type="bar" />
							</Card>
						</Col>
						<Col xs={24} lg={10}>
							<Card size="small" title="요일별 매출">
								<ApexChart options={weekdayChart.options} series={weekdayChart.series} height={280} type="bar" />
							</Card>
						</Col>
					</Row>

					<div className="mb-2 mt-1">
						<Typography.Title level={4} className="mb-1">
							대기
						</Typography.Title>
						<Text type="secondary">현재 대기 지점을 보여 줍니다.</Text>
					</div>
					<Row gutter={[16, 16]} className="mb-4">
						<Col xs={12} sm={8} md={6}>
							<Card size="small" title="발주 확인 대기">
								<Statistic value={q?.orderItemsPendingConfirmation ?? 0} suffix="건" />
							</Card>
						</Col>
						<Col xs={12} sm={8} md={6}>
							<Card size="small" title={`배송 대기기 장기(${q?.deliveriesReadyDelayedDaysThreshold ?? 3}일+)`}>
								<Statistic value={q?.deliveriesReadyDelayed ?? 0} suffix="건" />
							</Card>
						</Col>
						<Col xs={12} sm={8} md={6}>
							<Card size="small" title="반품 처리 중">
								<Statistic value={q?.returnsPendingProcessing ?? 0} suffix="건" />
							</Card>
						</Col>
						<Col xs={12} sm={8} md={6}>
							<Card size="small" title="상품 승인 대기">
								<Statistic
									value={
										(q?.productsPendingNewApproval ?? 0) +
										(q?.productsPendingUpdateApproval ?? 0) +
										(q?.optionsPendingNewApproval ?? 0) +
										(q?.optionsPendingUpdateApproval ?? 0)
									}
									suffix="건"
								/>
							</Card>
						</Col>
					</Row>

					<div className="mb-2 mt-1">
						<Typography.Title level={4} className="mb-1">
							정산
						</Typography.Title>
					</div>
					<Row gutter={[16, 16]} className="mb-4">
						<Col xs={24} md={12} lg={8}>
							<Card
								size="small"
								title="미지급 요약"
								extra={<Link to={`${APP_PREFIX_PATH}/apps/admin/settlement`}>정산 화면</Link>}
							>
								<div>미지급 {sb?.pendingSettlementCount ?? 0}건</div>
								<div className="mt-1">{formatWon(sb?.pendingSettlementAmountKrw)}</div>
								<div className="mt-2 text-gray-light font-size-sm">
									이번 달 생성 대기 {sb?.pendingCreatedThisMonthCount ?? 0}건
								</div>
							</Card>
						</Col>
					</Row>

					<div className="mb-2 mt-1">
						<Typography.Title level={4} className="mb-1">
							최근 주문·반품
						</Typography.Title>
					</div>
					<Row gutter={[16, 16]} className="mb-4">
						<Col xs={24} lg={12}>
							<Card size="small" title="최근 주문" extra={<Link to={`${APP_PREFIX_PATH}/apps/order`}>전체</Link>}>
								<Table
									size="small"
									rowKey="orderNo"
									pagination={false}
									columns={recentOrderColumns}
									dataSource={insights?.recentOrders || []}
								/>
							</Card>
						</Col>
						<Col xs={24} lg={12}>
							<Card size="small" title="최근 반품" extra={<Link to={`${APP_PREFIX_PATH}/apps/return`}>전체</Link>}>
								<Table
									size="small"
									rowKey="returnNo"
									pagination={false}
									columns={recentReturnColumns}
									dataSource={insights?.recentReturns || []}
								/>
							</Card>
						</Col>
					</Row>
				</>
			)}
		</div>
	);
};

export default AdminAnalyticDashboard;
