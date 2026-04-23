import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, Input, Form, message, Tag, Space, Row, Col, Alert, Statistic, Select } from 'antd';
import { CarOutlined, CheckCircleOutlined, EditOutlined, InfoCircleOutlined, ShoppingCartOutlined, LinkOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import DeliveryService from 'services/DeliveryService';

const { Option } = Select;

const getDeliveryStatusColor = (status) => {
	switch (status) {
		case 'READY':
			return 'blue';
		case 'SHIPPED':
			return 'cyan';
		case 'DELIVERED':
			return 'green';
		default:
			return 'default';
	}
};

const getDeliveryStatusLabel = (status) => {
	switch (status) {
		case 'READY':
			return '배송 준비';
		case 'SHIPPED':
			return '배송 중';
		case 'DELIVERED':
			return '배송 완료';
		default:
			return status;
	}
};

const getReturnStatusColor = (status) => {
	if (!status) return null;
	switch (status) {
		case 'REQUESTED':
			return 'orange';
		case 'APPROVED':
			return 'cyan';
		case 'REJECTED':
			return 'red';
		case 'PICKUP_COMPLETED':
			return 'blue';
		case 'REFUNDED':
			return 'green';
		default:
			return 'default';
	}
};

const getReturnStatusLabel = (status) => {
	if (!status) return null;
	switch (status) {
		case 'REQUESTED':
			return '반품 신청';
		case 'APPROVED':
			return '반품 승인';
		case 'REJECTED':
			return '반품 거절';
		case 'PICKUP_COMPLETED':
			return '수거 완료';
		case 'REFUNDED':
			return '환불 완료';
		default:
			return status;
	}
};

const DeliveryManagement = () => {
	const navigate = useNavigate();
	const [deliveries, setDeliveries] = useState([]);
	const [loading, setLoading] = useState(false);
	const [startModalVisible, setStartModalVisible] = useState(false);
	const [updateModalVisible, setUpdateModalVisible] = useState(false);
	const [editingDelivery, setEditingDelivery] = useState(null);
	const [form] = Form.useForm();
	const [statusFilter, setStatusFilter] = useState('ALL'); // ALL, READY, SHIPPED, DELIVERED

	useEffect(() => {
		fetchAllDeliveries();
	}, []);

	const fetchAllDeliveries = async () => {
		try {
			setLoading(true);
			const response = await DeliveryService.getAllDeliveries();
			const deliveriesData = response.data || response || [];
			setDeliveries(Array.isArray(deliveriesData) ? deliveriesData : []);
		} catch (err) {
			console.error('배송 목록 조회 실패:', err);
			message.error(err.response?.data?.message || '배송 목록을 불러오는데 실패했습니다.');
			setDeliveries([]);
		} finally {
			setLoading(false);
		}
	};

	const handleStartDelivery = async (values) => {
		try {
			await DeliveryService.startDelivery(editingDelivery.deliveryNo, {
				trackingNumber: values.trackingNumber,
				courier: values.courier,
			});
			message.success('배송이 시작되었습니다.');
			setStartModalVisible(false);
			setEditingDelivery(null);
			form.resetFields();
			fetchAllDeliveries();
		} catch (err) {
			message.error(err.response?.data?.message || '배송 시작에 실패했습니다.');
		}
	};

	const handleCompleteDelivery = async (deliveryNo) => {
		Modal.confirm({
			title: '배송 완료 처리',
			content: '배송을 완료 처리하시겠습니까?',
			onOk: async () => {
				try {
					await DeliveryService.completeDelivery(deliveryNo);
					message.success('배송이 완료되었습니다.');
					fetchAllDeliveries();
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
			setUpdateModalVisible(false);
			setEditingDelivery(null);
			form.resetFields();
			fetchAllDeliveries();
		} catch (err) {
			message.error(err.response?.data?.message || '배송 정보 수정에 실패했습니다.');
		}
	};

	const openStartModal = (delivery) => {
		setEditingDelivery(delivery);
		form.resetFields();
		setStartModalVisible(true);
	};

	const openUpdateModal = (delivery) => {
		setEditingDelivery(delivery);
		form.setFieldsValue({
			trackingNumber: delivery.deliveryTrackingNumber || '',
			courier: delivery.deliveryCourier || '',
		});
		setUpdateModalVisible(true);
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

	// 배송 상태별 통계 계산
	const getDeliveryStats = () => {
		const stats = {
			total: deliveries.length,
			ready: 0,
			shipped: 0,
			delivered: 0,
		};
		deliveries.forEach((delivery) => {
			switch (delivery.deliveryStatus) {
				case 'READY':
					stats.ready++;
					break;
				case 'SHIPPED':
					stats.shipped++;
					break;
				case 'DELIVERED':
					stats.delivered++;
					break;
			}
		});
		return stats;
	};

	// 필터링된 배송 목록
	const getFilteredDeliveries = () => {
		if (statusFilter === 'ALL') {
			return deliveries;
		}
		return deliveries.filter((delivery) => delivery.deliveryStatus === statusFilter);
	};

	const stats = getDeliveryStats();
	const filteredDeliveries = getFilteredDeliveries();

	// 주문 관리 페이지로 이동하고 주문 상세 모달 자동 열기
	const handleGoToOrder = (orderNo) => {
		if (orderNo) {
			navigate('/app/apps/order', { state: { openOrderNo: orderNo } });
		}
	};

	const tableColumns = [
		{
			title: '배송 번호',
			dataIndex: 'deliveryNo',
			key: 'deliveryNo',
		},
		{
			title: '주문 번호',
			dataIndex: 'orderNo',
			key: 'orderNo',
			render: (text, record) => (
				text ? (
					<Space>
						<span>{text}</span>
						<Button
							type="link"
							size="small"
							icon={<LinkOutlined />}
							onClick={() => handleGoToOrder(text)}
						>
							주문 보기
						</Button>
					</Space>
				) : '-'
			),
		},
		{
			title: '주문 아이템 번호',
			dataIndex: 'orderItemNo',
			key: 'orderItemNo',
			render: (text) => text || '-',
		},
		{
			title: '상품 정보',
			key: 'productInfo',
			render: (_, record) => (
				<div>
					<div style={{ fontWeight: 'bold' }}>{record.productName || '-'}</div>
					{record.color && record.size && (
						<div style={{ fontSize: '12px', color: '#666' }}>
							{record.color} / {record.size}
						</div>
					)}
				</div>
			),
		},
		{
			title: '배송 상태',
			dataIndex: 'deliveryStatus',
			key: 'deliveryStatus',
			render: (status) => (
				<Tag color={getDeliveryStatusColor(status)}>
					{getDeliveryStatusLabel(status)}
				</Tag>
			),
		},
		{
			title: '반품 상태',
			dataIndex: 'returnStatus',
			key: 'returnStatus',
			render: (status) => {
				if (!status) return '-';
				return (
					<Tag color={getReturnStatusColor(status)}>
						{getReturnStatusLabel(status)}
					</Tag>
				);
			},
		},
		{
			title: '택배사',
			dataIndex: 'deliveryCourier',
			key: 'deliveryCourier',
			render: (text) => text || '-',
		},
		{
			title: '송장번호',
			dataIndex: 'deliveryTrackingNumber',
			key: 'deliveryTrackingNumber',
			render: (text) => text || '-',
		},
		{
			title: '배송 시작일',
			dataIndex: 'deliveryStartDate',
			key: 'deliveryStartDate',
			render: (date) => formatDate(date),
		},
		{
			title: '배송 완료일',
			dataIndex: 'deliveryEndDate',
			key: 'deliveryEndDate',
			render: (date) => formatDate(date),
		},
		{
			title: '작업',
			key: 'actions',
			render: (_, record) => (
				<Space>
					{record.deliveryStatus === 'READY' && (
						<Button
							type="primary"
							icon={<CarOutlined />}
							size="small"
							onClick={() => openStartModal(record)}
						>
							배송 시작
						</Button>
					)}
					{record.deliveryStatus === 'SHIPPED' && (
						<>
							<Button
								type="primary"
								icon={<CheckCircleOutlined />}
								size="small"
								onClick={() => handleCompleteDelivery(record.deliveryNo)}
							>
								배송 완료
							</Button>
							<Button
								icon={<EditOutlined />}
								size="small"
								onClick={() => openUpdateModal(record)}
							>
								정보 수정
							</Button>
						</>
					)}
				</Space>
			),
		},
	];

	return (
		<div>
			{/* 안내 카드 */}
			<Alert
				message="배송 전용 페이지"
				description={
					<div>
						<p style={{ marginBottom: '8px' }}>
							이 페이지는 <strong>배송 상태만 집중적으로</strong> 보고 싶을 때 사용합니다.
						</p>
						<p style={{ marginBottom: 0 }}>
							주문 전체 정보(결제, 주문 상태 등)를 보려면{' '}
							<Button
								type="link"
								size="small"
								icon={<ShoppingCartOutlined />}
								onClick={() => navigate('/app/apps/order')}
								style={{ padding: 0, height: 'auto' }}
							>
								주문 관리
							</Button>
							로 이동하세요.
						</p>
					</div>
				}
				type="info"
				icon={<InfoCircleOutlined />}
				showIcon
				style={{ marginBottom: 16 }}
			/>

			{/* 통계 카드 */}
			<Row gutter={16} style={{ marginBottom: 16 }}>
				<Col span={6}>
					<Card>
						<Statistic
							title="전체 배송"
							value={stats.total}
							prefix={<CarOutlined />}
						/>
					</Card>
				</Col>
				<Col span={6}>
					<Card>
						<Statistic
							title="배송 준비"
							value={stats.ready}
							valueStyle={{ color: '#1890ff' }}
							prefix={<CarOutlined />}
						/>
					</Card>
				</Col>
				<Col span={6}>
					<Card>
						<Statistic
							title="배송 중"
							value={stats.shipped}
							valueStyle={{ color: '#13c2c2' }}
							prefix={<CarOutlined />}
						/>
					</Card>
				</Col>
				<Col span={6}>
					<Card>
						<Statistic
							title="배송 완료"
							value={stats.delivered}
							valueStyle={{ color: '#52c41a' }}
							prefix={<CheckCircleOutlined />}
						/>
					</Card>
				</Col>
			</Row>

			<Card>
				<div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
					<h4 style={{ margin: 0 }}>배송 목록</h4>
					<Select
						value={statusFilter}
						onChange={setStatusFilter}
						style={{ width: 150 }}
					>
						<Option value="ALL">전체</Option>
						<Option value="READY">배송 준비</Option>
						<Option value="SHIPPED">배송 중</Option>
						<Option value="DELIVERED">배송 완료</Option>
					</Select>
				</div>
				{filteredDeliveries.length === 0 ? (
					<div className="text-center p-4">
						{deliveries.length === 0 ? '배송 정보가 없습니다.' : '선택한 조건에 맞는 배송이 없습니다.'}
					</div>
				) : (
					<Table
						columns={tableColumns}
						dataSource={filteredDeliveries}
						rowKey="deliveryNo"
						loading={loading}
						pagination={{
							pageSize: 10,
							showSizeChanger: true,
							showTotal: (total) => `총 ${total}건`,
						}}
					/>
				)}
			</Card>

			{/* 배송 시작 모달 */}
			<Modal
				title="배송 시작"
				open={startModalVisible}
				onCancel={() => {
					setStartModalVisible(false);
					setEditingDelivery(null);
					form.resetFields();
				}}
				onOk={() => form.submit()}
			>
				<Form
					form={form}
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
						<Input placeholder="택배사를 입력하세요 (예: CJ대한통운)" />
					</Form.Item>
				</Form>
			</Modal>

			{/* 배송 정보 수정 모달 */}
			<Modal
				title="배송 정보 수정"
				open={updateModalVisible}
				onCancel={() => {
					setUpdateModalVisible(false);
					setEditingDelivery(null);
					form.resetFields();
				}}
				onOk={() => form.submit()}
			>
				<Form
					form={form}
					layout="vertical"
					onFinish={handleUpdateDelivery}
				>
					<Form.Item
						label="송장번호"
						name="trackingNumber"
					>
						<Input placeholder="송장번호를 입력하세요 (선택)" />
					</Form.Item>
					<Form.Item
						label="택배사"
						name="courier"
					>
						<Input placeholder="택배사를 입력하세요 (선택)" />
					</Form.Item>
				</Form>
			</Modal>
		</div>
	);
};

export default DeliveryManagement;
