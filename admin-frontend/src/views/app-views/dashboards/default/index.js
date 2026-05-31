import React, { useState, useEffect, useMemo } from 'react';
import { Row, Col, Button, Card as AntdCard, Statistic, Spin, message } from 'antd';
import { Link, useNavigate } from 'react-router-dom';
import ApexChart from 'react-apexcharts';
import AdminService from 'services/AdminService';
import { APP_PREFIX_PATH } from 'configs/AppConfig';
import { useSelector } from 'react-redux';
import { COLORS, COLOR_TEXT } from 'constants/ChartConstant';
import PartnerTodayOperations from './PartnerTodayOperations';

/** 대기 건수에 따른 강조(주의 / 긴급) — 숫자가 커질수록 눈에 띄게 */
const queueAccent = (value, { warn = 1, danger = 10 } = {}) => {
	const v = Number(value) || 0;
	if (v >= danger) {
		return 'danger';
	}
	if (v >= warn) {
		return 'warn';
	}
	return 'ok';
};

const cardShellStyle = (accent) => {
	if (accent === 'danger') {
		return { borderLeft: '4px solid #ff4d4f', background: 'rgba(255, 77, 79, 0.06)' };
	}
	if (accent === 'warn') {
		return { borderLeft: '4px solid #faad14', background: 'rgba(250, 173, 20, 0.08)' };
	}
	return {};
};

const queueValueStyle = (accent) => {
	if (accent === 'danger') {
		return { fontSize: 28, color: '#cf1322', fontWeight: 600 };
	}
	if (accent === 'warn') {
		return { fontSize: 28, color: '#ad6800', fontWeight: 600 };
	}
	return { fontSize: 28 };
};

const AdminProcessingQueue = () => {
	const navigate = useNavigate();
	const role = useSelector((state) => state.auth.user?.role);
	const [queue, setQueue] = useState(null);
	const [loading, setLoading] = useState(false);

	useEffect(() => {
		if (role !== 'ADMIN') {
			return undefined;
		}
		let cancelled = false;
		setLoading(true);
		AdminService.getDashboardQueue()
			.then((res) => {
				const dto = res?.data ?? res;
				if (!cancelled) setQueue(dto);
			})
			.catch(() => {
				if (!cancelled) message.error('처리 대기 목록을 불러오지 못했습니다.');
			})
			.finally(() => {
				if (!cancelled) setLoading(false);
			});
		return () => {
			cancelled = true;
		};
	}, [role]);

	if (role !== 'ADMIN') {
		return null;
	}

	const go = (path) => () => navigate(path);

	const card = (title, path, accent, children) => (
		<AntdCard
			size="small"
			style={cardShellStyle(accent)}
			title={<span className="font-weight-semibold">{title}</span>}
			extra={
				<Button type="link" size="small" onClick={go(path)}>
					바로가기
				</Button>
			}
		>
			{children}
		</AntdCard>
	);

	const p = queue?.pendingPartnerApplications ?? 0;
	const prod = (queue?.productsPendingNewApproval ?? 0) + (queue?.productsPendingUpdateApproval ?? 0);
	const opt = (queue?.optionsPendingNewApproval ?? 0) + (queue?.optionsPendingUpdateApproval ?? 0);
	const ret = queue?.returnsPendingProcessing ?? 0;
	const pendPay = queue?.ordersPendingPayment ?? 0;
	const failPay = queue?.ordersPaymentFailed ?? 0;
	const confirm = queue?.orderItemsPendingConfirmation ?? 0;
	const delay = queue?.deliveriesReadyDelayed ?? 0;

	return (
		<div className="mb-4">
			<h4 className="mb-2">처리 대기</h4>
			{loading && !queue ? (
				<div className="text-center py-5">
					<Spin />
				</div>
			) : (
				<Row gutter={[16, 16]}>
					<Col xs={24} sm={12} lg={8}>
						{card(
							'입점 승인 대기',
							`${APP_PREFIX_PATH}/apps/admin/partner-approval`,
							queueAccent(p, { warn: 1, danger: 5 }),
							<Statistic value={p} valueStyle={queueValueStyle(queueAccent(p, { warn: 1, danger: 5 }))} />
						)}
					</Col>
					<Col xs={24} sm={12} lg={8}>
						{card(
							'상품 승인 대기',
							`${APP_PREFIX_PATH}/apps/admin/product-approval?approval=all`,
							queueAccent(prod, { warn: 3, danger: 25 }),
							<>
								<Statistic
									value={prod}
									valueStyle={queueValueStyle(queueAccent(prod, { warn: 3, danger: 25 }))}
								/>
								<div className="text-gray-light font-size-sm mt-2">
									신규 {queue?.productsPendingNewApproval ?? 0} · 수정 {queue?.productsPendingUpdateApproval ?? 0}
								</div>
							</>
						)}
					</Col>
					<Col xs={24} sm={12} lg={8}>
						{card(
							'옵션 승인 대기',
							`${APP_PREFIX_PATH}/apps/admin/product-approval?approval=all`,
							queueAccent(opt, { warn: 3, danger: 25 }),
							<>
								<Statistic
									value={opt}
									valueStyle={queueValueStyle(queueAccent(opt, { warn: 3, danger: 25 }))}
								/>
								<div className="text-gray-light font-size-sm mt-2">
									신규 {queue?.optionsPendingNewApproval ?? 0} · 수정 {queue?.optionsPendingUpdateApproval ?? 0}
								</div>
							</>
						)}
					</Col>
					<Col xs={24} sm={12} lg={8}>
						{card(
							'반품·교환 처리 중',
							`${APP_PREFIX_PATH}/apps/return?filter=processing`,
							queueAccent(ret, { warn: 1, danger: 12 }),
							<Statistic value={ret} valueStyle={queueValueStyle(queueAccent(ret, { warn: 1, danger: 12 }))} />
						)}
					</Col>
					<Col xs={24} sm={12} lg={8}>
						{card(
							'결제 대기 주문',
							`${APP_PREFIX_PATH}/apps/order?filter=PENDING_PAYMENT`,
							queueAccent(pendPay, { warn: 1, danger: 15 }),
							<Statistic value={pendPay} valueStyle={queueValueStyle(queueAccent(pendPay, { warn: 1, danger: 15 }))} />
						)}
					</Col>
					<Col xs={24} sm={12} lg={8}>
						{card(
							'결제 실패 주문',
							`${APP_PREFIX_PATH}/apps/order?filter=PAYMENT_FAILED`,
							queueAccent(failPay, { warn: 1, danger: 1 }),
							<Statistic value={failPay} valueStyle={queueValueStyle(queueAccent(failPay, { warn: 1, danger: 1 }))} />
						)}
					</Col>
					<Col xs={24} sm={12} lg={8}>
						{card(
							'발주 확인 대기',
							`${APP_PREFIX_PATH}/apps/order?filter=PENDING_CONFIRMATION`,
							queueAccent(confirm, { warn: 8, danger: 40 }),
							<Statistic value={confirm} valueStyle={queueValueStyle(queueAccent(confirm, { warn: 8, danger: 40 }))} />
						)}
					</Col>
					<Col xs={24} sm={12} lg={8}>
						{card(
							'출고 지연 의심',
							`${APP_PREFIX_PATH}/apps/order?filter=DELIVERY_DELAY`,
							queueAccent(delay, { warn: 1, danger: 6 }),
							<>
								<Statistic value={delay} valueStyle={queueValueStyle(queueAccent(delay, { warn: 1, danger: 6 }))} />
								<div className="text-gray-light font-size-sm mt-2">
									발주 확인 후 배송준비(READY) {queue?.deliveriesReadyDelayedDaysThreshold ?? 3}일 초과
								</div>
							</>
						)}
					</Col>
				</Row>
			)}
		</div>
	);
};

const formatWon = (n) => {
	const v = Number(n);
	if (Number.isNaN(v)) return '-';
	return `${v.toLocaleString('ko-KR')}원`;
};

/** 디폴트용 요약 + 최근 일주일 매출 미니 차트 */
const AdminHomeKpis = () => {
	const role = useSelector((state) => state.auth.user?.role);
	const [health, setHealth] = useState(null);
	const [dailyTrend, setDailyTrend] = useState([]);
	const [loading, setLoading] = useState(false);

	useEffect(() => {
		if (role !== 'ADMIN') {
			return undefined;
		}
		let cancelled = false;
		setLoading(true);
		Promise.all([AdminService.getDashboardHealth(), AdminService.getDashboardInsights({ days: 14 })])
			.then(([hRes, iRes]) => {
				if (cancelled) return;
				setHealth(hRes?.data ?? hRes);
				const ins = iRes?.data ?? iRes;
				setDailyTrend(ins?.dailyPaidTrend || []);
			})
			.catch(() => {
				if (!cancelled) message.error('요약·추이를 불러오지 못했습니다.');
			})
			.finally(() => {
				if (!cancelled) setLoading(false);
			});
		return () => {
			cancelled = true;
		};
	}, [role]);

	const last7Trend = useMemo(() => {
		const arr = dailyTrend || [];
		if (arr.length <= 7) {
			return arr;
		}
		return arr.slice(-7);
	}, [dailyTrend]);

	const miniChart = useMemo(() => {
		const t = last7Trend;
		const categories = t.map((p) => String(p.date || '').slice(5));
		const rev = t.map((p) => Number(p.paidRevenueKrw) || 0);
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
	}, [last7Trend]);

	if (role !== 'ADMIN') {
		return null;
	}

	return (
		<div className="mb-4">
			<h4 className="mb-2">오늘 요약</h4>
			<p className="text-gray-light font-size-sm mb-3">
				더 자세한 정보는 <strong>매출·운영 상세</strong>에서 확인하세요.
			</p>
			{loading && !health ? (
				<div className="text-center py-4">
					<Spin />
				</div>
			) : (
				<>
					<AntdCard
						size="small"
						title="오늘 · 최근 일주일"
						extra={
							<Link to={`${APP_PREFIX_PATH}/dashboards/analytic`}>매출·운영 상세 →</Link>
						}
					>
						<Row gutter={[16, 16]}>
							<Col xs={12} sm={6}>
								<Statistic title="오늘 결제 주문" value={health?.paidOrdersTodayCount ?? 0} suffix="건" />
							</Col>
							<Col xs={12} sm={6}>
								<Statistic title="오늘 매출" value={formatWon(health?.paidRevenueTodayKrw)} />
							</Col>
							<Col xs={12} sm={6}>
								<Statistic title="최근 일주일 매출" value={formatWon(health?.paidRevenueLast7DaysKrw)} />
								<div className="text-gray-light font-size-sm mt-1">주문 {health?.paidOrdersLast7DaysCount ?? 0}건</div>
							</Col>
							<Col xs={12} sm={6}>
								<Statistic title="신규 가입(일주일)" value={health?.newCustomersLast7DaysCount ?? 0} suffix="명" />
								<div className="text-gray-light font-size-sm mt-1">오늘 {health?.newCustomersTodayCount ?? 0}명</div>
							</Col>
						</Row>
					</AntdCard>

					<AntdCard size="small" title="최근 일주일 매출 추이" className="mt-3">
						{last7Trend.length === 0 ? (
							<div className="text-gray-light text-center py-4">표시할 결제 데이터가 없습니다.</div>
						) : (
							<ApexChart options={miniChart.options} series={miniChart.series} height={220} type="area" />
						)}
					</AntdCard>
				</>
			)}
		</div>
	);
};

export const DefaultDashboard = () => (
	<>
		<PartnerTodayOperations />
		<AdminProcessingQueue />
		<AdminHomeKpis />
	</>
);

export default DefaultDashboard;
