import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, message, Tag, Space, Descriptions, Row, Col, Badge, Form, Input, Tabs } from 'antd';
import { EyeOutlined, CheckCircleOutlined, BellOutlined, CarOutlined, EditOutlined, ReloadOutlined } from '@ant-design/icons';
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

const PartnerOrderManagement = () => {
	const [orders, setOrders] = useState([]);
	const [loading, setLoading] = useState(false);
	const [detailModalVisible, setDetailModalVisible] = useState(false);
	const [selectedOrder, setSelectedOrder] = useState(null);
	const [statusFilter, setStatusFilter] = useState(null); // null = 전체, 'PAID' = 발주 대기, 'ACTIVE' = 주문 진행중, 'CANCELLED' = 주문 취소
	const [deliveryStartModalVisible, setDeliveryStartModalVisible] = useState(false);
	const [deliveryUpdateModalVisible, setDeliveryUpdateModalVisible] = useState(false);
	const [editingDelivery, setEditingDelivery] = useState(null);
	const [deliveryForm] = Form.useForm();

	// 서버 페이지네이션 상태
	const [currentPage, setCurrentPage] = useState(1);
	const [pageSize, setPageSize] = useState(10);
	const [totalItems, setTotalItems] = useState(0);

	useEffect(() => {
		fetchAllOrders(currentPage, pageSize, statusFilter);
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [currentPage, pageSize, statusFilter]);

	const fetchAllOrders = async (page = 1, size = 10, status = null) => {
		try {
			setLoading(true);
			console.log('[Partner Orders] request params =>', { page, size, status: status || undefined });
			const response = await OrderService.getAllOrders({
				page,
				size,
				status: status || undefined
			});
			const data = response?.data || response;
			const items = data?.items || [];
			const total = data?.total ?? 0;
			// 서버 0-based → UI 1-based
			const respPage = ((data?.page ?? (page - 1)) + 1);
			const respSize = data?.size ?? size;
			console.log('[Partner Orders] response meta =>', { page: data?.page, size: data?.size, total: data?.total, itemsCount: Array.isArray(items) ? items.length : 0 });
			setOrders(Array.isArray(items) ? items : []);
			setTotalItems(Number.isFinite(total) ? total : 0);
			setCurrentPage(respPage);
			setPageSize(respSize);
		} catch (err) {
			message.error(err.response?.data?.message || '주문 목록을 불러오는데 실패했습니다.');
			setOrders([]);
			setTotalItems(0);
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

	// 발주 대기 주문 수 계산 (파트너의 상품 중 발주 확인되지 않은 것이 있는 주문)
	const pendingOrderCount = orders.filter(order => {
		if (order.orderStatus !== 'PAID') return false;
		if (!order.orderItems || !Array.isArray(order.orderItems) || order.orderItems.length === 0) {
			return true;
		}
		// 자신의 상품 중 하나라도 발주 확인되지 않은 것이 있으면 발주 대기
		const hasUnconfirmedItems = order.orderItems.some(item => item.confirmedAt == null);
		return hasUnconfirmedItems;
	}).length;

	// 주문 상태 표시용 함수 (OrderItem 상태를 고려한 집계 상태)
	const getDisplayOrderStatus = (order) => {
		const status = order.orderStatus;
		
		if (status === 'CANCELLED') {
			return { status: 'CANCELLED', label: '주문 취소' };
		}
		
		if (!order.orderItems || !Array.isArray(order.orderItems) || order.orderItems.length === 0) {
			return { status, label: getOrderStatusLabel(status) };
		}
		
		// 반품 진행 중이 있는지 확인
		const hasReturnInProgress = order.orderItems.some(item => 
			item.status === 'RETURN_IN_PROGRESS'
		);
		if (hasReturnInProgress) {
			return { status: 'RETURN_IN_PROGRESS', label: '반품 진행 중' };
		}
		
		// 모든 아이템이 완료되었는지 확인
		const isAllItemsCompleted = order.orderItems.every(item => 
			item.status === 'COMPLETED' || 
			item.status === 'REFUNDED' || 
			item.status === 'RETURN_REJECTED'
		);
		if (isAllItemsCompleted) {
			return { status: 'COMPLETED', label: '완료' };
		}
		
		// 파트너의 상품이 발주 확인되었으면 "주문 진행중"으로 표시
		if (status === 'PAID') {
			const hasConfirmedItems = order.orderItems.some(item => item.confirmedAt != null);
			if (hasConfirmedItems) {
				return { status: 'ACTIVE', label: '주문 진행중' };
			}
		}
		
		return { status, label: getOrderStatusLabel(status) };
	};

	// 탭 변경 시 서버 필터 적용 (페이지 1로 리셋)
	const handleChangeTab = (key) => {
		const nextStatus = key === 'ALL' ? null : key;
		setCurrentPage(1);
		setStatusFilter(nextStatus);
	};

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
				</Space>
			),
		},
	];

	return (
		<Card>
			<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
				<h4 style={{ margin: 0 }}>주문 관리</h4>
				<Button
					type="default"
					icon={<ReloadOutlined />}
					onClick={() => fetchAllOrders(currentPage, pageSize, statusFilter)}
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
						label: `전체`,
					},
					{
						key: 'PAID',
						label: `발주 대기`,
					},
					{
						key: 'ACTIVE',
						label: `주문 진행중`,
					},
					{
						key: 'CANCELLED',
						label: `주문 취소`,
					},
				]}
				onChange={handleChangeTab}
				style={{ marginBottom: 16 }}
			/>

			<div style={{ marginBottom: 8, color: '#666' }}>
				{`총 ${totalItems}건 • 페이지 ${currentPage}/${Math.max(1, Math.ceil(totalItems / pageSize))}`}
			</div>

			{orders.length === 0 ? (
				<div className="text-center p-4">
					{!statusFilter ? '주문 정보가 없습니다.' : '해당 상태의 주문이 없습니다.'}
				</div>
			) : (
				<Table
					columns={tableColumns}
					dataSource={orders}
					rowKey="orderNo"
					loading={loading}
					pagination={{
						current: currentPage,
						pageSize: pageSize,
						total: totalItems,
						showSizeChanger: true,
						showTotal: (total) => `총 ${total}건`,
						onChange: (page, size) => {
							setCurrentPage(page);
							setPageSize(size);
						},
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
					</Button>
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
						</Descriptions>

						{/* 결제 정보 */}
						<h5 style={{ marginBottom: 16 }}>결제 정보</h5>
						<Descriptions bordered column={2} size="small" style={{ marginBottom: 24 }}>
							{selectedOrder.paymentNo && (
								<Descriptions.Item label="결제 번호">{selectedOrder.paymentNo}</Descriptions.Item>
							)}
							<Descriptions.Item label="결제 방법">{selectedOrder.paymentMethod || '-'}</Descriptions.Item>
							<Descriptions.Item label="결제 금액">{formatPrice(selectedOrder.paymentAmount)}</Descriptions.Item>
							{selectedOrder.paidAt && (
								<Descriptions.Item label="결제 승인일시">{formatDate(selectedOrder.paidAt)}</Descriptions.Item>
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

export default PartnerOrderManagement;
