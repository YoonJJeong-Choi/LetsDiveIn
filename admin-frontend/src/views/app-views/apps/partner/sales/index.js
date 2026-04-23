import React, { useEffect, useMemo, useState } from 'react'
import {
	Alert,
	Button,
	Card,
	DatePicker,
	Form,
	InputNumber,
	Modal,
	Popconfirm,
	Tooltip,
	Switch,
	Select,
	Space,
	Table,
	Tag,
	message
} from 'antd'
import dayjs from 'dayjs'
import PartnerService from 'services/PartnerService'

const SCOPE_OPTIONS = [
	{ value: 'PRODUCT', label: '상품 단위' },
	{ value: 'OPTION', label: '옵션 단위' }
]

const DISCOUNT_TYPE_OPTIONS = [
	{ value: 'PERCENT', label: '정률(%)' },
	{ value: 'FIXED', label: '정액(원)' }
]

const STATUS_LABELS = {
	ACTIVE: '활성',
	PENDING_APPROVAL: '승인 대기',
	INACTIVE: '비활성',
	EXPIRED: '만료',
	CANCELLED: '취소',
	REJECTED: '거절'
}

const extractApiErrorMessage = (e, fallback) => {
	const data = e?.response?.data
	if (data) {
		// ApiResponse 형태: { success, message, errors }
		if (typeof data === 'object') {
			if (Array.isArray(data.errors) && data.errors.length > 0) {
				return data.errors.join('\n')
			}
			if (data.message) {
				return data.message
			}
		}
		if (typeof data === 'string') {
			return data
		}
	}
	return e?.message || fallback
}

const PartnerSales = () => {
	const [loading, setLoading] = useState(false)
	const [saving, setSaving] = useState(false)
	const [list, setList] = useState([])
	const [products, setProducts] = useState([])
	const [saleEvents, setSaleEvents] = useState([])
	const [statusFilter, setStatusFilter] = useState()
	const [todayOnly, setTodayOnly] = useState(false)
	const [tablePage, setTablePage] = useState(1)
	const pageSize = 10
	const [createOpen, setCreateOpen] = useState(false)
	const [form] = Form.useForm()
	const [detailOpen, setDetailOpen] = useState(false)
	const [detailPolicies, setDetailPolicies] = useState([])
	const [detailMeta, setDetailMeta] = useState(null)
	const [cancelSubmitting, setCancelSubmitting] = useState(false)
	const detailTotalCount = detailPolicies?.length || 0
	const detailPendingCount = (detailPolicies || []).filter((p) => p.status === 'PENDING_APPROVAL').length
	const detailHasSaleEventLink = useMemo(
		() => (detailMeta?.policies || []).some((p) => p?.eventType === 'SALE'),
		[detailMeta]
	)

	const scope = Form.useWatch('scope', form)
	const useTime = Form.useWatch('useTime', form)
	const selectedSaleEventNo = Form.useWatch('eventNo', form)

	const selectedSaleEvent = useMemo(
		() =>
			(saleEvents || []).find(
				(e) => String(e?.eventNo) === String(selectedSaleEventNo)
			) || null,
		[saleEvents, selectedSaleEventNo]
	)

	const isForcedSaleEvent = !!selectedSaleEvent

	useEffect(() => {
		// scope 전환 시 이전 입력값이 섞여 UI가 깨지는 현상을 방지
		if (!scope) return
		const forcedDiscountType = selectedSaleEvent?.saleDiscountType || 'PERCENT'
		const forcedDiscountValue = selectedSaleEvent?.saleDiscountValue ?? null
		const forcedMaxDiscountAmount = selectedSaleEvent?.saleMaxDiscountAmount ?? null
		form.setFieldsValue({
			targetProductNo: undefined,
			targetOptionNo: undefined,
			discountType: undefined,
			discountValue: undefined,
			maxDiscountAmount: undefined,
			targets: [
				{
					targetProductNo: null,
					targetOptionNo: null,
					discountType: forcedDiscountType,
					discountValue: forcedDiscountValue,
					maxDiscountAmount: forcedMaxDiscountAmount
				}
			]
		})
	}, [scope, selectedSaleEvent, form])

	useEffect(() => {
		if (!selectedSaleEvent) return
		const targets = form.getFieldValue('targets') || []
		if (!Array.isArray(targets) || targets.length === 0) return
		form.setFieldsValue({
			useTime: true,
			period: [
				selectedSaleEvent?.customerEventStartAt ? dayjs(selectedSaleEvent.customerEventStartAt) : null,
				selectedSaleEvent?.customerEventEndAt ? dayjs(selectedSaleEvent.customerEventEndAt) : null
			],
			targets: targets.map((t) => ({
				...t,
				discountType: selectedSaleEvent.saleDiscountType,
				discountValue: selectedSaleEvent.saleDiscountValue,
				maxDiscountAmount: selectedSaleEvent.saleMaxDiscountAmount ?? null
			}))
		})
	}, [selectedSaleEventNo, selectedSaleEvent, form])

	const loadProducts = async () => {
		try {
			const res = await PartnerService.getMyProducts()
			// GET /api/partner/products 는 ApiResponse 래핑이 아니라 List를 그대로 반환합니다.
			// fetch interceptor가 response.data만 넘기므로 res는 보통 배열 형태입니다.
			// 다만 서버 응답 래핑이 달라질 수도 있어 방어적으로 처리합니다.
			const list = Array.isArray(res)
				? res
				: Array.isArray(res?.data)
					? res.data
					: Array.isArray(res?.data?.data)
						? res.data.data
						: []

			setProducts(list)
			if (list.length === 0) {
				message.warning('등록 가능한 상품이 없습니다. (본인 파트너 소유 상품이 맞는지 확인해주세요.)')
			}
		} catch (e) {
			message.error(e?.message || '상품 목록 조회에 실패했습니다.')
		}
	}

	const loadSales = async () => {
		try {
			setLoading(true)
			const res = await PartnerService.getPartnerSalePolicies(
				statusFilter ? { status: statusFilter } : {}
			)
			setList(Array.isArray(res?.data) ? res.data : [])
		} catch (e) {
			message.error(e?.message || '세일 목록 조회에 실패했습니다.')
		} finally {
			setLoading(false)
		}
	}

	const loadSaleEvents = async () => {
		try {
			setLoading(true)
			const res = await PartnerService.getPartnerVisibleEvents()
			const list = Array.isArray(res)
				? res
				: Array.isArray(res?.data)
					? res.data
					: Array.isArray(res?.data?.data)
						? res.data.data
						: []

			// 강제형 시즌 SALE: 파트너가 참여중이고 eventType=SALE 인 것만 노출
			const saleEventList = (list || []).filter(
				(e) =>
					e?.eventType === 'SALE' &&
					e?.participationEnabled &&
					e?.saleDiscountType &&
					e?.saleDiscountValue
			)
			setSaleEvents(saleEventList)
		} catch (e) {
			message.warning(e?.response?.data?.message || '세일 이벤트 목록 조회에 실패했습니다.')
		} finally {
			setLoading(false)
		}
	}

	useEffect(() => {
		loadProducts()
		loadSales()
		loadSaleEvents()
	}, [])

	useEffect(() => {
		loadSales()
	}, [statusFilter])

	const productOptions = useMemo(
		() =>
			(products || []).map((p) => ({
				value: p.productNo,
				label: `${p.productNo} - ${p.productName}`
			})),
		[products]
	)

	const optionOptions = useMemo(() => {
		const rows = []
		;(products || []).forEach((p) => {
			;(p.options || []).forEach((opt) => {
				rows.push({
					value: opt.optionNo,
					label: `${p.productName} - ${opt.color || '-'} / ${opt.size || '-'}`
				})
			})
		})
		return rows
	}, [products])

	// 대상 라벨 유틸 (상품명/옵션명 표시)
	const productNameByNo = useMemo(() => {
		const map = new Map()
		;(products || []).forEach(p => {
			map.set(p.productNo, p.productName)
		})
		return map
	}, [products])

	const optionLabelByNo = useMemo(() => {
		const map = new Map()
		;(products || []).forEach(p => {
			;(p.options || []).forEach(opt => {
				const label = `${opt.color || '-'}${opt.color && opt.size ? ' / ' : ''}${opt.size || ''}`.trim()
				map.set(opt.optionNo, label || '-')
			})
		})
		return map
	}, [products])

	const handleCreate = async (values) => {
		try {
			setSaving(true)

			let startAt = null
			let endAt = null

			// 강제형(이벤트 연동) SALE: 날짜/할인값은 관리자 이벤트 기준으로 덮어씁니다.
			if (
				isForcedSaleEvent &&
				selectedSaleEvent?.customerEventStartAt &&
				selectedSaleEvent?.customerEventEndAt
			) {
				startAt = dayjs(selectedSaleEvent.customerEventStartAt).format('YYYY-MM-DDTHH:mm:ss')
				endAt = dayjs(selectedSaleEvent.customerEventEndAt).format('YYYY-MM-DDTHH:mm:ss')
			} else {
				startAt = values.period?.[0]
					? (
						values.useTime
							? values.period[0].second(0).format('YYYY-MM-DDTHH:mm:ss')
							: values.period[0].startOf('day').format('YYYY-MM-DDTHH:mm:ss')
					)
					: null
				endAt = values.period?.[1]
					? (
						values.useTime
							? values.period[1].second(0).format('YYYY-MM-DDTHH:mm:ss')
							: values.period[1].endOf('day').format('YYYY-MM-DDTHH:mm:ss')
					)
					: null
			}

			if (!startAt || !endAt) {
				throw new Error('세일 기간을 선택해주세요.')
			}

			const targets = (values.targets || []).map((t) => ({
				targetProductNo: values.scope === 'PRODUCT' ? t.targetProductNo : null,
				targetOptionNo: values.scope === 'OPTION' ? t.targetOptionNo : null,
				discountType: t.discountType,
				discountValue: t.discountValue,
				maxDiscountAmount: t.maxDiscountAmount || null
			}))

			if (!targets || targets.length === 0) {
				throw new Error('세일 타겟을 최소 1개 이상 선택해주세요.')
			}

			const payload = {
				eventNo: values.eventNo || null,
				scope: values.scope,
				startAt,
				endAt,
				targets
			}

			await PartnerService.createPartnerSaleCampaign(payload)
			message.success('세일 캠페인이 등록되었습니다.')
			setCreateOpen(false)
			form.resetFields()
			loadSales()
		} catch (e) {
			message.warning(extractApiErrorMessage(e, '세일 등록에 실패했습니다.'))
		} finally {
			setSaving(false)
		}
	}

	const handleCancelSale = async (id) => {
		try {
			await PartnerService.cancelPartnerSalePolicy(id)
			message.success('세일을 긴급 중단했습니다.')
			await loadSales()
			return true
		} catch (e) {
			message.error(extractApiErrorMessage(e, '세일 긴급 중단에 실패했습니다.'))
			return false
		}
	}

	const handleCancelCampaign = async (campaignId) => {
		try {
			await PartnerService.cancelPartnerSaleCampaign(campaignId)
			message.success('세일 캠페인을 긴급 중단했습니다.')
			await loadSales()
			return true
		} catch (e) {
			message.error(extractApiErrorMessage(e, '세일 캠페인 긴급 중단에 실패했습니다.'))
			return false
		}
	}

	const handleCancelFromDetail = async () => {
		if (!detailMeta) return
		setCancelSubmitting(true)
		let ok = false
		try {
			if (detailMeta.campaignId) {
				ok = await handleCancelCampaign(detailMeta.campaignId)
			} else {
				ok = await handleCancelSale(detailMeta.policies?.[0]?.id)
			}
			if (ok) {
				setDetailOpen(false)
				setDetailMeta(null)
			}
		} finally {
			setCancelSubmitting(false)
		}
	}

	const saleStatusRank = (status) => {
		if (!status) return 99
		return (
			{
				ACTIVE: 0,
				PENDING_APPROVAL: 1,
				INACTIVE: 2,
				EXPIRED: 3,
				CANCELLED: 4,
				REJECTED: 5
			}[status] ?? 99
		)
	}

	const groupedSales = useMemo(() => {
		const groups = new Map()
		;(list || []).forEach((p) => {
			const key = p.campaignId ? `campaign:${p.campaignId}` : `single:${p.id}`
			if (!groups.has(key)) groups.set(key, [])
			groups.get(key).push(p)
		})

		return Array.from(groups.entries())
			.map(([key, policies]) => {
				const first = policies[0]
				const best = policies.reduce((acc, cur) => {
					return saleStatusRank(cur.status) < saleStatusRank(acc.status) ? cur : acc
				}, policies[0])

				return {
					id: key,
					campaignId: first.campaignId || null,
					saleName: first.eventTitle || '파트너 단독 세일',
					policies,
					scope: first.scope,
					targetCount: policies.length,
					startAt: first.startAt,
					endAt: first.endAt,
					status: best.status
				}
			})
			.sort((a, b) => {
				// 최신 생성순 느낌으로 정렬 (캠페인은 구성 정책들의 createdAt이 같을 확률이 높음)
				return dayjs(b.startAt).valueOf() - dayjs(a.startAt).valueOf()
			})
	}, [list])

	const filteredGroupedSales = useMemo(() => {
		if (!todayOnly) return groupedSales
		const now = dayjs()
		return groupedSales.filter((g) => {
			const inPeriod = !now.isBefore(dayjs(g.startAt)) && !now.isAfter(dayjs(g.endAt))
			return g.status === 'ACTIVE' && inPeriod
		})
	}, [groupedSales, todayOnly])

	useEffect(() => {
		// 필터 변경 시 첫 페이지로 이동 (새로고침처럼 보이는 점프 최소화)
		setTablePage(1)
	}, [statusFilter, todayOnly])

	useEffect(() => {
		// 현재 페이지가 데이터 범위를 벗어나면 마지막 유효 페이지로 보정
		const total = filteredGroupedSales.length
		const maxPage = Math.max(1, Math.ceil(total / pageSize))
		if (tablePage > maxPage) {
			setTablePage(maxPage)
		}
	}, [filteredGroupedSales, tablePage, pageSize])

	const columns = [
		{
			title: '세일명',
			dataIndex: 'saleName',
			width: 220,
			render: (value) => value || '-'
		},
		{
			title: '범위',
			dataIndex: 'scope',
			width: 100,
			render: (v) => (v === 'OPTION' ? '옵션' : '상품')
		},
		{
			title: '대상',
			key: 'target',
			render: (_, record) => {
				if (record.targetCount <= 1) {
					const p = record.policies[0]
					if (!p) return '-'
					const opacity = record.status === 'INACTIVE' ? 0.45 : 1
					if (record.scope === 'OPTION') {
						const name = optionLabelByNo.get(p.targetOptionNo) || '-'
						return <span style={{ opacity }}>{`옵션 #${p.targetOptionNo} (${name})`}</span>
					}
					const name = productNameByNo.get(p.targetProductNo) || '-'
					return <span style={{ opacity }}>{`상품 #${p.targetProductNo} (${name})`}</span>
				}

				// 목록에서는 개수 표시
				const opacity = record.status === 'INACTIVE' ? 0.45 : 1
				return record.scope === 'OPTION'
					? <span style={{ opacity }}>{`옵션 ${record.targetCount}개`}</span>
					: <span style={{ opacity }}>{`상품 ${record.targetCount}개`}</span>
			}
		},
		{
			title: '기간',
			key: 'period',
			render: (_, record) =>
				`${dayjs(record.startAt).format('YYYY-MM-DD')} ~ ${dayjs(record.endAt).format('YYYY-MM-DD')}`
		},
		{
			title: '상태',
			width: 140,
			render: (_, record) => {
				const pendingCountInGroup = (record.policies || []).filter((p) => p.status === 'PENDING_APPROVAL').length
				return (
					<Space size={6}>
						<Tag color={record.status === 'ACTIVE' ? 'green' : 'default'}>
							{STATUS_LABELS[record.status] || record.status}
						</Tag>
						{pendingCountInGroup > 0 && record.status !== 'PENDING_APPROVAL' && (
							<Tag color="orange">{`승인 대기 ${pendingCountInGroup}건`}</Tag>
						)}
					</Space>
				)
			}
		},
		{
			title: '작업',
			key: 'action',
			width: 100,
			render: (_, record) => (
				<Button
					size="small"
					onClick={() => {
						setDetailPolicies(record.policies)
						setDetailMeta(record)
						setDetailOpen(true)
					}}
				>
					상세
				</Button>
			)
		}
	]

	return (
		<Card title="세일 관리">
			<Alert
				type="info"
				showIcon
				style={{ marginBottom: 16 }}
				message={
					<span>
						고객 주문 시 확정된 세일만 반영되며, 적용된 할인은{' '}
						<Tooltip
							title={
								<>
									고객이 주문할 때 할인 금액이 한 번 확정되면,
									그 뒤 세일이 바뀌거나 중단돼도,<br />
									그 주문 건은 처음 확정된 할인 금액으로 결제됩니다.
								</>
							}
						>
							<u>결제 전</u>
						</Tooltip>
						까지 유지됩니다.
					</span>
				}
			/>
			<Space style={{ marginBottom: 16 }} wrap>
				<Select
					allowClear
					placeholder="상태 필터"
					value={statusFilter}
					onChange={setStatusFilter}
					style={{ width: 180 }}
					options={Object.keys(STATUS_LABELS).map((key) => ({
						value: key,
						label: STATUS_LABELS[key]
					}))}
				/>
				<Button onClick={loadSales}>새로고침</Button>
				<Space size={6}>
					<Switch checked={todayOnly} onChange={setTodayOnly} />
					<span>오늘 세일 중</span>
				</Space>
				<Button type="primary" onClick={() => setCreateOpen(true)}>
					세일 등록
				</Button>
			</Space>

			<Table
				rowKey="id"
				loading={loading}
				columns={columns}
				dataSource={filteredGroupedSales}
				pagination={{
					current: tablePage,
					pageSize,
					showSizeChanger: false,
					onChange: (page) => setTablePage(page)
				}}
			/>

			<Modal
				title="세일 등록"
				open={createOpen}
				onCancel={() => setCreateOpen(false)}
				onOk={() => form.submit()}
				confirmLoading={saving}
				okText="등록"
				cancelText="닫기"
			>
				<Form form={form} layout="vertical" onFinish={handleCreate} initialValues={{ useTime: false }}>
					{(saleEvents || []).length > 0 && (
						<Form.Item name="eventNo" label="시즌 SALE 이벤트(선택)">
							<Select
								placeholder="시즌 SALE 이벤트를 선택하면 할인 값이 이벤트 기준으로 강제됩니다."
								allowClear
								options={(saleEvents || []).map((e) => ({
									value: e.eventNo,
									label: e.eventTitle
								}))}
							/>
						</Form.Item>
					)}
					{isForcedSaleEvent && (
						<Alert
							type="info"
							showIcon
							style={{ marginBottom: 12 }}
							message="이벤트 연동 세일은 할인값/기간이 관리자 기준으로 자동 적용됩니다."
						/>
					)}

					<Form.Item
						name="scope"
						label="세일 범위"
						rules={[{ required: true, message: '세일 범위를 선택해주세요.' }]}
					>
						<Select options={SCOPE_OPTIONS} />
					</Form.Item>

					<Form.Item name="useTime" label="시간 포함" valuePropName="checked">
						<Switch disabled={isForcedSaleEvent} />
					</Form.Item>

					{scope === 'PRODUCT' && (
						<Form.List name="targets">
							{(fields, { add, remove }) => (
								<>
									{fields.map(({ key, name }) => (
										<Card
											key={key}
											type="inner"
											style={{ marginBottom: 12, border: '1px solid #f0f0f0' }}
										>
											<Space direction="vertical" style={{ width: '100%' }}>
												<Form.Item
													name={[name, 'targetProductNo']}
													label="대상 상품"
													rules={[{ required: true, message: '상품을 선택해주세요.' }]}
												>
													<Select
														options={productOptions}
														showSearch
														optionFilterProp="label"
													/>
												</Form.Item>

												<Form.Item
													name={[name, 'discountType']}
													label="할인 타입"
													rules={
														isForcedSaleEvent
															? []
															: [{ required: true, message: '할인 타입을 선택해주세요.' }]
													}
												>
													<Select options={DISCOUNT_TYPE_OPTIONS} disabled={isForcedSaleEvent} />
												</Form.Item>

												<Form.Item
													name={[name, 'discountValue']}
													label="할인 값"
													rules={
														isForcedSaleEvent
															? []
															: [{ required: true, message: '할인 값을 입력해주세요.' }]
													}
												>
													<InputNumber min={1} style={{ width: '100%' }} disabled={isForcedSaleEvent} />
												</Form.Item>

												<Form.Item name={[name, 'maxDiscountAmount']} label="최대 할인 금액(선택)">
													<InputNumber min={1} style={{ width: '100%' }} disabled={isForcedSaleEvent} />
												</Form.Item>

												<Button type="link" danger onClick={() => remove(name)}>
													삭제
												</Button>
											</Space>
										</Card>
									))}

									<Button
										type="dashed"
										onClick={() =>
											add({
												targetProductNo: null,
												targetOptionNo: null,
												discountType: selectedSaleEvent?.saleDiscountType || 'PERCENT',
												discountValue: selectedSaleEvent?.saleDiscountValue ?? null,
												maxDiscountAmount: selectedSaleEvent?.saleMaxDiscountAmount ?? null
											})
										}
										style={{ width: '100%' }}
									>
										타겟 추가
									</Button>
								</>
							)}
						</Form.List>
					)}

					{scope === 'OPTION' && (
						<Form.List name="targets">
							{(fields, { add, remove }) => (
								<>
									{fields.map(({ key, name }) => (
										<Card
											key={key}
											type="inner"
											style={{ marginBottom: 12, border: '1px solid #f0f0f0' }}
										>
											<Space direction="vertical" style={{ width: '100%' }}>
												<Form.Item
													name={[name, 'targetOptionNo']}
													label="대상 옵션"
													rules={[{ required: true, message: '옵션을 선택해주세요.' }]}
												>
													<Select
														options={optionOptions}
														showSearch
														optionFilterProp="label"
														placeholder="옵션 선택"
													/>
												</Form.Item>

												<Form.Item
													name={[name, 'discountType']}
													label="할인 타입"
													rules={
														isForcedSaleEvent
															? []
															: [{ required: true, message: '할인 타입을 선택해주세요.' }]
													}
												>
													<Select options={DISCOUNT_TYPE_OPTIONS} disabled={isForcedSaleEvent} />
												</Form.Item>

												<Form.Item
													name={[name, 'discountValue']}
													label="할인 값"
													rules={
														isForcedSaleEvent
															? []
															: [{ required: true, message: '할인 값을 입력해주세요.' }]
													}
												>
													<InputNumber min={1} style={{ width: '100%' }} disabled={isForcedSaleEvent} />
												</Form.Item>

												<Form.Item name={[name, 'maxDiscountAmount']} label="최대 할인 금액(선택)">
													<InputNumber min={1} style={{ width: '100%' }} disabled={isForcedSaleEvent} />
												</Form.Item>

												<Button type="link" danger onClick={() => remove(name)}>
													삭제
												</Button>
											</Space>
										</Card>
									))}

									<Button
										type="dashed"
										onClick={() =>
											add({
												targetProductNo: null,
												targetOptionNo: null,
												discountType: selectedSaleEvent?.saleDiscountType || 'PERCENT',
												discountValue: selectedSaleEvent?.saleDiscountValue ?? null,
												maxDiscountAmount: selectedSaleEvent?.saleMaxDiscountAmount ?? null
											})
										}
										style={{ width: '100%' }}
									>
										타겟 추가
									</Button>
								</>
							)}
						</Form.List>
					)}

					<Form.Item
						name="period"
						label="세일 기간"
						rules={
							isForcedSaleEvent ? [] : [{ required: true, message: '세일 기간을 선택해주세요.' }]
						}
					>
						<DatePicker.RangePicker
							showTime={useTime ? { format: 'HH:mm' } : false}
							format={useTime ? 'YYYY-MM-DD HH:mm' : 'YYYY-MM-DD'}
							style={{ width: '100%' }}
							disabled={isForcedSaleEvent}
						/>
					</Form.Item>
				</Form>
			</Modal>

			<Modal
				title={null}
				open={detailOpen}
				width={980}
				onCancel={() => {
					setDetailOpen(false)
					setDetailMeta(null)
				}}
				footer={[
					<Button
						key="close"
						onClick={() => {
							setDetailOpen(false)
							setDetailMeta(null)
						}}
					>
						닫기
					</Button>,
					detailMeta?.status === 'ACTIVE' && !detailHasSaleEventLink && (
						<Popconfirm
							key="cancel-sale-confirm"
							title="세일 긴급 중단"
							description="긴급 중단 시 즉시 할인 적용이 멈춥니다. 계속 진행할까요?"
							okText="확인"
							cancelText="닫기"
							onConfirm={() => {
								// Promise를 직접 반환하지 않고 수동 로딩 상태로 제어해,
								// Popconfirm 확인 버튼 로딩이 고정되는 현상을 방지합니다.
								handleCancelFromDetail()
							}}
							disabled={cancelSubmitting || !detailMeta}
							okButtonProps={{ loading: cancelSubmitting }}
						>
							<Button
								danger
								loading={cancelSubmitting}
								disabled={cancelSubmitting || !detailMeta}
							>
								세일 긴급 중단
							</Button>
						</Popconfirm>
					)
				]}
			>
				<Space style={{ marginBottom: 12 }}>
					<Tag>{`총 ${detailTotalCount}건`}</Tag>
					{detailPendingCount > 0 && <Tag color="orange">{`승인 대기 ${detailPendingCount}건`}</Tag>}
				</Space>
				{detailMeta?.status === 'ACTIVE' && detailHasSaleEventLink && (
					<Alert
						type="info"
						showIcon
						style={{ marginBottom: 12 }}
						message="이 캠페인은 시즌 SALE 이벤트 연동 세일이라 파트너 긴급 중단이 불가합니다. 관리자에게 중단 요청해주세요."
					/>
				)}
				{detailPolicies && detailPolicies.length > 0 ? (
					<Table
						size="small"
						rowKey="id"
						pagination={false}
						scroll={{ x: 900 }}
						dataSource={detailPolicies}
						columns={[
							{
								title: '범위',
								width: 90,
								render: (_, r) => (r.scope === 'OPTION' ? '옵션' : '상품')
							},
							{
								title: '대상',
								width: 360,
								render: (_, r) => {
									const opacity = r.status === 'INACTIVE' ? 0.45 : 1
									if (r.scope === 'OPTION') {
										const name = optionLabelByNo.get(r.targetOptionNo) || '-'
										return <span style={{ opacity }}>{`옵션 #${r.targetOptionNo} (${name})`}</span>
									}
									const name = productNameByNo.get(r.targetProductNo) || '-'
									return <span style={{ opacity }}>{`상품 #${r.targetProductNo} (${name})`}</span>
								}
							},
							{
								title: '할인',
								render: (_, r) =>
									r.discountType === 'PERCENT'
										? `${r.discountValue}%`
										: `${Number(r.discountValue || 0).toLocaleString()}원`
							},
							{
								title: '상태',
								width: 140,
								render: (_, r) => (
									<Tag color={r.status === 'ACTIVE' ? 'green' : 'default'}>{STATUS_LABELS[r.status] || r.status}</Tag>
								)
							},
							{
								title: '기간',
								width: 170,
								render: (_, r) => `${dayjs(r.startAt).format('YYYY-MM-DD')} ~ ${dayjs(r.endAt).format('YYYY-MM-DD')}`
							}
						]}
					/>
				) : (
					<p>표시할 항목이 없습니다.</p>
				)}
			</Modal>
		</Card>
	)
}

export default PartnerSales
