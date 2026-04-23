import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, Form, Input, InputNumber, Switch, Space, message, Tag } from 'antd';
import { EditOutlined, ReloadOutlined } from '@ant-design/icons';
import AdminService from 'services/AdminService';

const CustomerGradeManagement = () => {
	const [loading, setLoading] = useState(false);
	const [grades, setGrades] = useState([]);
	const [modalVisible, setModalVisible] = useState(false);
	const [editingGrade, setEditingGrade] = useState(null);
	const [form] = Form.useForm();

	// 등급별 고객 모달 상태
	const [customerModalVisible, setCustomerModalVisible] = useState(false);
	const [customerModalLoading, setCustomerModalLoading] = useState(false);
	const [customerModalGrade, setCustomerModalGrade] = useState(null);
	const [customerModalData, setCustomerModalData] = useState([]);
	const [customerModalPage, setCustomerModalPage] = useState(0);
	const [customerModalPageSize, setCustomerModalPageSize] = useState(10);
	const [customerModalTotal, setCustomerModalTotal] = useState(0);

	useEffect(() => {
		fetchGrades();
	}, []);

	const fetchGrades = async () => {
		try {
			setLoading(true);
			const response = await AdminService.getCustomerGrades();
			const data = response.data || response;
			setGrades(Array.isArray(data) ? data : []);
		} catch (err) {
			console.error('등급 목록 조회 실패:', err);
			message.error(err.response?.data?.message || '등급 목록을 불러오는데 실패했습니다.');
			setGrades([]);
		} finally {
			setLoading(false);
		}
	};

	const handleOpenModal = (grade) => {
		if (!grade) {
			message.warning('등급을 선택해주세요.');
			return;
		}
		
		setEditingGrade(grade);
		form.setFieldsValue({
			gradeName: grade.gradeName,
			gradeLevel: grade.gradeLevel,
			minPurchaseAmount: grade.minPurchaseAmount,
			minOrderCount: grade.minOrderCount,
			discountRate: grade.discountRate,
			pointAccumulationRate: grade.pointAccumulationRate,
			isActive: grade.isActive !== false
		});
		setModalVisible(true);
	};

	// 특정 등급에 속한 고객 목록 조회 모달 열기
	const handleOpenCustomerModal = async (grade) => {
		if (!grade) {
			message.warning('등급을 선택해주세요.');
		 return;
		}
		setCustomerModalGrade(grade);
		setCustomerModalVisible(true);
		// 첫 페이지 로딩
		await fetchCustomersByGrade(grade.gradeName, 0, customerModalPageSize);
		setCustomerModalPage(0);
	};

	const fetchCustomersByGrade = async (gradeName, page, pageSize) => {
		try {
			setCustomerModalLoading(true);
			const response = await AdminService.getCustomerList({
				page,
				pageSize,
				grade: gradeName,
			});
			const data = response.data || response;
			setCustomerModalData(data.customers || []);
			setCustomerModalTotal(data.totalCount || 0);
		} catch (err) {
			console.error('등급별 고객 목록 조회 실패:', err);
			message.error(err.response?.data?.message || '고객 목록을 불러오는데 실패했습니다.');
			setCustomerModalData([]);
			setCustomerModalTotal(0);
		} finally {
			setCustomerModalLoading(false);
		}
	};

	const handleSave = async () => {
		try {
			const values = await form.validateFields();
			
			if (!editingGrade) {
				message.error('등급을 선택해주세요.');
				return;
			}
			
			await AdminService.updateCustomerGrade(editingGrade.gradeId, values);
			message.success('등급이 수정되었습니다.');
			
			setModalVisible(false);
			setEditingGrade(null);
			form.resetFields();
			await fetchGrades();
		} catch (err) {
			console.error('등급 저장 실패:', err);
			message.error(err.response?.data?.message || '등급 저장에 실패했습니다.');
		}
	};

	const formatCurrency = (amount) => {
		return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(amount || 0);
	};

	// 등급명을 한글로 변환
	const getGradeNameLabel = (gradeName) => {
		const gradeNameMap = {
			'BEGINNER': '초보자',
			'SWIMMER': '수영인',
			'PRO': '프로',
			'MASTER': '마스터',
			'LEGEND': '레전드'
		};
		return gradeNameMap[gradeName] || gradeName;
	};

	const columns = [
		{
			title: '등급명',
			dataIndex: 'gradeName',
			key: 'gradeName',
			render: (text, record) => (
				<Space>
					<strong>{getGradeNameLabel(text)}</strong>
					<Tag color="default">{text}</Tag>
					{!record.isActive && <Tag color="red">비활성</Tag>}
				</Space>
			),
		},
		{
			title: '등급 순서',
			dataIndex: 'gradeLevel',
			key: 'gradeLevel',
			align: 'center',
			sorter: (a, b) => a.gradeLevel - b.gradeLevel,
		},
		{
			title: '최소 누적 구매액',
			dataIndex: 'minPurchaseAmount',
			key: 'minPurchaseAmount',
			align: 'right',
			render: (amount) => formatCurrency(amount),
			sorter: (a, b) => a.minPurchaseAmount - b.minPurchaseAmount,
		},
		{
			title: '최소 주문 건수',
			dataIndex: 'minOrderCount',
			key: 'minOrderCount',
			align: 'right',
			sorter: (a, b) => a.minOrderCount - b.minOrderCount,
		},
		{
			title: '할인율',
			dataIndex: 'discountRate',
			key: 'discountRate',
			align: 'right',
			render: (rate) => `${rate || 0}%`,
			sorter: (a, b) => (a.discountRate || 0) - (b.discountRate || 0),
		},
		{
			title: '포인트 적립률',
			dataIndex: 'pointAccumulationRate',
			key: 'pointAccumulationRate',
			align: 'right',
			render: (rate) => `${rate || 0}%`,
			sorter: (a, b) => (a.pointAccumulationRate || 0) - (b.pointAccumulationRate || 0),
		},
		{
			title: '작업',
			key: 'action',
			width: 200,
			render: (_, record) => (
				<Space>
					<Button
						type="link"
						icon={<EditOutlined />}
						onClick={() => handleOpenModal(record)}
					>
						수정
					</Button>
					<Button
						type="link"
						onClick={() => handleOpenCustomerModal(record)}
					>
						해당 고객 보기
					</Button>
				</Space>
			),
		},
	];

	return (
		<div>
			<Card
				title="고객 등급 관리"
				extra={
					<Button icon={<ReloadOutlined />} onClick={fetchGrades} loading={loading}>
						새로고침
					</Button>
				}
			>
				<Table
					columns={columns}
					dataSource={grades}
					rowKey="gradeId"
					loading={loading}
					pagination={false}
					size="small"
				/>
			</Card>

			{/* 등급별 고객 목록 모달 */}
			<Modal
				title={customerModalGrade ? `${getGradeNameLabel(customerModalGrade.gradeName)} 등급 고객 목록` : '등급별 고객 목록'}
				open={customerModalVisible}
				onCancel={() => {
					setCustomerModalVisible(false);
					setCustomerModalData([]);
					setCustomerModalTotal(0);
					setCustomerModalGrade(null);
					setCustomerModalPage(0);
				}}
				footer={null}
				width={800}
			>
				<Table
					rowKey="customerId"
					dataSource={customerModalData}
					loading={customerModalLoading}
					size="small"
					columns={[
						{
							title: '고객 ID',
							dataIndex: 'customerId',
							key: 'customerId',
							width: 80,
						},
						{
							title: '이름',
							dataIndex: 'customerName',
							key: 'customerName',
							width: 120,
						},
						{
							title: '이메일',
							dataIndex: 'customerEmail',
							key: 'customerEmail',
							width: 200,
						},
						{
							title: '가입일',
							dataIndex: 'customerCreateAt',
							key: 'customerCreateAt',
							render: (value) => (value ? new Date(value).toLocaleString() : '-'),
						},
						{
							title: '주문 건수',
							dataIndex: 'orderCount',
							key: 'orderCount',
							width: 100,
							render: (count) => `${count || 0}건`,
						},
					]}
					pagination={{
						current: customerModalPage + 1,
						pageSize: customerModalPageSize,
						total: customerModalTotal,
						showSizeChanger: true,
						pageSizeOptions: ['10', '20', '50'],
						showTotal: (total) => `총 ${total}명`,
						onChange: (page, pageSize) => {
							const newPage = page - 1;
							setCustomerModalPage(newPage);
							setCustomerModalPageSize(pageSize);
							if (customerModalGrade) {
								fetchCustomersByGrade(customerModalGrade.gradeName, newPage, pageSize);
							}
						},
					}}
				/>
			</Modal>

			<Modal
				title="등급 수정"
				open={modalVisible}
				onOk={handleSave}
				onCancel={() => {
					setModalVisible(false);
					setEditingGrade(null);
					form.resetFields();
				}}
				width={600}
				okText="저장"
				cancelText="취소"
			>
				<Form
					form={form}
					layout="vertical"
				>
					<Form.Item
						name="gradeName"
						label="등급명"
					>
						<Input 
							disabled 
							placeholder="등급명은 고정되어 있습니다"
							addonBefore={editingGrade ? getGradeNameLabel(editingGrade.gradeName) : ''}
						/>
					</Form.Item>

					<Form.Item
						name="gradeLevel"
						label="등급 순서"
						tooltip="등급 순서는 고정되어 있습니다"
					>
						<InputNumber disabled style={{ width: '100%' }} />
					</Form.Item>

					<Form.Item
						name="minPurchaseAmount"
						label="최소 누적 구매액 (원)"
						rules={[{ required: true, message: '최소 누적 구매액을 입력해주세요.' }]}
					>
						<InputNumber min={0} style={{ width: '100%' }} formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')} />
					</Form.Item>

					<Form.Item
						name="minOrderCount"
						label="최소 주문 건수"
						rules={[{ required: true, message: '최소 주문 건수를 입력해주세요.' }]}
					>
						<InputNumber min={0} style={{ width: '100%' }} />
					</Form.Item>

					<Form.Item
						name="discountRate"
						label="할인율 (%)"
						rules={[{ required: true, message: '할인율을 입력해주세요.' }]}
					>
						<InputNumber min={0} max={100} step={0.1} style={{ width: '100%' }} />
					</Form.Item>

					<Form.Item
						name="pointAccumulationRate"
						label="포인트 적립률 (%)"
						rules={[{ required: true, message: '포인트 적립률을 입력해주세요.' }]}
					>
						<InputNumber min={0} max={100} step={0.1} style={{ width: '100%' }} />
					</Form.Item>

					<Form.Item
						name="isActive"
						label="활성화"
						valuePropName="checked"
					>
						<Switch />
					</Form.Item>
				</Form>
			</Modal>
		</div>
	);
};

export default CustomerGradeManagement;
