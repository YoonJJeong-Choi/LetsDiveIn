import React, { useState, useEffect, useCallback } from 'react';
import { useSelector } from 'react-redux';
import { useLocation, useSearchParams } from 'react-router-dom';
import { Card, Table, Button, Modal, message, Tag, Space, Descriptions, Row, Col, Badge, Input, Form, Tabs } from 'antd';
import { ShoppingCartOutlined, EyeOutlined, CheckCircleOutlined, BellOutlined, CarOutlined, EditOutlined, ReloadOutlined } from '@ant-design/icons';
import OrderService from 'services/OrderService';
import DeliveryService from 'services/DeliveryService';

const getOrderStatusColor = (status) => {
	switch (status) {
		case 'PENDING_PAYMENT':
			return 'orange';
		case 'PAID':
			return 'blue';
		case 'ACTIVE':
			return 'green';
		case 'PAYMENT_FAILED':
			return 'red';
		case 'CANCELLED':
			return 'default';
		default:
			return 'default';
	}
};

const DELIVERY_READY_DELAY_DAYS = 3;

/** 발주 확인이 필요한 주문 상품이 하나라도 있는 주문 (결제 완료·진행 중) */
const orderHasPendingConfirmation = (order) => {
	if (!order?.orderItems?.length) return false;
	const st = order.orderStatus;
	if (st !== 'PAID' && st !== 'ACTIVE') return false;
	return order.orderItems.some((item) => !item.isCancelled && item.confirmedAt == null);
};

/** 배송 READY가 발주 확인 후 N일 이상 지속 (출고 지연 의심, 대시보드 집계와 동일 기준) */
const orderHasDelayedReadyDelivery = (order, days = DELIVERY_READY_DELAY_DAYS) => {
	if (!order?.orderItems?.length) return false;
	const threshold = Date.now() - days * 24 * 60 * 60 * 1000;
	return order.orderItems.some((item) => {
		if (item.isCancelled) return false;
		if (item.deliveryStatus !== 'READY') return false;
		if (!item.confirmedAt) return false;
		return new Date(item.confirmedAt).getTime() < threshold;
	});
};

const URL_ORDER_FILTERS = new Set([
	'PAID',
	'ACTIVE',
	'CANCELLED',
	'PENDING_PAYMENT',
	'PAYMENT_FAILED',
	'PENDING_CONFIRMATION',
	'DELIVERY_DELAY',
]);

const getOrderFilterFromSearchParams = (searchParams) => {
	const v = searchParams.get('filter');
	if (!v || v === 'ALL') return null;
	return URL_ORDER_FILTERS.has(v) ? v : null;
};

const getOrderStatusLabel = (status) => {
	switch (status) {
		case 'PENDING_PAYMENT':
			return '결제 대기';
		case 'PAID':
			return '결제 완료';
		case 'ACTIVE':
			return '주문 진행중';
		case 'PAYMENT_FAILED':
			return '결제 실패';
		case 'CANCELLED':
			return '주문 취소';
		default:
			return status;
	}
};

// OrderItem 상태 색상
const getOrderItemStatusColor = (status) => {
	switch (status) {
		case 'PENDING_CONFIRMATION':
			return 'orange';
		case 'CONFIRMED':
			return 'blue';
		case 'READY':
			return 'cyan';
		case 'SHIPPED':
			return 'purple';
		case 'DELIVERED':
			return 'geekblue';
		case 'COMPLETED':
			return 'green';
		case 'RETURN_IN_PROGRESS':
			return 'gold';
		case 'RETURN_REJECTED':
			return 'volcano';
		case 'REFUNDED':
			return 'red';
		case 'CANCELLED':
			return 'default';
		default:
			return 'default';
	}
};

// OrderItem 상태 라벨
const getOrderItemStatusLabel = (status) => {
	switch (status) {
		case 'PENDING_CONFIRMATION':
			return '발주 확인 대기';
		case 'CONFIRMED':
			return '발주 확인됨';
		case 'READY':
			return '배송 준비';
		case 'SHIPPED':
			return '배송 중';
		case 'DELIVERED':
			return '배송 완료';
		case 'COMPLETED':
			return '구매 확정';
		case 'RETURN_IN_PROGRESS':
			return '반품 진행 중';
		case 'RETURN_REJECTED':
			return '반품 거절';
		case 'REFUNDED':
			return '환불 완료';
		case 'CANCELLED':
			return '취소됨';
		default:
			return status;
	}
};

const OrderManagement = () => {
	const location = useLocation();
	const [searchParams, setSearchParams] = useSearchParams();
	const user = useSelector((state) => state.auth.user);
	const userRole = user?.role || 'ADMIN'; // 기본값 ADMIN
	
	const [orders, setOrders] = useState([]);
	const [totalOrders, setTotalOrders] = useState(0);
	const [statusCounts, setStatusCounts] = useState({});
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(10);
	const [loading, setLoading] = useState(false);
	const [detailModalVisible, setDetailModalVisible] = useState(false);
	const [selectedOrder, setSelectedOrder] = useState(null);
	const [statusFilter, setStatusFilter] = useState(null); // null=전체 + URL `filter`와 동기화
	const [deliveryStartModalVisible, setDeliveryStartModalVisible] = useState(false);
	const [deliveryUpdateModalVisible, setDeliveryUpdateModalVisible] = useState(false);
	const [editingDelivery, setEditingDelivery] = useState(null);
	const [deliveryForm] = Form.useForm();

	useEffect(() => {
		fetchAllOrders();
	}, [page, pageSize, statusFilter]);

	useEffect(() => {
		const next = getOrderFilterFromSearchParams(searchParams);
		setStatusFilter(next);
		setPage(1);
	}, [searchParams]);

	const applyOrderFilter = (key) => {
		const next = key === 'ALL' ? null : key;
		setStatusFilter(next);
		setPage(1);
		if (next) {
			setSearchParams({ filter: next }, { replace: true });
		} else {
			setSearchParams({}, { replace: true });
		}
	};

	// 배송 페이지에서 주문 번호와 함께 이동한 경우 주문 상세 모달 자동 열기
	useEffect(() => {
		const openOrderNo = location.state?.openOrderNo;
		if (openOrderNo && orders.length > 0) {
			handleViewDetail(openOrderNo);
			// state를 초기화하여 다시 방문했을 때 자동으로 열리지 않도록 함
			window.history.replaceState({}, '');
		}
	}, [location.state, orders]);

	const fetchAllOrders = async () => {
		try {
			setLoading(true);
			console.log('[Admin Orders] request params =>', { page, size: pageSize, status: statusFilter || undefined });
			const response = await OrderService.getAllOrders({ page, size: pageSize, status: statusFilter || undefined });
			const payload = response?.data || response;
			const items = payload?.items || [];
			const total = payload?.total ?? items.length;
			const nextStatusCounts = payload?.statusCounts || {};
			console.log('[Admin Orders] response meta =>', { page: payload?.page, size: payload?.size, total: payload?.total, itemsCount: Array.isArray(items) ? items.length : 0 });
			setOrders(Array.isArray(items) ? items : []);
			setTotalOrders(Number(total) || 0);
			setStatusCounts(nextStatusCounts);
		} catch (err) {
			message.error(err.response?.data?.message || '주문 목록을 불러오는데 실패했습니다.');
			setOrders([]);
			setTotalOrders(0);
			setStatusCounts({});
		} finally {
			setLoading(false);
		}
	};

	const handleViewDetail = useCallback(async (orderNo) => {
		try {
			const response = await OrderService.getOrderForAdmin(orderNo);
			const orderData = response.data || response;
			setSelectedOrder(orderData);
			setDetailModalVisible(true);
		} catch (err) {
			message.error(err.response?.data?.message || '주문 상세 정보를 불러오는데 실패했습니다.');
		}
	}, []);

	/** 대시보드 등에서 `?orderNo=` 로 진입 시 상세 모달 자동 오픈 */
	useEffect(() => {
		const raw = searchParams.get('orderNo');
		if (!raw) {
			return undefined;
		}
		const no = Number(raw);
		if (!Number.isFinite(no) || no <= 0) {
			return undefined;
		}
		handleViewDetail(no);
		setSearchParams(
			(prev) => {
				const n = new URLSearchParams(prev);
				n.delete('orderNo');
				return n;
			},
			{ replace: true }
		);
		return undefined;
	}, [searchParams, handleViewDetail, setSearchParams]);

	const handleConfirmOrder = async (orderNo) => {
		try {
			await OrderService.confirmOrder(orderNo);
			message.success('발주 확인이 완료되었습니다.');
			fetchAllOrders();
			// 주문 상세 모달이 열려있으면 상세 정보도 갱신
			if (detailModalVisible && selectedOrder?.orderNo === orderNo) {
				await handleViewDetail(orderNo);
			}
		} catch (err) {
			message.error(err.response?.data?.message || '발주 확인에 실패했습니다.');
		}
	};

	const handleConfirmOrderItem = async (orderItemNo) => {
		try {
			await OrderService.confirmOrderItem(orderItemNo);
			message.success('발주 확인이 완료되었습니다.');
			fetchAllOrders();
			// 주문 상세 모달이 열려있으면 상세 정보도 갱신
			if (detailModalVisible && selectedOrder) {
				await handleViewDetail(selectedOrder.orderNo);
			}
		} catch (err) {
			message.error(err.response?.data?.message || '발주 확인에 실패했습니다.');
		}
	};

	const handleStartDelivery = async (values) => {
		try {
			await DeliveryService.startDelivery(editingDelivery.deliveryNo, {
				trackingNumber: values.trackingNumber,
				courier: values.courier,
			});
			message.success('배송이 시작되었습니다.');
			setDeliveryStartModalVisible(false);
			setEditingDelivery(null);
			deliveryForm.resetFields();
			// 주문 상세 정보 갱신
			if (selectedOrder) {
				await handleViewDetail(selectedOrder.orderNo);
			}
			fetchAllOrders();
		} catch (err) {
			message.error(err.response?.data?.message || '배송 시작에 실패했습니다.');
		}
	};

	const handleCompleteDelivery = async (deliveryNo) => {
		Modal.confirm({
			title: '배송 완료 처리',
			content: '배송을 완료 처리하시겠습니까?',
			okText: '확인',
			cancelText: '닫기',
			onOk: async () => {
				try {
					await DeliveryService.completeDelivery(deliveryNo);
					message.success('배송이 완료되었습니다.');
					// 주문 상세 정보 갱신
					if (selectedOrder) {
						await handleViewDetail(selectedOrder.orderNo);
					}
					fetchAllOrders();
				} catch (err) {
					message.error(err.response?.data?.message || '배송 완료 처리에 실패했습니다.');
				}
			},
		});
	};

	const handleUpdateDelivery = async (values) => {
		try {
			await DeliveryService.updateDelivery(editingDelivery.deliveryNo, {
				trackingNumber: values.trackingNumber || null,
				courier: values.courier || null,
			});
			message.success('배송 정보가 수정되었습니다.');
			setDeliveryUpdateModalVisible(false);
			setEditingDelivery(null);
			deliveryForm.resetFields();
			// 주문 상세 정보 갱신
			if (selectedOrder) {
				await handleViewDetail(selectedOrder.orderNo);
			}
			fetchAllOrders();
		} catch (err) {
			message.error(err.response?.data?.message || '배송 정보 수정에 실패했습니다.');
		}
	};

	// 발주 대기 주문 수 계산
	const pendingOrderCount = orders.filter(order => {
		// 주문 상태가 PAID가 아니면 발주 대기 아님
		if (order.orderStatus !== 'PAID') return false;
		
		// 파트너인 경우: 자신의 상품이 모두 발주 확인되었으면 발주 대기 아님
		if (userRole === 'PARTNER') {
			if (!order.orderItems || !Array.isArray(order.orderItems) || order.orderItems.length === 0) {
				return true; // 상품이 없으면 발주 대기
			}
			// 자신의 상품 중 하나라도 발주 확인되지 않은 것이 있으면 발주 대기
			const hasUnconfirmedItems = order.orderItems.some(item => item.confirmedAt == null);
			return hasUnconfirmedItems;
		}
		
		// 관리자인 경우: 주문 상태가 PAID이면 발주 대기
		return true;
	}).length;
	
	// 발주 확인된 주문 상품이 있는지 확인
	const hasConfirmedItems = (order) => {
		if (!order.orderItems || !Array.isArray(order.orderItems)) return false;
		return order.orderItems.some(item => item.confirmedAt != null);
	};
	
	// 모든 주문 상품이 완료되었는지 확인 (구매 확정, 환불 완료, 반품 거절 포함)
	const isAllItemsCompleted = (order) => {
		if (!order || !order.orderItems || !Array.isArray(order.orderItems)) return false;
		if (order.orderItems.length === 0) return false;
		
		// 모든 주문 상품이 완료 상태인지 확인
		return order.orderItems.every(item => 
			item.status === 'COMPLETED' || 
			item.status === 'REFUNDED' || 
			item.status === 'RETURN_REJECTED'
		);
	};
	
	// 주문 상태 표시용 함수 (OrderItem 상태를 고려한 집계 상태)
	const getDisplayOrderStatus = (order) => {
		const status = order.orderStatus;
		
		// 취소된 주문은 항상 "주문 취소"
		if (status === 'CANCELLED') {
			return { status: 'CANCELLED', label: '주문 취소' };
		}
		
		if (!order.orderItems || !Array.isArray(order.orderItems) || order.orderItems.length === 0) {
			// 주문 상품이 없으면 기본 주문 상태 반환
			return { status, label: getOrderStatusLabel(status) };
		}
		
		// 반품 진행 중이 있는지 확인
		const hasReturnInProgress = order.orderItems.some(item => 
			item.status === 'RETURN_IN_PROGRESS'
		);
		if (hasReturnInProgress) {
			return { status: 'RETURN_IN_PROGRESS', label: '반품 진행 중' };
		}
		
		// 모든 아이템이 완료되었는지 확인 (구매 확정, 환불 완료, 반품 거절)
		if (isAllItemsCompleted(order)) {
			return { status: 'COMPLETED', label: '완료' };
		}
		
		// 파트너인 경우: 자신의 상품이 발주 확인되었으면 "주문 진행중"으로 표시
		// (파트너는 자신의 상품만 보이므로, 자신의 상품이 발주 확인되면 진행중으로 표시)
		if (userRole === 'PARTNER' && status === 'PAID') {
			const hasConfirmedItems = order.orderItems.some(item => item.confirmedAt != null);
			if (hasConfirmedItems) {
				return { status: 'ACTIVE', label: '주문 진행중' };
			}
		}
		
		// 기본 주문 상태 반환
		return { status, label: getOrderStatusLabel(status) };
	};
	
	// 주문 상태별 개수 계산 (서버가 내려준 전체 결과 기준)
	const orderCounts = {
		ALL: Number(statusCounts.ALL) || 0,
		PAID: Number(statusCounts.PAID) || 0,
		ACTIVE: Number(statusCounts.ACTIVE) || 0,
		CANCELLED: Number(statusCounts.CANCELLED) || 0,
		PENDING_PAYMENT: Number(statusCounts.PENDING_PAYMENT) || 0,
		PAYMENT_FAILED: Number(statusCounts.PAYMENT_FAILED) || 0,
		PENDING_CONFIRMATION: Number(statusCounts.PENDING_CONFIRMATION) || 0,
		DELIVERY_DELAY: Number(statusCounts.DELIVERY_DELAY) || 0,
	};

	// 필터링된 주문 목록
	const filteredOrders = (() => {
		if (!statusFilter) return orders;
		if (statusFilter === 'PENDING_CONFIRMATION') {
			return orders.filter(orderHasPendingConfirmation);
		}
		if (statusFilter === 'DELIVERY_DELAY') {
			return orders.filter((o) => orderHasDelayedReadyDelivery(o));
		}
		return orders.filter((order) => order.orderStatus === statusFilter);
	})();

	const formatDate = (dateString) => {
		if (!dateString) return '-';
		const date = new Date(dateString);
		return date.toLocaleString('ko-KR', {
			year: 'numeric',
			month: '2-digit',
			day: '2-digit',
			hour: '2-digit',
			minute: '2-digit',
		});
	};

	const formatPrice = (price) => {
		if (!price) return '0원';
		return new Intl.NumberFormat('ko-KR').format(price) + '원';
	};

	const tableColumns = [
		{
			title: '주문 번호',
			dataIndex: 'orderNo',
			key: 'orderNo',
		},
		// 전체 탭에서만 상태 열 표시
		...(statusFilter === null ? [{
			title: '주문 상태',
			dataIndex: 'orderStatus',
			key: 'orderStatus',
			render: (status, record) => {
				const displayStatus = getDisplayOrderStatus(record);
				// 반품 진행 중이나 완료 상태는 특별한 색상 사용
				let color = getOrderStatusColor(displayStatus.status);
				if (displayStatus.status === 'RETURN_IN_PROGRESS') {
					color = 'gold';
				} else if (displayStatus.status === 'COMPLETED') {
					color = 'green';
				}
				return (
					<Tag color={color}>
						{displayStatus.label}
					</Tag>
				);
			},
		}] : []),
		{
			title: '주문 금액',
			dataIndex: 'orderTotalPrice',
			key: 'orderTotalPrice',
			render: (price) => formatPrice(price),
		},
		{
			title: '수령인',
			dataIndex: 'recipientName',
			key: 'recipientName',
		},
		{
			title: '주문일시',
			dataIndex: 'orderCreatedAt',
			key: 'orderCreatedAt',
			render: (date) => formatDate(date),
		},
			{
			title: '작업',
			key: 'actions',
			render: (_, record) => (
				<Space>
					<Button
						icon={<EyeOutlined />}
						size="small"
						onClick={() => handleViewDetail(record.orderNo)}
					>
						상세보기
					</Button>
					{/* 발주 확인 버튼 (관리자만, PAID 상태이고 아직 발주 확인되지 않은 경우) */}
					{userRole === 'ADMIN' && record.orderStatus === 'PAID' && !hasConfirmedItems(record) && (
						<Button
							type="primary"
							icon={<CheckCircleOutlined />}
							size="small"
							onClick={() => handleConfirmOrder(record.orderNo)}
							style={{ backgroundColor: '#52c41a', borderColor: '#52c41a' }}
						>
							전체 발주 확인
						</Button>
					)}
				</Space>
			),
		},
	];

	return (
		<Card>
			<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
				<h4 style={{ margin: 0 }}>주문 목록</h4>
				<Button
					type="default"
					icon={<ReloadOutlined />}
					onClick={fetchAllOrders}
					loading={loading}
				>
					새로고침
				</Button>
			</div>
			
			{/* 발주 대기 알림 카드 */}
			{pendingOrderCount > 0 && (
				<Card 
					style={{ 
						marginBottom: 16, 
						background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
						border: 'none',
						color: 'white'
					}}
				>
					<Row align="middle" justify="space-between">
						<Col>
							<Space size="large">
								<BellOutlined style={{ fontSize: 24 }} />
								<div>
									<div style={{ fontSize: 18, fontWeight: 'bold', marginBottom: 4 }}>
										발주 대기 주문이 {pendingOrderCount}건 있습니다
									</div>
									<div style={{ fontSize: 14, opacity: 0.9 }}>
										결제 완료된 주문을 발주 확인해주세요
									</div>
								</div>
							</Space>
						</Col>
						<Col>
							<Button
								type="primary"
								size="large"
								icon={<CheckCircleOutlined />}
								onClick={() => applyOrderFilter('PAID')}
								style={{ 
									background: 'white', 
									color: '#667eea',
									border: 'none',
									fontWeight: 'bold'
								}}
							>
								발주 대기 보기
							</Button>
						</Col>
					</Row>
				</Card>
			)}

			{/* 상태별 필터 탭 */}
			<Tabs
				activeKey={statusFilter || 'ALL'}
				items={[
					{
						key: 'ALL',
						label: `전체 (${orderCounts.ALL})`,
					},
					{
						key: 'PENDING_PAYMENT',
						label: `결제 대기 (${orderCounts.PENDING_PAYMENT})`,
					},
					{
						key: 'PAYMENT_FAILED',
						label: `결제 실패 (${orderCounts.PAYMENT_FAILED})`,
					},
					{
						key: 'PAID',
						label: `발주 대기 (${orderCounts.PAID})`,
					},
					{
						key: 'PENDING_CONFIRMATION',
						label: `발주 확인 필요 (${orderCounts.PENDING_CONFIRMATION})`,
					},
					{
						key: 'ACTIVE',
						label: `주문 진행중 (${orderCounts.ACTIVE})`,
					},
					{
						key: 'DELIVERY_DELAY',
						label: `출고 지연 의심 (${orderCounts.DELIVERY_DELAY})`,
					},
					{
						key: 'CANCELLED',
						label: `주문 취소 (${orderCounts.CANCELLED})`,
					},
				]}
				onChange={applyOrderFilter}
				style={{ marginBottom: 16 }}
			/>

			{filteredOrders.length === 0 ? (
				<div className="text-center p-4">
					{!statusFilter ? '주문 정보가 없습니다.' : '해당 상태의 주문이 없습니다.'}
				</div>
			) : (
				<Table
					columns={tableColumns}
					dataSource={filteredOrders}
					rowKey="orderNo"
					loading={loading}
					pagination={{
						current: page,
						pageSize,
						total: totalOrders,
						showSizeChanger: true,
						showTotal: (total) => `총 ${total}건`,
						onChange: (p, s) => {
							setPage(p);
							setPageSize(s);
						}
					}}
				/>
			)}

			{/* 주문 상세 모달 */}
			<Modal
				title="주문 상세 정보"
				open={detailModalVisible}
				onCancel={() => {
					setDetailModalVisible(false);
					setSelectedOrder(null);
				}}
				footer={[
					<Button key="close" onClick={() => {
						setDetailModalVisible(false);
						setSelectedOrder(null);
					}}>
						닫기
					</Button>,
					// 전체 발주 확인 버튼 (관리자만, PAID 상태이고 아직 발주 확인되지 않은 경우)
					userRole === 'ADMIN' && selectedOrder && selectedOrder.orderStatus === 'PAID' && !hasConfirmedItems(selectedOrder) && (
						<Button 
							key="confirm" 
							type="primary"
							icon={<CheckCircleOutlined />}
							onClick={async () => {
								try {
									await OrderService.confirmOrder(selectedOrder.orderNo);
									message.success('발주 확인이 완료되었습니다.');
									await handleViewDetail(selectedOrder.orderNo);
									fetchAllOrders();
								} catch (err) {
									message.error(err.response?.data?.message || '발주 확인에 실패했습니다.');
								}
							}}
						>
							전체 발주 확인
						</Button>
					)
				]}
				width={1000}
				maskClosable={false}
			>
				{selectedOrder && (
					<div>
						{/* 주문 정보 */}
						<h5 style={{ marginTop: 0, marginBottom: 16 }}>주문 정보</h5>
						<Descriptions bordered column={2} size="small" style={{ marginBottom: 24 }}>
							<Descriptions.Item label="주문 번호">{selectedOrder.orderNo}</Descriptions.Item>
							<Descriptions.Item label="주문 상태">
								{(() => {
									const displayStatus = getDisplayOrderStatus(selectedOrder);
									let color = getOrderStatusColor(displayStatus.status);
									if (displayStatus.status === 'RETURN_IN_PROGRESS') {
										color = 'gold';
									} else if (displayStatus.status === 'COMPLETED') {
										color = 'green';
									}
									return (
										<Tag color={color}>
											{displayStatus.label}
										</Tag>
									);
								})()}
							</Descriptions.Item>
							<Descriptions.Item label="주문 금액">{formatPrice(selectedOrder.orderTotalPrice)}</Descriptions.Item>
							<Descriptions.Item label="주문일시">{formatDate(selectedOrder.orderCreatedAt)}</Descriptions.Item>
							{/* 구매 확정은 OrderItem.completedAt으로 관리하므로 주문 레벨에서는 표시하지 않음 */}
						</Descriptions>

						{/* 결제 정보 */}
						<h5 style={{ marginBottom: 16 }}>결제 정보</h5>
						<Descriptions bordered column={2} size="small" style={{ marginBottom: 24 }}>
							{selectedOrder.paymentNo && (
								<Descriptions.Item label="결제 번호">{selectedOrder.paymentNo}</Descriptions.Item>
							)}
							<Descriptions.Item label="결제 방법">{selectedOrder.paymentMethod || '-'}</Descriptions.Item>
							<Descriptions.Item label="결제 금액">{formatPrice(selectedOrder.paymentAmount)}</Descriptions.Item>
							{selectedOrder.paymentCreatedAt && (
								<Descriptions.Item label="결제 생성일시">{formatDate(selectedOrder.paymentCreatedAt)}</Descriptions.Item>
							)}
							{selectedOrder.paidAt && (
								<Descriptions.Item label="결제 승인일시">{formatDate(selectedOrder.paidAt)}</Descriptions.Item>
							)}
							{selectedOrder.paymentCancelYn && (
								<Descriptions.Item label="결제 취소 여부">
									<Tag color={selectedOrder.paymentCancelYn ? 'red' : 'green'}>
										{selectedOrder.paymentCancelYn ? '취소됨' : '정상'}
									</Tag>
								</Descriptions.Item>
							)}
						</Descriptions>

						{/* 배송지 정보 */}
						<h5 style={{ marginBottom: 16 }}>배송지 정보</h5>
						<Descriptions bordered column={2} size="small" style={{ marginBottom: 24 }}>
							<Descriptions.Item label="수령인">{selectedOrder.recipientName}</Descriptions.Item>
							<Descriptions.Item label="연락처">{selectedOrder.recipientPhone}</Descriptions.Item>
							<Descriptions.Item label="배송지" span={2}>
								{selectedOrder.deliveryAddress} {selectedOrder.deliveryAddressDetail || ''}
								{selectedOrder.deliveryZipCode && ` (${selectedOrder.deliveryZipCode})`}
							</Descriptions.Item>
							{selectedOrder.orderMemo && (
								<Descriptions.Item label="주문 메모" span={2}>
									{selectedOrder.orderMemo}
								</Descriptions.Item>
							)}
						</Descriptions>

						{/* 주문 상품 및 배송 정보 */}
						<h5 style={{ marginBottom: 16 }}>주문 상품</h5>
						{selectedOrder.orderItems && selectedOrder.orderItems.length > 0 ? (
							<div style={{ marginBottom: 24 }}>
								{selectedOrder.orderItems.map((item, index) => (
									<Card key={index} size="small" style={{ marginBottom: 12 }}>
										<div style={{ display: 'flex', gap: 16 }}>
											{item.productImageUrl && (
												<img 
													src={item.productImageUrl} 
													alt={item.productName}
													style={{ width: 80, height: 80, objectFit: 'cover', borderRadius: 4 }}
												/>
											)}
											<div style={{ flex: 1 }}>
												<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 8 }}>
													<div style={{ fontWeight: 'bold' }}>
														{item.productName}
														{item.color && ` (${item.color}`}
														{item.size && ` / ${item.size}`}
														{(item.color || item.size) && ')'}
													</div>
													{/* OrderItem 상태 표시 */}
													{item.status && (
														<Tag color={getOrderItemStatusColor(item.status)}>
															{getOrderItemStatusLabel(item.status)}
														</Tag>
													)}
												</div>
												<div style={{ color: '#666', fontSize: '14px', marginBottom: 8 }}>
													수량: {item.quantity}개 | 단가: {formatPrice(item.itemPrice)} | 총액: {formatPrice(item.itemTotalPrice)}
												</div>
												{/* 발주 확인 버튼 및 일시 표시 */}
												<div style={{ marginBottom: 8 }}>
													{item.confirmedAt ? (
														<div style={{ color: '#999', fontSize: '12px', marginBottom: 4 }}>
															발주 확인: {formatDate(item.confirmedAt)}
														</div>
													) : (
														selectedOrder && selectedOrder.orderStatus === 'PAID' && (
															<Button
																type="primary"
																size="small"
																icon={<CheckCircleOutlined />}
																onClick={() => handleConfirmOrderItem(item.orderItemNo)}
																style={{ backgroundColor: '#52c41a', borderColor: '#52c41a', marginBottom: 4 }}
															>
																발주 확인
															</Button>
														)
													)}
												</div>
												{/* 구매 확정일시 표시 */}
												{item.completedAt && (
													<div style={{ color: '#999', fontSize: '12px', marginBottom: 4 }}>
														구매 확정: {formatDate(item.completedAt)}
													</div>
												)}
												{/* 배송 정보 */}
												{item.deliveryNo && (
													<div style={{ marginTop: 12, padding: 12, background: '#f5f5f5', borderRadius: 4 }}>
														<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
															<div style={{ fontWeight: 'bold' }}>배송 정보</div>
															<Space>
																{/* 배송 시작 버튼 (READY 상태일 때만) */}
																{item.deliveryStatus === 'READY' && (
																	<Button
																		type="primary"
																		size="small"
																		icon={<CarOutlined />}
																		onClick={() => {
																			setEditingDelivery({
																				deliveryNo: item.deliveryNo,
																				orderItemNo: item.orderItemNo,
																				trackingNumber: item.deliveryTrackingNumber || '',
																				courier: item.deliveryCourier || ''
																			});
																			deliveryForm.setFieldsValue({
																				trackingNumber: item.deliveryTrackingNumber || '',
																				courier: item.deliveryCourier || ''
																			});
																			setDeliveryStartModalVisible(true);
																		}}
																	>
																		배송 시작
																	</Button>
																)}
																{/* 배송 완료 버튼 (SHIPPED 상태일 때만) */}
																{item.deliveryStatus === 'SHIPPED' && (
																	<Button
																		type="primary"
																		size="small"
																		icon={<CheckCircleOutlined />}
																		onClick={() => handleCompleteDelivery(item.deliveryNo)}
																		style={{ backgroundColor: '#52c41a', borderColor: '#52c41a' }}
																	>
																		배송 완료
																	</Button>
																)}
																{/* 배송 정보 수정 버튼 (모든 상태에서 가능) */}
																{item.deliveryStatus !== 'DELIVERED' && (
																	<Button
																		size="small"
																		icon={<EditOutlined />}
																		onClick={() => {
																			setEditingDelivery({
																				deliveryNo: item.deliveryNo,
																				orderItemNo: item.orderItemNo,
																				trackingNumber: item.deliveryTrackingNumber || '',
																				courier: item.deliveryCourier || ''
																			});
																			deliveryForm.setFieldsValue({
																				trackingNumber: item.deliveryTrackingNumber || '',
																				courier: item.deliveryCourier || ''
																			});
																			setDeliveryUpdateModalVisible(true);
																		}}
																	>
																		정보 수정
																	</Button>
																)}
															</Space>
														</div>
														<div style={{ fontSize: '14px' }}>
															<div>배송 상태: 
																<Tag color={
																	item.deliveryStatus === 'DELIVERED' ? 'green' :
																	item.deliveryStatus === 'SHIPPED' ? 'cyan' :
																	'blue'
																} style={{ marginLeft: 8 }}>
																	{item.deliveryStatus === 'DELIVERED' ? '배송 완료' :
																	 item.deliveryStatus === 'SHIPPED' ? '배송 중' :
																	 item.deliveryStatus === 'READY' ? '배송 준비' : item.deliveryStatus}
																</Tag>
															</div>
															{item.deliveryTrackingNumber && (
																<div style={{ marginTop: 4 }}>
																	송장번호: {item.deliveryTrackingNumber}
																</div>
															)}
															{item.deliveryCourier && (
																<div style={{ marginTop: 4 }}>
																	택배사: {item.deliveryCourier}
																</div>
															)}
															{item.deliveryStartDate && (
																<div style={{ marginTop: 4 }}>
																	배송 시작일: {formatDate(item.deliveryStartDate)}
																</div>
															)}
															{item.deliveryEndDate && (
																<div style={{ marginTop: 4 }}>
																	배송 완료일: {formatDate(item.deliveryEndDate)}
																</div>
															)}
														</div>
													</div>
												)}
											</div>
										</div>
									</Card>
								))}
							</div>
						) : (
							<div style={{ textAlign: 'center', padding: 20, color: '#999' }}>
								주문 상품이 없습니다.
							</div>
						)}
					</div>
				)}
			</Modal>

			{/* 배송 시작 모달 */}
			<Modal
				title="배송 시작"
				open={deliveryStartModalVisible}
				onOk={() => deliveryForm.submit()}
				onCancel={() => {
					setDeliveryStartModalVisible(false);
					setEditingDelivery(null);
					deliveryForm.resetFields();
				}}
				okText="확인"
				cancelText="닫기"
			>
				<Form
					form={deliveryForm}
					layout="vertical"
					onFinish={handleStartDelivery}
				>
					<Form.Item
						label="송장번호"
						name="trackingNumber"
						rules={[{ required: true, message: '송장번호를 입력해주세요.' }]}
					>
						<Input placeholder="송장번호를 입력하세요" />
					</Form.Item>
					<Form.Item
						label="택배사"
						name="courier"
						rules={[{ required: true, message: '택배사를 입력해주세요.' }]}
					>
						<Input placeholder="택배사를 입력하세요 (예: CJ대한통운, 한진택배)" />
					</Form.Item>
				</Form>
			</Modal>

			{/* 배송 정보 수정 모달 */}
			<Modal
				title="배송 정보 수정"
				open={deliveryUpdateModalVisible}
				onOk={() => deliveryForm.submit()}
				onCancel={() => {
					setDeliveryUpdateModalVisible(false);
					setEditingDelivery(null);
					deliveryForm.resetFields();
				}}
				okText="확인"
				cancelText="닫기"
			>
				<Form
					form={deliveryForm}
					layout="vertical"
					onFinish={handleUpdateDelivery}
				>
					<Form.Item
						label="송장번호"
						name="trackingNumber"
					>
						<Input placeholder="송장번호를 입력하세요" />
					</Form.Item>
					<Form.Item
						label="택배사"
						name="courier"
					>
						<Input placeholder="택배사를 입력하세요 (예: CJ대한통운, 한진택배)" />
					</Form.Item>
				</Form>
			</Modal>
		</Card>
	);
};

export default OrderManagement;
