import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
	Row,
	Col,
	Card,
	Statistic,
	Spin,
	message,
	Segmented,
	Select,
	Button,
	Typography,
	Table,
	Tag,
	Collapse,
} from 'antd';
import ApexChart from 'react-apexcharts';
import PartnerService from 'services/PartnerService';
import { APP_PREFIX_PATH } from 'configs/AppConfig';
import { COLORS, COLOR_TEXT } from 'constants/ChartConstant';

const { Paragraph, Text } = Typography;

/** 상품 필터: 전체 선택용(실제 상품 번호와 겹치지 않게 문자열) */
const PRODUCT_FILTER_ALL = '__all__';

const formatWon = (n) => {
	const v = Number(n);
	if (Number.isNaN(v)) return '-';
	return `${v.toLocaleString('ko-KR')}원`;
};

const returnReasonKo = (code) => {
	const m = {
		CHANGE_OF_MIND: '단순 변심',
		DEFECT: '상품 하자',
		WRONG_ITEM: '오배송',
		ORDER_MISTAKE: '주문 실수',
		OTHER: '기타',
	};
	return m[code] || code || '-';
};

const PartnerAnalyticDashboard = () => {
	const [trendDays, setTrendDays] = useState(30);
	const [productNo, setProductNo] = useState(undefined);
	const [data, setData] = useState(null);
	const [products, setProducts] = useState([]);
	const [loading, setLoading] = useState(false);

	useEffect(() => {
		let cancelled = false;
		PartnerService.getMyProducts()
			.then((res) => {
				const list = Array.isArray(res) ? res : res?.data ?? [];
				if (!cancelled) setProducts(list);
			})
			.catch(() => {});
		return () => {
			cancelled = true;
		};
	}, []);

	useEffect(() => {
		let cancelled = false;
		setLoading(true);
		const params = { trendDays };
		if (productNo != null) params.productNo = productNo;
		PartnerService.getDashboardAnalytics(params)
			.then((res) => {
				const d = res?.data ?? res;
				if (!cancelled) setData(d);
			})
			.catch(() => {
				if (!cancelled) message.error('매출·운영 분석을 불러오지 못했습니다.');
			})
			.finally(() => {
				if (!cancelled) setLoading(false);
			});
		return () => {
			cancelled = true;
		};
	}, [trendDays, productNo]);

	const lineChart = useMemo(() => {
		const trend = data?.dailyPaidTrend || [];
		const categories = trend.map((p) => String(p.date || '').slice(5));
		const counts = trend.map((p) => Number(p.paidOrderCount) || 0);
		const rev = trend.map((p) => Number(p.paidRevenueKrw) || 0);
		const n = trend.length;
		const chartHeight = n >= 30 ? 420 : n >= 21 ? 380 : 320;
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
				xaxis: {
					categories,
					labels: { style: { colors: COLOR_TEXT }, rotate: n > 24 ? -45 : 0 },
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
	}, [data]);

	const productOptions = useMemo(
		() => [
			{ value: PRODUCT_FILTER_ALL, label: '전체 상품' },
			...(products || []).map((p) => ({
				value: p.productNo,
				label: `${p.productName || ''} (${p.sku || p.productNo})`,
			})),
		],
		[products]
	);

	const topColumns = [
		{ title: '상품', dataIndex: 'productName', key: 'productName', ellipsis: true },
		{ title: '상품코드', dataIndex: 'sku', key: 'sku', width: 120 },
		{
			title: '판매량',
			dataIndex: 'quantitySold',
			key: 'quantitySold',
			width: 88,
			render: (v) => `${v ?? 0}`,
		},
		{
			title: '상품 매출',
			dataIndex: 'lineRevenueKrw',
			key: 'lineRevenueKrw',
			width: 120,
			render: (v) => formatWon(v),
		},
	];

	const reasonColumns = [
		{
			title: '사유',
			dataIndex: 'reasonType',
			key: 'reasonType',
			render: (c) => returnReasonKo(c),
		},
		{ title: '건수', dataIndex: 'count', key: 'count', width: 80 },
	];

	const fulfillmentTotal =
		(Number(data?.ordersPreShipmentDistinctCount) || 0) + (Number(data?.ordersInDeliveryDistinctCount) || 0);
	const prePct =
		fulfillmentTotal > 0
			? Math.round(((Number(data?.ordersPreShipmentDistinctCount) || 0) / fulfillmentTotal) * 1000) / 10
			: 0;
	const shipPct =
		fulfillmentTotal > 0
			? Math.round(((Number(data?.ordersInDeliveryDistinctCount) || 0) / fulfillmentTotal) * 1000) / 10
			: 0;

	return (
		<div className="mb-4">
			<Row className="mb-3">
				<Col span={24}>
					<h3 className="mb-1">매출·운영 분석</h3>
				</Col>
			</Row>

			{loading && !data ? (
				<div className="text-center py-5">
					<Spin />
				</div>
			) : (
				<>
					<div className="mb-2">
						<Typography.Title level={4} className="mb-1">
							현재 상태
						</Typography.Title>
					</div>
					<Row gutter={[16, 16]} className="mb-4">
						<Col xs={24} md={8}>
							<Card size="small" title="진행 중 반품(미환불)">
								<Statistic value={data?.returnsOpenCount ?? 0} suffix="건" />
								<div className="text-gray-light font-size-sm mt-1">
									<Link to={`${APP_PREFIX_PATH}/partner/order`}>주문 관리에서 처리</Link>
								</div>
							</Card>
						</Col>
					</Row>

					<div className="mb-2">
						<Typography.Title level={4} className="mb-1">
							정산
						</Typography.Title>
					</div>
					<Row gutter={[16, 16]} className="mb-4">
						<Col xs={24} md={8}>
							<Card size="small" title="정산 준비 완료(예상)">
								<div>정산액 {formatWon(data?.settlementReadyTotalSettlementAmountKrw)}</div>
								<div className="mt-1">판매액 {formatWon(data?.settlementReadyTotalSalesAmountKrw)}</div>
								<div className="mt-1 text-gray-light font-size-sm">정산 {data?.settlementReadyLineCount ?? 0}건</div>
							</Card>
						</Col>
						<Col xs={24} md={8}>
							<Card
								size="small"
								title="미지급 정산"
								extra={<Link to={`${APP_PREFIX_PATH}/partner/settlement`}>정산 조회</Link>}
							>
								<div>건수 {data?.pendingSettlementBatchCount ?? 0}</div>
								<div className="mt-1">{formatWon(data?.pendingSettlementBatchAmountKrw)}</div>
							</Card>
						</Col>
					</Row>

					<div className="mb-2">
						<Typography.Title level={4} className="mb-1">
							상품·재고
						</Typography.Title>
					</div>
					<Row gutter={[16, 16]} className="mb-4">
						<Col xs={12} sm={8}>
							<Card size="small" title="상품 등록 대기">
								<Statistic value={data?.productsPendingNewApprovalCount ?? 0} suffix="건" />
							</Card>
						</Col>
						<Col xs={12} sm={8}>
							<Card size="small" title="상품 수정 대기">
								<Statistic value={data?.productsPendingUpdateApprovalCount ?? 0} suffix="건" />
							</Card>
						</Col>
						<Col xs={12} sm={8}>
							<Card size="small" title="옵션 승인 대기">
								<Statistic
									value={
										(Number(data?.optionsPendingNewApprovalCount) || 0) +
										(Number(data?.optionsPendingUpdateApprovalCount) || 0)
									}
									suffix="건"
								/>
								<div className="text-gray-light font-size-sm mt-1">
									등록 {data?.optionsPendingNewApprovalCount ?? 0} · 수정{' '}
									{data?.optionsPendingUpdateApprovalCount ?? 0}
								</div>
							</Card>
						</Col>
						<Col xs={24} sm={12}>
							<Card size="small" title={`저재고 품목 수 (재고 ≤ ${data?.lowStockThresholdUsed ?? 5})`}>
								<Statistic value={data?.lowStockLineCount ?? 0} suffix="개 품목" />
								<div className="mt-2">
									<Link to={`${APP_PREFIX_PATH}/partner/inventory`}>재고 관리</Link>
								</div>
							</Card>
						</Col>
						<Col xs={24} sm={12}>
							<Card size="small" title="품절 품목 수 (재고 0)">
								<Statistic value={data?.outOfStockLineCount ?? 0} suffix="개 품목" />
							</Card>
						</Col>
					</Row>

					<div className="mb-2">
						<Typography.Title level={4} className="mb-1">
						주문·배송 처리
						</Typography.Title>
					</div>
					<Row gutter={[16, 16]} className="mb-4">
						<Col xs={24} sm={8}>
							<Card size="small" title="발송 전">
								<Statistic value={data?.ordersPreShipmentDistinctCount ?? 0} suffix="건" />
								{fulfillmentTotal > 0 ? (
									<div className="mt-2">
										<Tag>비중 약 {prePct}%</Tag>
									</div>
								) : null}
							</Card>
						</Col>
						<Col xs={24} sm={8}>
							<Card size="small" title="배송 중">
								<Statistic value={data?.ordersInDeliveryDistinctCount ?? 0} suffix="건" />
								{fulfillmentTotal > 0 ? (
									<div className="mt-2">
										<Tag color="blue">비중 약 {shipPct}%</Tag>
									</div>
								) : null}
							</Card>
						</Col>
						<Col xs={24} sm={8}>
							<Card size="small" title="출고 지연 의심">
								<Statistic value={data?.deliveriesReadyDelayedPartnerLineCount ?? 0} suffix="건" />
							</Card>
						</Col>
					</Row>

					<Row gutter={[12, 12]} className="mb-3 mt-1" align="middle">
						<Col xs={24} md={14}>
							<Segmented
								options={[
									{ label: '7일', value: 7 },
									{ label: '14일', value: 14 },
									{ label: '30일', value: 30 },
									{ label: '60일', value: 60 },
									{ label: '90일', value: 90 },
								]}
								value={trendDays}
								onChange={(v) => setTrendDays(v)}
							/>
						</Col>
						<Col xs={24} md={10}>
							<Row gutter={[8, 8]} align="middle">
								<Col flex="auto" style={{ minWidth: 0 }}>
									<Select
										allowClear
										placeholder="상품 선택"
										style={{ width: '100%' }}
										options={productOptions}
										value={productNo === undefined ? PRODUCT_FILTER_ALL : productNo}
										onChange={(v) =>
											setProductNo(
												v === undefined || v === PRODUCT_FILTER_ALL ? undefined : v
											)
										}
										showSearch
										optionFilterProp="label"
									/>
								</Col>
								<Col flex="none">
									<Button
										size="small"
										disabled={productNo === undefined}
										onClick={() => setProductNo(undefined)}
									>
										초기화
									</Button>
								</Col>
							</Row>
						</Col>
					</Row>

					<Row gutter={[16, 16]} className="mb-3">
						<Col xs={24} sm={12} md={8}>
							<Card size="small" title={`${data?.trendDays ?? trendDays}일 매출`}>
								<Statistic value={formatWon(data?.periodPartnerLineRevenueKrw)} />
							</Card>
						</Col>
						<Col xs={24} sm={12} md={8}>
							<Card size="small" title="결제 완료 주문">
								<Statistic value={data?.periodPaidDistinctOrderCount ?? 0} suffix="건" />
							</Card>
						</Col>
					</Row>

					<div className="mb-2">
						<Typography.Title level={4} className="mb-1">
							일별 추이
						</Typography.Title>
					</div>
					<Card size="small" className="mb-4" title={`최근 ${data?.trendDays ?? trendDays}일`}>
						{loading && data ? (
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

					<div className="mb-2">
						<Typography.Title level={4} className="mb-1">
							상품별 상위 실적
						</Typography.Title>
					</div>
					<Card size="small" className="mb-4">
						<Table
							size="small"
							rowKey="productNo"
							pagination={false}
							columns={topColumns}
							dataSource={data?.topProductsByLineRevenue || []}
							locale={{ emptyText: '데이터 없음' }}
						/>
					</Card>

					<div className="mb-2">
						<Typography.Title level={4} className="mb-1">
							반품
						</Typography.Title>
					</div>
					<Row gutter={[16, 16]} className="mb-4">
						<Col xs={24} md={8}>
							<Card size="small" title="반품 신청">
								<Statistic value={data?.returnsRequestedInPeriod ?? 0} suffix="건" />
							</Card>
						</Col>
						<Col xs={24} md={16}>
							<Card size="small" title="사유 분포">
								<Table
									size="small"
									rowKey="reasonType"
									pagination={false}
									columns={reasonColumns}
									dataSource={data?.returnReasonBreakdownInPeriod || []}
									locale={{ emptyText: '해당 기간 데이터 없음' }}
								/>
							</Card>
						</Col>
					</Row>

					<Collapse
						items={[
							{
								key: 'help',
								label: '지표 요약',
								children: (
									<ul className="mb-0 pl-3 text-gray-light font-size-sm">
										<li>
											<strong>현재 상태·정산·재고·이행</strong>: 기간 탭·상품 필터와 무관한 스냅샷입니다.
										</li>
										<li>
											매출·주문 추이·상품별 실적·기간 반품: 결제 완료 시각 기준으로 같은 구간에서만 집계합니다.
										</li>
										<li>상품 필터: 선택한 상품만 포함합니다.</li>
										<li>정산 예상·미지급: 정산 모듈과 같은 규칙입니다.</li>
									</ul>
								),
							},
						]}
					/>
				</>
			)}
		</div>
	);
};

export default PartnerAnalyticDashboard;
