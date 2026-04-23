import React, { useState, useEffect } from 'react';
import { useSelector } from 'react-redux';
import { useLocation } from 'react-router-dom';
import { Card, Table, Button, Modal, Select, message, Tag, Space, Descriptions, Row, Col, Badge, Input, Form, Tabs } from 'antd';
import { ShoppingCartOutlined, EyeOutlined, CheckCircleOutlined, BellOutlined, CarOutlined, EditOutlined, ReloadOutlined } from '@ant-design/icons';
import OrderService from 'services/OrderService';
import DeliveryService from 'services/DeliveryService';

const { Option } = Select;

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
	const user = useSelector((state) => state.auth.user);
	const userRole = user?.role || 'ADMIN'; // 기본값 ADMIN
	
	const [orders, setOrders] = useState([]);
	const [totalOrders, setTotalOrders] = useState(0);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(10);
	const [loading, setLoading] = useState(false);
	const [detailModalVisible, setDetailModalVisible] = useState(false);
	const [statusModalVisible, setStatusModalVisible] = useState(false);
	const [selectedOrder, setSelectedOrder] = useState(null);
	const [selectedStatus, setSelectedStatus] = useState(null);
	const [statusFilter, setStatusFilter] = useState(null); // null = 전체, 'PAID' = 발주 대기, 'ACTIVE' = 주문 진행중, 'CANCELLED' = 주문 취소
	const [deliveryStartModalVisible, setDeliveryStartModalVisible] = useState(false);
	const [deliveryUpdateModalVisible, setDeliveryUpdateModalVisible] = useState(false);
	const [editingDelivery, setEditingDelivery] = useState(null);
	const [deliveryForm] = Form.useForm();

	useEffect(() => {
		fetchAllOrders();
	}, [page, pageSize]);

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
			console.log('[Admin Orders] response meta =>', { page: payload?.page, size: payload?.size, total: payload?.total, itemsCount: Array.isArray(items) ? items.length : 0 });
			setOrders(Array.isArray(items) ? items : []);
			setTotalOrders(Number(total) || 0);
		} catch (err) {
			message.error(err.response?.data?.message || '주문 목록을 불러오는데 실패했습니다.');
			setOrders([]);
			setTotalOrders(0);
		} finally {
			setLoading(false);
		}
	};

	const handleViewDetail = async (orderNo) => {
		try {
			const response = await OrderService.getOrderForAdmin(orderNo);
			const orderData = response.data || response;
			setSelectedOrder(orderData);
			setDetailModalVisible(true);
		} catch (err) {
			message.error(err.response?.data?.message || '주문 상세 정보를 불러오는데 실패했습니다.');
		}
	};

	const handleStatusChange = (order) => {
		setSelectedOrder(order);
		setSelectedStatus(order.orderStatus);
		setStatusModalVisible(true);
	};

	const handleUpdateStatus = async () => {
		if (!selectedOrder || !selectedStatus) return;

		try {
			await OrderService.updateOrderStatus(selectedOrder.orderNo, selectedStatus);
			message.success('주문 상태가 변경되었습니다.');
			setStatusModalVisible(false);
			setSelectedStatus(null);
			
			// 주문 상세 모달이 열려있으면 상세 정보도 갱신
			if (detailModalVisible) {
				await handleViewDetail(selectedOrder.orderNo);
			}
			
			fetchAllOrders();
		} catch (err) {
			message.error(err.response?.data?.message || '주문 상태 변경에 실패했습니다.');
		}
	};

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
	
	// 주문에 반품 진행 중이거나 환불 완료된 아이템이 있는지 확인 (OrderItem.status 기반)
	const hasActiveReturnOrRefunded = (order) => {
		if (!order.orderItems || !Array.isArray(order.orderItems)) return false;
		return order.orderItems.some(item => {
			if (!item.status) return false;
			// 반품 진행 중이거나 환불 완료된 경우
			return item.status === 'RETURN_IN_PROGRESS' || 
				   item.status === 'REFUNDED';
		});
	};
	
	// 구매 확정된 주문 상품이 있는지 확인 (OrderItem.status 기반)
	const hasCompletedItems = (order) => {
		if (!order.orderItems || !Array.isArray(order.orderItems)) return false;
		return order.orderItems.some(item => item.status === 'COMPLETED');
	};
	
	// 발주 확인된 주문 상품이 있는지 확인
	const hasConfirmedItems = (order) => {
		if (!order.orderItems || !Array.isArray(order.orderItems)) return false;
		return order.orderItems.some(item => item.confirmedAt != null);
	};
	
	// 배송이 시작된 주문 상품이 있는지 확인
	const hasShippedDelivery = (order) => {
		if (!order.orderItems || !Array.isArray(order.orderItems)) return false;
		return order.orderItems.some(item => 
			item.deliveryStatus === 'SHIPPED' || item.deliveryStatus === 'DELIVERED'
		);
	};
	
	// 주문 상태 변경 가능 여부 확인
	const canChangeOrderStatus = (order) => {
		// 취소된 주문은 변경 불가
		if (order.orderStatus === 'CANCELLED') return false;
		
		// 구매 확정된 주문 상품이 있으면 변경 불가
		if (hasCompletedItems(order)) return false;
		
		// 반품 진행 중이거나 환불 완료된 주문은 변경 불가
		if (hasActiveReturnOrRefunded(order)) return false;
		
		// 발주 확인된 OrderItem이 있으면 변경 불가
		if (hasConfirmedItems(order)) return false;
		
		// 배송이 시작된 경우 변경 불가
		if (hasShippedDelivery(order)) return false;
		
		// PAID 상태는 발주 확인 버튼만 표시 (상태 변경 버튼 아님)
		if (order.orderStatus === 'PAID') return false;
		
		return true;
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
	
	// 주문 상태별 개수 계산
	const orderCounts = {
		ALL: orders.length,
		PAID: orders.filter(order => order.orderStatus === 'PAID').length,
		ACTIVE: orders.filter(order => order.orderStatus === 'ACTIVE').length,
		CANCELLED: orders.filter(order => order.orderStatus === 'CANCELLED').length,
	};

	// 필터링된 주문 목록
	const filteredOrders = !statusFilter 
		? orders 
		: orders.filter(order => order.orderStatus === statusFilter);

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
					{/* 관리자만 주문 상태 변경 가능 */}
					{userRole === 'ADMIN' && canChangeOrderStatus(record) && (
						<Button
							type="primary"
							icon={<CheckCircleOutlined />}
							size="small"
							onClick={() => handleStatusChange(record)}
						>
							상태 변경
						</Button>
					)}
					{userRole === 'ADMIN' && !canChangeOrderStatus(record) && record.orderStatus !== 'PAID' && (
						<Button
							type="primary"
							icon={<CheckCircleOutlined />}
							size="small"
							disabled
							title={
								hasConfirmedItems(record) || hasShippedDelivery(record)
									? '발주 확인되었거나 배송이 시작된 주문은 상태 변경할 수 없습니다'
									: hasActiveReturnOrRefunded(record)
									? '반품 진행 중이거나 환불 완료된 주문은 상태 변경할 수 없습니다'
									: hasCompletedItems(record)
									? '구매 확정된 주문 상품이 있는 주문은 상태 변경할 수 없습니다'
									: '주문 상태를 변경할 수 없습니다'
							}
						>
							상태 변경
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
								onClick={() => setStatusFilter('PAID')}
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
						key: 'PAID',
						label: `발주 대기 (${orderCounts.PAID})`,
					},
					{
						key: 'ACTIVE',
						label: `주문 진행중 (${orderCounts.ACTIVE})`,
					},
					{
						key: 'CANCELLED',
						label: `주문 취소 (${orderCounts.CANCELLED})`,
					},
				]}
				onChange={(key) => setStatusFilter(key === 'ALL' ? null : key)}
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
					),
					// 관리자만 주문 상태 변경 가능
					userRole === 'ADMIN' && selectedOrder && canChangeOrderStatus(selectedOrder) && (
						<Button 
							key="status" 
							type="primary"
							icon={<CheckCircleOutlined />}
							onClick={() => {
								if (selectedOrder) {
									setSelectedStatus(selectedOrder.orderStatus);
									setStatusModalVisible(true);
								}
							}}
						>
							상태 변경
						</Button>
					),
					userRole === 'ADMIN' && selectedOrder && !canChangeOrderStatus(selectedOrder) && selectedOrder.orderStatus !== 'PAID' && (
						<Button 
							key="status-disabled" 
							type="primary"
							icon={<CheckCircleOutlined />}
							disabled
							title={
								hasConfirmedItems(selectedOrder) || hasShippedDelivery(selectedOrder)
									? '발주 확인되었거나 배송이 시작된 주문은 상태 변경할 수 없습니다'
									: hasActiveReturnOrRefunded(selectedOrder)
									? '반품 진행 중이거나 환불 완료된 주문은 상태 변경할 수 없습니다'
									: hasCompletedItems(selectedOrder)
									? '구매 확정된 주문 상품이 있는 주문은 상태 변경할 수 없습니다'
									: '주문 상태를 변경할 수 없습니다'
							}
						>
							상태 변경
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

			{/* 주문 상태 변경 모달 */}
			<Modal
				title="주문 상태 변경"
				open={statusModalVisible}
				onOk={handleUpdateStatus}
				onCancel={() => {
					setStatusModalVisible(false);
					setSelectedStatus(null);
					// selectedOrder는 주문 상세 모달에서 사용하므로 null로 설정하지 않음
				}}
				okText="확인"
				cancelText="닫기"
				zIndex={1001}
				maskClosable={false}
			>
				{selectedOrder && (
					<div>
						<p>주문 번호: {selectedOrder.orderNo}</p>
						<p>현재 상태: 
							<Tag color={getOrderStatusColor(selectedOrder.orderStatus)} style={{ marginLeft: 8 }}>
								{getOrderStatusLabel(selectedOrder.orderStatus)}
							</Tag>
						</p>
						{/* 발주 확인/배송 상태 안내 */}
						{(() => {
							const hasConfirmed = hasConfirmedItems(selectedOrder);
							const hasShippedDelivery = selectedOrder.orderItems?.some(item => 
								item.deliveryStatus === 'SHIPPED' || item.deliveryStatus === 'DELIVERED'
							);
							
							if (hasConfirmed || hasShippedDelivery) {
								return (
									<div style={{ 
										marginBottom: 16, 
										padding: 12, 
										background: '#fff7e6', 
										border: '1px solid #ffd591',
										borderRadius: 4,
										color: '#d46b08'
									}}>
										<strong>⚠️ 안내:</strong> {
											hasConfirmed && hasShippedDelivery 
												? '발주 확인되었거나 배송이 시작된 주문은 취소할 수 없습니다. 반품으로 처리해주세요.'
												: hasConfirmed 
													? '발주 확인된 주문은 취소할 수 없습니다. 반품으로 처리해주세요.'
													: '배송이 시작된 주문은 취소할 수 없습니다. 반품으로 처리해주세요.'
										}
									</div>
								);
							}
							return null;
						})()}
						<p style={{ marginTop: 16 }}>변경할 상태:</p>
						<Select
							style={{ width: '100%' }}
							value={selectedStatus}
							onChange={setSelectedStatus}
							placeholder="상태를 선택하세요"
							optionLabelProp="children"
							getPopupContainer={(trigger) => document.body}
							dropdownStyle={{ zIndex: 1002 }}
						>
							{/* 현재 상태에 따라 선택 가능한 옵션만 표시 */}
							{selectedOrder.orderStatus === 'PENDING_PAYMENT' && (() => {
								// 발주 확인된 OrderItem이 있는지 확인 (일반적으로 없지만 안전을 위해 확인)
								const hasConfirmed = hasConfirmedItems(selectedOrder);
								
								return (
									<>
										<Option value="PAID">결제 완료</Option>
										<Option value="PAYMENT_FAILED">결제 실패</Option>
										{hasConfirmed ? (
											<Option value="CANCELLED" disabled>
												주문 취소 (발주 확인된 주문은 취소 불가)
											</Option>
										) : (
											<Option value="CANCELLED">주문 취소</Option>
										)}
									</>
								);
							})()}
							{selectedOrder.orderStatus === 'PAID' && (() => {
								// 발주 확인된 OrderItem이 있는지 확인
								const hasConfirmed = hasConfirmedItems(selectedOrder);
								
								return (
									<>
										<Option value="ACTIVE">주문 진행중</Option>
										{hasConfirmed ? (
											<Option value="CANCELLED" disabled>
												주문 취소 (발주 확인된 주문은 취소 불가)
											</Option>
										) : (
											<Option value="CANCELLED">주문 취소</Option>
										)}
									</>
								);
							})()}
							{selectedOrder.orderStatus === 'ACTIVE' && (() => {
								// 발주 확인된 OrderItem이 있는지 확인
								const hasConfirmed = hasConfirmedItems(selectedOrder);
								// 배송 상태 확인: 배송 중(SHIPPED) 또는 배송 완료(DELIVERED)인 경우 취소 불가
								const hasShippedDelivery = selectedOrder.orderItems?.some(item => 
									item.deliveryStatus === 'SHIPPED' || item.deliveryStatus === 'DELIVERED'
								);
								
								// 발주 확인되었거나 배송이 시작된 경우 취소 불가
								const cannotCancel = hasConfirmed || hasShippedDelivery;
								
								return (
									<>
										{/* 구매 확정은 OrderItem.completedAt으로 관리하므로 주문 상태 변경 옵션에서 제거 */}
										{cannotCancel ? (
											<Option value="CANCELLED" disabled>
												주문 취소 ({hasConfirmed ? '발주 확인된' : '배송 중/완료 상태인'} 주문은 취소 불가)
											</Option>
										) : (
											<Option value="CANCELLED">주문 취소</Option>
										)}
									</>
								);
							})()}
							{selectedOrder.orderStatus === 'PAYMENT_FAILED' && (
								<Option value="PENDING_PAYMENT">결제 대기 (재시도)</Option>
							)}
						</Select>
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
