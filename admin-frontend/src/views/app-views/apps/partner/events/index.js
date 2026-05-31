import React, { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Card, Col, Empty, Modal, Row, Select, Space, Spin, Tag, Typography, message } from 'antd';
import { CalendarOutlined, EyeOutlined, ReloadOutlined } from '@ant-design/icons';
import { useSelector } from 'react-redux';
import dayjs from 'dayjs';
import { useNavigate } from 'react-router-dom';
import PartnerService from 'services/PartnerService';
import { APP_PREFIX_PATH } from 'configs/AppConfig';

const { Text } = Typography;
const { Option } = Select;

const STATUS_LABELS = {
	PRIVATE: '비공개',
	PUBLISHED: '공개',
	ENDED: '종료',
};

const STATUS_COLORS = {
	PRIVATE: 'default',
	PUBLISHED: 'blue',
	ENDED: 'green',
};

const EVENT_TYPE_LABELS = {
	SALE: '세일',
	POINT: '포인트',
	NOTICE: '공지',
};

const DISCOUNT_TYPE_LABELS = {
	PERCENT: '정률(%)',
	FIXED: '정액(원)',
};

const CATEGORY_LABELS = {
	SWIMSUIT_MEN: '남성 수영복',
	SWIMSUIT_WOMEN: '여성 수영복',
	SWIMSUIT_KIDS: '아동 수영복',
	SWIM_CAP: '수모',
	SWIM_GOGGLES: '수경',
	FINS: '오리발',
	SWIM_TOY: '수영용품',
	ETC: '기타',
};

const STATUS_PRIORITY = {
	PUBLISHED: 0,
	ENDED: 1,
};

const formatDate = (value) => {
	if (!value) return '-';
	const date = dayjs(value);
	return date.isValid() ? date.format('YYYY.MM.DD') : '-';
};

const getDdayText = (event) => {
	const now = dayjs();
	const start = event?.customerEventStartAt ? dayjs(event.customerEventStartAt) : null;
	const end = event?.customerEventEndAt ? dayjs(event.customerEventEndAt) : null;

	if (event?.eventStatus === 'ENDED') return '종료됨';
	if (start && now.isBefore(start)) return `시작 D-${Math.max(0, start.startOf('day').diff(now.startOf('day'), 'day'))}`;
	if (end && !now.isAfter(end)) return `종료 D-${Math.max(0, end.startOf('day').diff(now.startOf('day'), 'day'))}`;
	return '기간 확인 필요';
};

const getApplyWindowState = (event) => {
	const now = dayjs();
	const start = event?.partnerApplyStartAt ? dayjs(event.partnerApplyStartAt) : null;
	const end = event?.partnerApplyEndAt ? dayjs(event.partnerApplyEndAt) : null;

	if (!start || !end || !start.isValid() || !end.isValid()) {
		return { canToggle: false, label: '기간 미설정', color: 'default' };
	}
	if (now.isBefore(start)) {
		return { canToggle: false, label: '신청 시작 전', color: 'gold' };
	}
	if (now.isAfter(end)) {
		return { canToggle: false, label: '신청 마감', color: 'default' };
	}
	return { canToggle: true, label: '신청 가능', color: 'blue' };
};

const getParticipationStatus = (event) => {
	if (event?.participationEnabled) {
		if (event?.eventType === 'SALE' && event?.participating) {
			return { label: '세일 등록 완료', color: 'blue' };
		}
		return { label: '신청 완료', color: 'green' };
	}
	return getApplyWindowState(event);
};

const getTargetSummary = (event) => {
	const targetType = event?.pointEventTargetType;
	const values = Array.isArray(event?.pointEventTargetValues) ? event.pointEventTargetValues : [];
	if (event?.eventType === 'SALE') return '세일 상품 등록';
	if (!targetType || targetType === 'ALL') return '전체 품목목';
	if (targetType === 'PRODUCT' && values.length) return `상품 #${values.slice(0, 3).join(', #')}${values.length > 3 ? ' 외' : ''}`;
	if (targetType === 'OPTION' && values.length) return `옵션 #${values.slice(0, 3).join(', #')}${values.length > 3 ? ' 외' : ''}`;
	if (targetType === 'CATEGORY' && values.length) return values.slice(0, 3).map((value) => CATEGORY_LABELS[value] || value).join(', ');
	if (targetType === 'MIN_ORDER_AMOUNT' && event?.pointEventMinOrderAmount != null) {
		return `${Number(event.pointEventMinOrderAmount).toLocaleString()}원 이상`;
	}
	return '전체 품목목';
};

const EventHeader = ({ event }) => (
	<div>
		<Space size={6} wrap className="mb-2">
			<Tag color={STATUS_COLORS[event.eventStatus] || 'default'}>{STATUS_LABELS[event.eventStatus] || event.eventStatus}</Tag>
			<Tag>{EVENT_TYPE_LABELS[event.eventType] || event.eventType || '종류 미지정'}</Tag>
			<Tag>파트너 참여형</Tag>
			<Tag icon={<CalendarOutlined />}>{getDdayText(event)}</Tag>
		</Space>
		<h4 className="mb-1">{event.eventTitle || `이벤트 #${event.eventNo}`}</h4>
		<Text type="secondary">#{event.eventNo}</Text>
	</div>
);

const EventSchedule = ({ event }) => (
	<Space direction="vertical" size={4} className="w-100">
		<div className="d-flex justify-content-between">
			<Text type="secondary">신청 기간</Text>
			<Text>{formatDate(event.partnerApplyStartAt)} ~ {formatDate(event.partnerApplyEndAt)}</Text>
		</div>
		<div className="d-flex justify-content-between">
			<Text type="secondary">이벤트 기간</Text>
			<Text>{formatDate(event.customerEventStartAt)} ~ {formatDate(event.customerEventEndAt)}</Text>
		</div>
		<div className="d-flex justify-content-between">
			<Text type="secondary">이벤트 유형</Text>
			<Text>{getTargetSummary(event)}</Text>
		</div>
	</Space>
);

const EventParticipation = ({ event }) => {
	const status = getParticipationStatus(event);
	return (
		<div>
			<Text type="secondary" className="mr-2">내 참여 상태</Text>
			<Tag color={status.color}>{status.label}</Tag>
			{event.eventType === 'SALE' && event.participationEnabled && !event.participating && (
				<Tag color="gold">세일 등록 필요</Tag>
			)}
		</div>
	);
};

const EventActions = ({ event, actionLoading, onOpenDetail, onToggleParticipation, onOpenPerformance, showDetail = true }) => {
	const windowState = getApplyWindowState(event);
	const rowLoading = Boolean(actionLoading[event.eventNo]);

	return (
		<Space wrap>
			{showDetail && (
				<Button icon={<EyeOutlined />} onClick={() => onOpenDetail(event)}>
					상세 보기
				</Button>
			)}
			{event.participationEnabled ? (
				<Button
					danger
					disabled={!windowState.canToggle}
					loading={rowLoading}
					onClick={() => onToggleParticipation(event, false)}
				>
					참여 해제
				</Button>
			) : (
				<Button
					type="primary"
					disabled={!windowState.canToggle}
					loading={rowLoading}
					onClick={() => onToggleParticipation(event, true)}
				>
					참여 신청
				</Button>
			)}
			<Button type="link" onClick={() => onOpenPerformance(event)}>
				실적
			</Button>
		</Space>
	);
};

const EventCard = ({ event, actionLoading, onOpenDetail, onToggleParticipation, onOpenPerformance }) => (
	<Card>
		<div className="mb-3">
			<EventHeader event={event} />
		</div>
		<EventSchedule event={event} />
		<div className="mt-3">
			<EventParticipation event={event} />
		</div>
		<div className="d-flex justify-content-end mt-3">
			<EventActions
				event={event}
				actionLoading={actionLoading}
				onOpenDetail={onOpenDetail}
				onToggleParticipation={onToggleParticipation}
				onOpenPerformance={onOpenPerformance}
			/>
		</div>
	</Card>
);

const PartnerEvents = () => {
	const { user } = useSelector((state) => state.auth);
	const navigate = useNavigate();
	const [loading, setLoading] = useState(false);
	const [actionLoading, setActionLoading] = useState({});
	const [events, setEvents] = useState([]);
	const [currentPage, setCurrentPage] = useState(1);
	const [pageSize, setPageSize] = useState(12);
	const [totalItems, setTotalItems] = useState(0);
	const [selectedEvent, setSelectedEvent] = useState(null);
	const [statusFilter, setStatusFilter] = useState('ALL');

	const partnerEvents = useMemo(
		() =>
			(events || [])
				.filter((event) => event?.eventMode === 'PARTNER_PARTICIPATION')
				.filter((event) => statusFilter === 'ALL' || event?.eventStatus === statusFilter)
				.sort((a, b) => {
					const aPriority = STATUS_PRIORITY[a?.eventStatus] ?? 99;
					const bPriority = STATUS_PRIORITY[b?.eventStatus] ?? 99;
					if (aPriority !== bPriority) return aPriority - bPriority;
					const aStart = a?.customerEventStartAt ? new Date(a.customerEventStartAt).getTime() : 0;
					const bStart = b?.customerEventStartAt ? new Date(b.customerEventStartAt).getTime() : 0;
					return aStart - bStart;
				}),
		[events, statusFilter]
	);

	const summary = useMemo(() => {
		const rows = events || [];
		return {
			total: rows.length,
			applyOpen: rows.filter((event) => getApplyWindowState(event).canToggle && !event.participationEnabled).length,
			participating: rows.filter((event) => event.participationEnabled).length,
			ended: rows.filter((event) => event.eventStatus === 'ENDED').length,
		};
	}, [events]);

	const loadEvents = async () => {
		setLoading(true);
		try {
			const res = await PartnerService.getPartnerVisibleEvents({ page: currentPage, size: pageSize });
			const data = res?.data ?? res;
			const meta = data?.meta;
			const items = Array.isArray(data) ? data : (data?.events ?? data?.items ?? []);
			const total = Array.isArray(data) ? data.length : (meta?.total ?? data?.total ?? 0);
			const respPage = Array.isArray(data) ? currentPage : (((meta?.page ?? data?.page) ?? 0) + 1);
			const respSize = Array.isArray(data) ? pageSize : (meta?.size ?? data?.size ?? pageSize);
			setEvents(Array.isArray(items) ? items : []);
			setTotalItems(Number(total) || 0);
			setCurrentPage(respPage);
			setPageSize(respSize);
		} catch (err) {
			message.warning(err?.response?.data?.message || '이벤트 목록 조회에 실패했습니다.');
			setEvents([]);
			setTotalItems(0);
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		loadEvents();
	// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [currentPage, pageSize]);

	const updateEventParticipation = (eventNo, nextParticipating) => {
		setEvents((prev) =>
			prev.map((event) =>
				event.eventNo === eventNo
					? { ...event, participationEnabled: nextParticipating, participating: nextParticipating ? event.participating : false }
					: event
			)
		);
		setSelectedEvent((prev) =>
			prev?.eventNo === eventNo
				? { ...prev, participationEnabled: nextParticipating, participating: nextParticipating ? prev.participating : false }
				: prev
		);
	};

	const handleToggleParticipation = async (event, nextParticipating) => {
		const eventNo = event.eventNo;
		setActionLoading((prev) => ({ ...prev, [eventNo]: true }));
		try {
			const isSaleEvent = event.eventType === 'SALE';

			if (nextParticipating) {
				await PartnerService.participateEvent(eventNo);
				if (isSaleEvent) {
					Modal.info({
						title: '세일 등록 안내',
						content: '참여가 완료되었습니다. 신청 기간 내 세일 관리에서 상품/옵션을 선택해 세일을 등록해주세요.',
						okText: '세일 관리로 이동',
						onOk: () => navigate(`${APP_PREFIX_PATH}/partner/sales`),
					});
				} else {
					message.success('이벤트 참여 신청이 완료되었습니다.');
				}
				if (isSaleEvent) {
					await loadEvents();
				} else {
					updateEventParticipation(eventNo, true);
				}
			} else {
				const ok = await new Promise((resolve) => {
					Modal.confirm({
						title: '이벤트 참여를 해제하시겠습니까?',
						content: isSaleEvent
							? '연동된 세일이 있으면 자동으로 비활성 처리됩니다.'
							: '참여 해제 후 신청 기간 안에는 다시 신청할 수 있습니다.',
						okText: '참여 해제',
						okButtonProps: { danger: true },
						cancelText: '취소',
						onOk: () => resolve(true),
						onCancel: () => resolve(false),
					});
				});
				if (!ok) return;
				await PartnerService.cancelEventParticipation(eventNo, { deactivateLinkedSales: isSaleEvent });
				message.success('이벤트 참여가 해제되었습니다.');
				if (isSaleEvent) {
					await loadEvents();
				} else {
					updateEventParticipation(eventNo, false);
				}
			}
		} catch (err) {
			message.warning(err?.response?.data?.message || '요청 처리에 실패했습니다.');
		} finally {
			setActionLoading((prev) => ({ ...prev, [eventNo]: false }));
		}
	};

	const openPerformance = (event) => {
		navigate(`${APP_PREFIX_PATH}/partner/events/performance`, { state: { eventNo: event.eventNo } });
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

	return (
		<>
			<Row gutter={16}>
				<Col xs={12} md={6}>
					<Card><span className="text-muted">전체 이벤트</span><h2 className="mb-0">{summary.total}</h2></Card>
				</Col>
				<Col xs={12} md={6}>
					<Card><span className="text-muted">신청 가능</span><h2 className="mb-0">{summary.applyOpen}</h2></Card>
				</Col>
				<Col xs={12} md={6}>
					<Card><span className="text-muted">참여 중</span><h2 className="mb-0">{summary.participating}</h2></Card>
				</Col>
				<Col xs={12} md={6}>
					<Card><span className="text-muted">종료</span><h2 className="mb-0">{summary.ended}</h2></Card>
				</Col>
			</Row>

			<Card
				extra={
					<Space wrap>
						<Select value={statusFilter} style={{ minWidth: 140 }} onChange={setStatusFilter}>
							<Option value="ALL">전체 상태</Option>
							<Option value="PUBLISHED">공개</Option>
							<Option value="ENDED">종료</Option>
						</Select>
						<Button icon={<ReloadOutlined />} onClick={loadEvents} loading={loading}>
							새로고침
						</Button>
					</Space>
				}
			>
				<Spin spinning={loading}>
					{partnerEvents.length === 0 ? (
						<Empty description="참여 가능한 이벤트가 없습니다." />
					) : (
						<>
							<Row gutter={[16, 16]}>
								{partnerEvents.map((event) => (
									<Col xs={24} lg={12} xl={8} key={event.eventNo}>
										<EventCard
											event={event}
											actionLoading={actionLoading}
											onOpenDetail={setSelectedEvent}
											onToggleParticipation={handleToggleParticipation}
											onOpenPerformance={openPerformance}
										/>
									</Col>
								))}
							</Row>
							<div className="d-flex justify-content-between align-items-center mt-3">
								<Text type="secondary">{`총 ${totalItems}건`}</Text>
								<Space>
									<Button disabled={currentPage <= 1} onClick={() => setCurrentPage((prev) => Math.max(1, prev - 1))}>
										이전
									</Button>
									<Text>{`${currentPage} / ${Math.max(1, Math.ceil(totalItems / pageSize))}`}</Text>
									<Button
										disabled={currentPage >= Math.max(1, Math.ceil(totalItems / pageSize))}
										onClick={() => setCurrentPage((prev) => prev + 1)}
									>
										다음
									</Button>
								</Space>
							</div>
						</>
					)}
				</Spin>
			</Card>

			<Modal
				title={selectedEvent?.eventTitle || '이벤트 상세'}
				open={Boolean(selectedEvent)}
				onCancel={() => setSelectedEvent(null)}
				footer={null}
				width={720}
			>
				{selectedEvent && (
					<Space direction="vertical" size={14} style={{ width: '100%' }}>
						<div>
							<Tag color={STATUS_COLORS[selectedEvent.eventStatus] || 'default'}>
								{STATUS_LABELS[selectedEvent.eventStatus] || selectedEvent.eventStatus || '-'}
							</Tag>
							<Tag>{EVENT_TYPE_LABELS[selectedEvent.eventType] || selectedEvent.eventType || '-'}</Tag>
							<Tag color={getParticipationStatus(selectedEvent).color}>
								{getParticipationStatus(selectedEvent).label}
							</Tag>
						</div>
						<div>
							<strong>파트너 신청 기간</strong>
							<div>{formatDate(selectedEvent.partnerApplyStartAt)} ~ {formatDate(selectedEvent.partnerApplyEndAt)}</div>
						</div>
						<div>
							<strong>이벤트 기간</strong>
							<div>{formatDate(selectedEvent.customerEventStartAt)} ~ {formatDate(selectedEvent.customerEventEndAt)}</div>
						</div>
						{selectedEvent.eventType === 'SALE' && (
							<Alert
								type="warning"
								showIcon
								message="신청 기간 내 세일 등록 필수"
								description="참여 신청만으로는 할인 적용이 되지 않습니다. 세일 관리에서 상품/옵션을 선택해 등록해야 합니다."
							/>
						)}
						{selectedEvent.eventType === 'POINT' && (
							<Alert
								type="info"
								showIcon
								message="포인트 이벤트 안내"
								description={`이벤트 유형: ${getTargetSummary(selectedEvent)}`}
							/>
						)}
						{selectedEvent.eventType === 'SALE' && (
							<div>
								<strong>세일 기준</strong>
								<div>{DISCOUNT_TYPE_LABELS[selectedEvent.saleDiscountType] || selectedEvent.saleDiscountType || '-'}</div>
								<div>
									할인 값: {selectedEvent.saleDiscountType === 'PERCENT'
										? `${Number(selectedEvent.saleDiscountValue || 0)}%`
										: `${Number(selectedEvent.saleDiscountValue || 0).toLocaleString()}원`}
								</div>
								<div>
									최대 할인 금액: {selectedEvent.saleMaxDiscountAmount
										? `${Number(selectedEvent.saleMaxDiscountAmount).toLocaleString()}원`
										: '-'}
								</div>
							</div>
						)}
						<div>
							<strong>이벤트 내용</strong>
							<div style={{ whiteSpace: 'pre-wrap' }}>{selectedEvent.eventContent || '-'}</div>
						</div>
						<div className="text-right">
							<EventActions
								event={selectedEvent}
								actionLoading={actionLoading}
								showDetail={false}
								onOpenDetail={setSelectedEvent}
								onToggleParticipation={handleToggleParticipation}
								onOpenPerformance={openPerformance}
							/>
						</div>
					</Space>
				)}
			</Modal>
		</>
	);
};

export default PartnerEvents;
