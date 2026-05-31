import React, { useEffect, useState } from 'react';
import dayjs from 'dayjs';
import { Card, Table, Button, Space, Tag, DatePicker, Drawer, Row, Col, Statistic, message } from 'antd';
import Chart from 'react-apexcharts';
import { useLocation } from 'react-router-dom';
import PartnerService from 'services/PartnerService';

const { RangePicker } = DatePicker;

const formatDate = (dt) => {
	if (!dt) return '-';
	const d = dayjs(dt);
	return d.isValid() ? d.format('YYYY-MM-DD') : '-';
};

const PartnerEventPerformance = () => {
	const location = useLocation();
	const [loading, setLoading] = useState(false);
	const [rows, setRows] = useState([]);
	const [from, setFrom] = useState(null);
	const [to, setTo] = useState(null);
	const [selected, setSelected] = useState(null);
	const [open, setOpen] = useState(false);
	const [detail, setDetail] = useState(null);
	const [detailLoading, setDetailLoading] = useState(false);
	const [tsData, setTsData] = useState([]);
	const [topType, setTopType] = useState('product');
	const [topData, setTopData] = useState([]);
	const [wide, setWide] = useState(false);
	const latestEnded = React.useMemo(() => {
		if (!rows || rows.length === 0) return null;
		const sorted = [...rows].sort((a, b) => {
			const aEnd = a?.customerEventEndAt ? new Date(a.customerEventEndAt).getTime() : 0;
			const bEnd = b?.customerEventEndAt ? new Date(b.customerEventEndAt).getTime() : 0;
			return bEnd - aEnd;
		});
		return sorted[0] || null;
	}, [rows]);
	const latest5 = React.useMemo(() => {
		if (!rows || rows.length === 0) return [];
		const endedSorted = [...rows].sort((a, b) => {
			const aEnd = a?.customerEventEndAt ? new Date(a.customerEventEndAt).getTime() : 0;
			const bEnd = b?.customerEventEndAt ? new Date(b.customerEventEndAt).getTime() : 0;
			return bEnd - aEnd;
		});
		return endedSorted.slice(0, 5);
	}, [rows]);

	const fetchList = async (all = false) => {
		try {
			setLoading(true);
			const fromStr = !all && from ? dayjs(from).format('YYYY-MM-DDTHH:mm:ss') : null;
			const toStr = !all && to ? dayjs(to).format('YYYY-MM-DDTHH:mm:ss') : null;
			const resp = await PartnerService.getPartnerEndedEventPerformanceList(fromStr, toStr);
			const data = resp?.data ?? resp ?? [];
			setRows(Array.isArray(data) ? data : []);
		} catch (e) {
			console.error(e);
			message.error(e?.response?.data?.message || '이벤트 실적 목록 조회 실패');
			setRows([]);
		} finally {
			setLoading(false);
		}
	};

	const fetchDetail = async (eventNo, fromVal, toVal) => {
		try {
			setDetailLoading(true);
			const fromStr = fromVal ? dayjs(fromVal).format('YYYY-MM-DDTHH:mm:ss') : null;
			const toStr = toVal ? dayjs(toVal).format('YYYY-MM-DDTHH:mm:ss') : null;
			const resp = await PartnerService.getPartnerEventPerformance(eventNo, fromStr, toStr);
			const data = resp?.data ?? resp;
			setDetail(data);
			// 차트 데이터 로드
			const [tsResp, topResp] = await Promise.all([
				PartnerService.getPartnerEventTimeseries({ eventNo, from: fromStr, to: toStr }),
				PartnerService.getPartnerEventTop(topType, { eventNo, from: fromStr, to: toStr, limit: 10 })
			]);
			setTsData(tsResp?.data ?? tsResp ?? []);
			setTopData(topResp?.data ?? topResp ?? []);
		} catch (e) {
			console.error(e);
			message.error(e?.response?.data?.message || '이벤트 실적 조회 실패');
			setDetail(null);
			setTsData([]);
			setTopData([]);
		} finally {
			setDetailLoading(false);
		}
	};


	useEffect(() => {
		fetchList(true);
	// eslint-disable-next-line react-hooks/exhaustive-deps
	}, []);

	useEffect(() => {
		const eventNo = location.state?.eventNo;
		if (eventNo) {
			setOpen(true);
			setDetail(null);
			fetchDetail(eventNo, null, null);
		}
	// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [location.state]);

	return (
		<div>
			<Card
				title="이벤트 실적(종료) - 파트너"
				extra={
					<Space>
						{selected ? <Tag color="blue">선택: #{selected.eventNo} {selected.eventTitle}</Tag> : <Tag>전체 종료 이벤트</Tag>}
						{selected && <Button onClick={() => setSelected(null)}>선택 해제</Button>}
						<RangePicker
							placeholder={['시작일', '종료일']}
							value={from && to ? [from, to] : []}
							onChange={(vals) => {
								if (vals && vals.length === 2) {
									setFrom(vals[0]);
									setTo(vals[1]);
								} else {
									setFrom(null);
									setTo(null);
								}
							}}
						/>
						<Button type="primary" loading={loading} onClick={() => fetchList()}>
							조회
						</Button>
						<Button onClick={() => { setFrom(null); setTo(null); fetchList(true); }}>
							전체 기간
						</Button>
					</Space>
				}
			>
				<Card size="small" style={{ marginBottom: 16 }}>
					{(() => {
						const totals = selected
							? {
									totalOrders: selected.totalOrders || 0,
									totalOrderItems: selected.totalOrderItems || 0,
									totalNetAmount: selected.totalNetAmount || 0,
									adminRewardPoint: 0,
									partnerRewardPoint: selected.partnerRewardPoint || 0
							  }
							: rows.reduce(
									(acc, r) => ({
										totalOrders: acc.totalOrders + (r.totalOrders || 0),
										totalOrderItems: acc.totalOrderItems + (r.totalOrderItems || 0),
										totalNetAmount: acc.totalNetAmount + (r.totalNetAmount || 0),
										adminRewardPoint: 0,
										partnerRewardPoint: acc.partnerRewardPoint + (r.partnerRewardPoint || 0)
									}),
									{ totalOrders: 0, totalOrderItems: 0, totalNetAmount: 0, adminRewardPoint: 0, partnerRewardPoint: 0 }
							  );
						return (
							<>
								<Row gutter={[16, 16]}>
									<Col xs={24}>
										<Card size="small" title="최근 참여 5개 이벤트 매출">
											<Chart
												type="line"
												height={280}
												series={[{ name: '매출(원)', data: latest5.map(e => e.totalNetAmount || 0) }]}
												options={{
													stroke: { curve: 'smooth', width: 3 },
													markers: { size: 3 },
													xaxis: {
														categories: latest5.map(e => {
															const title = e.eventTitle || '';
															return title.length > 14 ? `${title.slice(0, 14)}…` : title;
														})
													},
													yaxis: {
														tickAmount: 4,
														labels: {
															formatter: (val) => {
																const n = Number(val || 0);
																return Math.abs(n) >= 100000
																	? new Intl.NumberFormat('ko-KR', { notation: 'compact' }).format(n)
																	: new Intl.NumberFormat('ko-KR').format(n);
															},
														},
													},
													grid: { yaxis: { lines: { show: true } }, xaxis: { lines: { show: false } } },
													dataLabels: { enabled: false },
													tooltip: {
														y: { formatter: (v) => new Intl.NumberFormat('ko-KR').format(Number(v || 0)) + ' 원' }
													}
												}}
											/>
										</Card>
									</Col>
								</Row>
								<Row gutter={[16, 16]}>
									<Col xs={12} md={6}><Statistic title="주문 수" value={totals.totalOrders} /></Col>
									<Col xs={12} md={6}><Statistic title="주문상품 수" value={totals.totalOrderItems} /></Col>
									<Col xs={24} md={12}><Statistic title="총 매출(원)" value={totals.totalNetAmount} /></Col>
								</Row>
								<Row gutter={[16, 16]} style={{ marginTop: 8 }}>
									<Col xs={24} md={12}>
										<Card size="small" title="조회 기간 매출">
											<div style={{ fontSize: 22, fontWeight: 700 }}>
												{(totals.totalNetAmount || 0).toLocaleString()} 원
											</div>
										</Card>
									</Col>
									<Col xs={24} md={12}>
										<Card size="small" title="조회 기간 보상(파트너)">
											<div style={{ fontSize: 22, fontWeight: 700 }}>
												{(totals.partnerRewardPoint || 0).toLocaleString()} P
											</div>
										</Card>
									</Col>
								</Row>
								<Row gutter={[16, 16]} style={{ marginTop: 8 }}>
									<Col xs={24}>
										<Card size="small" title="최근 종료 이벤트 매출">
											{latestEnded ? (
												<Space direction="vertical" size={4}>
													<div style={{ fontSize: 20, fontWeight: 700 }}>
														{(latestEnded.totalNetAmount || 0).toLocaleString()} 원
													</div>
													<div style={{ color: '#666' }}>
														#{latestEnded.eventNo} {latestEnded.eventTitle || ''} · {formatDate(latestEnded.customerEventStartAt)} ~ {formatDate(latestEnded.customerEventEndAt)}
													</div>
												</Space>
											) : (
												<div style={{ color: '#888' }}>최근 종료 이벤트가 없습니다.</div>
											)}
										</Card>
									</Col>
								</Row>
							</>
						);
					})()}
				</Card>

				<Table
					rowKey="eventNo"
					loading={loading}
					dataSource={rows}
					columns={[
						{ title: '번호', dataIndex: 'eventNo', width: 90 },
						{ title: '이벤트명', dataIndex: 'eventTitle', ellipsis: true },
						{ title: '상태', dataIndex: 'eventStatus', width: 110, render: (s) => <Tag>{s}</Tag> },
						{ title: '기간', width: 260, render: (_, r) => `${formatDate(r.customerEventStartAt)} ~ ${formatDate(r.customerEventEndAt)}` },
						{ title: '주문수', dataIndex: 'totalOrders', width: 100 },
						{ title: '주문상품수', dataIndex: 'totalOrderItems', width: 110 },
						{ title: '총 매출(원)', dataIndex: 'totalNetAmount', width: 140, render: (v) => (v || 0).toLocaleString() },
						{ title: '파트너 보상', dataIndex: 'partnerRewardPoint', width: 120, render: (v) => (v || 0).toLocaleString() },
						{ title: '작업', width: 120, render: (_, r) => <Button type="link" onClick={() => { setOpen(true); setDetail(null); fetchDetail(r.eventNo, null, null); }}>상세</Button> }
					]}
					onRow={(record) => ({
						onClick: () => setSelected(record),
					})}
					rowClassName={(record) => (selected?.eventNo === record.eventNo ? 'ant-table-row-selected' : '')}
					pagination={{ pageSize: 10, showSizeChanger: true }}
				/>
			</Card>

			<Drawer
				title={`이벤트 실적(파트너) ${detail?.eventNo ? `(#${detail.eventNo})` : ''}`}
				open={open}
				onClose={() => setOpen(false)}
				width={wide ? '90%' : 720}
				extra={
					<Button size="small" onClick={() => setWide((v) => !v)}>
						{wide ? '축소' : '확대'}
					</Button>
				}
				destroyOnClose
			>
				<Space direction="vertical" style={{ width: '100%' }} size={16}>
					<Space wrap>
						<RangePicker
							placeholder={['시작일', '종료일']}
							onChange={(vals) => {
								if (vals && vals.length === 2) {
									fetchDetail(detail?.eventNo, vals[0], vals[1]);
								} else {
									fetchDetail(detail?.eventNo, null, null);
								}
							}}
						/>
					</Space>

					<Card loading={detailLoading}>
						<Row gutter={[16, 16]}>
							<Col xs={12} md={8}><Statistic title="주문 수" value={detail?.totalOrders || 0} /></Col>
							<Col xs={12} md={8}><Statistic title="주문상품 수" value={detail?.totalOrderItems || 0} /></Col>
							<Col xs={24} md={8}><Statistic title="총 매출(원)" value={detail?.totalNetAmount || 0} /></Col>
							<Col xs={24} md={12}><Statistic title="파트너 보상 포인트" value={detail?.partnerRewardPoint || 0} /></Col>
							<Col xs={24} md={12}>
								<Card size="small" title="보상/매출 비율(파트너)">
									<div style={{ fontSize: 22, fontWeight: 700 }}>
										{detail?.totalNetAmount > 0
											? `${Math.round(((detail?.partnerRewardPoint || 0) / detail?.totalNetAmount) * 1000) / 10}%`
											: '0%'}
									</div>
								</Card>
							</Col>
						</Row>
						<Row gutter={[16, 16]} style={{ marginTop: 8 }}>
							<Col xs={24}>
								<Card size="small" title="일자별 추이(주문/매출)">
									<Chart
										type="area"
										height={wide ? 320 : 260}
										series={[
											{ name: '주문 수', data: (tsData || []).map(p => ({ x: p.date, y: p.orders })) },
											{ name: '매출(원)', data: (tsData || []).map(p => ({ x: p.date, y: p.netAmount })) },
										]}
										options={{
											dataLabels: { enabled: false },
											stroke: { curve: 'smooth' },
											xaxis: { type: 'datetime' },
											yaxis: [
												{ labels: { formatter: (v) => new Intl.NumberFormat('ko-KR').format(Number(v||0)) } },
												{ opposite: true, labels: { formatter: (v) => {
													const n = Number(v||0);
													return Math.abs(n) >= 100000 ? new Intl.NumberFormat('ko-KR', { notation: 'compact' }).format(n) : new Intl.NumberFormat('ko-KR').format(n);
												}}}
											]
										}}
									/>
								</Card>
							</Col>
						</Row>
						<Row gutter={[16, 16]} style={{ marginTop: 8 }}>
							<Col xs={24}>
								<Card
									size="small"
									title={`Top 10 ${topType === 'partner' ? '파트너' : '상품'}`}
									extra={
										<Space size={4}>
											<Button size="small" type={topType === 'product' ? 'primary' : 'default'} onClick={async () => {
												setTopType('product');
												const fromStr = from ? dayjs(from).format('YYYY-MM-DDTHH:mm:ss') : null;
												const toStr = to ? dayjs(to).format('YYYY-MM-DDTHH:mm:ss') : null;
												const topResp = await PartnerService.getPartnerEventTop('product', { eventNo: detail?.eventNo, from: fromStr, to: toStr, limit: 10 });
												setTopData(topResp?.data ?? topResp ?? []);
											}}>상품</Button>
											<Button size="small" type={topType === 'partner' ? 'primary' : 'default'} onClick={async () => {
												setTopType('partner');
												const fromStr = from ? dayjs(from).format('YYYY-MM-DDTHH:mm:ss') : null;
												const toStr = to ? dayjs(to).format('YYYY-MM-DDTHH:mm:ss') : null;
												const topResp = await PartnerService.getPartnerEventTop('partner', { eventNo: detail?.eventNo, from: fromStr, to: toStr, limit: 10 });
												setTopData(topResp?.data ?? topResp ?? []);
											}}>파트너</Button>
										</Space>
									}
								>
									<Chart
										type="bar"
										height={wide ? 320 : 260}
										series={[{ name: '매출(원)', data: (topData || []).map(it => it.netAmount) }]}
										options={{
											plotOptions: { bar: { horizontal: true } },
											xaxis: {
												labels: {
													formatter: (v) => {
														const n = Number(v||0);
														return Math.abs(n) >= 100000 ? new Intl.NumberFormat('ko-KR', { notation: 'compact' }).format(n) : new Intl.NumberFormat('ko-KR').format(n);
													}
												}
											},
											yaxis: { categories: (topData || []).map(it => it.name?.length > 22 ? `${it.name.slice(0, 22)}…` : it.name) },
											dataLabels: { enabled: false },
											legend: { show: false }
										}}
									/>
								</Card>
							</Col>
						</Row>
					</Card>
				</Space>
			</Drawer>
		</div>
	);
};

export default PartnerEventPerformance;

