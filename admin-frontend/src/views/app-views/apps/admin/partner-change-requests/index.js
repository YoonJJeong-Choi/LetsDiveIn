import React, { useEffect, useState } from 'react';
import { Card, Table, Space, Tag, Button, Select, message, Modal, Input, Descriptions } from 'antd';
import AdminService from 'services/AdminService';

const { Option } = Select;

const PartnerChangeRequests = () => {
	const [loading, setLoading] = useState(false);
	const [statusFilter, setStatusFilter] = useState('PENDING');
	const [data, setData] = useState([]);
	const [rejectModal, setRejectModal] = useState({ open: false, requestId: null, reason: '' });
	const [detailModal, setDetailModal] = useState({ open: false, record: null });

	const load = async () => {
		try {
			setLoading(true);
			const res = await AdminService.getPartnerChangeRequests(
				statusFilter ? { status: statusFilter } : {}
			);
			const list = res?.data || res;
			setData(Array.isArray(list) ? list : []);
		} catch (e) {
			message.error(e?.response?.data?.message || e?.message || '변경 신청 목록을 불러오지 못했습니다.');
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		load();
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [statusFilter]);

	const handleApprove = (record) => {
		Modal.confirm({
			title: '해당 변경 신청을 승인하시겠습니까?',
			okText: '승인',
			cancelText: '취소',
			onOk: async () => {
				try {
					await AdminService.approvePartnerChangeRequest(record.requestId);
					message.success('승인되었습니다.');
					load();
					setDetailModal({ open: false, record: null });
				} catch (e) {
					message.error(e?.response?.data?.message || e?.message || '승인에 실패했습니다.');
				}
			},
		});
	};

	const handleRejectOpen = (record) => {
		setDetailModal({ open: false, record: null });
		setRejectModal({ open: true, requestId: record.requestId, reason: '' });
	};

	const handleOpenDetail = (record) => {
		setDetailModal({ open: true, record });
	};

	const submitReject = async () => {
		try {
			if (!rejectModal.reason || rejectModal.reason.trim().length < 2) {
				message.error('거절 사유를 입력해주세요.');
				return;
			}
			await AdminService.rejectPartnerChangeRequest(rejectModal.requestId, rejectModal.reason.trim());
			message.success('거절 처리되었습니다.');
			setRejectModal({ open: false, requestId: null, reason: '' });
			load();
		} catch (e) {
			message.error(e?.response?.data?.message || e?.message || '거절에 실패했습니다.');
		}
	};

	const columns = [
		{
			title: '요청ID',
			dataIndex: 'requestId',
			key: 'requestId',
			width: 110,
		},
		{
			title: '파트너',
			key: 'partner',
			width: 240,
			render: (_, r) => (
				<div style={{ lineHeight: 1.3 }}>
					<div style={{ fontWeight: 600 }}>{r.partnerName || '-'}</div>
					<div style={{ color: '#999', fontSize: 12 }}>ID: {r.partnerId}</div>
				</div>
			),
		},
		{
			title: '요청일',
			dataIndex: 'createdAt',
			key: 'createdAt',
			width: 180,
			render: (v) => (v ? new Date(v).toLocaleString('ko-KR') : '-'),
		},
		{
			title: '상태',
			dataIndex: 'status',
			key: 'status',
			width: 120,
			render: (s, r) => {
				const statusTag =
					s === 'APPROVED' ? (
						<Tag color="green">승인</Tag>
					) : s === 'REJECTED' ? (
						<Tag color="red">거절</Tag>
					) : (
						<Tag color="orange">대기</Tag>
					);

				return (
					<Space size={8}>
						{statusTag}
						<Button size="small" onClick={() => handleOpenDetail(r)}>
							상세보기
						</Button>
					</Space>
				);
			},
		},
	];

	return (
		<Card
			title="파트너 정보 변경 승인"
			extra={
				<Space>
					<Select
						value={statusFilter}
						style={{ width: 160 }}
						onChange={setStatusFilter}
						allowClear
						placeholder="상태 필터"
					>
						<Option value="PENDING">대기</Option>
						<Option value="APPROVED">승인</Option>
						<Option value="REJECTED">거절</Option>
					</Select>
					<Button onClick={load}>새로고침</Button>
				</Space>
			}
		>
			<Table
				rowKey="requestId"
				loading={loading}
				columns={columns}
				dataSource={data}
				pagination={{ pageSize: 10 }}
			/>

			<Modal
				title="변경 신청 상세"
				open={detailModal.open}
				onCancel={() => setDetailModal({ open: false, record: null })}
				footer={
					<Space>
						<Button
							type="primary"
							disabled={!detailModal.record || detailModal.record.status !== 'PENDING'}
							onClick={() => detailModal.record && handleApprove(detailModal.record)}
						>
							승인
						</Button>
						<Button
							danger
							disabled={!detailModal.record || detailModal.record.status !== 'PENDING'}
							onClick={() => detailModal.record && handleRejectOpen(detailModal.record)}
						>
							거절
						</Button>
					</Space>
				}
			>
				{detailModal.record ? (
					<Descriptions bordered size="small" column={1}>
						<Descriptions.Item label="요청ID">{detailModal.record.requestId}</Descriptions.Item>
						<Descriptions.Item label="요청일">
							{detailModal.record.createdAt ? new Date(detailModal.record.createdAt).toLocaleString('ko-KR') : '-'}
						</Descriptions.Item>
						<Descriptions.Item label="상태">
							{detailModal.record.status === 'APPROVED' && <Tag color="green">승인</Tag>}
							{detailModal.record.status === 'REJECTED' && <Tag color="red">거절</Tag>}
							{detailModal.record.status === 'PENDING' && <Tag color="orange">대기</Tag>}
						</Descriptions.Item>
						<Descriptions.Item label="파트너">
							<div>
								<div style={{ fontWeight: 600 }}>{detailModal.record.partnerName || '-'}</div>
								<div style={{ color: '#999', fontSize: 12 }}>ID: {detailModal.record.partnerId}</div>
							</div>
						</Descriptions.Item>

						<Descriptions.Item label="변경 상호명">
							{detailModal.record.requestedPartnerName || '-'}
						</Descriptions.Item>
						<Descriptions.Item label="변경 정산계좌">
							{detailModal.record.requestedPartnerBankAccount || '-'}
						</Descriptions.Item>
						<Descriptions.Item label="변경 사업자등록번호">
							{detailModal.record.requestedBusinessRegistrationNumber || '-'}
						</Descriptions.Item>
						<Descriptions.Item label="변경 브랜드 코드 (필터용)">
							{detailModal.record.requestedRepresentativeBrandCode || '-'}
						</Descriptions.Item>

						<Descriptions.Item label="신청 사유">{detailModal.record.requestReason || '-'}</Descriptions.Item>
						<Descriptions.Item label="거절 사유">{detailModal.record.rejectReason || '-'}</Descriptions.Item>
					</Descriptions>
				) : (
					<div>-</div>
				)}
			</Modal>

			<Modal
				title="변경 신청 거절"
				open={rejectModal.open}
				onOk={submitReject}
				onCancel={() => setRejectModal({ open: false, requestId: null, reason: '' })}
				okText="거절"
				cancelText="취소"
			>
				<Input.TextArea
					rows={4}
					placeholder='거절 사유를 입력하세요. 예) 증빙서류 불충분'
					value={rejectModal.reason}
					onChange={(e) => setRejectModal({ ...rejectModal, reason: e.target.value })}
					maxLength={500}
					showCount
				/>
			</Modal>
		</Card>
	);
};

export default PartnerChangeRequests;

