import React, { useEffect, useMemo, useState } from 'react';
import { useLocation } from 'react-router-dom';
import dayjs from 'dayjs';
import {
	Card,
	Table,
	Button,
	Modal,
	Drawer,
	Form,
	Input,
	InputNumber,
	Select,
	Radio,
	DatePicker,
	Space,
	Tag,
	message,
	Statistic,
	Row,
	Col,
	Tabs,
	Empty,
	Spin,
	Tooltip,
	Typography,
	Upload
} from 'antd';
import Chart from 'react-apexcharts';
import {
	AppstoreOutlined,
	CalendarOutlined,
	EyeOutlined,
	PlusOutlined,
	ReloadOutlined,
	UnorderedListOutlined,
	UploadOutlined,
	DeleteOutlined
} from '@ant-design/icons';
import AdminService from 'services/AdminService';
import FileService from 'services/FileService';

const { RangePicker } = DatePicker;
const { TextArea } = Input;
const { Text } = Typography;
const VIEW_LIST = 'LIST';
const VIEW_GRID = 'GRID';

const beforeUploadImage = (file) => {
	const isAllowed = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type);
	if (!isAllowed) {
		message.error('이미지 파일만 업로드할 수 있습니다 (jpg, png, gif, webp).');
		return Upload.LIST_IGNORE;
	}
	const isLt10M = file.size / 1024 / 1024 < 10;
	if (!isLt10M) {
		message.error('파일 용량은 10MB 이하만 허용됩니다.');
		return Upload.LIST_IGNORE;
	}
	return true;
};

const EVENT_STATUSES = ['PRIVATE', 'PUBLISHED', 'ENDED'];
const STATUS_LABELS = {
	PRIVATE: '비공개',
	PUBLISHED: '공개',
	ENDED: '종료'
};

const EVENT_MODE_OPTIONS = [
	{ value: 'ADMIN_ONLY', label: '운영 직접 등록' },
	{ value: 'PARTNER_PARTICIPATION', label: '파트너 신청·등록' }
];

const EVENT_TYPE_OPTIONS = [
	// backend EventType enum과 동일해야 함 (SALE | POINT | NOTICE)
	{ value: 'SALE', label: '세일' },
	{ value: 'POINT', label: '포인트' },
	{ value: 'NOTICE', label: '공지' }
];

/** 참여 형태별 허용 종류 — backend EventService 검증과 동일 */
const EVENT_TYPES_BY_MODE = {
	ADMIN_ONLY: ['POINT', 'NOTICE'],
	PARTNER_PARTICIPATION: ['SALE', 'POINT']
};

const getEventTypeOptionsForMode = (mode) => {
	if (!mode) return EVENT_TYPE_OPTIONS;
	const allowed = EVENT_TYPES_BY_MODE[mode];
	if (!allowed?.length) return EVENT_TYPE_OPTIONS;
	return EVENT_TYPE_OPTIONS.filter((o) => allowed.includes(o.value));
};

const getDefaultEventTypeForMode = (mode) => {
	const opts = getEventTypeOptionsForMode(mode);
	const point = opts.find((o) => o.value === 'POINT');
	if (point) return 'POINT';
	return opts[0]?.value;
};

const SALE_DISCOUNT_TYPE_OPTIONS = [
	{ value: 'PERCENT', label: '정률(%)' },
	{ value: 'FIXED', label: '정액(원)' }
]

const POINT_TARGET_TYPE_OPTIONS = [
	{ value: 'ALL', label: '전체' },
	{ value: 'PARTNER', label: '파트너' },
	{ value: 'PRODUCT', label: '상품' },
	{ value: 'OPTION', label: '옵션' },
	{ value: 'CATEGORY', label: '카테고리' },
	{ value: 'MIN_ORDER_AMOUNT', label: '최소주문금액' }
]

/** 파트너 참여형: 특정 파트너/상품/옵션 고정은 다자 신청 흐름과 맞지 않아 폼에서 숨김 */
const POINT_TARGET_TYPES_HIDDEN_FOR_PARTNER_MODE = ['PARTNER', 'PRODUCT', 'OPTION']

const getPointTargetTypeOptionsForMode = (mode) => {
	if (mode === 'PARTNER_PARTICIPATION') {
		return POINT_TARGET_TYPE_OPTIONS.filter((o) => !POINT_TARGET_TYPES_HIDDEN_FOR_PARTNER_MODE.includes(o.value))
	}
	return POINT_TARGET_TYPE_OPTIONS
}

const CATEGORY_OPTIONS = [
	{ value: 'SWIMSUIT_MEN', label: '남성 수영복' },
	{ value: 'SWIMSUIT_WOMEN', label: '여성 수영복' },
	{ value: 'SWIMSUIT_KIDS', label: '아동 수영복' },
	{ value: 'SWIM_CAP', label: '수모' },
	{ value: 'SWIM_GOGGLES', label: '수경' },
	{ value: 'FINS', label: '오리발' },
	{ value: 'SWIM_TOY', label: '수영용품' },
	{ value: 'ETC', label: '기타' }
]

const STATUS_COLORS = {
	PRIVATE: 'default',
	PUBLISHED: 'blue',
	ENDED: 'green'
};

const EVENT_TYPE_LABELS = {
	SALE: '세일',
	POINT: '포인트',
	NOTICE: '공지'
};

const EVENT_MODE_LABELS = {
	ADMIN_ONLY: '관리자 단독',
	PARTNER_PARTICIPATION: '파트너 참여형'
};

const STATUS_OPTIONS = [
	{ value: '', label: '전체 상태' },
	...EVENT_STATUSES.map((status) => ({
		value: status,
		label: STATUS_LABELS[status] || status
	}))
];

const formatOperationDate = value => value ? dayjs(value).format('YYYY.MM.DD HH:mm') : '-';
const formatOperationDateOnly = value => value ? dayjs(value).format('YYYY.MM.DD') : '-';

const getCustomerExposureStatus = event => {
	const now = dayjs();
	const exposeAt = event?.customerExposeAt ? dayjs(event.customerExposeAt) : null;

	if (event?.eventStatus === 'ENDED') {
		return { label: '노출 종료', color: 'default' };
	}
	if (!exposeAt) {
		return { label: '고객 미노출', color: 'default' };
	}
	if (now.isBefore(exposeAt)) {
		return { label: '노출 예정', color: 'gold' };
	}
	return { label: '노출 중', color: 'blue' };
};

const getDdayText = event => {
	const now = dayjs();
	const start = event?.customerEventStartAt ? dayjs(event.customerEventStartAt) : null;
	const end = event?.customerEventEndAt ? dayjs(event.customerEventEndAt) : null;

	if (event?.eventStatus === 'ENDED') return '종료됨';
	if (start && now.isBefore(start)) return `시작 D-${Math.max(0, start.startOf('day').diff(now.startOf('day'), 'day'))}`;
	if (end && !now.isAfter(end)) return `종료 D-${Math.max(0, end.startOf('day').diff(now.startOf('day'), 'day'))}`;
	return '기간 확인 필요';
};

const EventOperationHeader = ({ event }) => (
	<div>
		<Space size={6} wrap className="mb-2">
			<Tag color={STATUS_COLORS[event.eventStatus] || 'default'}>{STATUS_LABELS[event.eventStatus] || event.eventStatus}</Tag>
			<Tag>{EVENT_TYPE_LABELS[event.eventType] || event.eventType || '종류 미지정'}</Tag>
			<Tag>{EVENT_MODE_LABELS[event.eventMode] || event.eventMode || '모드 미지정'}</Tag>
			<Tag icon={<CalendarOutlined />}>{getDdayText(event)}</Tag>
		</Space>
		<h4 className="mb-1">{event.eventTitle || `이벤트 #${event.eventNo}`}</h4>
		<Text type="secondary">#{event.eventNo}</Text>
	</div>
);

const EventOperationSchedule = ({ event }) => (
	<Space direction="vertical" size={4} className="w-100">
		<div className="d-flex justify-content-between">
			<Text type="secondary">공개 시작</Text>
			<Text>{event.customerExposeAt ? formatOperationDate(event.customerExposeAt) : '미공개'}</Text>
		</div>
		<div className="d-flex justify-content-between">
			<Text type="secondary">이벤트 기간</Text>
			<Text>{formatOperationDateOnly(event.customerEventStartAt)} ~ {formatOperationDateOnly(event.customerEventEndAt)}</Text>
		</div>
		{event.eventMode === 'PARTNER_PARTICIPATION' && (
			<div className="d-flex justify-content-between">
				<Text type="secondary">파트너 신청</Text>
				<Text>{formatOperationDateOnly(event.partnerApplyStartAt)} ~ {formatOperationDateOnly(event.partnerApplyEndAt)}</Text>
			</div>
		)}
	</Space>
);

const EventOperationStatus = ({ event }) => {
	const exposureStatus = getCustomerExposureStatus(event);

	return (
		<Space direction="vertical" size={6}>
			<div>
				<Text type="secondary" className="mr-2">고객 노출</Text>
				<Tag color={exposureStatus.color}>{exposureStatus.label}</Tag>
			</div>
		</Space>
	);
};

const EventOperationActions = ({ eventNo, onEdit, onPerformance }) => (
	<Space>
		<Tooltip title="이벤트 수정 모달을 엽니다.">
			<Button icon={<EyeOutlined />} onClick={() => onEdit(eventNo)}>상세·수정</Button>
		</Tooltip>
		<Button type="link" onClick={() => onPerformance(eventNo)}>실적</Button>
	</Space>
);

const EventOperationListItem = ({ event, onEdit, onPerformance }) => (
	<Card>
		<Row align="middle" gutter={[16, 16]}>
			<Col xs={24} lg={7}>
				<EventOperationHeader event={event} />
			</Col>
			<Col xs={24} lg={7}>
				<EventOperationSchedule event={event} />
			</Col>
			<Col xs={24} lg={5}>
				<EventOperationStatus event={event} />
			</Col>
			<Col xs={24} lg={5}>
				<div className="text-right">
					<EventOperationActions eventNo={event.eventNo} onEdit={onEdit} onPerformance={onPerformance} />
				</div>
			</Col>
		</Row>
	</Card>
);

const EventOperationGridItem = ({ event, onEdit, onPerformance }) => (
	<Card>
		<div className="mb-3">
			<EventOperationHeader event={event} />
		</div>
		<EventOperationSchedule event={event} />
		<div className="mt-3">
			<EventOperationStatus event={event} />
		</div>
		<div className="d-flex justify-content-end align-items-center mt-3">
			<EventOperationActions eventNo={event.eventNo} onEdit={onEdit} onPerformance={onPerformance} />
		</div>
	</Card>
);

const EventManagement = () => {
	const location = useLocation();
	const [loading, setLoading] = useState(false);
	const [saving, setSaving] = useState(false);
	const [events, setEvents] = useState([]);
	const [participants, setParticipants] = useState([]);
	const [modalVisible, setModalVisible] = useState(false);
	const [editingEvent, setEditingEvent] = useState(null);
	const [statusFilter, setStatusFilter] = useState(undefined);
	const [keywordFilter, setKeywordFilter] = useState('');
	const [partnerTargetOptions, setPartnerTargetOptions] = useState([]);
	const [productTargetOptions, setProductTargetOptions] = useState([]);
	const [optionTargetOptions, setOptionTargetOptions] = useState([]);
	const [perfOpen, setPerfOpen] = useState(false);
	const [perfLoading, setPerfLoading] = useState(false);
	const [perfEventNo, setPerfEventNo] = useState(null);
	const [perfFrom, setPerfFrom] = useState(null);
	const [perfTo, setPerfTo] = useState(null);
	const [perfData, setPerfData] = useState(null);
	const [endedRows, setEndedRows] = useState([]);
	const [activeTab, setActiveTab] = useState('manage');
	const [eventView, setEventView] = useState(VIEW_GRID);
	const [selectedPerfRow, setSelectedPerfRow] = useState(null);
	const [timeseries, setTimeseries] = useState([]);
	const [topType, setTopType] = useState('partner');
	const [topData, setTopData] = useState([]);
	const [topLimit] = useState(10);
	const [form] = Form.useForm();

	const eventMode = Form.useWatch('eventMode', form);
	const eventType = Form.useWatch('eventType', form);
	const pointEventTargetType = Form.useWatch('pointEventTargetType', form);
	const thumbnailUrl = Form.useWatch('thumbnailUrl', form);

	const eventSummary = useMemo(() => {
		return events.reduce((acc, event) => {
			acc.total += 1;
			acc[event.eventStatus] = (acc[event.eventStatus] || 0) + 1;
			return acc;
		}, { total: 0, PRIVATE: 0, PUBLISHED: 0, ENDED: 0 });
	}, [events]);

	const toStartOfDay = (dateValue) => {
		if (!dateValue) return null;
		return dateValue.hour(0).minute(0).second(0).millisecond(0);
	};

	const toEndOfDay = (dateValue) => {
		if (!dateValue) return null;
		return dateValue.hour(23).minute(59).second(59).millisecond(0);
	};

	const handleUploadThumbnail = async ({ file, onSuccess, onError }) => {
		try {
			setSaving(true);
			const res = await FileService.uploadFile(file, 'event');
			const data = res?.data || res;
			const fileUrl = data?.fileUrl;
			if (!fileUrl) {
				throw new Error('업로드 응답에 fileUrl이 없습니다.');
			}
			form.setFieldsValue({ thumbnailUrl: fileUrl });
			message.success('이벤트 썸네일이 업로드되었습니다.');
			onSuccess && onSuccess({}, file);
		} catch (error) {
			onError && onError(error);
			message.error(error?.response?.data?.message || error?.message || '이벤트 썸네일 업로드에 실패했습니다.');
		} finally {
			setSaving(false);
		}
	};

	const formatDateTimeForList = (dt) => {
		if (!dt) return '-';
		const d = dayjs(dt);
		if (!d.isValid()) return '-';
		// 목록에서는 시간이 있더라도 날짜만 보여줍니다.
		return d.format('YYYY-MM-DD');
	};

	useEffect(() => {
		fetchEvents();
	// eslint-disable-next-line react-hooks/exhaustive-deps
	}, []);

	const normalizeResponseArray = (response, collectionKey = null) => {
		const data = response?.data ?? response;
		const payload = data?.data ?? data;
		if (Array.isArray(payload)) return payload;
		if (collectionKey && Array.isArray(payload?.[collectionKey])) return payload[collectionKey];
		if (Array.isArray(payload?.items)) return payload.items;
		return [];
	};

	const loadPointTargetOptions = async () => {
		try {
			const [partnersResponse, productsResponse] = await Promise.all([
				AdminService.getAllPartners('APPROVED'),
				AdminService.getAllProducts('ACTIVE', 1, 1000)
			]);
			const partners = normalizeResponseArray(partnersResponse);
			const products = normalizeResponseArray(productsResponse, 'products');

			setPartnerTargetOptions(
				partners.map((p) => ({
					value: String(p.partnerId),
					label: `${p.partnerName} (#${p.partnerId})`
				}))
			);

			setProductTargetOptions(
				products.map((p) => ({
					value: String(p.productNo),
					label: `${p.productName} (#${p.productNo})`
				}))
			);

			const optionRows = [];
			products.forEach((p) => {
				(p.options || []).forEach((opt) => {
					optionRows.push({
						value: String(opt.optionNo),
						label: `${p.productName} - ${opt.color || '-'} / ${opt.size || '-'} (#${opt.optionNo})`
					});
				});
			});
			setOptionTargetOptions(optionRows);
		} catch (err) {
			console.error('포인트 타겟 목록 로드 실패:', err);
		}
	};

	const openPerformance = async (eventNo) => {
		setPerfEventNo(eventNo);
		setPerfOpen(true);
		setPerfFrom(null);
		setPerfTo(null);
		await fetchPerformance(eventNo, null, null);
	};

	const fetchPerformance = async (eventNo, fromVal, toVal) => {
		try {
			setPerfLoading(true);
			const fromStr = fromVal ? dayjs(fromVal).format('YYYY-MM-DDTHH:mm:ss') : null;
			const toStr = toVal ? dayjs(toVal).format('YYYY-MM-DDTHH:mm:ss') : null;
			const resp = await AdminService.getEventPerformance(eventNo, fromStr, toStr);
			const data = resp?.data ?? resp;
			setPerfData(data);
		} catch (err) {
			console.error('이벤트 실적 조회 실패:', err);
			message.error(err?.response?.data?.message || '이벤트 실적 조회 실패');
			setPerfData(null);
		} finally {
			setPerfLoading(false);
		}
	};

	const fetchEndedList = async (all = false) => {
		try {
			setPerfLoading(true);
			const fromStr = !all && perfFrom ? dayjs(perfFrom).format('YYYY-MM-DDTHH:mm:ss') : null;
			const toStr = !all && perfTo ? dayjs(perfTo).format('YYYY-MM-DDTHH:mm:ss') : null;
			const resp = await AdminService.getEndedEventPerformanceList(fromStr, toStr);
			const rows = resp?.data ?? resp ?? [];
			setEndedRows(Array.isArray(rows) ? rows : []);
		} catch (err) {
			console.error('종료 이벤트 실적 조회 실패:', err);
			message.error(err?.response?.data?.message || '종료 이벤트 실적 조회 실패');
			setEndedRows([]);
		} finally {
			setPerfLoading(false);
		}
	};

	useEffect(() => {
		if (activeTab === 'endedPerformance') {
			fetchEndedList(true);
			refreshCharts();
		}
	// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [activeTab]);

	useEffect(() => {
		if (activeTab === 'endedPerformance') {
			refreshCharts();
		}
	// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [selectedPerfRow, perfFrom, perfTo, topType]);

	const refreshCharts = async () => {
		try {
			const fromStr = perfFrom ? dayjs(perfFrom).startOf('day').format('YYYY-MM-DDTHH:mm:ss') : null;
			const toStr = perfTo ? dayjs(perfTo).endOf('day').format('YYYY-MM-DDTHH:mm:ss') : null;
			const targetEventNo = selectedPerfRow?.eventNo || null;
			const [tsResp, topResp] = await Promise.all([
				AdminService.getEventTimeseries({ eventNo: targetEventNo, from: fromStr, to: toStr }),
				AdminService.getEventTop(topType, { eventNo: targetEventNo, from: fromStr, to: toStr, limit: topLimit })
			]);
			setTimeseries(tsResp?.data ?? tsResp ?? []);
			setTopData(topResp?.data ?? topResp ?? []);
		} catch (e) {
			console.error('차트 데이터 로드 실패:', e);
		}
	};
	const fetchEvents = async (overrides = {}) => {
		const effectiveStatus = Object.prototype.hasOwnProperty.call(overrides, 'status')
			? overrides.status
			: statusFilter;
		const effectiveKeyword = Object.prototype.hasOwnProperty.call(overrides, 'keyword')
			? overrides.keyword
			: keywordFilter;

		try {
			setLoading(true);
			const resp = await AdminService.getAdminEventList({
				status: effectiveStatus,
				keyword: effectiveKeyword?.trim() || undefined,
				page: 1,
				size: 60
			});
			const data = resp?.data ?? resp;
			const rawItems = Array.isArray(data) ? data : (data?.events ?? data?.items ?? data?.data ?? []);

			// 로컬 필터 백업(서버 필터 미지원 대비)
			let filtered = Array.isArray(rawItems) ? rawItems : [];
			if (effectiveStatus) {
				filtered = filtered.filter(e => e?.eventStatus === effectiveStatus);
			}
			if (effectiveKeyword) {
				const kw = effectiveKeyword.toLowerCase();
				filtered = filtered.filter(e => (e?.eventTitle || '').toLowerCase().includes(kw));
			}

			setEvents(filtered);
		} catch (err) {
			console.error('이벤트 목록 조회 실패:', err);
			message.error(err?.response?.data?.message || '이벤트 목록을 불러오는데 실패했습니다.');
			setEvents([]);
		} finally {
			setLoading(false);
		}
	};

	const handleStatusToggle = async (nextStatus) => {
		setStatusFilter(nextStatus);
		await fetchEvents({ status: nextStatus });
	};

	const openCreateModal = async () => {
		setEditingEvent(null);
		setParticipants([]);
		form.resetFields();
		await loadPointTargetOptions();
		form.setFieldsValue({
			eventStatus: 'PRIVATE',
			eventMode: 'ADMIN_ONLY',
			eventType: getDefaultEventTypeForMode('ADMIN_ONLY'),
			pointEventTargetType: 'ALL',
			pointEventTargetValues: []
		});
		setModalVisible(true);
	};

	useEffect(() => {
		const searchParams = new URLSearchParams(location.search);
		if (searchParams.get('tab') === 'performance') {
			setActiveTab('endedPerformance');
		} else {
			setActiveTab('manage');
		}
		if (searchParams.get('create') === '1') {
			openCreateModal();
		}
	// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [location.search]);

	const openEditModal = async (eventNo) => {
		try {
			setLoading(true);
			const [response, participantsResponse] = await Promise.all([
				AdminService.getAdminEventDetail(eventNo),
				AdminService.getAdminEventParticipants(eventNo)
			]);
			const detail = response?.data ?? response;
			const participantRows = participantsResponse?.data ?? participantsResponse ?? [];
			setEditingEvent(detail);
			setParticipants(Array.isArray(participantRows) ? participantRows : []);
			// 멀티 Select는 option.value(문자열)와 초기값 타입이 맞아야 태그가 보입니다.
			await loadPointTargetOptions();

			const eventStart = detail.customerEventStartAt ? dayjs(detail.customerEventStartAt) : null;
			const eventEnd = detail.customerEventEndAt ? dayjs(detail.customerEventEndAt) : null;

			const partnerApplyEnabledValue = detail.eventMode === 'PARTNER_PARTICIPATION';
			const participationStart = detail.partnerApplyStartAt ? dayjs(detail.partnerApplyStartAt) : null;
			const participationEnd = detail.partnerApplyEndAt ? dayjs(detail.partnerApplyEndAt) : null;

			const modeForDetail = detail.eventMode || 'ADMIN_ONLY';
			let pointEventTargetType = detail.pointEventTargetType || 'ALL';
			let pointEventTargetValues =
				Array.isArray(detail.pointEventTargetValues) && detail.pointEventTargetValues.length > 0
					? detail.pointEventTargetValues.map((v) => (v !== undefined && v !== null ? String(v) : '')).filter(Boolean)
					: [];
			let pointEventMinOrderAmount = detail.pointEventMinOrderAmount || null;

			if (detail.eventType === 'POINT' && modeForDetail === 'PARTNER_PARTICIPATION') {
				const allowedPt = getPointTargetTypeOptionsForMode(modeForDetail).map((o) => o.value);
				if (!allowedPt.includes(pointEventTargetType)) {
					pointEventTargetType = 'ALL';
					pointEventTargetValues = [];
					pointEventMinOrderAmount = null;
				}
			}

			form.setFieldsValue({
				eventTitle: detail.eventTitle,
				eventContent: detail.eventContent,
				eventStatus: detail.eventStatus,
				eventMode: modeForDetail,
				customerEventStartAt: eventStart,
				customerEventEndAt: eventEnd,

				partnerApplyEnabled: partnerApplyEnabledValue,
				partnerApplyStartAt: partnerApplyEnabledValue ? participationStart : null,
				partnerApplyEndAt: partnerApplyEnabledValue ? participationEnd : null,

				thumbnailUrl: detail.thumbnailUrl || '',
				eventType: detail.eventType || '',
				saleDiscountType: detail.saleDiscountType || null,
				saleDiscountValue: detail.saleDiscountValue || null,
				saleMaxDiscountAmount: detail.saleMaxDiscountAmount || null,
				pointEventTargetType,
				pointEventTargetValues,
				pointEventMinOrderAmount,
				adminMemo: detail.adminMemo || ''
			});
			setModalVisible(true);
		} catch (err) {
			console.error('이벤트 상세 조회 실패:', err);
			message.error(err?.response?.data?.message || '이벤트 상세 조회에 실패했습니다.');
		} finally {
			setLoading(false);
		}
	};

	const handleSave = async () => {
		try {
			const values = await form.validateFields();
			// ENDED 전환 가드: 기존이 ENDED가 아니고, 저장 값이 ENDED이면 컨펌
			if (editingEvent && editingEvent.eventStatus !== 'ENDED' && values.eventStatus === 'ENDED') {
				const now = dayjs();
				const endAt = editingEvent?.customerEventEndAt ? dayjs(editingEvent.customerEventEndAt) : null;
				const early = endAt && now.isBefore(endAt);
				const ok = await new Promise((resolve) => {
					Modal.confirm({
						title: '이벤트 강제 종료',
						content: early
							? '고객기간 종료 전 조기 종료됩니다. 재활성화할 수 없습니다. 계속하시겠습니까?'
							: '이 이벤트를 종료합니다. 재활성화할 수 없습니다. 계속하시겠습니까?',
						okText: '강제 종료',
						okButtonProps: { danger: true },
						cancelText: '취소',
						onOk: () => resolve(true),
						onCancel: () => resolve(false),
					});
				});
				if (!ok) return;
			}

			setSaving(true);

			const finalEventStartAt = toStartOfDay(values.customerEventStartAt);
			const finalEventEndAt = toEndOfDay(values.customerEventEndAt);

			const enabledParticipation = values.eventMode === 'PARTNER_PARTICIPATION';
			const finalParticipationStartAt = enabledParticipation
				? toStartOfDay(values.partnerApplyStartAt)
				: null;
			const finalParticipationEndAt = enabledParticipation
				? toEndOfDay(values.partnerApplyEndAt)
				: null;

			const payload = {
				eventTitle: values.eventTitle?.trim(),
				eventContent: values.eventContent?.trim(),
				eventStatus: values.eventStatus,
				eventMode: values.eventMode,
				// LocalDateTime API에 맞춰 timezone 없는 문자열로 전송
				customerEventStartAt: finalEventStartAt?.format('YYYY-MM-DDTHH:mm:ss'),
				customerEventEndAt: finalEventEndAt?.format('YYYY-MM-DDTHH:mm:ss'),
				customerExposeAt: values.customerExposeAt
					? dayjs(values.customerExposeAt).second(0).millisecond(0).format('YYYY-MM-DDTHH:mm:ss')
					: null,
				partnerApplyEnabled: enabledParticipation,
				partnerApplyStartAt: finalParticipationStartAt?.format('YYYY-MM-DDTHH:mm:ss'),
				partnerApplyEndAt: finalParticipationEndAt?.format('YYYY-MM-DDTHH:mm:ss'),
				thumbnailUrl: values.thumbnailUrl?.trim() || null,
				eventType: values.eventType?.trim() || null,
				saleDiscountType: values.saleDiscountType || null,
				saleDiscountValue: values.saleDiscountValue || null,
				saleMaxDiscountAmount: values.saleMaxDiscountAmount || null,
				pointEventTargetType: values.eventType === 'POINT' ? (values.pointEventTargetType || 'ALL') : null,
				pointEventTargetValues:
					values.eventType === 'POINT' &&
					values.pointEventTargetType &&
					values.pointEventTargetType !== 'ALL' &&
					values.pointEventTargetType !== 'MIN_ORDER_AMOUNT'
						? (values.pointEventTargetValues || [])
						: [],
				pointEventMinOrderAmount:
					values.eventType === 'POINT' && values.pointEventTargetType === 'MIN_ORDER_AMOUNT'
						? (values.pointEventMinOrderAmount || null)
						: null,
				adminMemo: values.adminMemo?.trim() || null
			};

			if (editingEvent?.eventNo) {
				await AdminService.updateAdminEvent(editingEvent.eventNo, payload);
				message.success('이벤트가 수정되었습니다.');
			} else {
				await AdminService.createAdminEvent(payload);
				message.success('이벤트가 생성되었습니다.');
			}

			setModalVisible(false);
			setEditingEvent(null);
			form.resetFields();
			await fetchEvents();
		} catch (err) {
			if (err?.errorFields) return;
			console.error('이벤트 저장 실패:', err);
			message.error(err?.response?.data?.message || '이벤트 저장에 실패했습니다.');
		} finally {
			setSaving(false);
		}
	};

	const getPointTargetSelectOptions = () => {
		switch (pointEventTargetType) {
			case 'PARTNER':
				return partnerTargetOptions;
			case 'PRODUCT':
				return productTargetOptions;
			case 'OPTION':
				return optionTargetOptions;
			case 'CATEGORY':
				return CATEGORY_OPTIONS;
			default:
				return [];
		}
	};

	return (
		<div>
			<Tabs activeKey={activeTab} onChange={setActiveTab} items={[
				{
					key: 'manage',
					label: '이벤트 운영 목록',
					children: (
			<>
			<Card
				extra={
					<Space wrap>
						<Select
							value={statusFilter || ''}
							options={STATUS_OPTIONS}
							onChange={(value) => handleStatusToggle(value || undefined)}
							style={{ width: 140 }}
						/>
						<Input.Search
							allowClear
							placeholder="이벤트명 검색"
							style={{ width: 220 }}
							value={keywordFilter}
							onChange={(e) => setKeywordFilter(e.target.value)}
							onSearch={() => fetchEvents()}
						/>
						<Radio.Group value={eventView} onChange={(e) => setEventView(e.target.value)}>
							<Radio.Button value={VIEW_GRID}><AppstoreOutlined /></Radio.Button>
							<Radio.Button value={VIEW_LIST}><UnorderedListOutlined /></Radio.Button>
						</Radio.Group>
						<Button icon={<ReloadOutlined />} onClick={() => fetchEvents()} loading={loading}>
							새로고침
						</Button>
						<Button type="primary" icon={<PlusOutlined />} onClick={openCreateModal}>
							이벤트 추가
						</Button>
					</Space>
				}
			>
				<Row gutter={16} className="mb-4">
					<Col xs={12} md={6}><Card size="small"><Text type="secondary">전체</Text><h3 className="mb-0">{eventSummary.total}</h3></Card></Col>
					<Col xs={12} md={6}><Card size="small"><Text type="secondary">비공개</Text><h3 className="mb-0">{eventSummary.PRIVATE}</h3></Card></Col>
					<Col xs={12} md={6}><Card size="small"><Text type="secondary">공개</Text><h3 className="mb-0">{eventSummary.PUBLISHED}</h3></Card></Col>
					<Col xs={12} md={6}><Card size="small"><Text type="secondary">종료</Text><h3 className="mb-0">{eventSummary.ENDED}</h3></Card></Col>
				</Row>
				<Spin spinning={loading}>
					{events.length === 0 ? (
						<Card><Empty description="표시할 이벤트가 없습니다." /></Card>
					) : eventView === VIEW_LIST ? (
						events.map(event => (
							<EventOperationListItem
								event={event}
								onEdit={openEditModal}
								onPerformance={openPerformance}
								key={event.eventNo}
							/>
						))
					) : (
						<Row gutter={16}>
							{events.map(event => (
								<Col xs={24} sm={24} lg={12} xl={8} xxl={6} key={event.eventNo}>
									<EventOperationGridItem
										event={event}
										onEdit={openEditModal}
										onPerformance={openPerformance}
									/>
								</Col>
							))}
						</Row>
					)}
				</Spin>
			</Card>

			<Modal
				title={editingEvent ? '이벤트 수정' : '이벤트 추가'}
				open={modalVisible}
				onOk={handleSave}
				okText="저장"
				cancelText="취소"
				footer={editingEvent?.eventStatus === 'ENDED' ? null : undefined}
				confirmLoading={saving}
				onCancel={() => {
					setModalVisible(false);
					setEditingEvent(null);
					setParticipants([]);
					form.resetFields();
				}}
				width={900}
				extra={
					editingEvent ? (
						<Space>
							<Button onClick={() => openPerformance(editingEvent.eventNo)}>
								실적 보기
							</Button>
							{editingEvent?.eventStatus !== 'ENDED' && (
								<Button
									danger
									onClick={() => {
										const now = dayjs();
										const endAt = editingEvent?.customerEventEndAt ? dayjs(editingEvent.customerEventEndAt) : null;
										const early = endAt && now.isBefore(endAt);
										Modal.confirm({
											title: '이벤트 강제 종료',
											content: early
												? '고객기간 종료 전 조기 종료됩니다. 재활성화할 수 없습니다. 계속하시겠습니까?'
												: '이 이벤트를 종료합니다. 재활성화할 수 없습니다. 계속하시겠습니까?',
											okText: '강제 종료',
											okButtonProps: { danger: true },
											cancelText: '취소',
											onOk: async () => {
												try {
													await AdminService.updateAdminEventStatus(editingEvent.eventNo, 'ENDED');
													message.success('이벤트가 종료되었습니다.');
													setModalVisible(false);
													setEditingEvent(null);
													await fetchEvents();
													if (activeTab === 'endedPerformance') {
														await fetchEndedList(true);
														await refreshCharts();
													}
												} catch (e) {
													message.error(e?.response?.data?.message || '강제 종료에 실패했습니다.');
												}
											}
										});
									}}
								>
									강제 종료
								</Button>
							)}
						</Space>
					) : null
				}
			>
				<Form
					form={form}
					layout="vertical"
					disabled={editingEvent?.eventStatus === 'ENDED'}
				>
					<Form.Item
						name="eventMode"
						label="참여 형태"
						rules={[{ required: true, message: '참여 형태를 선택해주세요.' }]}
					>
						<Radio.Group
							options={EVENT_MODE_OPTIONS}
							onChange={(e) => {
								const nextMode = e.target.value;
								const curType = form.getFieldValue('eventType');
								const allowed = EVENT_TYPES_BY_MODE[nextMode] || [];
								const nextType =
									curType && allowed.includes(curType)
										? curType
										: getDefaultEventTypeForMode(nextMode);
								const updates = { eventType: nextType };
								if (nextType === 'POINT' && nextMode === 'PARTNER_PARTICIPATION') {
									const allowedPt = getPointTargetTypeOptionsForMode(nextMode).map((o) => o.value);
									const curPt = form.getFieldValue('pointEventTargetType');
									if (!curPt || !allowedPt.includes(curPt)) {
										updates.pointEventTargetType = 'ALL';
										updates.pointEventTargetValues = [];
										updates.pointEventMinOrderAmount = null;
									}
								}
								form.setFieldsValue(updates);
							}}
						/>
					</Form.Item>

					<Space style={{ width: '100%' }} size={16} align="start" wrap>
						<Form.Item
							name="eventType"
							label="이벤트 종류"
							rules={[{ required: true, message: '이벤트 종류를 선택해주세요.' }]}
							style={{ minWidth: 220 }}
						>
							<Select
								placeholder="이벤트 종류 선택"
								options={getEventTypeOptionsForMode(eventMode || 'ADMIN_ONLY')}
								onChange={(v) => {
									const mode = form.getFieldValue('eventMode') || 'ADMIN_ONLY';
									if (v === 'POINT' && mode === 'PARTNER_PARTICIPATION') {
										const allowedPt = getPointTargetTypeOptionsForMode('PARTNER_PARTICIPATION').map((o) => o.value);
										const curPt = form.getFieldValue('pointEventTargetType');
										if (!curPt || !allowedPt.includes(curPt)) {
											form.setFieldsValue({
												pointEventTargetType: 'ALL',
												pointEventTargetValues: [],
												pointEventMinOrderAmount: null
											});
										}
									}
								}}
							/>
						</Form.Item>

						<Form.Item
							name="eventStatus"
							label="상태"
							rules={[{ required: true, message: '이벤트 상태를 선택해주세요.' }]}
							style={{ width: 200 }}
						>
							<Select
								disabled={editingEvent?.eventStatus === 'ENDED'}
								options={EVENT_STATUSES.map((status) => ({
									value: status,
									label: STATUS_LABELS[status] || status
								}))}
							/>
						</Form.Item>
					</Space>

					<Form.Item
						name="eventTitle"
						label="이벤트명"
						rules={[{ required: true, message: '이벤트명을 입력해주세요.' }]}
					>
						<Input placeholder="이벤트명을 입력하세요" />
					</Form.Item>

					<Form.Item
						name="eventContent"
						label="이벤트 내용"
						rules={[{ required: true, message: '이벤트 내용을 입력해주세요.' }]}
					>
						<TextArea rows={5} placeholder="이벤트 내용을 입력하세요" />
					</Form.Item>

					{(eventType === 'SALE' || eventType === 'POINT') && (
						<Space style={{ width: '100%' }} size={16} align="start">
							<Form.Item
								name="saleDiscountType"
								label={eventType === 'POINT' ? '추가 포인트 지급 종류' : '세일 종류'}
								rules={[{ required: true, message: '종류를 선택해주세요.' }]}
								style={{ width: 240 }}
							>
								<Select options={SALE_DISCOUNT_TYPE_OPTIONS} />
							</Form.Item>

							<Form.Item
								name="saleDiscountValue"
								label={eventType === 'POINT' ? '추가 포인트 지급 값' : '세일 값'}
								rules={[{ required: true, message: '할인 값을 입력해주세요.' }]}
								style={{ width: 240 }}
							>
								<InputNumber min={1} style={{ width: '100%' }} />
							</Form.Item>

							{eventType === 'SALE' && (
								<Form.Item
									name="saleMaxDiscountAmount"
									label="최대 할인 금액(선택)"
									style={{ width: 240 }}
								>
									<InputNumber min={1} style={{ width: '100%' }} />
								</Form.Item>
							)}
						</Space>
					)}

					{eventType === 'POINT' && (
						<>
							<div style={{ marginBottom: 16, padding: 12, background: '#f6f8fa', borderRadius: 6, color: '#555' }}>
								포인트는 동일 이벤트 기준 고객 1인당 최대 3회 지급
							</div>
							<Space style={{ width: '100%' }} size={16} align="start">
								<Form.Item
									name="pointEventTargetType"
									label="포인트 대상 종류"
									rules={[{ required: true, message: '포인트 대상 종류를 선택해주세요.' }]}
									style={{ width: 280 }}
								>
									<Select
										options={getPointTargetTypeOptionsForMode(eventMode || 'ADMIN_ONLY')}
										onChange={() => {
											form.setFieldsValue({
												pointEventTargetValues: [],
												pointEventMinOrderAmount: null
											});
										}}
									/>
								</Form.Item>

								{pointEventTargetType === 'MIN_ORDER_AMOUNT' && (
									<Form.Item
										name="pointEventMinOrderAmount"
										label="최소 주문 금액(원)"
										rules={[{ required: true, message: '최소 주문 금액을 입력해주세요.' }]}
										style={{ width: 280 }}
									>
										<InputNumber min={1} style={{ width: '100%' }} />
									</Form.Item>
								)}
							</Space>

							{pointEventTargetType &&
								pointEventTargetType !== 'ALL' &&
								pointEventTargetType !== 'MIN_ORDER_AMOUNT' && (
									<Form.Item
										name="pointEventTargetValues"
										label="포인트 대상 선택"
										rules={[{ required: true, message: '대상을 하나 이상 선택해주세요.' }]}
									>
										<Select
											mode="multiple"
											showSearch
											allowClear
											placeholder="대상을 선택하세요"
											options={getPointTargetSelectOptions()}
											optionFilterProp="label"
											maxTagCount="responsive"
											maxTagPlaceholder={(omittedValues) => `외 ${omittedValues.length}개`}
										/>
									</Form.Item>
								)}
						</>
					)}

					{eventType === 'NOTICE' && (
						<div style={{ marginBottom: 16, padding: 12, background: '#fffbe6', borderRadius: 6, color: '#555' }}>
							공지형 이벤트는 제목·내용·기간만 노출됩니다.
						</div>
					)}

					{eventType === 'SALE' && (
						<div style={{ marginBottom: 16, padding: 12, background: '#f6f8fa', borderRadius: 6, color: '#555' }}>
							할인율/기간은 이벤트 정책에 따라 자동 적용됩니다.
						</div>
					)}

					<Space style={{ width: '100%' }} size={16} align="start">
						<Form.Item
							name="customerEventStartAt"
							label="혜택 적용 시작일"
							rules={[{ required: true, message: '이벤트 시작일을 선택해주세요.' }]}
							style={{ width: 260 }}
						>
							<DatePicker format="YYYY-MM-DD" style={{ width: '100%' }} />
						</Form.Item>
						<Form.Item
							name="customerEventEndAt"
							label="혜택 적용 종료일"
							rules={[{ required: true, message: '이벤트 종료일을 선택해주세요.' }]}
							style={{ width: 260 }}
						>
							<DatePicker format="YYYY-MM-DD" style={{ width: '100%' }} />
						</Form.Item>
					</Space>

					<Form.Item
						name="customerExposeAt"
						label="사이트 공개 시작일"
						tooltip="시각이 지나면 고객 사이트에에 노출됩니다."
					>
						<DatePicker showTime format="YYYY-MM-DD HH:mm" style={{ width: '100%', maxWidth: 280 }} allowClear placeholder="선택 안 함 (고객 비공개)" />
					</Form.Item>

					{eventMode === 'PARTNER_PARTICIPATION' && (
						<Space style={{ width: '100%' }} size={16} align="start">
							<Form.Item
								name="partnerApplyStartAt"
								label="파트너 신청 시작일"
								rules={[{ required: true, message: '파트너 신청 시작일을 선택해주세요.' }]}
								style={{ width: 260 }}
							>
								<DatePicker format="YYYY-MM-DD" style={{ width: '100%' }} />
							</Form.Item>
							<Form.Item
								name="partnerApplyEndAt"
								label="파트너 신청 종료일"
								rules={[{ required: true, message: '파트너 신청 종료일을 선택해주세요.' }]}
								style={{ width: 260 }}
							>
								<DatePicker format="YYYY-MM-DD" style={{ width: '100%' }} />
							</Form.Item>
						</Space>
					)}

					<Form.Item name="thumbnailUrl" label="썸네일 이미지">
						<Input type="hidden" />
					</Form.Item>
					<Form.Item label="썸네일 업로드">
						<Space align="start" wrap>
							{thumbnailUrl && (
								<img
									src={thumbnailUrl}
									alt="이벤트 썸네일 미리보기"
									style={{ width: 120, height: 80, objectFit: 'cover', borderRadius: 6, border: '1px solid #f0f0f0' }}
								/>
							)}
							<Space direction="vertical">
								<Upload
									maxCount={1}
									beforeUpload={beforeUploadImage}
									customRequest={handleUploadThumbnail}
									showUploadList={false}
								>
									<Button icon={<UploadOutlined />} loading={saving}>
										이미지 업로드
									</Button>
								</Upload>
								{thumbnailUrl && (
									<Button
										icon={<DeleteOutlined />}
										onClick={() => form.setFieldsValue({ thumbnailUrl: '' })}
									>
										이미지 제거
									</Button>
								)}
								<Text type="secondary">jpg, png, gif, webp / 10MB 이하</Text>
							</Space>
						</Space>
					</Form.Item>

					<Form.Item name="adminMemo" label="관리자 메모">
						<TextArea rows={3} placeholder="관리자 메모를 입력하세요" />
					</Form.Item>

					{editingEvent?.eventMode === 'PARTNER_PARTICIPATION' && (
						<Form.Item label="참여 파트너">
							<Table
								size="small"
								rowKey={(row) => `${row.partnerId}-${row.participatedAt || 'na'}`}
								pagination={false}
								dataSource={participants}
								locale={{ emptyText: '참여 이력이 없습니다.' }}
								columns={[
									{
										title: '파트너 ID',
										dataIndex: 'partnerId',
										width: 110
									},
									{
										title: '파트너명',
										dataIndex: 'partnerName',
										width: 180
									},
									{
										title: '연락처',
										dataIndex: 'partnerContact',
										width: 150
									},
									{
										title: '상태',
										dataIndex: 'active',
										width: 100,
										render: (active) => (
											<Tag color={active ? 'green' : 'default'}>
												{active ? '참여중' : '해제'}
											</Tag>
										)
									},
									{
										title: '신청일시',
										dataIndex: 'participatedAt',
										width: 140,
										render: (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')
									},
									{
										title: '연동 세일 건수',
										dataIndex: 'linkedSalePolicyCount',
										width: 120,
										render: (value) => Number(value || 0)
									},
									{
										title: '세일 대상(상품/옵션)',
										dataIndex: 'linkedSaleTargets',
										render: (value) => value || '-'
									}
								]}
							/>
						</Form.Item>
					)}
				</Form>
			</Modal>
			</>
			)},
				{
					key: 'endedPerformance',
					label: '이벤트 실적',
					children: (
						<Card
							title={null}
							extra={
								<Space>
									{selectedPerfRow ? (
										<Tag color="blue">
											선택: #{selectedPerfRow.eventNo} {selectedPerfRow.eventTitle}
										</Tag>
									) : (
										<Tag>전체 종료 이벤트 합산</Tag>
									)}
									{selectedPerfRow && (
										<Button onClick={() => setSelectedPerfRow(null)}>선택 해제</Button>
									)}
									<RangePicker
										placeholder={['시작일', '종료일']}
										value={perfFrom && perfTo ? [perfFrom, perfTo] : []}
										onChange={(vals) => {
											if (vals && vals.length === 2) {
												setPerfFrom(vals[0]);
												setPerfTo(vals[1]);
											} else {
												setPerfFrom(null);
												setPerfTo(null);
											}
										}}
									/>
									<Button type="primary" loading={perfLoading} onClick={() => fetchEndedList()}>
										조회
									</Button>
									<Button onClick={() => { setPerfFrom(null); setPerfTo(null); fetchEndedList(true); }}>
										전체 기간
									</Button>
								</Space>
							}
						>
							{/* 요약/차트 영역 */}
							<Card size="small" style={{ marginBottom: 16 }}>
								{(() => {
									const src = selectedPerfRow || {};
									const totals = selectedPerfRow
										? {
												totalOrders: src.totalOrders || 0,
												totalOrderItems: src.totalOrderItems || 0,
												totalNetAmount: src.totalNetAmount || 0,
												adminRewardPoint: src.adminRewardPoint || 0,
												partnerRewardPoint: src.partnerRewardPoint || 0,
										  }
										: endedRows.reduce(
												(acc, r) => ({
													totalOrders: acc.totalOrders + (r.totalOrders || 0),
													totalOrderItems: acc.totalOrderItems + (r.totalOrderItems || 0),
													totalNetAmount: acc.totalNetAmount + (r.totalNetAmount || 0),
													adminRewardPoint: acc.adminRewardPoint + (r.adminRewardPoint || 0),
													partnerRewardPoint: acc.partnerRewardPoint + (r.partnerRewardPoint || 0),
												}),
												{ totalOrders: 0, totalOrderItems: 0, totalNetAmount: 0, adminRewardPoint: 0, partnerRewardPoint: 0 }
										  );
									return (
										<>
											{/* 상단: 일자별 추이 + Top N 파트너 먼저 배치 */}
											<Row gutter={[16, 16]}>
												<Col xs={24}>
													<Card size="small" title="일자별 추이(주문/매출)">
														<Chart
															type="area"
															height={280}
															series={[
																{ name: '주문 수', data: (timeseries || []).map(p => ({ x: p.date, y: p.orders })) },
																{ name: '매출(원)', data: (timeseries || []).map(p => ({ x: p.date, y: p.netAmount })) }
															]}
															options={{
																dataLabels: { enabled: false },
																stroke: { curve: 'smooth' },
																xaxis: { type: 'datetime' },
																legend: { position: 'top' },
																yaxis: [
																	{
																		title: { text: '주문 수' },
																		labels: {
																			formatter: (val) => {
																				const n = Number(val || 0);
																				return new Intl.NumberFormat('ko-KR').format(n);
																			}
																		}
																	},
																	{
																		opposite: true,
																		title: { text: '매출(원)' },
																		labels: {
																			formatter: (val) => {
																				const n = Number(val || 0);
																				if (Math.abs(n) >= 100000) {
																					return new Intl.NumberFormat('ko-KR', { notation: 'compact' }).format(n);
																				}
																				return new Intl.NumberFormat('ko-KR').format(n);
																			}
																		}
																	}
																],
																tooltip: {
																	y: {
																		formatter: (val, { seriesIndex }) => {
																			const n = Number(val || 0);
																			if (seriesIndex === 1) {
																				return `${new Intl.NumberFormat('ko-KR').format(n)} 원`;
																			}
																			return new Intl.NumberFormat('ko-KR').format(n);
																		}
																	}
																}
															}}
														/>
													</Card>
												</Col>
											</Row>
											<Row gutter={[16, 16]} style={{ marginTop: 8 }}>
												<Col xs={24}>
													<Card
														size="small"
														title={`Top ${topLimit} ${topType === 'partner' ? '파트너' : '상품'}`}
														extra={
															<Space size={4}>
																<Button type={topType === 'partner' ? 'primary' : 'default'} size="small" onClick={() => setTopType('partner')}>파트너</Button>
																<Button type={topType === 'product' ? 'primary' : 'default'} size="small" onClick={() => setTopType('product')}>상품</Button>
															</Space>
														}
													>
														<Chart
															type="bar"
															height={320}
															series={[{ name: '매출(원)', data: (topData || []).map(it => it.netAmount) }]}
															options={{
																plotOptions: { bar: { horizontal: true, barHeight: '70%' } },
																xaxis: {
																	labels: {
																		formatter: (val) => {
																			const n = Number(val || 0);
																			if (Math.abs(n) >= 100000) {
																				return new Intl.NumberFormat('ko-KR', { notation: 'compact' }).format(n);
																			}
																			return new Intl.NumberFormat('ko-KR').format(n);
																		}
																	},
																	tickAmount: 4
																},
																yaxis: {
																	categories: (topData || []).map(it => it.name?.length > 22 ? `${it.name.slice(0, 22)}…` : it.name)
																},
																grid: { xaxis: { lines: { show: false } } },
																dataLabels: { enabled: false },
																tooltip: {
																	y: {
																		formatter: (val) => `${new Intl.NumberFormat('ko-KR').format(Number(val || 0))} 원`
																	}
																},
																legend: { show: false }
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
													<Card size="small" title="이벤트 유형별 비율(운영 직접 등록 / 파트너 신청·등록)">
														<Chart
															type="donut"
															height={260}
															series={[totals.adminRewardPoint || 0, totals.partnerRewardPoint || 0]}
															options={{
																labels: ['운영 직접 등록', '파트너 신청·등록'],
																dataLabels: { enabled: true },
																legend: { position: 'bottom' }
															}}
														/>
													</Card>
												</Col>
												<Col xs={24} md={12}>
													<Space direction="vertical" style={{ width: '100%' }} size={12}>
														<Card size="small" title="관리자 단독 이벤트 금액">
															<div style={{ fontSize: 22, fontWeight: 700 }}>
																{(totals.adminRewardPoint || 0).toLocaleString()} P
															</div>
														</Card>
														<Card size="small" title="파트너 참여형 이벤트 금액">
															<div style={{ fontSize: 22, fontWeight: 700 }}>
																{(totals.partnerRewardPoint || 0).toLocaleString()} P
															</div>
														</Card>
													</Space>
												</Col>
											</Row>
										</>
									);
								})()}
							</Card>

							<Table
								rowKey="eventNo"
								loading={perfLoading}
								dataSource={endedRows}
								columns={[
									{ title: '번호', dataIndex: 'eventNo', width: 90 },
									{ title: '이벤트명', dataIndex: 'eventTitle', ellipsis: true },
									{ title: '상태', dataIndex: 'eventStatus', width: 120, render: (s) => <Tag>{s}</Tag> },
									{ title: '기간', width: 260, render: (_, r) => `${formatDateTimeForList(r.customerEventStartAt)} ~ ${formatDateTimeForList(r.customerEventEndAt)}` },
									{ title: '주문수', dataIndex: 'totalOrders', width: 100 },
									{ title: '주문상품수', dataIndex: 'totalOrderItems', width: 110 },
									{ title: '총 매출(원)', dataIndex: 'totalNetAmount', width: 140, render: (v) => (v || 0).toLocaleString() },
									{
										title: '이벤트 유형',
										key: 'eventTypeDisplay',
										width: 220,
										render: (_, r) => {
											const adminAmt = r.adminRewardPoint || 0;
											const partnerAmt = r.partnerRewardPoint || 0;
											// 파트너 보상이 있으면 파트너 신청·등록으로 간주
											const isPartner = partnerAmt > 0;
											return (
												<Space size={6}>
													<Tag color={isPartner ? 'blue' : 'default'}>
														{isPartner ? '파트너 신청·등록' : '운영 직접 등록'}
													</Tag>
													<span style={{ color: '#555' }}>
														{(isPartner ? partnerAmt : adminAmt).toLocaleString()} P
													</span>
												</Space>
											);
										}
									},
									{ title: '작업', width: 120, render: (_, r) => <Button type="link" onClick={() => openPerformance(r.eventNo)}>상세</Button> }
								]}
								onRow={(record) => ({
									onClick: () => setSelectedPerfRow(record),
								})}
								rowClassName={(record) => (selectedPerfRow?.eventNo === record.eventNo ? 'ant-table-row-selected' : '')}
								pagination={{ pageSize: 10, showSizeChanger: true }}
							/>
						</Card>
					)
				}
			]} />

			{/* 성과 Drawer (상세) */}
			<Drawer
				title={`이벤트 실적 ${perfEventNo ? `(#${perfEventNo})` : ''}`}
				open={perfOpen}
				onClose={() => setPerfOpen(false)}
				width={720}
				destroyOnClose
			>
				<Space direction="vertical" style={{ width: '100%' }} size={16}>
					<Space wrap>
						<RangePicker
							placeholder={['시작일', '종료일']}
							value={perfFrom && perfTo ? [perfFrom, perfTo] : []}
							onChange={(vals) => {
								if (vals && vals.length === 2) {
                                    setPerfFrom(vals[0]);
                                    setPerfTo(vals[1]);
								} else {
									setPerfFrom(null);
									setPerfTo(null);
								}
							}}
						/>
						<Button
							type="primary"
							loading={perfLoading}
							onClick={() => fetchPerformance(perfEventNo, perfFrom, perfTo)}
						>
							조회
						</Button>
						<Button
							onClick={() => {
								setPerfFrom(null);
								setPerfTo(null);
								fetchPerformance(perfEventNo, null, null);
							}}
						>
							전체 기간
						</Button>
					</Space>

					<Card loading={perfLoading}>
						<Row gutter={[16, 16]}>
							<Col xs={12} md={8}>
								<Statistic title="주문 수" value={perfData?.totalOrders || 0} />
							</Col>
							<Col xs={12} md={8}>
								<Statistic title="주문상품 수" value={perfData?.totalOrderItems || 0} />
							</Col>
							<Col xs={24} md={8}>
								<Statistic
									title="총 매출(원)"
									value={perfData?.totalNetAmount || 0}
									precision={0}
								/>
							</Col>
							<Col xs={12} md={12}>
								<Statistic title="관리자 보상 포인트" value={perfData?.adminRewardPoint || 0} />
							</Col>
							<Col xs={12} md={12}>
								<Statistic title="파트너 보상 포인트" value={perfData?.partnerRewardPoint || 0} />
							</Col>
							<Col xs={24}>
								<Card size="small">
									{(() => {
										const net = perfData?.totalNetAmount || 0;
										const reward = (perfData?.adminRewardPoint || 0) + (perfData?.partnerRewardPoint || 0);
										const ratio = net > 0 ? Math.round((reward / net) * 1000) / 10 : 0;
										return (
											<div style={{ display: 'flex', alignItems: 'baseline', gap: 12 }}>
												<div style={{ fontSize: 14, color: '#666' }}>보상/매출 비율</div>
												<div style={{ fontSize: 22, fontWeight: 700 }}>{ratio}%</div>
												<div style={{ color: '#888' }}>
													보상 {reward.toLocaleString()} P / 매출 {net.toLocaleString()} 원
												</div>
											</div>
										);
									})()}
								</Card>
							</Col>
						</Row>
					</Card>

					{/* 관리자 뷰에서는 식별자 노출 최소화를 위해 샘플 주문상품 번호는 표시하지 않습니다. */}
				</Space>
			</Drawer>
		</div>
	);
};

export default EventManagement;
