import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, Input, Form, message, Tag, Space, Row, Col, Alert, Statistic, Descriptions, Spin, Select } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined, ReloadOutlined, InfoCircleOutlined, ShopOutlined } from '@ant-design/icons';
import { useDispatch, useSelector } from 'react-redux';
import { fetchAllPartners, approvePartner, rejectPartner } from 'store/slices/adminSlice';

const { TextArea } = Input;
const { Option } = Select;

const PartnerApproval = () => {
	const dispatch = useDispatch();
	const { allPartners, loading, error } = useSelector((state) => state.admin);
	const [approveModalVisible, setApproveModalVisible] = useState(false);
	const [rejectModalVisible, setRejectModalVisible] = useState(false);
	const [rejectSubmitting, setRejectSubmitting] = useState(false);
	const [selectedPartner, setSelectedPartner] = useState(null);
	const [statusFilter, setStatusFilter] = useState('PENDING');
	const [rejectForm] = Form.useForm();

	useEffect(() => {
		fetchPartners();
	}, [statusFilter]);

	const fetchPartners = async () => {
		try {
			await dispatch(fetchAllPartners({ status: statusFilter === 'ALL' ? null : statusFilter })).unwrap();
		} catch (err) {
			console.error('파트너 목록 조회 실패:', err);
		}
	};

	const handleApprove = (partner) => {
		setSelectedPartner(partner);
		Modal.confirm({
			title: '파트너 승인',
			content: (
				<div>
					<p>다음 파트너를 승인하시겠습니까?</p>
					<Descriptions column={1} size="small" bordered>
						<Descriptions.Item label="파트너명">{partner.partnerName}</Descriptions.Item>
						<Descriptions.Item label="이메일">{partner.email}</Descriptions.Item>
						<Descriptions.Item label="연락처">{partner.partnerContact}</Descriptions.Item>
						<Descriptions.Item label="사업자등록번호">{partner.businessRegistrationNumber}</Descriptions.Item>
						<Descriptions.Item label="정산계좌">{partner.partnerBankAccount}</Descriptions.Item>
					</Descriptions>
					<p style={{ marginTop: '16px', color: '#ff4d4f' }}>
						승인 시 임시 비밀번호가 이메일로 발송됩니다.
					</p>
				</div>
			),
			okText: '승인',
			okType: 'primary',
			cancelText: '취소',
			onOk: async () => {
				try {
					await dispatch(approvePartner(partner.partnerId)).unwrap();
					message.success('파트너가 성공적으로 승인되었습니다.');
					fetchPartners();
				} catch (err) {
					message.error(err || '파트너 승인에 실패했습니다.');
				}
			}
		});
	};

	const handleReject = (partner) => {
		setSelectedPartner(partner);
		setRejectModalVisible(true);
		rejectForm.resetFields();
	};

	const handleRejectSubmit = async (values) => {
		if (rejectSubmitting) return;
		if (!values.rejectionReason || !values.rejectionReason.trim()) {
			message.error('거절 사유를 입력해주세요.');
			return;
		}

		Modal.confirm({
			title: '거절 처리하시겠습니까?',
			content: '해당 입점 신청은 거절 상태로 표시되며, 신청자에게 안내 메일이 발송됩니다.',
			okText: '거절',
			okButtonProps: { danger: true, loading: rejectSubmitting },
			cancelText: '취소',
			onOk: async () => {
				try {
					setRejectSubmitting(true);
					await dispatch(rejectPartner({
						partnerId: selectedPartner.partnerId,
						rejectionReason: values.rejectionReason.trim()
					})).unwrap();
					message.success('파트너가 성공적으로 거절되었습니다.');
					setRejectModalVisible(false);
					setSelectedPartner(null);
					rejectForm.resetFields();
					fetchPartners();
				} catch (err) {
					message.error(err || '파트너 거절에 실패했습니다.');
				} finally {
					setRejectSubmitting(false);
				}
			}
		});
	};

	const tableColumns = [
		{
			title: '파트너 ID',
			dataIndex: 'partnerId',
			key: 'partnerId',
			width: 100,
		},
		{
			title: '파트너명',
			dataIndex: 'partnerName',
			key: 'partnerName',
			render: (text) => (
				<div>
					<ShopOutlined style={{ marginRight: 8 }} />
					<strong>{text}</strong>
				</div>
			),
		},
		{
			title: '이메일',
			dataIndex: 'email',
			key: 'email',
		},
		{
			title: '상태',
			dataIndex: 'partnerStatus',
			key: 'partnerStatus',
			width: 100,
			render: (status) => {
				if (status === 'APPROVED') return <Tag color="green">승인</Tag>;
				if (status === 'REJECTED') return <Tag color="red">거절</Tag>;
				if (status === 'INACTIVE') return <Tag color="default">비활성</Tag>;
				return <Tag color="orange">입점 신청</Tag>;
			},
		},
		{
			title: '작업',
			key: 'action',
			width: 200,
			render: (_, record) => (
				<Space>
					<Button
						type="primary"
						icon={<CheckCircleOutlined />}
						onClick={() => handleApprove(record)}
						size="small"
						disabled={record.partnerStatus !== 'PENDING'}
					>
						승인
					</Button>
					<Button
						danger
						icon={<CloseCircleOutlined />}
						onClick={() => handleReject(record)}
						size="small"
						disabled={record.partnerStatus !== 'PENDING'}
					>
						거절
					</Button>
				</Space>
			),
		},
	];

	return (
		<>
			<Card title="파트너 입점 승인">
				<Row gutter={16} style={{ marginBottom: 24 }}>
					<Col span={8}>
						<Statistic
							title="조회 결과"
							value={allPartners.length}
							prefix={<InfoCircleOutlined />}
							valueStyle={{ color: '#1890ff' }}
						/>
					</Col>
				</Row>

				<Space style={{ marginBottom: 16 }}>
					<Select value={statusFilter} style={{ width: 180 }} onChange={setStatusFilter}>
						<Option value="ALL">전체</Option>
						<Option value="PENDING">입점 신청</Option>
						<Option value="APPROVED">승인</Option>
						<Option value="REJECTED">거절</Option>
						<Option value="INACTIVE">비활성</Option>
					</Select>
					<Button
						icon={<ReloadOutlined />}
						onClick={fetchPartners}
						loading={loading}
					>
						새로고침
					</Button>
				</Space>

				{error && (
					<Alert
						message="오류"
						description={error}
						type="error"
						showIcon
						className="mb-3"
						closable
					/>
				)}

				<Spin spinning={loading}>
					<Table
						columns={tableColumns}
						dataSource={allPartners}
						rowKey="partnerId"
						pagination={{
							pageSize: 10,
							showSizeChanger: true,
							showTotal: (total) => `총 ${total}개 신청`,
						}}
						locale={{
							emptyText: '조회된 파트너가 없습니다.',
						}}
						expandable={{
							expandedRowRender: (record) => (
								<div style={{ padding: '16px', background: '#fafafa' }}>
									<Descriptions title="파트너 입점 신청 상세 정보" column={2} bordered size="small">
										<Descriptions.Item label="파트너 ID" span={1}>
											{record.partnerId}
										</Descriptions.Item>
										<Descriptions.Item label="상태" span={1}>
											<Tag color="orange">입점 신청</Tag>
										</Descriptions.Item>
										<Descriptions.Item label="파트너명" span={1}>
											{record.partnerName}
										</Descriptions.Item>
										<Descriptions.Item label="이메일" span={1}>
											{record.email}
										</Descriptions.Item>
										<Descriptions.Item label="연락처" span={1}>
											{record.partnerContact}
										</Descriptions.Item>
										<Descriptions.Item label="사업자등록번호" span={1}>
											{record.businessRegistrationNumber}
										</Descriptions.Item>
										<Descriptions.Item label="정산계좌" span={2}>
											{record.partnerBankAccount}
										</Descriptions.Item>
										{record.rejectionReason && (
											<Descriptions.Item label="거절 사유" span={2}>
												{record.rejectionReason}
											</Descriptions.Item>
										)}
										<Descriptions.Item label="사업자등록증 사본" span={1}>
											{record.businessRegistrationFileId ? (
												<Button size="small" onClick={() => import('services/AdminService').then(m => m.default ? m.default.downloadFile(record.businessRegistrationFileId) : m.downloadFile(record.businessRegistrationFileId))}>
													다운로드
												</Button>
											) : '-'}
										</Descriptions.Item>
										<Descriptions.Item label="통장 사본" span={1}>
											{record.bankAccountFileId ? (
												<Button size="small" onClick={() => import('services/AdminService').then(m => m.default ? m.default.downloadFile(record.bankAccountFileId) : m.downloadFile(record.bankAccountFileId))}>
													다운로드
												</Button>
											) : '-'}
										</Descriptions.Item>
									</Descriptions>
								</div>
							),
						}}
					/>
				</Spin>
			</Card>

			{/* 거절 모달 */}
			<Modal
				title="파트너 거절"
				open={rejectModalVisible}
				onCancel={() => {
					setRejectModalVisible(false);
					setSelectedPartner(null);
					rejectForm.resetFields();
				}}
				onOk={() => { if (!rejectSubmitting) rejectForm.submit() }}
				okText="거절"
				okButtonProps={{ danger: true, disabled: rejectSubmitting, loading: rejectSubmitting }}
				cancelText="취소"
			>
				{selectedPartner && (
					<div style={{ marginBottom: 16 }}>
						<p>다음 파트너를 거절하시겠습니까?</p>
						<Descriptions column={2} size="small" bordered>
							<Descriptions.Item label="파트너명">{selectedPartner.partnerName}</Descriptions.Item>
							<Descriptions.Item label="이메일">{selectedPartner.email}</Descriptions.Item>
							<Descriptions.Item label="연락처">{selectedPartner.partnerContact}</Descriptions.Item>
							<Descriptions.Item label="사업자등록번호">{selectedPartner.businessRegistrationNumber}</Descriptions.Item>
							<Descriptions.Item label="정산계좌" span={2}>{selectedPartner.partnerBankAccount}</Descriptions.Item>
						</Descriptions>
					</div>
				)}
				<Form
					form={rejectForm}
					onFinish={handleRejectSubmit}
					layout="vertical"
				>
					<Form.Item
						name="rejectionReason"
						label="거절 사유"
						rules={[
							{ required: true, message: '거절 사유를 입력해주세요.' },
							{ min: 10, message: '거절 사유는 최소 10자 이상 입력해주세요.' }
						]}
					>
						<TextArea
							rows={4}
							placeholder="거절 사유를 입력해주세요. (최소 10자 이상)"
							maxLength={500}
							showCount
						/>
					</Form.Item>
				</Form>
			</Modal>
		</>
	);
};

export default PartnerApproval;
