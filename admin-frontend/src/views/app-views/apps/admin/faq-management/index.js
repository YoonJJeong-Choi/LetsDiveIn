import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, Form, Input, Select, Space, message, Popconfirm, Tag } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import AdminService from 'services/AdminService';

const { TextArea } = Input;
const { Option } = Select;

const FaqManagement = () => {
	const [loading, setLoading] = useState(false);
	const [faqs, setFaqs] = useState([]);
	const [modalVisible, setModalVisible] = useState(false);
	const [editingFaq, setEditingFaq] = useState(null);
	const [form] = Form.useForm();

	// FAQ 카테고리 옵션
	const faqCategories = [
		{ value: '주문/결제', label: '주문/결제' },
		{ value: '배송', label: '배송' },
		{ value: '취소/반품/교환', label: '취소/반품/교환' },
		{ value: '회원정보', label: '회원정보' },
		{ value: '상품', label: '상품' },
		{ value: '포인트/쿠폰', label: '포인트/쿠폰' },
		{ value: '기타', label: '기타' }
	];

	useEffect(() => {
		fetchFaqs();
	}, []);

	const fetchFaqs = async () => {
		try {
			setLoading(true);
			const response = await AdminService.getFaqList();
			const data = response.data || response;
			setFaqs(Array.isArray(data) ? data : []);
		} catch (err) {
			console.error('FAQ 목록 조회 실패:', err);
			message.error(err.response?.data?.message || 'FAQ 목록을 불러오는데 실패했습니다.');
			setFaqs([]);
		} finally {
			setLoading(false);
		}
	};

	const handleOpenModal = (faq = null) => {
		setEditingFaq(faq);
		if (faq) {
			form.setFieldsValue({
				faqQuestion: faq.faqQuestion,
				faqAnswer: faq.faqAnswer,
				faqCategory: faq.faqCategory
			});
		} else {
			form.resetFields();
		}
		setModalVisible(true);
	};

	const handleSave = async () => {
		try {
			const values = await form.validateFields();
			
			if (editingFaq) {
				await AdminService.updateFaq(editingFaq.faqNo, values);
				message.success('FAQ가 수정되었습니다.');
			} else {
				await AdminService.createFaq(values);
				message.success('FAQ가 생성되었습니다.');
			}
			
			setModalVisible(false);
			setEditingFaq(null);
			form.resetFields();
			await fetchFaqs();
		} catch (err) {
			console.error('FAQ 저장 실패:', err);
			message.error(err.response?.data?.message || 'FAQ 저장에 실패했습니다.');
		}
	};

	const handleDelete = async (faqNo) => {
		try {
			await AdminService.deleteFaq(faqNo);
			message.success('FAQ가 삭제되었습니다.');
			await fetchFaqs();
		} catch (err) {
			console.error('FAQ 삭제 실패:', err);
			message.error(err.response?.data?.message || 'FAQ 삭제에 실패했습니다.');
		}
	};

	const columns = [
		{
			title: '번호',
			dataIndex: 'faqNo',
			key: 'faqNo',
			width: 80,
			align: 'center',
			sorter: (a, b) => a.faqNo - b.faqNo,
		},
		{
			title: '카테고리',
			dataIndex: 'faqCategory',
			key: 'faqCategory',
			width: 150,
			render: (category) => <Tag color="blue">{category}</Tag>,
			filters: faqCategories.map(cat => ({ text: cat.label, value: cat.value })),
			onFilter: (value, record) => record.faqCategory === value,
		},
		{
			title: '질문',
			dataIndex: 'faqQuestion',
			key: 'faqQuestion',
			ellipsis: true,
		},
		{
			title: '답변',
			dataIndex: 'faqAnswer',
			key: 'faqAnswer',
			ellipsis: true,
			render: (text) => text?.substring(0, 50) + (text?.length > 50 ? '...' : ''),
		},
		{
			title: '작성자',
			dataIndex: 'adminName',
			key: 'adminName',
			width: 120,
		},
		{
			title: '작업',
			key: 'action',
			width: 150,
			render: (_, record) => (
				<Space>
					<Button
						type="link"
						icon={<EditOutlined />}
						onClick={() => handleOpenModal(record)}
					>
						수정
					</Button>
					<Popconfirm
						title="FAQ 삭제"
						description="이 FAQ를 삭제하시겠습니까?"
						onConfirm={() => handleDelete(record.faqNo)}
						okText="삭제"
						cancelText="취소"
					>
						<Button
							type="link"
							danger
							icon={<DeleteOutlined />}
						>
							삭제
						</Button>
					</Popconfirm>
				</Space>
			),
		},
	];

	return (
		<div>
			<Card
				title="FAQ 관리"
				extra={
					<Space>
						<Button icon={<ReloadOutlined />} onClick={fetchFaqs} loading={loading}>
							새로고침
						</Button>
						<Button type="primary" icon={<PlusOutlined />} onClick={() => handleOpenModal(null)}>
							FAQ 추가
						</Button>
					</Space>
				}
			>
				<Table
					columns={columns}
					dataSource={faqs}
					rowKey="faqNo"
					loading={loading}
					pagination={{
						pageSize: 10,
						showSizeChanger: true,
						showTotal: (total) => `총 ${total}개`,
					}}
					size="small"
				/>
			</Card>

			<Modal
				title={editingFaq ? 'FAQ 수정' : 'FAQ 추가'}
				open={modalVisible}
				onOk={handleSave}
				onCancel={() => {
					setModalVisible(false);
					setEditingFaq(null);
					form.resetFields();
				}}
				width={700}
				okText="저장"
				cancelText="취소"
			>
				<Form
					form={form}
					layout="vertical"
				>
					<Form.Item
						name="faqCategory"
						label="카테고리"
						rules={[{ required: true, message: '카테고리를 선택해주세요.' }]}
					>
						<Select placeholder="카테고리를 선택하세요">
							{faqCategories.map(cat => (
								<Option key={cat.value} value={cat.value}>{cat.label}</Option>
							))}
						</Select>
					</Form.Item>

					<Form.Item
						name="faqQuestion"
						label="질문"
						rules={[{ required: true, message: '질문을 입력해주세요.' }]}
					>
						<Input placeholder="FAQ 질문을 입력하세요" />
					</Form.Item>

					<Form.Item
						name="faqAnswer"
						label="답변"
						rules={[{ required: true, message: '답변을 입력해주세요.' }]}
					>
						<TextArea 
							rows={6} 
							placeholder="FAQ 답변을 입력하세요"
							showCount
							maxLength={1000}
						/>
					</Form.Item>
				</Form>
			</Modal>
		</div>
	);
};

export default FaqManagement;
