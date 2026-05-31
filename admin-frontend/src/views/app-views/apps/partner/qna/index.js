import React, { useState, useEffect } from 'react';
import {
	Card,
	Table,
	Button,
	Modal,
	Input,
	message,
	Tag,
	Descriptions,
	Typography,
} from 'antd';
import { ReloadOutlined, MessageOutlined, RobotOutlined } from '@ant-design/icons';
import QnaService from 'services/QnaService';
import { getCategoryLabel } from 'constants/inquiryCategories';

const { TextArea } = Input;
const { Text } = Typography;

const statusColor = { PENDING: 'orange', ANSWERED: 'green' };

const PartnerQna = () => {
	const [loading, setLoading] = useState(false);
	const [qnas, setQnas] = useState([]);
	const [detailVisible, setDetailVisible] = useState(false);
	const [detail, setDetail] = useState(null);
	const [replyBody, setReplyBody] = useState('');
	const [replyLoading, setReplyLoading] = useState(false);
	const [draftLoading, setDraftLoading] = useState(false);
	const [referencedFaqs, setReferencedFaqs] = useState([]);

	const fetchList = async () => {
		try {
			setLoading(true);
			const res = await QnaService.getPartnerQnaList();
			const data = res.data || res;
			setQnas(Array.isArray(data) ? data : []);
		} catch (err) {
			message.error(err.response?.data?.message || 'QnA 목록 조회에 실패했습니다.');
			setQnas([]);
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		fetchList();
	}, []);

	const openDetail = async (qnaNo) => {
		try {
			setLoading(true);
			const res = await QnaService.getPartnerQnaDetail(qnaNo);
			setDetail(res.data || res);
			setReplyBody('');
			setReferencedFaqs([]);
			setDetailVisible(true);
		} catch (err) {
			message.error(err.response?.data?.message || '상세 조회에 실패했습니다.');
		} finally {
			setLoading(false);
		}
	};

	const handleDraftAssist = async () => {
		if (!detail?.qnaNo) return;
		try {
			setDraftLoading(true);
			const res = await QnaService.draftAssistPartnerQna(detail.qnaNo);
			const data = res.data || res;
			if (data.draftBody) {
				setReplyBody(data.draftBody);
			}
			setReferencedFaqs(data.referencedFaqs || []);
			if (data.confidence === 'FALLBACK') {
				message.info('AI 초안을 생성할 수 없어 관련 FAQ를 안내합니다.');
			} else {
				message.success('AI 답변 초안이 생성되었습니다.');
			}
		} catch (err) {
			message.error(err.response?.data?.message || 'AI 답변 초안 생성에 실패했습니다.');
		} finally {
			setDraftLoading(false);
		}
	};

	const handleReply = async () => {
		if (!detail?.qnaNo || !replyBody.trim()) return;
		try {
			setReplyLoading(true);
			await QnaService.replyPartnerQna(detail.qnaNo, replyBody.trim());
			message.success('답변이 등록되었습니다.');
			setDetailVisible(false);
			await fetchList();
		} catch (err) {
			message.error(err.response?.data?.message || '답변 등록에 실패했습니다.');
		} finally {
			setReplyLoading(false);
		}
	};

	const columns = [
		{ title: '번호', dataIndex: 'qnaNo', width: 80 },
		{
			title: '카테고리',
			dataIndex: 'category',
			width: 130,
			render: (c) => <Tag>{getCategoryLabel(c)}</Tag>,
		},
		{
			title: '상태',
			dataIndex: 'status',
			width: 100,
			render: (s, r) => <Tag color={statusColor[s]}>{r.statusLabel || s}</Tag>,
		},
		{ title: '제목', dataIndex: 'title', ellipsis: true },
		{ title: '고객', dataIndex: 'customerName', width: 100 },
		{ title: '상품', dataIndex: 'productName', width: 140, ellipsis: true },
		{
			title: '',
			key: 'action',
			width: 90,
			render: (_, r) => (
				<Button type="link" icon={<MessageOutlined />} onClick={() => openDetail(r.qnaNo)}>
					상세
				</Button>
			),
		},
	];

	const canReply = detail?.status === 'PENDING';

	return (
		<Card
			title="QnA (판매자 문의)"
			extra={
				<Button icon={<ReloadOutlined />} onClick={fetchList}>
					새로고침
				</Button>
			}
		>
			<Table rowKey="qnaNo" loading={loading} columns={columns} dataSource={qnas} pagination={{ pageSize: 15 }} />

			<Modal
				title={`QnA #${detail?.qnaNo || ''}`}
				open={detailVisible}
				onCancel={() => setDetailVisible(false)}
				width={720}
				footer={
					canReply
						? [
								<Button onClick={() => setDetailVisible(false)}>닫기</Button>,
								<Button type="primary" loading={replyLoading} onClick={handleReply}>
									답변 등록
								</Button>,
						  ]
						: [<Button onClick={() => setDetailVisible(false)}>닫기</Button>]
				}
			>
				{detail && (
					<>
						<Descriptions column={2} size="small" bordered className="mb-3">
							<Descriptions.Item label="카테고리">{detail.categoryLabel}</Descriptions.Item>
							<Descriptions.Item label="상태">{detail.statusLabel}</Descriptions.Item>
							<Descriptions.Item label="고객">{detail.customerName}</Descriptions.Item>
							<Descriptions.Item label="상품">{detail.productName || '-'}</Descriptions.Item>
						</Descriptions>
						<Text strong>{detail.title}</Text>
						{(detail.messages || []).map((m) => (
							<div key={m.messageNo} className="mt-3 p-3 border rounded">
								<Text type="secondary">{m.authorName}</Text>
								<div className="mt-2" style={{ whiteSpace: 'pre-wrap' }}>
									{m.body}
								</div>
							</div>
						))}
					{canReply && (
						<div className="mt-4">
							<div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
								<Text strong>답변 작성</Text>
								<Button
									type="button"
									htmlType="button"
									icon={<RobotOutlined />}
									loading={draftLoading}
									onClick={handleDraftAssist}
									size="small"
								>
									AI 답변 초안
								</Button>
							</div>
							<TextArea
								rows={4}
								value={replyBody}
								onChange={(e) => setReplyBody(e.target.value)}
								placeholder="답변 내용"
							/>
							{referencedFaqs.length > 0 && (
								<div className="mt-2 p-2" style={{ background: '#f6f8fa', borderRadius: 4 }}>
									<Text type="secondary" style={{ fontSize: 12 }}>참고 FAQ:</Text>
									{referencedFaqs.map((faq) => (
										<div key={faq.faqNo} style={{ fontSize: 12, marginTop: 2 }}>
											• {faq.question}
										</div>
									))}
								</div>
							)}
						</div>
					)}
					</>
				)}
			</Modal>
		</Card>
	);
};

export default PartnerQna;
