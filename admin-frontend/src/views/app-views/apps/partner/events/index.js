import React, { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Card, Modal, Space, Spin, Table, Tag, Typography, message, Tabs } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { useSelector } from 'react-redux';
import dayjs from 'dayjs';
import { useNavigate } from 'react-router-dom';
import PartnerService from 'services/PartnerService';
import PartnerEventPerformance from '../event-performance';
import { APP_PREFIX_PATH } from 'configs/AppConfig';

const { Text } = Typography;

const STATUS_LABEL = {
	DRAFT: '임시저장',
	SCHEDULED: '예정',
	ACTIVE: '진행중',
	INACTIVE: '중지',
	ENDED: '종료',
};

const STATUS_PRIORITY = {
	ACTIVE: 0,
	SCHEDULED: 1,
	ENDED: 2,
};

const EVENT_TYPE_LABEL = {
	SALE: '세일',
	GENERAL: '일반',
	POINT: '포인트',
	ATTENDANCE: '출석',
	COUPON: '쿠폰',
};

const DISCOUNT_TYPE_LABEL = {
	PERCENT: '정률(%)',
	FIXED: '정액(원)',
};

const CATEGORY_KO = {
  SWIMSUIT_MEN: '남성 수영복',
  SWIMSUIT_WOMEN: '여성 수영복',
  SWIMSUIT_KIDS: '아동 수영복',
  SWIM_CAP: '수영모자',
  SWIM_GOGGLES: '수영안경',
  FINS: '오리발',
  SWIM_TOY: '수영용품',
  ETC: '기타',
};
const toKoCategory = (code) => CATEGORY_KO[code] || code;
const formatDateOnly = (value) => {
	if (!value) return '-';
	return dayjs(value).format('YYYY-MM-DD');
};

const normalizeData = (res) => res?.data ?? res ?? [];

const getApplyWindowState = (event) => {
	const now = dayjs();
	const start = event?.partnerApplyStartAt ? dayjs(event.partnerApplyStartAt) : null;
	const end = event?.partnerApplyEndAt ? dayjs(event.partnerApplyEndAt) : null;

	if (!start || !end || !start.isValid() || !end.isValid()) {
		return { canToggle: false, label: '기간 미설정' };
	}
	if (now.isBefore(start)) {
		return { canToggle: false, label: '신청 시작 전' };
	}
	if (now.isAfter(end)) {
		return { canToggle: false, label: '신청 마감' };
	}
	return { canToggle: true, label: '신청 가능' };
};

const PartnerEvents = () => {
	const { user } = useSelector((state) => state.auth);
	const navigate = useNavigate();
	const [loading, setLoading] = useState(false);
	const [actionLoading, setActionLoading] = useState({});
	const [events, setEvents] = useState([]);
	const [currentPage, setCurrentPage] = useState(1);
	const [pageSize, setPageSize] = useState(10);
	const [totalItems, setTotalItems] = useState(0);
	const [selectedEvent, setSelectedEvent] = useState(null);
	const [statusFilter, setStatusFilter] = useState('ALL');

	const partnerEvents = useMemo(
		() =>
			events
				.filter((event) => event?.eventMode === 'PARTNER_PARTICIPATION')
				.filter((event) => statusFilter === 'ALL' || event?.eventStatus === statusFilter)
				.sort((a, b) => {
					const aPriority = STATUS_PRIORITY[a?.eventStatus] ?? 99;
					const bPriority = STATUS_PRIORITY[b?.eventStatus] ?? 99;
					if (aPriority !== bPriority) return aPriority - bPriority;

					// 종료 이벤트는 최근 종료순, 그 외는 시작일 기준 오름차순(가까운 일정 우선)
					if (a?.eventStatus === 'ENDED' && b?.eventStatus === 'ENDED') {
						const aEnd = a?.customerEventEndAt ? new Date(a.customerEventEndAt).getTime() : 0;
						const bEnd = b?.customerEventEndAt ? new Date(b.customerEventEndAt).getTime() : 0;
						return bEnd - aEnd;
					}
					const aStart = a?.customerEventStartAt ? new Date(a.customerEventStartAt).getTime() : 0;
					const bStart = b?.customerEventStartAt ? new Date(b.customerEventStartAt).getTime() : 0;
					return aStart - bStart;
				}),
		[events, statusFilter]
	);

	const loadEvents = async () => {
		setLoading(true);
		try {
			console.log('[Partner Events] request params =>', { page: currentPage, size: pageSize });
			const res = await PartnerService.getPartnerVisibleEvents({ page: currentPage, size: pageSize });
			const data = res?.data ?? res;
			// 서버 표준: data.events + meta. 호환: items / 배열
			const meta = data?.meta;
			let items = Array.isArray(data) ? data : (data?.events ?? data?.items ?? []);
			let total = Array.isArray(data) ? data.length : (meta?.total ?? data?.total ?? 0);
			let respPage = Array.isArray(data) ? currentPage : (((meta?.page ?? data?.page) ?? 0) + 1);
			let respSize = Array.isArray(data) ? pageSize : (meta?.size ?? data?.size ?? pageSize);
			console.log('[Partner Events] response meta =>', { page: meta?.page ?? data?.page, size: respSize, total, itemsCount: Array.isArray(items) ? items.length : 0 });
			setEvents(Array.isArray(items) ? items : []);
			setTotalItems(Number(total) || 0);
			setCurrentPage(respPage);
			setPageSize(respSize);
		} catch (err) {
			console.error('파트너 이벤트 목록 조회 실패:', err);
			message.warning(err?.response?.data?.message || '이벤트 목록 조회에 실패했습니다.');
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		loadEvents();
	}, [currentPage, pageSize]);

	const handleToggleParticipation = async (eventNo, nextParticipating) => {
		setActionLoading((prev) => ({ ...prev, [eventNo]: true }));
		try {
			const targetEvent = (events || []).find((e) => e?.eventNo === eventNo);
			const isSaleEvent = targetEvent?.eventType === 'SALE';
			const isPointEvent = targetEvent?.eventType === 'POINT';

			if (nextParticipating) {
				await PartnerService.participateEvent(eventNo);
				// 참여 완료 안내는 토스트 대신 모달/상세로 제공
				const typeLabel =
					targetEvent?.pointEventTargetType === 'PARTNER' ? '파트너 대상' :
					targetEvent?.pointEventTargetType === 'PRODUCT' ? '상품 지정 대상' :
					targetEvent?.pointEventTargetType === 'OPTION' ? '옵션 지정 대상' :
					targetEvent?.pointEventTargetType === 'CATEGORY' ? '카테고리 지정 대상' :
					targetEvent?.pointEventTargetType === 'MIN_ORDER_AMOUNT' ? '최소 주문금액 기준' :
					'전체 대상';

				if (isSaleEvent) {
					const applyEndLabel = targetEvent?.partnerApplyEndAt
						? dayjs(targetEvent.partnerApplyEndAt).format('YYYY-MM-DD')
						: null;
					Modal.info({
						title: '세일 등록 안내',
						content: applyEndLabel
							? `참여가 완료되었습니다. ${applyEndLabel}까지 세일 등록 페이지에서 상품/옵션을 선택해 세일을 등록해주세요.`
							: '참여가 완료되었습니다. 세일 등록 페이지로 가서 상품/옵션을 선택해주세요.',
						okText: '확인',
						onOk: () => {
							navigate(`${APP_PREFIX_PATH}/partner/sales`);
						}
					});
				} else if (isPointEvent) {
					// POINT 안내 모달: 간단 안내(상세 고정 배너가 있으므로 축약)
					const targetType = targetEvent?.pointEventTargetType;
					const typeLabel2 =
						targetType === 'PARTNER' ? '파트너 대상' :
						targetType === 'PRODUCT' ? '상품 지정 대상' :
						targetType === 'OPTION' ? '옵션 지정 대상' :
						targetType === 'CATEGORY' ? '카테고리 지정 대상' :
						targetType === 'MIN_ORDER_AMOUNT' ? '최소 주문금액 기준' :
						'전체 대상';

					Modal.info({
						title: '참여 완료',
						content: (
							<Space direction="vertical" size={6}>
								<span>{`[${targetEvent?.eventTitle || '이벤트'}]에 참여하셨습니다.`}</span>
								<span>{`${typeLabel2}이 자동으로 적용됩니다.`}</span>
							</Space>
						),
						okText: '확인'
					});
				}
			} else {
				if (isSaleEvent) {
					const ok = await new Promise((resolve) => {
						Modal.confirm({
							title: '이벤트 참여 해제 확인',
							content: (
								<Space direction="vertical" size={8}>
									<span>{`${targetEvent?.eventTitle || '이벤트'}의 연동 세일도 자동으로 비활성 처리됩니다.`}</span>
								</Space>
							),
							okText: '해제하기',
							cancelText: '취소',
							onOk: () => resolve(true),
							onCancel: () => resolve(false)
						});
					});
					if (!ok) return;
					await PartnerService.cancelEventParticipation(eventNo, { deactivateLinkedSales: true });
				} else {
					await PartnerService.cancelEventParticipation(eventNo);
				}
				message.success('이벤트 참여가 해제되었습니다.');
			}

			// SALE 이벤트는 `participating` 값이 "실제 sale_policy 존재 여부"에 의해 결정됩니다.
			// 토글 직후 프론트에서 임의로 true/false를 넣지 않고, 서버 계산값으로 다시 동기화합니다.
			if (isSaleEvent) {
				await loadEvents();
			} else {
				setEvents((prev) =>
					prev.map((event) =>
						event.eventNo === eventNo ? { ...event, participating: nextParticipating } : event
					)
				);
			}
		} catch (err) {
			message.warning(err?.response?.data?.message || '요청 처리에 실패했습니다.');
		} finally {
			setActionLoading((prev) => ({ ...prev, [eventNo]: false }));
		}
	};

	if (user && user.role !== 'PARTNER') {
		return (
			<Card>
				<Alert
					message="접근 권한 없음"
					description="이 페이지는 파트너만 접근할 수 있습니다."
					type="error"
					showIcon
				/>
			</Card>
		);
	}

	const columns = [
		{
			title: '이벤트명',
			dataIndex: 'eventTitle',
			key: 'eventTitle',
			width: 280,
			render: (value, record) => (
				<Button type="link" style={{ padding: 0 }} onClick={() => setSelectedEvent(record)}>
					{value || '-'}
				</Button>
			),
		},
		// 상태 뱃지는 유지 (간단 표시)
		{
			title: '상태',
			dataIndex: 'eventStatus',
			key: 'eventStatus',
			width: 100,
			render: (status) => <Tag>{STATUS_LABEL[status] || status || '-'}</Tag>,
		},
		// 기간/유형/작업 컬럼은 목록에서 비표시 (요청 반영)
		// 상세 모달에서 기간/유형/신청/해제 가능
		{
			title: '파트너 신청 기간',
			key: 'partnerPeriod',
			width: 220,
			render: (_, record) =>
				`${formatDateOnly(record.partnerApplyStartAt)} ~ ${formatDateOnly(record.partnerApplyEndAt)}`,
		},
		{
			title: '참여 상태',
			key: 'participating',
			width: 180,
			render: (_, record) => {
				const saleBadge =
					record.eventType === 'SALE' && record.participationEnabled ? (
						<Tag color={record.participating ? 'blue' : 'default'}>
							{record.participating ? '세일 등록됨' : '세일 미등록'}
						</Tag>
					) : null;

				let participationTag;
				if (record.participationEnabled) {
					participationTag = <Tag color="green">참여중</Tag>;
				} else {
					const windowState = getApplyWindowState(record);
					participationTag = windowState.canToggle ? <Tag color="blue">신청 가능</Tag> : <Tag>미참여</Tag>;
				}

				// 대상 요약 배지
				const t = record?.pointEventTargetType;
				const targetLabel =
					t === 'PARTNER' ? '파트너 대상' :
					t === 'PRODUCT' ? '상품 지정' :
					t === 'OPTION' ? '옵션 지정' :
					t === 'CATEGORY' ? '카테고리' :
					t === 'MIN_ORDER_AMOUNT' ? '최소주문금액' :
					(record?.eventType === 'SALE' ? 'SALE' : '전체');
				const targetBadge = <Tag color="default">{targetLabel}</Tag>;

				return saleBadge ? <Space size={6}>{participationTag}{saleBadge}{targetBadge}</Space> : <Space size={6}>{participationTag}{targetBadge}</Space>;
			},
		},
		// 필요 시 별도 컬럼으로 분리하려면 아래 주석 해제
		// {
		// 	title: '대상',
		// 	key: 'targetSummary',
		// 	width: 140,
		// 	render: (_, record) => {
		// 		const t = record?.pointEventTargetType;
		// 		const label =
		// 			t === 'PARTNER' ? '파트너 대상' :
		// 			t === 'PRODUCT' ? '상품 지정' :
		// 			t === 'OPTION' ? '옵션 지정' :
		// 			t === 'CATEGORY' ? '카테고리' :
		// 			t === 'MIN_ORDER_AMOUNT' ? '최소주문금액' :
		// 			(record?.eventType === 'SALE' ? 'SALE' : '전체');
		// 		return <Tag>{label}</Tag>;
		// 	}
		// },
	];

	return (
		<Tabs
			defaultActiveKey="participation"
			items={[
				{
					key: 'participation',
					label: '이벤트 참여',
					children: (
						<>
							<Card
								title="파트너 이벤트 참여"
								extra={
									<Button icon={<ReloadOutlined />} onClick={loadEvents} loading={loading}>
										새로고침
									</Button>
								}
							>
								<Space direction="vertical" size={12} style={{ width: '100%' }}>
									<Space size={8}>
										<Button type={statusFilter === 'ALL' ? 'primary' : 'default'} onClick={() => setStatusFilter('ALL')}>전체</Button>
										<Button type={statusFilter === 'ACTIVE' ? 'primary' : 'default'} onClick={() => setStatusFilter('ACTIVE')}>진행중</Button>
										<Button type={statusFilter === 'SCHEDULED' ? 'primary' : 'default'} onClick={() => setStatusFilter('SCHEDULED')}>예정</Button>
										<Button type={statusFilter === 'ENDED' ? 'primary' : 'default'} onClick={() => setStatusFilter('ENDED')}>종료</Button>
									</Space>
									{loading ? <Spin /> : (
										<>
											<div style={{ marginBottom: 8, color: '#666' }}>
												{`총 ${totalItems}건 • 페이지 ${currentPage}/${Math.max(1, Math.ceil(totalItems / pageSize))}`}
											</div>
											<Table
												rowKey="eventNo"
												columns={columns}
												dataSource={partnerEvents}
												pagination={{
													current: currentPage,
													pageSize: pageSize,
													total: totalItems,
													showSizeChanger: true,
													showTotal: (t) => `총 ${t}건`,
													onChange: (p, s) => {
														setCurrentPage(p);
														setPageSize(s);
													}
												}}
												locale={{ emptyText: <Text type="secondary">참여 가능한 이벤트가 없습니다.</Text> }}
												scroll={{ x: 1200 }}
											/>
										</>
									)}
								</Space>
							</Card>
							<Modal
								title={selectedEvent?.eventTitle || '이벤트 상세'}
								open={Boolean(selectedEvent)}
								onCancel={() => setSelectedEvent(null)}
								footer={null}
								width={680}
							>
								{selectedEvent && (
									<Space direction="vertical" size={14} style={{ width: '100%' }}>
										<div>
											<Tag>{STATUS_LABEL[selectedEvent.eventStatus] || selectedEvent.eventStatus || '-'}</Tag>
											<Tag>{EVENT_TYPE_LABEL[selectedEvent.eventType] || selectedEvent.eventType || '-'}</Tag>
											<Tag color={selectedEvent.participationEnabled ? 'green' : 'default'}>
												{selectedEvent.participationEnabled ? '참여중' : '미참여'}
											</Tag>
											{selectedEvent.eventType === 'SALE' && (
												<Tag color={selectedEvent.participating ? 'blue' : 'default'}>
													{selectedEvent.participating ? '세일 등록됨' : '세일 미등록'}
												</Tag>
											)}
										</div>
										<div>
											<strong>고객 이벤트 기간</strong>
											<div>{formatDateOnly(selectedEvent.customerEventStartAt)} ~ {formatDateOnly(selectedEvent.customerEventEndAt)}</div>
										</div>
										<div>
											<strong>파트너 신청 기간</strong>
											<div>{formatDateOnly(selectedEvent.partnerApplyStartAt)} ~ {formatDateOnly(selectedEvent.partnerApplyEndAt)}</div>
										</div>
										{selectedEvent.eventType === 'SALE' && (
											<Alert
												type="warning"
												showIcon
												message="신청 기간 내 세일 등록 필수"
												description="참여 신청만으로는 할인 적용이 되지 않습니다. 신청 기간 내에 세일 등록 페이지에서 상품/옵션을 선택해 등록해야 합니다."
											/>
										)}
										{selectedEvent.eventType === 'POINT' && (
											<Alert
												type="info"
												showIcon
												message="포인트 이벤트 안내"
												description="추가 포인트는 고객 1인당 최대 3회(주문 기준) 지급됩니다."
											/>
										)}
										{selectedEvent.eventType === 'SALE' && (
											<div>
												<strong>관리자 세일 기준</strong>
												<div>{DISCOUNT_TYPE_LABEL[selectedEvent.saleDiscountType] || selectedEvent.saleDiscountType || '-'}</div>
												<div>
													할인 값: {
														selectedEvent.saleDiscountType === 'PERCENT'
															? `${Number(selectedEvent.saleDiscountValue || 0)}%`
															: `${Number(selectedEvent.saleDiscountValue || 0).toLocaleString()}원`
													}
												</div>
												<div>
													최대 할인 금액: {
														selectedEvent.saleMaxDiscountAmount
															? `${Number(selectedEvent.saleMaxDiscountAmount).toLocaleString()}원`
															: '-'
													}
												</div>
											</div>
										)}
										{selectedEvent.eventType === 'POINT' && (
											<div>
												<strong>포인트 대상</strong>
												<div>
													{(() => {
														const t = selectedEvent?.pointEventTargetType;
														const vals = Array.isArray(selectedEvent?.pointEventTargetValues) ? selectedEvent.pointEventTargetValues : [];
														if (t === 'ALL' || !t) return '전체(ALL)';
														if (t === 'PRODUCT' && vals.length) return `선택 대상: 상품 #${vals.slice(0, 8).join(', #')}${vals.length > 8 ? ' …' : ''}`;
														if (t === 'OPTION' && vals.length) return `선택 대상: 옵션 #${vals.slice(0, 8).join(', #')}${vals.length > 8 ? ' …' : ''}`;
														if (t === 'CATEGORY' && vals.length) return `카테고리: ${vals.slice(0, 8).map(toKoCategory).join(', ')}${vals.length > 8 ? ' …' : ''}`;
														if (t === 'MIN_ORDER_AMOUNT' && selectedEvent?.pointEventMinOrderAmount != null) {
															return `최소 주문금액: ${Number(selectedEvent.pointEventMinOrderAmount).toLocaleString()}원 이상`;
														}
														return '-';
													})()}
												</div>
											</div>
										)}
										<div>
											<strong>이벤트 내용</strong>
											<div style={{ whiteSpace: 'pre-wrap' }}>{selectedEvent.eventContent || '-'}</div>
										</div>
										{(() => {
											const isParticipating = Boolean(selectedEvent?.participationEnabled);
											const t = selectedEvent?.pointEventTargetType;
											const typeLabel =
												t === 'PARTNER' ? '파트너 대상' :
												t === 'PRODUCT' ? '상품 지정 대상' :
												t === 'OPTION' ? '옵션 지정 대상' :
												t === 'CATEGORY' ? '카테고리 지정 대상' :
												t === 'MIN_ORDER_AMOUNT' ? '최소 주문금액 기준' :
												'전체 대상';
											return isParticipating ? (
												<Alert
													type="success"
													showIcon
													message="참여 안내"
													description={`[${selectedEvent?.eventTitle || '이벤트'}]에 참여하셨습니다. ${typeLabel}이 자동으로 적용됩니다.`}
												/>
											) : null;
										})()}
										<div style={{ textAlign: 'right' }}>
											{(() => {
												const windowState = getApplyWindowState(selectedEvent);
												const disabled = !windowState.canToggle;
												const rowLoading = Boolean(actionLoading[selectedEvent.eventNo]);
												return selectedEvent.participationEnabled ? (
													<Button
														danger
														disabled={disabled}
														loading={rowLoading}
														onClick={async () => {
															await handleToggleParticipation(selectedEvent.eventNo, false);
															setSelectedEvent((prev) =>
																prev ? { ...prev, participationEnabled: false, participating: false } : prev
															);
														}}
													>
														참여 해제
													</Button>
												) : (
													<Button
														type="primary"
														disabled={disabled}
														loading={rowLoading}
														onClick={async () => {
															await handleToggleParticipation(selectedEvent.eventNo, true);
															setSelectedEvent((prev) => (prev ? { ...prev, participationEnabled: true } : prev));
														}}
													>
														참여 신청
													</Button>
												);
											})()}
										</div>
									</Space>
								)}
							</Modal>
						</>
					)
				},
				{
					key: 'performance',
					label: '이벤트 실적(종료)',
					children: (<PartnerEventPerformance />)
				}
			]}
		/>
	);
};

export default PartnerEvents;
