import React, { useEffect, useState } from 'react';
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
	Switch,
	DatePicker,
	TimePicker,
	Space,
	Tag,
	message,
	Statistic,
	Row,
	Col,
	Tabs
} from 'antd';
import Chart from 'react-apexcharts';
import { PlusOutlined, EditOutlined, ReloadOutlined } from '@ant-design/icons';
import AdminService from 'services/AdminService';

const { RangePicker } = DatePicker;
const { TextArea } = Input;

const EVENT_STATUSES = ['DRAFT', 'SCHEDULED', 'ACTIVE', 'ENDED', 'INACTIVE'];
const STATUS_LABELS = {
	DRAFT: '임시 저장',
	SCHEDULED: '오픈 예정',
	ACTIVE: '진행 중',
	ENDED: '종료',
	INACTIVE: '비활성화'
};

const EVENT_MODE_OPTIONS = [
	{ value: 'ADMIN_ONLY', label: '관리자 단독' },
	{ value: 'PARTNER_PARTICIPATION', label: '파트너 참여형' }
];

const EVENT_TYPE_OPTIONS = [
	// backend에서 내려오는 eventType 값(영문) 기준으로 value를 맞춰야
	// edit modal 진입 시 영문이 그대로 노출되지 않습니다.
	{ value: 'SALE', label: '세일(SALE)' },
	{ value: 'GENERAL', label: '일반' },
	{ value: 'COUPON', label: '쿠폰' },
	{ value: 'POINT', label: '포인트' },
	{ value: 'COMMENT', label: '댓글' },
	{ value: 'ATTENDANCE', label: '출석' }
];

const SALE_DISCOUNT_TYPE_OPTIONS = [
	{ value: 'PERCENT', label: '정률(%)' },
	{ value: 'FIXED', label: '정액(원)' }
]

const POINT_TARGET_TYPE_OPTIONS = [
	{ value: 'ALL', label: '전체(ALL)' },
	{ value: 'PARTNER', label: '파트너(PARTNER)' },
	{ value: 'PRODUCT', label: '상품(PRODUCT)' },
	{ value: 'OPTION', label: '옵션(OPTION)' },
	{ value: 'CATEGORY', label: '카테고리(CATEGORY)' },
	{ value: 'MIN_ORDER_AMOUNT', label: '최소주문금액(MIN_ORDER_AMOUNT)' }
]

const CATEGORY_OPTIONS = [
	{ value: 'SWIMSUIT_MEN', label: '남성 수영복' },
	{ value: 'SWIMSUIT_WOMEN', label: '여성 수영복' },
	{ value: 'SWIMSUIT_KIDS', label: '아동 수영복' },
	{ value: 'SWIM_CAP', label: '수영모자' },
	{ value: 'SWIM_GOGGLES', label: '수영안경' },
	{ value: 'FINS', label: '오리발' },
	{ value: 'SWIM_TOY', label: '수영용품' },
	{ value: 'ETC', label: '기타' }
]

const EventManagement = () => {
	const [loading, setLoading] = useState(false);
	const [saving, setSaving] = useState(false);
	const [events, setEvents] = useState([]);
	const [currentPage, setCurrentPage] = useState(1);
	const [pageSize, setPageSize] = useState(10);
	const [totalItems, setTotalItems] = useState(0);
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
	const [selectedPerfRow, setSelectedPerfRow] = useState(null);
	const [timeseries, setTimeseries] = useState([]);
	const [topType, setTopType] = useState('partner');
	const [topData, setTopData] = useState([]);
	const [topLimit, setTopLimit] = useState(10);
	const [perfExtraLoading, setPerfExtraLoading] = useState(false);
	const [form] = Form.useForm();

	const eventUseTime = Form.useWatch('eventUseTime', form);
	const participationUseTime = Form.useWatch('participationUseTime', form);
	const eventMode = Form.useWatch('eventMode', form);
	const eventType = Form.useWatch('eventType', form);
	const pointEventTargetType = Form.useWatch('pointEventTargetType', form);

	const combineDateAndTime = (dateValue, timeValue) => {
		if (!dateValue || !timeValue) return null;
		return dateValue
			.hour(timeValue.hour())
			.minute(timeValue.minute())
			.second(0)
			.millisecond(0);
	};

	const toStartOfDay = (dateValue) => {
		if (!dateValue) return null;
		return dateValue.hour(0).minute(0).second(0).millisecond(0);
	};

	const toEndOfDay = (dateValue) => {
		if (!dateValue) return null;
		return dateValue.hour(23).minute(59).second(59).millisecond(0);
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
	}, [currentPage, pageSize]);

	const normalizeResponseArray = (response) => {
		const data = response?.data ?? response;
		return Array.isArray(data) ? data : [];
	};

	const loadPointTargetOptions = async () => {
		try {
			const [partnersResponse, productsResponse] = await Promise.all([
				AdminService.getAllPartners('APPROVED'),
				AdminService.getAllProducts('ACTIVE')
			]);
			const partners = normalizeResponseArray(partnersResponse);
			const products = normalizeResponseArray(productsResponse);

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
	}, [activeTab]);

	useEffect(() => {
		if (activeTab === 'endedPerformance') {
			refreshCharts();
		}
	// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [selectedPerfRow, perfFrom, perfTo, topType]);

	const refreshCharts = async () => {
		try {
			setPerfExtraLoading(true);
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
		} finally {
			setPerfExtraLoading(false);
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
			// 0-based 호환: 서버가 0-based일 수 있으므로 page-1 보냄(음수 방지)
			const reqPage = Math.max(0, (currentPage ?? 1) - 1);
			console.log('[Admin Events] request params =>', { page: reqPage, size: pageSize, status: effectiveStatus, keyword: effectiveKeyword });
			const resp = await AdminService.getAdminEventList({
				status: effectiveStatus,
				keyword: effectiveKeyword?.trim() || undefined,
				page: reqPage,
				size: pageSize
			});
			const data = resp?.data ?? resp;
			const rawItems = Array.isArray(data) ? data : (data?.events ?? data?.items ?? data?.data ?? []);
			const meta = data?.meta;
			console.log('[Admin Events] response meta =>', { page: meta?.page ?? data?.page, size: meta?.size ?? data?.size, total: meta?.total ?? data?.total, itemsCount: Array.isArray(rawItems) ? rawItems.length : 0 });

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
			// 배열 fallback: total/페이지 계산 보정
			const totalFromServer = Number((meta?.total ?? data?.total));
			const total = Number.isFinite(totalFromServer) ? totalFromServer : filtered.length;
			setTotalItems(total);

			// 0-based 응답 페이지를 1-based로 보정
			if (meta?.page !== undefined) setCurrentPage((meta.page ?? 0) + 1);
			else if (data?.page !== undefined) setCurrentPage((data.page ?? 0) + 1);
			if (meta?.size !== undefined) setPageSize(meta.size);
			else if (data?.size !== undefined) setPageSize(data.size);
		} catch (err) {
			console.error('이벤트 목록 조회 실패:', err);
			message.error(err?.response?.data?.message || '이벤트 목록을 불러오는데 실패했습니다.');
			setEvents([]);
			setTotalItems(0);
		} finally {
			setLoading(false);
		}
	};

	const handleStatusToggle = async (nextStatus) => {
		setStatusFilter(nextStatus);
		await fetchEvents({ status: nextStatus });
	};

	const openCreateModal = () => {
		setEditingEvent(null);
		setParticipants([]);
		form.resetFields();
		loadPointTargetOptions();
		form.setFieldsValue({
			eventStatus: 'DRAFT',
			eventMode: 'ADMIN_ONLY',
			pointEventTargetType: 'ALL',
			pointEventTargetValues: [],
			eventUseTime: false,
			participationUseTime: false
		});
		setModalVisible(true);
	};

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
			loadPointTargetOptions();

			const eventStart = detail.customerEventStartAt ? dayjs(detail.customerEventStartAt) : null;
			const eventEnd = detail.customerEventEndAt ? dayjs(detail.customerEventEndAt) : null;

			const isDefaultEventStart = eventStart?.isValid() && eventStart.hour() === 0 && eventStart.minute() === 0;
			const isDefaultEventEnd =
				eventEnd?.isValid() && eventEnd.hour() === 23 && eventEnd.minute() === 59 && eventEnd.second() === 59;
			const inferredEventUseTime = !(isDefaultEventStart && isDefaultEventEnd);

			const partnerApplyEnabledValue = detail.eventMode === 'PARTNER_PARTICIPATION';
			const participationStart = detail.partnerApplyStartAt ? dayjs(detail.partnerApplyStartAt) : null;
			const participationEnd = detail.partnerApplyEndAt ? dayjs(detail.partnerApplyEndAt) : null;

			const isDefaultParticipationStart =
				participationStart?.isValid() && participationStart.hour() === 0 && participationStart.minute() === 0;
			const isDefaultParticipationEnd =
				participationEnd?.isValid() &&
				participationEnd.hour() === 23 &&
				participationEnd.minute() === 59 &&
				participationEnd.second() === 59;

			const inferredParticipationUseTime = partnerApplyEnabledValue ? !(isDefaultParticipationStart && isDefaultParticipationEnd) : false;

			form.setFieldsValue({
				eventTitle: detail.eventTitle,
				eventContent: detail.eventContent,
				eventStatus: detail.eventStatus,
				eventMode: detail.eventMode || 'ADMIN_ONLY',
				customerEventStartAt: eventStart,
				customerEventEndAt: eventEnd,
				eventUseTime: inferredEventUseTime,
				eventStartTime: eventStart,
				eventEndTime: eventEnd,

				partnerApplyEnabled: partnerApplyEnabledValue,
				participationUseTime: inferredParticipationUseTime,
				partnerApplyStartAt: partnerApplyEnabledValue ? participationStart : null,
				partnerApplyEndAt: partnerApplyEnabledValue ? participationEnd : null,
				partnerApplyStartTime: partnerApplyEnabledValue ? participationStart : null,
				partnerApplyEndTime: partnerApplyEnabledValue ? participationEnd : null,

				thumbnailUrl: detail.thumbnailUrl || '',
				eventType: detail.eventType || '',
				saleDiscountType: detail.saleDiscountType || null,
				saleDiscountValue: detail.saleDiscountValue || null,
				saleMaxDiscountAmount: detail.saleMaxDiscountAmount || null,
				pointEventTargetType: detail.pointEventTargetType || 'ALL',
				pointEventTargetValues:
					(Array.isArray(detail.pointEventTargetValues) && detail.pointEventTargetValues.length > 0)
						? detail.pointEventTargetValues
						: [],
				pointEventMinOrderAmount: detail.pointEventMinOrderAmount || null,
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

			const finalEventStartAt = eventUseTime
				? combineDateAndTime(values.customerEventStartAt, values.eventStartTime)
				: toStartOfDay(values.customerEventStartAt);
			const finalEventEndAt = eventUseTime
				? combineDateAndTime(values.customerEventEndAt, values.eventEndTime)
				: toEndOfDay(values.customerEventEndAt);

			const enabledParticipation = values.eventMode === 'PARTNER_PARTICIPATION';
			const finalParticipationStartAt = enabledParticipation
				? participationUseTime
					? combineDateAndTime(values.partnerApplyStartAt, values.partnerApplyStartTime)
					: toStartOfDay(values.partnerApplyStartAt)
				: null;
			const finalParticipationEndAt = enabledParticipation
				? participationUseTime
					? combineDateAndTime(values.partnerApplyEndAt, values.partnerApplyEndTime)
					: toEndOfDay(values.partnerApplyEndAt)
				: null;

			const payload = {
				eventTitle: values.eventTitle?.trim(),
				eventContent: values.eventContent?.trim(),
				eventStatus: values.eventStatus,
				eventMode: values.eventMode,
				// LocalDateTime API에 맞춰 timezone 없는 문자열로 전송
				customerEventStartAt: finalEventStartAt?.format('YYYY-MM-DDTHH:mm:ss'),
				customerEventEndAt: finalEventEndAt?.format('YYYY-MM-DDTHH:mm:ss'),
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

	const columns = [
		{
			title: '번호',
			dataIndex: 'eventNo',
			key: 'eventNo',
			width: 80,
			align: 'center',
			sorter: (a, b) => a.eventNo - b.eventNo
		},
		{
			title: '이벤트명',
			dataIndex: 'eventTitle',
			key: 'eventTitle',
			ellipsis: true
		},
		{
			title: '상태',
			dataIndex: 'eventStatus',
			key: 'eventStatus',
			width: 140,
			render: (status) => <Tag color="blue">{STATUS_LABELS[status] || status}</Tag>
		},
		{
			title: '이벤트 기간',
			key: 'eventPeriod',
			width: 300,
			render: (_, record) =>
					`${formatDateTimeForList(record.customerEventStartAt)} ~ ${formatDateTimeForList(record.customerEventEndAt)}`
		},
		{
			title: '작업',
			key: 'action',
			width: 120,
			render: (_, record) => (
				<Space>
					<Button type="link" icon={<EditOutlined />} onClick={() => openEditModal(record.eventNo)}>
						수정
					</Button>
					<Button type="link" onClick={() => openPerformance(record.eventNo)}>
						실적
					</Button>
				</Space>
			)
		}
	];

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
					label: '이벤트 관리',
					children: (
			<>
			<Card
				title={null}
				extra={
					<Space>
						<Space size={6}>
							<Button type={statusFilter === undefined ? 'primary' : 'default'} onClick={() => handleStatusToggle(undefined)}>
								전체
							</Button>
							{EVENT_STATUSES.map((status) => (
								<Button
									key={status}
									type={statusFilter === status ? 'primary' : 'default'}
									onClick={() => handleStatusToggle(status)}
								>
									{STATUS_LABELS[status] || status}
								</Button>
							))}
						</Space>
						<Input
							placeholder="이벤트명 검색"
							style={{ width: 220 }}
							value={keywordFilter}
							onChange={(e) => setKeywordFilter(e.target.value)}
							onPressEnter={() => { setCurrentPage(1); fetchEvents(); }}
						/>
						<Button icon={<ReloadOutlined />} onClick={() => { setCurrentPage(1); fetchEvents(); }} loading={loading}>
							조회
						</Button>
						<Button type="primary" icon={<PlusOutlined />} onClick={openCreateModal}>
							이벤트 추가
						</Button>
					</Space>
				}
			>
				<div style={{ marginBottom: 8, color: '#666' }}>
					{`총 ${totalItems}건 • 페이지 ${currentPage}/${Math.max(1, Math.ceil(totalItems / pageSize))}`}
				</div>
				<Table
					columns={columns}
					dataSource={events}
					rowKey="eventNo"
					loading={loading}
					size="small"
					pagination={{
						current: currentPage,
						pageSize: pageSize,
						total: totalItems,
						showSizeChanger: true,
						showTotal: (total) => `총 ${total}개`,
						onChange: (p, s) => {
							setCurrentPage(p);
							setPageSize(s);
						}
					}}
				/>
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
					initialValues={{ eventUseTime: false, participationUseTime: false }}
				>
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

					<Space style={{ width: '100%' }} size={16} align="start">
						<Form.Item
							name="eventMode"
							label="이벤트 모드"
							rules={[{ required: true, message: '이벤트 모드를 선택해주세요.' }]}
							style={{ width: 200 }}
						>
							<Select options={EVENT_MODE_OPTIONS} />
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

						<Form.Item
							name="eventType"
							label="이벤트 타입"
							style={{ width: 200 }}
						>
							<Select
								allowClear
								placeholder="이벤트 타입 선택"
								options={EVENT_TYPE_OPTIONS}
							/>
						</Form.Item>

					</Space>

					{(eventType === 'SALE' || eventType === 'POINT') && (
						<Space style={{ width: '100%' }} size={16} align="start">
							<Form.Item
								name="saleDiscountType"
								label={eventType === 'POINT' ? '추가 포인트 지급 타입' : '세일 할인 타입'}
								rules={[{ required: true, message: '할인 타입을 선택해주세요.' }]}
								style={{ width: 240 }}
							>
								<Select options={SALE_DISCOUNT_TYPE_OPTIONS} />
							</Form.Item>

							<Form.Item
								name="saleDiscountValue"
								label={eventType === 'POINT' ? '추가 포인트 지급 값' : '세일 할인 값'}
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
								현재 정책: POINT 이벤트 추가 포인트는 동일 이벤트 기준 고객 1인당 최대 3회 지급(고정)
							</div>
							{pointEventTargetType && (
								<div style={{ marginBottom: 12, padding: 10, background: '#fafbfc', borderRadius: 6, color: '#666' }}>
									{/* 대상 상세 미리보기 */}
									{pointEventTargetType === 'ALL' && <div>대상: 전체(ALL)</div>}
									{pointEventTargetType === 'PRODUCT' && (
										<div>선택 대상: 상품 #{(form.getFieldValue('pointEventTargetValues') || []).slice(0, 5).join(', #')}{(form.getFieldValue('pointEventTargetValues') || []).length > 5 ? ' …' : ''}</div>
									)}
									{pointEventTargetType === 'OPTION' && (
										<div>선택 대상: 옵션 #{(form.getFieldValue('pointEventTargetValues') || []).slice(0, 5).join(', #')}{(form.getFieldValue('pointEventTargetValues') || []).length > 5 ? ' …' : ''}</div>
									)}
									{pointEventTargetType === 'CATEGORY' && (
										<div>선택 대상: {(form.getFieldValue('pointEventTargetValues') || []).slice(0, 5).join(', ')}{(form.getFieldValue('pointEventTargetValues') || []).length > 5 ? ' …' : ''}</div>
									)}
									{pointEventTargetType === 'MIN_ORDER_AMOUNT' && (
										<div>최소 주문금액: {(form.getFieldValue('pointEventMinOrderAmount') || 0).toLocaleString()}원 이상</div>
									)}
								</div>
							)}
							<Space style={{ width: '100%' }} size={16} align="start">
								<Form.Item
									name="pointEventTargetType"
									label="포인트 대상 타입"
									rules={[{ required: true, message: '포인트 대상 타입을 선택해주세요.' }]}
									style={{ width: 280 }}
								>
									<Select
										options={POINT_TARGET_TYPE_OPTIONS}
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
										/>
									</Form.Item>
								)}
						</>
					)}

					{eventType === 'SALE' && (
						<div style={{ marginBottom: 16, padding: 12, background: '#f6f8fa', borderRadius: 6, color: '#555' }}>
							할인율/기간은 이벤트 정책에 따라 자동 적용됩니다.
						</div>
					)}

					<Space style={{ width: '100%' }} size={16} align="start">
						<Form.Item
							name="customerEventStartAt"
							label="고객 이벤트 시작일"
							rules={[{ required: true, message: '이벤트 시작일을 선택해주세요.' }]}
							style={{ width: 260 }}
						>
							<DatePicker format="YYYY-MM-DD" style={{ width: '100%' }} />
						</Form.Item>
						<Form.Item
							name="customerEventEndAt"
							label="고객 이벤트 종료일"
							rules={[{ required: true, message: '이벤트 종료일을 선택해주세요.' }]}
							style={{ width: 260 }}
						>
							<DatePicker format="YYYY-MM-DD" style={{ width: '100%' }} />
						</Form.Item>
					</Space>

					<Form.Item name="eventUseTime" label="시간 포함" valuePropName="checked">
						<Switch
							onChange={(checked) => {
								// 시간 미사용일 때는 이전 선택값을 정리해두면 UX가 깔끔합니다.
								if (!checked) {
									form.setFieldsValue({
										eventStartTime: null,
										eventEndTime: null
									});
								}
							}}
						/>
					</Form.Item>

					{eventUseTime && (
						<Space style={{ width: '100%' }} size={16} align="start">
							<Form.Item
								name="eventStartTime"
								label="이벤트 시작 시간"
								rules={[{ required: true, message: '이벤트 시작 시간을 선택해주세요.' }]}
								style={{ width: 260 }}
							>
								<TimePicker format="HH:mm" style={{ width: '100%' }} />
							</Form.Item>
							<Form.Item
								name="eventEndTime"
								label="이벤트 종료 시간"
								rules={[{ required: true, message: '이벤트 종료 시간을 선택해주세요.' }]}
								style={{ width: 260 }}
							>
								<TimePicker format="HH:mm" style={{ width: '100%' }} />
							</Form.Item>
						</Space>
					)}

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

					{eventMode === 'PARTNER_PARTICIPATION' && (
						<Form.Item name="participationUseTime" label="파트너 신청 시간 포함" valuePropName="checked">
							<Switch
								onChange={(checked) => {
									if (!checked) {
										form.setFieldsValue({
											partnerApplyStartTime: null,
											partnerApplyEndTime: null
										});
									}
								}}
							/>
						</Form.Item>
					)}

					{eventMode === 'PARTNER_PARTICIPATION' && participationUseTime && (
						<Space style={{ width: '100%' }} size={16} align="start">
							<Form.Item
								name="partnerApplyStartTime"
								label="파트너 신청 시작 시간"
								rules={[{ required: true, message: '파트너 신청 시작 시간을 선택해주세요.' }]}
								style={{ width: 260 }}
							>
								<TimePicker format="HH:mm" style={{ width: '100%' }} />
							</Form.Item>
							<Form.Item
								name="partnerApplyEndTime"
								label="파트너 신청 종료 시간"
								rules={[{ required: true, message: '파트너 신청 종료 시간을 선택해주세요.' }]}
								style={{ width: 260 }}
							>
								<TimePicker format="HH:mm" style={{ width: '100%' }} />
							</Form.Item>
						</Space>
					)}

					<Form.Item name="thumbnailUrl" label="썸네일 URL">
						<Input placeholder="https://..." />
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
					label: '이벤트 실적(종료)',
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
									const rewardSum = totals.adminRewardPoint + totals.partnerRewardPoint;
									const adminPct = rewardSum > 0 ? Math.round((totals.adminRewardPoint / rewardSum) * 100) : 0;
									const partnerPct = rewardSum > 0 ? 100 - adminPct : 0;
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
													<Card size="small" title="이벤트 유형별 비율(관리자 단독/파트너 참여형)">
														<Chart
															type="donut"
															height={260}
															series={[totals.adminRewardPoint || 0, totals.partnerRewardPoint || 0]}
															options={{
																labels: ['관리자 단독 이벤트', '파트너 참여형 이벤트'],
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
											// 파트너 보상이 있으면 참여형으로 간주, 아니면 관리자 단독
											const isPartner = partnerAmt > 0;
											return (
												<Space size={6}>
													<Tag color={isPartner ? 'blue' : 'default'}>
														{isPartner ? '파트너 참여형 이벤트' : '관리자 단독 이벤트'}
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
