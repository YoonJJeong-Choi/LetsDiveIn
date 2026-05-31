import React, { useEffect, useMemo, useState } from 'react';
import { useSelector } from 'react-redux';
import { Link } from 'react-router-dom';
import { Row, Col, Card, Statistic, Spin, message, Typography } from 'antd';
import ApexChart from 'react-apexcharts';
import PartnerService from 'services/PartnerService';
import { APP_PREFIX_PATH } from 'configs/AppConfig';
import { COLORS, COLOR_TEXT } from 'constants/ChartConstant';

const { Paragraph } = Typography;

const formatWon = (n) => {
	const v = Number(n);
	if (Number.isNaN(v)) return '-';
	return `${v.toLocaleString('ko-KR')}원`;
};

const PartnerTodayOperations = () => {
	const role = useSelector((state) => state.auth.user?.role);
	const [dto, setDto] = useState(null);
	const [loading, setLoading] = useState(false);

	useEffect(() => {
		if (role !== 'PARTNER') {
			return undefined;
		}
		let cancelled = false;
		setLoading(true);
		PartnerService.getTodayOperations({ trendDays: 7 })
			.then((res) => {
				const data = res?.data ?? res;
				if (!cancelled) setDto(data);
			})
			.catch(() => {
				if (!cancelled) message.error('요약 정보를 불러오지 못했습니다.');
			})
			.finally(() => {
				if (!cancelled) setLoading(false);
			});
		return () => {
			cancelled = true;
		};
	}, [role]);

	const trend = dto?.dailyPaidTrend || [];
	const sumLast7Revenue = useMemo(
		() => trend.reduce((s, p) => s + (Number(p.paidRevenueKrw) || 0), 0),
		[trend]
	);
	const sumLast7Orders = useMemo(
		() => trend.reduce((s, p) => s + (Number(p.paidOrderCount) || 0), 0),
		[trend]
	);

	const miniChart = useMemo(() => {
		const categories = trend.map((p) => String(p.date || '').slice(5));
		const rev = trend.map((p) => Number(p.paidRevenueKrw) || 0);
		return {
			series: [{ name: '매출(원)', data: rev }],
			options: {
				chart: { toolbar: { show: false }, fontFamily: 'inherit', zoom: { enabled: false } },
				stroke: { curve: 'smooth', width: 3 },
				colors: [COLORS[1]],
				xaxis: { categories, labels: { style: { colors: COLOR_TEXT } } },
				yaxis: {
					labels: {
						style: { colors: COLOR_TEXT },
						formatter: (v) => `${Math.round(Number(v) / 1000)}k`,
					},
				},
				dataLabels: { enabled: false },
				tooltip: { y: { formatter: (val) => formatWon(val) } },
				grid: { strokeDashArray: 4 },
			},
		};
	}, [trend]);

	const totalApprovalPending =
		(Number(dto?.productsPendingNewApprovalCount) || 0) +
		(Number(dto?.productsPendingUpdateApprovalCount) || 0) +
		(Number(dto?.optionsPendingNewApprovalCount) || 0) +
		(Number(dto?.optionsPendingUpdateApprovalCount) || 0);

	if (role !== 'PARTNER') {
		return null;
	}

	return (
		<div className="mb-4">
			<h4 className="mb-2">매출·운영 요약</h4>

			{loading && !dto ? (
				<div className="text-center py-4">
					<Spin />
				</div>
			) : (
				<>
					<Card
						size="small"
						title="오늘 · 일주일"
						extra={<Link to={`${APP_PREFIX_PATH}/dashboards/analytic`}>매출·운영 분석 →</Link>}
					>
						<Row gutter={[16, 16]}>
							<Col xs={12} sm={6}>
								<Statistic title="오늘 주문" value={dto?.todayPaidDistinctOrderCount ?? 0} suffix="건" />
							</Col>
							<Col xs={12} sm={6}>
								<Statistic title="오늘 매출" value={formatWon(dto?.todayPartnerLineRevenueKrw)} />
							</Col>
							<Col xs={12} sm={6}>
								<Statistic title="일주일 매출" value={formatWon(sumLast7Revenue)} />
								<div className="text-gray-light font-size-sm mt-1">결제 완료일 기준 일별 합계</div>
							</Col>
							<Col xs={12} sm={6}>
								<Statistic title="일주일 주문" value={sumLast7Orders} suffix="건" />
								<div className="text-gray-light font-size-sm mt-1">일별 건수 합계</div>
							</Col>
						</Row>
					</Card>

					<Card size="small" title="일주일 매출 추이" className="mt-3">
						{trend.length === 0 ? (
							<div className="text-gray-light text-center py-4">표시할 결제 데이터가 없습니다.</div>
						) : (
							<ApexChart options={miniChart.options} series={miniChart.series} height={220} type="area" />
						)}
					</Card>

					<h4 className="mb-2 mt-4">바로 처리</h4>
					<Row gutter={[16, 16]} className="mb-3">
						<Col xs={12} sm={6}>
							<Card
								size="small"
								title="발송 대기"
								extra={<Link to={`${APP_PREFIX_PATH}/partner/order`}>주문 관리</Link>}
							>
								<Statistic value={dto?.ordersPreShipmentDistinctCount ?? 0} suffix="건" />
							</Card>
						</Col>
						<Col xs={12} sm={6}>
							<Card
								size="small"
								title="승인 대기 합계"
								extra={
									totalApprovalPending > 0 ? (
										<Link to={`${APP_PREFIX_PATH}/partner/products`}>상품·옵션</Link>
									) : null
								}
							>
								<Statistic value={totalApprovalPending} suffix="건" />
							</Card>
						</Col>
						<Col xs={12} sm={6}>
							<Card
								size="small"
								title={`저재고 품목 수 (재고 ≤${dto?.lowStockThresholdUsed ?? 5})`}
								extra={<Link to={`${APP_PREFIX_PATH}/partner/inventory`}>재고</Link>}
							>
								<Statistic value={dto?.lowStockLineCount ?? 0} suffix="개 품목" />
							</Card>
						</Col>
						<Col xs={12} sm={6}>
							<Card
								size="small"
								title="반품 진행 중"
								extra={<Link to={`${APP_PREFIX_PATH}/partner/order`}>주문</Link>}
							>
								<Statistic value={dto?.returnsOpenCount ?? 0} suffix="건" />
							</Card>
						</Col>
					</Row>

					<Card
						size="small"
						title="정산 한눈에"
						extra={<Link to={`${APP_PREFIX_PATH}/partner/settlement`}>정산 조회</Link>}
					>
						<Row gutter={[16, 16]}>
							<Col xs={24} md={12}>
								<div className="text-gray-light font-size-sm mb-1">정산액</div>
								<div className="font-weight-semibold">{formatWon(dto?.settlementReadyTotalSettlementAmountKrw)}</div>
								<div className="text-muted mt-1 font-size-sm">
									판매 {formatWon(dto?.settlementReadyTotalSalesAmountKrw)} · 품목 수 {dto?.settlementReadyLineCount ?? 0}건
								</div>
							</Col>
							<Col xs={24} md={12}>
								<div className="text-gray-light font-size-sm mb-1">미지급</div>
								<div className="font-weight-semibold">{formatWon(dto?.pendingSettlementBatchAmountKrw)}</div>
								<div className="text-muted mt-1 font-size-sm">배치 {dto?.pendingSettlementBatchCount ?? 0}건</div>
							</Col>
						</Row>
						<div className="mt-3 pt-2 border-top text-gray-light font-size-sm">
							<Link to={`${APP_PREFIX_PATH}/partner/sales-statistics`}>판매 통계</Link>
							<span className="mx-1">·</span>
							구매확정·배송완료 기준(결제 기준 분석과 숫자가 다를 수 있음)
						</div>
					</Card>
				</>
			)}
		</div>
	);
};

export default PartnerTodayOperations;
